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

package org.eclipse.codewind.filewatchers.core;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Simple singleton logger with 4 log levels, and ability to print the
 * class/method/line # of method calling the log statement.
 * 
 * <pre>
 * Log levels: 
 * - DEBUG: Fine-grained, noisy, excessively detailed, and mostly irrelevant messages.  
 * - INFO: Coarse-grained messages related to the high-level inner workings of the code.
 * - ERROR: Errors which are bad, but not entirely unexpected, such as errors I/O errors when running on a flaky network connection. 
 * - SEVERE: Unexpected errors that strongly suggest a client/server implementation bug or a serious client/server runtime issue.
 * </pre>
 * 
 */
public class FWLogger {

	private static final FWLogger instance = new FWLogger();

	private final LogLevel level;

	private final LogLevel DEFAULT_LOG_LEVEL = LogLevel.INFO;

	private final SimpleDateFormat PRETTY_DATE_FORMAT = new SimpleDateFormat("MMM d h:mm:ss.SSS a");

	/**
	 * Whether to use reflection to print the caller of the logging methods, as part
	 * of the outputted log statement.
	 */
	private static final boolean printCallingMethods = false;

	private enum LogLevel {
		DEBUG, INFO, ERROR, SEVERE
	};

	private final long startTimeInNanos = System.nanoTime();

	/** Synchronize on lock when accessing */
	private RollingFileLogger fileLogger_synch_lock = null;

	private boolean outputLogsToScreen = true;

	private final Object lock = new Object();

	private FWLogger() {

		// Look for 'filewatcher_log_level' environment variable with any case.
		LogLevel newLogLevel = DEFAULT_LOG_LEVEL;
		outer: for (Map.Entry<String, String> e : System.getenv().entrySet()) {

			if (e.getKey().equalsIgnoreCase("filewatcher_log_level")) {

				for (LogLevel en : LogLevel.values()) {
					if (en.name().equalsIgnoreCase(e.getValue())) {
						newLogLevel = en;
						break outer;
					}

				}

			}

		}

		level = newLogLevel;

		out("Logging at log level: " + level.name());

	}

	public static FWLogger getInstance() {
		return instance;
	}

	public void setOutputLogsToScreen(boolean outputLogsToSecreen) {
		this.outputLogsToScreen = outputLogsToSecreen;
	}

	public void setRollingFileLoggerOutputDir(File outputDir) {
		synchronized (lock) {
			if (fileLogger_synch_lock == null) {
				fileLogger_synch_lock = new RollingFileLogger(outputDir, this);
				fileLogger_synch_lock.start();
			}
		}
	}

	private final String time() {
		long time = TimeUnit.MILLISECONDS.convert(System.nanoTime() - startTimeInNanos, TimeUnit.NANOSECONDS);

		long seconds = time / 1000;

		long msecs = time % 1000;

		String msecsStr = Long.toString(msecs);

		while (msecsStr.length() < 3) {
			msecsStr = "0" + msecsStr;
		}

		return PRETTY_DATE_FORMAT.format(new Date()) + " [" + seconds + "." + msecsStr + "]";

	}

	private void out(String str) {
		String callingMethod = printCallingMethods ? "   " + getCallingMethod(FWLogger.class) : "";

		String output = time() + " " + str + callingMethod;

		if (outputLogsToScreen) {
			System.out.println(output);
		}

		synchronized (lock) {
			if (fileLogger_synch_lock != null) {
				fileLogger_synch_lock.addOut(output);
			}
		}
	}

	private void err(String str) {
		String callingMethod = printCallingMethods ? "   " + getCallingMethod(FWLogger.class) : "";
		String output = time() + " " + str + callingMethod;

		if (outputLogsToScreen) {
			System.err.println(output);
		}

		synchronized (lock) {
			if (fileLogger_synch_lock != null) {
				fileLogger_synch_lock.addErr(output);
			}
		}

	}

	public boolean isDebug() {
		return level == LogLevel.DEBUG;
	}

	public void logDebug(String msg) {
		if (level != LogLevel.DEBUG) {
			return;
		}
		out(msg);
	}

	public void logDebug(String msg, String projectId) {
		if (level != LogLevel.DEBUG) {
			return;
		}

		if (projectId != null) {
			out(msg + " [project-id:" + projectId + "]");
		} else {
			out(msg);
		}
	}

	public void logInfo(String msg) {
		if (level == LogLevel.ERROR || level == LogLevel.SEVERE) {
			return;
		}
		out(msg);
	}

	public void logInfo(String msg, String projectId) {
		if (level == LogLevel.ERROR || level == LogLevel.SEVERE) {
			return;
		}

		if (projectId != null) {
			out(msg + " [project-id:" + projectId + "]");

		} else {
			out(msg);
		}

	}

	public void logError(String msg) {
		this.logError(msg, null);
	}

	public void logError(String msg, Throwable t) {
		if (level == LogLevel.SEVERE) {
			return;
		}

		String outputMsg = "! ERROR ! " + msg;

		if (t != null) {
			outputMsg += " " + FilewatcherUtils.convertStackTraceToString(t);
		}

		err(outputMsg);
	}

	public void logSevere(String msg) {
		this.logSevere(msg, null, null);
	}

	public void logSevere(String msg, Throwable t, String projectId) {

		String outputMsg = "!!! SEVERE !!! " + msg;

		if (t != null) {
			outputMsg += " " + FilewatcherUtils.convertStackTraceToString(t);
		}

		err(outputMsg);

	}

	/**
	 * Return the name of the class and method that is calling the logging method.
	 */
	private static String getCallingMethod(Class<?> loggerClass) {
		String loggerClassName = loggerClass.getName();
		try {
			StackTraceElement[] steList = Thread.currentThread().getStackTrace();

			// Locate the index of the last STE that contains loggerClass
			int lastIndexOfLoggerClass = -1;
			for (int x = 0; x < steList.length; x++) {
				StackTraceElement ste = steList[x];

				if (ste.getClassName() != null && ste.getClassName().equals(loggerClassName)) {
					lastIndexOfLoggerClass = x;
				}
			}

			// Return the class directly _after_ loggerClass in the stack
			if (lastIndexOfLoggerClass != -1 && lastIndexOfLoggerClass + 1 < steList.length) {
				StackTraceElement result = steList[lastIndexOfLoggerClass + 1];

				String resultClassName = result.getClassName();
				int index = resultClassName.lastIndexOf(".");
				if (index != -1) {
					resultClassName = resultClassName.substring(index + 1);
				}

				return "[" + resultClassName + "." + result.getMethodName() + ":" + result.getLineNumber() + "] ";
			}

		} catch (Exception e) {
			/* ignore, so that we don't break any calling methods. */
		}

		return "";
	}

	/**
	 * A file-based logger that maintains at most only the last (2 *
	 * MAX_LOG_FILE_SIZE)-1 bytes, in at most 2 log files. Log files are stored in
	 * the given directory.
	 * 
	 * We log to the file system on a separate thread from the log-calling thread,
	 * so as to reduce application latency due to file I/O.
	 * 
	 * At most 2 log files will exist at any one time: n-1, n
	 */
	private static class RollingFileLogger extends Thread {
		private final File logDir;

		private List<FileLoggerEntry> entries_synch_lock = new ArrayList<>();

		private final Object lock = new Object();

		private final String FILE_PREFIX = "filewatcherd-";
		private final String FILE_SUFFIX = ".log";

		private final long MAX_LOG_FILE_SIZE = 1024 * 1024 * 12;

		private final FWLogger parent;

		public RollingFileLogger(File logDir, FWLogger parent) {
			setName(this.getClass().getName());
			setDaemon(true);
			this.logDir = logDir;
			this.parent = parent;
		}

		@Override
		public void run() {

			addOut("Logging at log level: " + parent.level.name());

			final String EOL = System.lineSeparator();

			// Wait for the log directory to exist if it doesn't yet (clear any entries to
			// prevent memory leak)
			while (!logDir.exists()) {
				FilewatcherUtils.sleepIgnoreInterrupt(1000);
				synchronized (lock) {
					entries_synch_lock.clear();
				}
			}

			// Erase old fw log files
			Arrays.asList(logDir.listFiles()).stream()
					.filter(e -> e.getName().startsWith(FILE_PREFIX) && e.getName().endsWith(FILE_SUFFIX))
					.forEach(e -> {
						if (!e.delete()) {
							System.err.println("Unable to delete old log file: " + e.getPath());
						}
					});

			// Log file #, beginning at 1
			int currNumber = 0;

			FileWriter fw = null;
			long charsLogged = 0; // Number of characters (bytes, in most cases) logged to current log file

			List<FileLoggerEntry> entries = new ArrayList<>();
			while (true) {

				if (fw == null) {
					try {
						currNumber++;
						charsLogged = 0;

						// Delete log file n-2, leaving n-1, and n.
						File toDelete = new File(logDir, FILE_PREFIX + (currNumber - 2) + FILE_SUFFIX);
						if (toDelete.exists()) {
							toDelete.delete();
						}

						fw = new FileWriter(new File(logDir, FILE_PREFIX + currNumber + FILE_SUFFIX));
					} catch (IOException e1) {
						fw = null;
						/* ignore */
					}
				}

				synchronized (lock) {
					try {
						lock.wait();

						entries.addAll(entries_synch_lock);
						entries_synch_lock.clear();

					} catch (InterruptedException e) {
						FilewatcherUtils.sleepIgnoreInterrupt(100);
					}

				}
				if (entries.size() > 0) {
					try {

						for (FileLoggerEntry e : entries) {
							fw.write(e.msg + EOL);
							charsLogged += e.msg.length();
						}

						fw.flush();

						if (charsLogged > MAX_LOG_FILE_SIZE) {
							fw.close();
							fw = null;
						}
					} catch (IOException e1) {
						/* ignore */
					}

					entries.clear();

				}

			}
		}

		public void addOut(String msg) {
			synchronized (lock) {
				entries_synch_lock.add(new FileLoggerEntry(FileLoggerEntry.Type.OUT, msg));
				lock.notify();
			}
		}

		public void addErr(String msg) {
			synchronized (lock) {
				entries_synch_lock.add(new FileLoggerEntry(FileLoggerEntry.Type.ERR, msg));
				lock.notify();
			}
		}

		/** The log entry value before it is logged to disk */
		private static class FileLoggerEntry {
			enum Type {
				OUT, ERR
			};

			private final String msg;
			private final Type type;

			public FileLoggerEntry(Type type, String msg) {
				this.msg = msg;
				this.type = type;
			}

		}
	}
}
