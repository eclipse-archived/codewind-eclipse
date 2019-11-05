/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
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
import org.eclipse.codewind.core.internal.messages.Messages;
import org.eclipse.core.runtime.IProgressMonitor;
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
	
	private static final String INSTALL_CMD = "install";
	private static final String START_CMD = "start";
	private static final String STOP_CMD = "stop";
	private static final String STOP_ALL_CMD = "stop-all";
	private static final String STATUS_CMD = "status";
	private static final String REMOVE_CMD = "remove";
	private static final String UPGRADE_CMD = "upgrade";
	
	public static final String DEFAULT_INSTALL_VERSION = "0.6.0";
	
	private static final String TAG_OPTION = "-t";
	private static final String INSTALL_VERSION_VAR = "INSTALL_VERSION";
	private static final String CW_TAG_VAR = "CW_TAG";
	private static final String WORKSPACE_OPTION = "--workspace";
	
	private static String installVersion = null;
	private static String requestedVersion = null;
	
	public static InstallStatus getInstallStatus(IProgressMonitor monitor) throws IOException, JSONException, TimeoutException {
		ProcessResult result = statusCodewind(monitor);
		if (result.getExitValue() != 0) {
			String error = result.getError();
			if (error == null || error.isEmpty()) {
				error = result.getOutput();
			}
			String msg = "Installer status command failed with rc: " + result.getExitValue() + " and error: " + error;  //$NON-NLS-1$ //$NON-NLS-2$
			Logger.logError(msg);
			throw new IOException(msg);
		}
		JSONObject status = new JSONObject(result.getOutput());
		return new InstallStatus(status);
	}
	
	public static ProcessResult startCodewind(String version, IProgressMonitor monitor) throws IOException, TimeoutException {
		SubMonitor mon = SubMonitor.convert(monitor, Messages.StartCodewindJobLabel, 100);
		Process process = null;
		try {
			CodewindManager.getManager().setInstallerStatus(InstallerStatus.STARTING);
			process = CLIUtil.runCWCTL(START_CMD, TAG_OPTION, version);
			ProcessResult result = ProcessHelper.waitForProcess(process, 500, getPrefs().getInt(CodewindCorePlugin.CW_START_TIMEOUT), mon.split(90));
			return result;
		} finally {
			if (process != null && process.isAlive()) {
				process.destroy();
			}
			CodewindManager.getManager().refreshInstallStatus(mon.split(10));
			CodewindManager.getManager().setInstallerStatus(null);
		}
	}
	
	public static ProcessResult stopCodewind(boolean stopAll, IProgressMonitor monitor) throws IOException, TimeoutException {
		SubMonitor mon = SubMonitor.convert(monitor, Messages.StopCodewindJobLabel, 100);
		Process process = null;
		try {
			CodewindManager.getManager().setInstallerStatus(InstallerStatus.STOPPING);
		    process = CLIUtil.runCWCTL(stopAll ? STOP_ALL_CMD : STOP_CMD);
		    ProcessResult result = ProcessHelper.waitForProcess(process, 500, getPrefs().getInt(CodewindCorePlugin.CW_STOP_TIMEOUT), mon.split(95));
		    return result;
		} finally {
			if (process != null && process.isAlive()) {
				process.destroy();
			}
			CodewindManager.getManager().refreshInstallStatus(mon.split(5));
			CodewindManager.getManager().setInstallerStatus(null);
		}
	}
	
	public static ProcessResult installCodewind(String version, IProgressMonitor monitor) throws IOException, TimeoutException {
		SubMonitor mon = SubMonitor.convert(monitor, Messages.InstallCodewindJobLabel, 100);
		Process process = null;
		try {
			CodewindManager.getManager().setInstallerStatus(InstallerStatus.INSTALLING);
		    process = CLIUtil.runCWCTL(INSTALL_CMD, TAG_OPTION, version);
		    ProcessResult result = ProcessHelper.waitForProcess(process, 1000, getPrefs().getInt(CodewindCorePlugin.CW_INSTALL_TIMEOUT), mon.split(95));
		    return result;
		} finally {
			if (process != null && process.isAlive()) {
				process.destroy();
			}
			CodewindManager.getManager().refreshInstallStatus(mon.split(5));
			CodewindManager.getManager().setInstallerStatus(null);
		}
	}
	
	public static ProcessResult removeCodewind(String version, IProgressMonitor monitor) throws IOException, TimeoutException {
		SubMonitor mon = SubMonitor.convert(monitor, Messages.RemovingCodewindJobLabel, 100);
		Process process = null;
		try {
			CodewindManager.getManager().setInstallerStatus(InstallerStatus.UNINSTALLING);
			if (version != null) {
				process = CLIUtil.runCWCTL(REMOVE_CMD, TAG_OPTION, version);
			} else {
				process = CLIUtil.runCWCTL(REMOVE_CMD);
			}
		    ProcessResult result = ProcessHelper.waitForProcess(process, 500, getPrefs().getInt(CodewindCorePlugin.CW_UNINSTALL_TIMEOUT), mon.split(90));
		    return result;
		} finally {
			if (process != null && process.isAlive()) {
				process.destroy();
			}
			CodewindManager.getManager().refreshInstallStatus(mon.split(10));
			CodewindManager.getManager().setInstallerStatus(null);
		}
	}
	
	public static ProcessResult upgradeWorkspace(String path, IProgressMonitor monitor) throws IOException, TimeoutException {
		SubMonitor mon = SubMonitor.convert(monitor, NLS.bind(Messages.UpgradeWorkspaceJobLabel, path), 100);
		Process process = null;
		try {
			process = CLIUtil.runCWCTL(UPGRADE_CMD, WORKSPACE_OPTION, path);
			ProcessResult result = ProcessHelper.waitForProcess(process, 500, 300, mon);
			return result;
		} finally {
			if (process != null && process.isAlive()) {
				process.destroy();
			}
		}
	}

	private static ProcessResult statusCodewind(IProgressMonitor monitor) throws IOException, TimeoutException {
		SubMonitor mon = SubMonitor.convert(monitor, Messages.CodewindStatusJobLabel, 100);
		Process process = null;
		try {
			process = CLIUtil.runCWCTL(STATUS_CMD, CLIUtil.JSON_OPTION);
			ProcessResult result = ProcessHelper.waitForProcess(process, 500, 120, mon);
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
