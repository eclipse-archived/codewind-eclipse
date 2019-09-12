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

package org.eclipse.codewind.core.internal;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeoutException;

import org.eclipse.codewind.core.CodewindCorePlugin;
import org.eclipse.codewind.core.internal.CodewindManager.InstallerStatus;
import org.eclipse.codewind.core.internal.PlatformUtil.OperatingSystem;
import org.eclipse.codewind.core.internal.ProcessHelper.ProcessResult;
import org.eclipse.codewind.core.internal.constants.CoreConstants;
import org.eclipse.codewind.core.internal.constants.ProjectInfo;
import org.eclipse.codewind.core.internal.messages.Messages;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
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
	
	private static final Map<OperatingSystem, String> installMap = new HashMap<OperatingSystem, String>();
	private static final Map<OperatingSystem, String> appsodyMap = new HashMap<OperatingSystem, String>();

	static {
		installMap.put(OperatingSystem.LINUX, "resources/codewind-installer-linux");
		installMap.put(OperatingSystem.MAC, "resources/codewind-installer-macos");
		installMap.put(OperatingSystem.WINDOWS, "resources/codewind-installer-win.exe");
	}
	
	static {
		appsodyMap.put(OperatingSystem.LINUX, "resources/appsody/linux/appsody");
		appsodyMap.put(OperatingSystem.MAC, "resources/appsody/macos/appsody");
		appsodyMap.put(OperatingSystem.WINDOWS, "resources/appsody/win/appsody.exe");		
	}
	
	private static final InstallOperation codewindInstall = new InstallOperation("Codewind", installMap);
	private static final InstallOperation appsodyInstall = new InstallOperation("Appsody", appsodyMap);
	
	private static final InstallOperation[] installOperations = {codewindInstall, appsodyInstall};
	
	
	private static final String INSTALLER_DIR = "installerWorkDir";
	private static final String INSTALL_CMD = "install";
	private static final String START_CMD = "start";
	private static final String STOP_CMD = "stop";
	private static final String STOP_ALL_CMD = "stop-all";
	private static final String STATUS_CMD = "status";
	private static final String REMOVE_CMD = "remove";
	private static final String PROJECT_CMD = "project";
	
	public static final String DEFAULT_INSTALL_VERSION = "0.4.0";
	
	private static final String TAG_OPTION = "-t";
	private static final String JSON_OPTION = "-j";
	private static final String URL_OPTION = "--url";
	private static final String INSTALL_VERSION_VAR = "INSTALL_VERSION";
	private static final String CW_TAG_VAR = "CW_TAG";
	
	public static final String STATUS_KEY = "status";
	public static final String URL_KEY = "url";
	
	private static String installVersion = null;
	
	public static InstallStatus getInstallStatus() throws IOException, JSONException, TimeoutException {
		ProcessResult result = statusCodewind();
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
			process = runInstaller(START_CMD, TAG_OPTION, version);
			ProcessResult result = ProcessHelper.waitForProcess(process, 500, getPrefs().getInt(CodewindCorePlugin.CW_START_TIMEOUT), mon.split(90));
			return result;
		} finally {
			if (process != null && process.isAlive()) {
				process.destroy();
			}
			CodewindManager.getManager().setInstallerStatus(null);
		}
	}
	
	public static ProcessResult stopCodewind(boolean stopAll, IProgressMonitor monitor) throws IOException, TimeoutException {
		SubMonitor mon = SubMonitor.convert(monitor, Messages.StopCodewindJobLabel, 100);
		Process process = null;
		try {
			CodewindManager.getManager().setInstallerStatus(InstallerStatus.STOPPING);
		    process = runInstaller(stopAll ? STOP_ALL_CMD : STOP_CMD);
		    ProcessResult result = ProcessHelper.waitForProcess(process, 500, getPrefs().getInt(CodewindCorePlugin.CW_STOP_TIMEOUT), mon);
		    return result;
		} finally {
			if (process != null && process.isAlive()) {
				process.destroy();
			}
			CodewindManager.getManager().setInstallerStatus(null);
		}
	}
	
	public static ProcessResult installCodewind(String version, IProgressMonitor monitor) throws IOException, TimeoutException {
		SubMonitor mon = SubMonitor.convert(monitor, Messages.InstallCodewindJobLabel, 100);
		Process process = null;
		try {
			CodewindManager.getManager().setInstallerStatus(InstallerStatus.INSTALLING);
		    process = runInstaller(INSTALL_CMD, TAG_OPTION, version);
		    ProcessResult result = ProcessHelper.waitForProcess(process, 1000, getPrefs().getInt(CodewindCorePlugin.CW_INSTALL_TIMEOUT), mon);
		    return result;
		} finally {
			if (process != null && process.isAlive()) {
				process.destroy();
			}
			CodewindManager.getManager().setInstallerStatus(null);
		}
	}
	
	public static ProcessResult removeCodewind(String version, IProgressMonitor monitor) throws IOException, TimeoutException {
		SubMonitor mon = SubMonitor.convert(monitor, Messages.RemovingCodewindJobLabel, 100);
		Process process = null;
		try {
			CodewindManager.getManager().setInstallerStatus(InstallerStatus.UNINSTALLING);
			if (version != null) {
				process = runInstaller(REMOVE_CMD, TAG_OPTION, version);
			} else {
				process = runInstaller(REMOVE_CMD);
			}
		    ProcessResult result = ProcessHelper.waitForProcess(process, 500, getPrefs().getInt(CodewindCorePlugin.CW_UNINSTALL_TIMEOUT), mon);
		    return result;
		} finally {
			if (process != null && process.isAlive()) {
				process.destroy();
			}
			CodewindManager.getManager().setInstallerStatus(null);
		}
	}
	
	public static void createProject(String name, String path, String url, IProgressMonitor monitor) throws IOException, JSONException, TimeoutException {
		SubMonitor mon = SubMonitor.convert(monitor, NLS.bind(Messages.CreateProjectTaskLabel, name), 100);
		Process process = null;
		try {
			process = runInstaller(PROJECT_CMD, path, URL_OPTION, url);
			ProcessResult result = ProcessHelper.waitForProcess(process, 500, 60, mon);
			if (result.getExitValue() != 0) {
				Logger.logError("Project create failed with rc: " + result.getExitValue() + " and error: " + result.getErrorMsg()); //$NON-NLS-1$ //$NON-NLS-2$
				throw new IOException(result.getErrorMsg());
			}
			if (result.getOutput() == null || result.getOutput().trim().isEmpty()) {
				// This should not happen
				Logger.logError("Project create had 0 return code but the output is empty"); //$NON-NLS-1$
				throw new IOException("The output from project create is empty."); //$NON-NLS-1$
			}
			JSONObject resultJson = new JSONObject(result.getOutput());
			if (!CoreConstants.VALUE_STATUS_SUCCESS.equals(resultJson.getString(CoreConstants.KEY_STATUS))) {
				String msg = "Project create failed for project: " + name + " with output: " + result.getOutput(); //$NON-NLS-1$ //$NON-NLS-2$
				Logger.logError(msg);
				throw new IOException(msg);
			}
		} finally {
			if (process != null && process.isAlive()) {
				process.destroy();
			}
		}
	}
	
	public static ProjectInfo validateProject(String name, String path, IProgressMonitor monitor) throws IOException, JSONException, TimeoutException {
		SubMonitor mon = SubMonitor.convert(monitor, NLS.bind(Messages.ValidateProjectTaskLabel, name), 100);
		Process process = null;
		try {
			process = runInstaller(PROJECT_CMD, path);
			ProcessResult result = ProcessHelper.waitForProcess(process, 500, 60, mon);
			if (result.getExitValue() != 0) {
				Logger.logError("Project validate failed with rc: " + result.getExitValue() + " and error: " + result.getErrorMsg()); //$NON-NLS-1$ //$NON-NLS-2$
				throw new IOException(result.getErrorMsg());
			}
			if (result.getOutput() == null || result.getOutput().trim().isEmpty()) {
				// This should not happen
				Logger.logError("Project validate had 0 return code but the output is empty"); //$NON-NLS-1$
				throw new IOException("The output from project validate is empty."); //$NON-NLS-1$
			}
		    
			JSONObject resultJson = new JSONObject(result.getOutput());
			if (CoreConstants.VALUE_STATUS_SUCCESS.equals(resultJson.getString(CoreConstants.KEY_STATUS))) {
				if (resultJson.has(CoreConstants.KEY_RESULT)) {
					JSONObject typeJson = resultJson.getJSONObject(CoreConstants.KEY_RESULT);
					String language = typeJson.getString(CoreConstants.KEY_LANGUAGE);
					String projectType = typeJson.getString(CoreConstants.KEY_PROJECT_TYPE);
					return new ProjectInfo(projectType, language);
				}
			}
			String msg = "Validation failed for project: " + name + " with output: " + result.getOutput(); //$NON-NLS-1$ //$NON-NLS-2$
			Logger.logError(msg);
			throw new IOException(msg);
		} finally {
			if (process != null && process.isAlive()) {
				process.destroy();
			}
		}
	}
	
	private static ProcessResult statusCodewind() throws IOException, TimeoutException {
		Process process = null;
		try {
			process = runInstaller(STATUS_CMD, JSON_OPTION);
			ProcessResult result = ProcessHelper.waitForProcess(process, 500, 60, new NullProgressMonitor());
			return result;
		} finally {
			if (process != null && process.isAlive()) {
				process.destroy();
			}
		}
	}
	
	public static Process runInstaller(String cmd, String... options) throws IOException {		
		// Install prerequistes
		int len = installOperations.length;
		for (int i=0; i< len; i++) {
			if (installOperations[i] != null)
				installOperations[i].setInstallPath(getInstallerExecutable(installOperations[i]));
		}
		
		List<String> cmdList = new ArrayList<String>();
		cmdList.add(codewindInstall.getInstallPath());
		cmdList.add(cmd);
		if (options != null) {
			for (String option : options) {
				cmdList.add(option);
			}
		}
		String[] command = cmdList.toArray(new String[cmdList.size()]);
		ProcessBuilder builder = new ProcessBuilder(command);
		if (PlatformUtil.getOS() == PlatformUtil.OperatingSystem.MAC) {
			String pathVar = System.getenv("PATH");
			pathVar = "/usr/local/bin:" + pathVar;
			Map<String, String> env = builder.environment();
			env.put("PATH", pathVar);
		}
		return builder.start();
	}
	
	public static String getInstallerExecutable(InstallOperation operation) throws IOException {
		String installPath = operation.getInstallPath();
		if (installPath != null && (new File(installPath).exists())) {
			return installPath;
		}

		// Get the current platform and choose the correct executable path
		OperatingSystem os = PlatformUtil.getOS(System.getProperty("os.name"));
		
		Map<OperatingSystem, String> osPathMap = operation.getOSPathMap();
		if (osPathMap == null) {
			String msg = "Failed to get the list of operating specific paths for installing the executable " + operation.getInstallName();
			Logger.logError(msg);
			throw new IOException(msg);
		}
		
		String relPath = osPathMap.get(os);
		if (relPath == null) {
			String msg = "Failed to get the relative path for the install executable " + operation.getInstallName();
			Logger.logError(msg);
			throw new IOException(msg);
		}
		
		// Get the executable path
		String installerDir = getInstallerDir();
		String execName = relPath.substring(relPath.lastIndexOf('/') + 1);
		String execPath = installerDir + File.separator + execName;
				
		// Make the installer directory
		if (!FileUtil.makeDir(installerDir)) {
			String msg = "Failed to make the directory for the installer utility: " + installerDir;
			Logger.logError(msg);
			throw new IOException(msg);
		}
		
		// Copy the executable over
		InputStream stream = null;
		try {
			stream = FileLocator.openStream(CodewindCorePlugin.getDefault().getBundle(), new Path(relPath), false);
			FileUtil.copyFile(stream, execPath);
			if (PlatformUtil.getOS() != PlatformUtil.OperatingSystem.WINDOWS) {
				Set<PosixFilePermission> permissions = PosixFilePermissions.fromString("rwxr-xr-x");
				File file = new File(execPath);
				Files.setPosixFilePermissions(file.toPath(), permissions);
			}
			return execPath;
		} finally {
			if (stream != null) {
				try {
					stream.close();
				} catch (IOException e) {
					// Ignore
				}
			}
		}
	}
	
	
	private static String getInstallerDir() {
		IPath stateLoc = CodewindCorePlugin.getDefault().getStateLocation();
		return stateLoc.append(INSTALLER_DIR).toOSString();
	}
	
	public static String getVersion() {
		if (installVersion == null) {
			String value = System.getenv(CW_TAG_VAR);
			if (value == null || value.isEmpty()) {
				// Try the old env var
				value = System.getenv(INSTALL_VERSION_VAR);
			}
			if (value != null && !value.isEmpty()) {
				installVersion = value;
			} else {
				installVersion = DEFAULT_INSTALL_VERSION;
			}
		}
		return installVersion;
	}
	
	private static IPreferenceStore getPrefs() {
		return CodewindCorePlugin.getDefault().getPreferenceStore();
	}

}
