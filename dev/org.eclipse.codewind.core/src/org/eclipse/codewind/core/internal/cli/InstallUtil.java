/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *	 IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.codewind.core.internal.cli;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import org.eclipse.codewind.core.CodewindCorePlugin;
import org.eclipse.codewind.core.internal.CodewindManager;
import org.eclipse.codewind.core.internal.CodewindManager.InstallerStatus;
import org.eclipse.codewind.core.internal.Logger;
import org.eclipse.codewind.core.internal.ProcessHelper;
import org.eclipse.codewind.core.internal.ProcessHelper.ProcessResult;
import org.eclipse.codewind.core.internal.connection.CodewindConnection;
import org.eclipse.codewind.core.internal.connection.CodewindConnectionManager;
import org.eclipse.codewind.core.internal.messages.Messages;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.osgi.util.NLS;
import org.json.JSONException;
import org.json.JSONObject;

public class InstallUtil {
	
	public static final String STOP_APP_CONTAINERS_PREFSKEY = "stopAppContainers";
	public static final String STOP_APP_CONTAINERS_ALWAYS = "stopAppContainersAlways";
	public static final String STOP_APP_CONTAINERS_NEVER = "stopAppContainersNever";
	public static final String STOP_APP_CONTAINERS_PROMPT = "stopAppContainersPrompt";
	public static final String STOP_APP_CONTAINERS_DEFAULT = STOP_APP_CONTAINERS_PROMPT;
	
	public static final int INSTALL_TIMEOUT_DEFAULT = 300;
	public static final int UNINSTALL_TIMEOUT_DEFAULT = 60;
	public static final int START_TIMEOUT_DEFAULT = 60;
	public static final int STOP_TIMEOUT_DEFAULT = 300;
	
	private static final String[] INSTALL_CMD = new String[] {"install"};
	private static final String[] START_CMD = new String[] {"start"};
	private static final String[] STOP_CMD = new String[] {"stop"};
	private static final String[] STOP_ALL_CMD = new String[] {"stop-all"};
	private static final String[] STATUS_CMD = new String[] {"status"};
	private static final String[] REMOVE_CMD = new String[] {"remove", "local"};
	private static final String[] UPGRADE_CMD = new String[] {"upgrade"};
	
	public static final String DEFAULT_INSTALL_VERSION = "latest";
	
	private static final String TAG_OPTION = "-t";
	private static final String INSTALL_VERSION_VAR = "INSTALL_VERSION";
	private static final String CW_TAG_VAR = "CW_TAG";
	private static final String WORKSPACE_OPTION = "--workspace";
	
	private static String installVersion = null;
	private static String requestedVersion = null;
	
	public static InstallStatus getInstallStatus(IProgressMonitor monitor) throws IOException, JSONException, TimeoutException {
		SubMonitor mon = SubMonitor.convert(monitor, Messages.CodewindStatusJobLabel, 100);
		Process process = null;
		try {
			process = CLIUtil.runCWCTL(CLIUtil.GLOBAL_JSON, STATUS_CMD, null);
			ProcessResult result = ProcessHelper.waitForProcess(process, 500, 120, mon);
			CLIUtil.checkResult(STATUS_CMD, result, true);
			JSONObject status = new JSONObject(result.getOutput());
			return new InstallStatus(status);
		} finally {
			if (process != null && process.isAlive()) {
				process.destroy();
			}
		}
	}
	
	public static ProcessResult startCodewind(String version, IProgressMonitor monitor) throws IOException, TimeoutException {
		SubMonitor mon = SubMonitor.convert(monitor, Messages.StartCodewindJobLabel, 100);
		Process process = null;
		try {
			CodewindManager.getManager().setInstallerStatus(InstallerStatus.STARTING);
			process = CLIUtil.runCWCTL(null, START_CMD, new String[] {TAG_OPTION, version});
			ProcessResult result = ProcessHelper.waitForProcess(process, 500, getPrefs().getInt(CodewindCorePlugin.CW_START_TIMEOUT), mon.split(90));
			return result;
		} finally {
			if (process != null && process.isAlive()) {
				process.destroy();
			}
			CodewindManager.getManager().refreshInstallStatus(mon.isCanceled() ? new NullProgressMonitor() : mon.split(10));
			CodewindManager.getManager().setInstallerStatus(null);
		}
	}
	
	public static ProcessResult stopCodewind(boolean stopAll, IProgressMonitor monitor) throws IOException, TimeoutException {
		SubMonitor mon = SubMonitor.convert(monitor, Messages.StopCodewindJobLabel, 100);
		Process process = null;
		try {
			// Disconnect the local connection(s). If there's an exception,
			// log it and continue to stop Codewind.
			CodewindConnectionManager.activeConnections().stream()
				.filter(CodewindConnection::isLocal)
				.forEach(connection -> {
					try {
						connection.disconnect();
					} catch (Exception e) {
						Logger.logError("Error disconnecting " + connection.getName() + " connection", e);
					}
				});
			// Yield to give the connections the chance to close
			try {
				Thread.sleep(250);
			} catch (InterruptedException e) {
				// ignore
			}

			CodewindManager.getManager().setInstallerStatus(InstallerStatus.STOPPING);
			process = CLIUtil.runCWCTL(null, stopAll ? STOP_ALL_CMD : STOP_CMD, null);
			ProcessResult result = ProcessHelper.waitForProcess(process, 500, getPrefs().getInt(CodewindCorePlugin.CW_STOP_TIMEOUT),
					mon.split(95));
			return result;
		} finally {
			if (process != null && process.isAlive()) {
				process.destroy();
			}
			CodewindManager.getManager().refreshInstallStatus(mon.isCanceled() ? new NullProgressMonitor() : mon.split(5));
			CodewindManager.getManager().setInstallerStatus(null);
		}
	}
	
	public static ProcessResult installCodewind(String version, IProgressMonitor monitor) throws IOException, TimeoutException {
		SubMonitor mon = SubMonitor.convert(monitor, Messages.InstallCodewindJobLabel, 100);
		Process process = null;
		try {
			CodewindManager.getManager().setInstallerStatus(InstallerStatus.INSTALLING);
		    process = CLIUtil.runCWCTL(null, INSTALL_CMD, new String[] {TAG_OPTION, version});
		    ProcessResult result = ProcessHelper.waitForProcess(process, 1000, getPrefs().getInt(CodewindCorePlugin.CW_INSTALL_TIMEOUT), mon.split(95));
		    return result;
		} finally {
			if (process != null && process.isAlive()) {
				process.destroy();
			}
			CodewindManager.getManager().refreshInstallStatus(mon.isCanceled() ? new NullProgressMonitor() : mon.split(5));
			CodewindManager.getManager().setInstallerStatus(null);
		}
	}
	
	public static ProcessResult removeCodewind(String version, IProgressMonitor monitor) throws IOException, TimeoutException {
		SubMonitor mon = SubMonitor.convert(monitor, Messages.RemovingCodewindJobLabel, 100);
		Process process = null;
		try {
			CodewindManager.getManager().setInstallerStatus(InstallerStatus.UNINSTALLING);
			if (version != null) {
				process = CLIUtil.runCWCTL(null, REMOVE_CMD, new String[] {TAG_OPTION, version});
			} else {
				process = CLIUtil.runCWCTL(null, REMOVE_CMD, null);
			}
		    ProcessResult result = ProcessHelper.waitForProcess(process, 500, getPrefs().getInt(CodewindCorePlugin.CW_UNINSTALL_TIMEOUT), mon.split(90));
		    return result;
		} finally {
			if (process != null && process.isAlive()) {
				process.destroy();
			}
			CodewindManager.getManager().refreshInstallStatus(mon.isCanceled() ? new NullProgressMonitor() : mon.split(10));
			CodewindManager.getManager().setInstallerStatus(null);
		}
	}
	
	public static ProcessResult upgradeWorkspace(String path, IProgressMonitor monitor) throws IOException, TimeoutException {
		SubMonitor mon = SubMonitor.convert(monitor, NLS.bind(Messages.UpgradeWorkspaceJobLabel, path), 100);
		Process process = null;
		try {
			process = CLIUtil.runCWCTL(null, UPGRADE_CMD, new String[] {WORKSPACE_OPTION, path});
			ProcessResult result = ProcessHelper.waitForProcess(process, 500, 300, mon);
			return result;
		} finally {
			if (process != null && process.isAlive()) {
				process.destroy();
			}
		}
	}
	
	public static String getRequestedVersion() {
		if (requestedVersion == null) {
			String value = System.getenv(CW_TAG_VAR);
			if (value == null || value.isEmpty()) {
				// Try the old env var
				value = System.getenv(INSTALL_VERSION_VAR);
			}
			if (value != null && !value.isEmpty()) {
				requestedVersion = value;
			}
		}
		return requestedVersion;
	}
	
	public static String getVersion() {
		if (installVersion == null) {
			String requestedVersion = getRequestedVersion();
			installVersion = requestedVersion != null ? requestedVersion : DEFAULT_INSTALL_VERSION;
		}
		return installVersion;
	}
	
	private static IPreferenceStore getPrefs() {
		return CodewindCorePlugin.getDefault().getPreferenceStore();
	}

}
