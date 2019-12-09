/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.codewind.filewatchers.core.internal;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.codewind.filewatchers.core.FWLogger;
import org.eclipse.codewind.filewatchers.core.FilewatcherUtils;

/* The purpose of this class is to call the cwctl project sync command, in order to allow the
 * Codewind CLI to detect and communicate file changes to the server.
 *
 * This class will ensure that only one instance of the cwctl project sync command is running
 * at a time, per project.
 *
 * For automated testing, if the `MOCK_CWCTL_INSTALLER_PATH` environment variable is specified, a mock cwctl command
 * written in Java (as a runnable JAR) can be used to test this class.
 */
public class CLIState {

	private final String projectId;

	/*
	 * Absolute time, in Unix epoch msecs, at which the last cwctl command was
	 * initiated.
	 */
	private long timestamp_synch_lock = 0;

	private final String installerPath;

	private final String projectPath;

	/** For automated testing only */
	private final String mockInstallerPath;

	private final FWLogger log = FWLogger.getInstance();

	private boolean processActive_synch_lock = false;
	private boolean requestWaiting_synch_lock = false;

	private final Object lock = new Object();

	public CLIState(String projectId, String installerPath, String projectPath) {

		if (installerPath == null) {
			throw new RuntimeException("Installer path is null: " + installerPath);
		}

		this.projectId = projectId;
		this.installerPath = installerPath;
		this.projectPath = projectPath;

		this.mockInstallerPath = System.getenv("MOCK_CWCTL_INSTALLER_PATH");
	}

	public void onFileChangeEvent(Long projectCreationTimeInAbsoluteMsecsParam /* nullable */) {

		if (this.projectPath == null || this.projectPath.trim().isEmpty()) {
			log.logSevere("Project path passed to CLIState is empty, so ignoring file change event.");
			return;
		}

		boolean callCLI = false;

		synchronized (lock) {
			// This, along with callCLI(), ensures that only one instance of `project sync`
			// is running at a time.
			if (processActive_synch_lock) {
				requestWaiting_synch_lock = true;
			} else {
				processActive_synch_lock = true;
				requestWaiting_synch_lock = false;
				callCLI = true;
			}
		}

		// Call CLI outside the lock
		if (callCLI) {

			Long debugOldTimestampValue = null;
			Long debugNewTimestampValue = null;
			boolean timestampUpdated = false;

			synchronized (lock) {
				// We only update the timestamp when 'callCLI' is true, because we don't want to
				// step on the toes of another running CLI process (and that one will probably
				// update the timestamp on it's own, with a more recent value, then ours)

				// Update the timestamp to the project creation value, but ONLY IF it is zero.
				if (projectCreationTimeInAbsoluteMsecsParam != null && this.timestamp_synch_lock == 0) {
					debugOldTimestampValue = timestamp_synch_lock;
					this.timestamp_synch_lock = projectCreationTimeInAbsoluteMsecsParam;
					debugNewTimestampValue = this.timestamp_synch_lock;
					timestampUpdated = true;
				}
			}

			if (timestampUpdated) {
				log.logInfo("Timestamp updated from " + debugOldTimestampValue + " to " + debugNewTimestampValue
						+ " from project creation time.");
			}

			FilewatcherUtils.newThread(() -> {
				callCLI();
			});
		}

	}

	private void callCLI() {

		// Sanity check the processActive field
		synchronized (lock) {
			if (!processActive_synch_lock) {
				log.logSevere(CLIState.class.getSimpleName()
						+ ".callCLI() was called while the processActive value was false. This should never happen.");
			}
		}

		final boolean DEBUG_FAKE_CMD_OUTPUT = false; // Enable this for debugging purposes.

		// This try block works in tandem with onFileChangeEvent() to ensure that only
		// one instance of the 'project sync' command is running at a time.
		try {

			RunProjectReturn result;

			if (DEBUG_FAKE_CMD_OUTPUT) {
				synchronized (lock) {
					log.logInfo("Faking a call to CLI with params " + timestamp_synch_lock + " " + projectId);
					result = new RunProjectReturn(0, "", System.currentTimeMillis());
				}

			} else {
				// Call CLI and wait for result
				result = runProjectCommand();
			}

			if (result != null) {
				if (result.errorCode != 0) {
					log.logSevere("Non-zero error code from installer: "
							+ (result != null && result.output != null ? result.output : ""));
				} else {
					synchronized (lock) {
						// Success, so update the tiemstamp to the process start time.
						this.timestamp_synch_lock = result.spawnTime;
						log.logInfo("Updating timestamp to latest: " + timestamp_synch_lock);

					}
				}
			}

		} catch (Throwable e) {
			// Log, handle, then bury the exception
			log.logSevere("Unexpected exception from CLI", e, projectId);
		}

		boolean requestWaiting = false;
		synchronized (lock) {
			this.processActive_synch_lock = false;

			// If another file change list occurred during the last invocation, then start
			// another one.
			requestWaiting = this.requestWaiting_synch_lock;

		}

		if (requestWaiting) {
			onFileChangeEvent(null);
		}

	}

	private RunProjectReturn runProjectCommand() throws IOException, InterruptedException {

		String currInstallerPath = this.installerPath;

		List<String> args = new ArrayList<>();

		long latestTimestamp;
		synchronized (lock) {
			latestTimestamp = timestamp_synch_lock;
		}

		if (this.mockInstallerPath == null || this.mockInstallerPath.trim().isEmpty()) {
			args.add(installerPath);

			// Example:
			// cwctl project sync -p
			// /Users/tobes/workspaces/git/eclipse/codewind/codewind-workspace/lib5 \
			// -i b1a78500-eaa5-11e9-b0c1-97c28a7e77c7 -t 12345
			args.addAll(Arrays.asList(new String[] { "project", "sync", "--insecure", "-p", projectPath, "-i",
					projectId, "-t", "" + latestTimestamp }));
		} else {

			args.add("java");

			args.addAll(Arrays.asList(new String[] { "-jar", this.mockInstallerPath, "-p", this.projectPath, "-i",
					this.projectId, "-t", "" + latestTimestamp }));

			currInstallerPath = mockInstallerPath;
		}

		String debugStr = args.stream().map(e -> "[" + e + "] ").reduce((a, b) -> a + b).get();

		log.logInfo("Calling cwctl project sync with: [" + this.projectId + "] { " + debugStr + "}");

		// Start process and wait for complete on this thread.

		String installerPwd = new File(currInstallerPath).getParent();

		// Time at which the new process was called.
		long spawnTime = System.currentTimeMillis();
		ProcessBuilder pb = new ProcessBuilder(args);
		pb.directory(new File(installerPwd));
		Process p = pb.start();

		ReadThread inputStreamThread = new ReadThread(p.getInputStream(), null);
		inputStreamThread.start();

		ReadThread errorStreamThread = new ReadThread(p.getErrorStream(), null);
		errorStreamThread.start();

		// Wait for complete.

		int result = p.waitFor();

		log.logInfo("Cwctl call completed, elapsed time of cwctl call: " + (System.currentTimeMillis() - spawnTime));

		String stdout = inputStreamThread.getOutput();
		String stderr = errorStreamThread.getOutput();

		// Log result.

		if (result != 0) {
			log.logError("Error running 'project sync' installer command");
			log.logError("Stdout: " + stdout);
			log.logError("Stderr:" + stderr);

			return new RunProjectReturn(result, stdout + stderr, spawnTime);

		} else {
			log.logInfo("Successfully ran installer command: " + debugStr);
			log.logInfo("Output:" + stdout + stderr); // TODO: Convert to DEBUG once everything matures.

			return new RunProjectReturn(result, stdout, spawnTime);

		}

	}

	/** Return value of runProjectCommand(). */
	private static class RunProjectReturn {
		int errorCode;
		String output;
		long spawnTime;

		public RunProjectReturn(int errorCode, String output, long spawnTime) {
			this.errorCode = errorCode;
			this.output = output;
			this.spawnTime = spawnTime;
		}

	}

	/**
	 * Read from an InputStream, append the result to a StringBuilder. Thread safe.
	 */
	private class ReadThread extends Thread {

		final InputStream is;
		final PrintStream ps;

		private final StringBuilder received_synch = new StringBuilder();

		@SuppressWarnings("unused")
		private boolean isFinished = false;

		public ReadThread(InputStream is, PrintStream ps) {
			this.is = is;
			this.ps = ps;
			setDaemon(true);
		}

		@Override
		public void run() {

			BufferedReader br = new BufferedReader(new InputStreamReader(is));

			String str;
			try {
				while (null != (str = br.readLine())) {

					if (ps != null) {
						ps.append(str + "\n");
					}

					synchronized (received_synch) {
						received_synch.append(str + "\n");
					}

				}
				isFinished = true;
			} catch (IOException e) {
				e.printStackTrace();
			}

		}

		public String getOutput() {
			synchronized (received_synch) {
				return received_synch.toString();

			}
		}
	}
}
