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
import org.eclipse.codewind.core.internal.messages.Messages;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubMonitor;

public class InstallUtil {
	
	public static final String STOP_APP_CONTAINERS_PREFSKEY = "stopAppContainers";
	public static final String STOP_APP_CONTAINERS_ALWAYS = "stopAppContainersAlways";
	public static final String STOP_APP_CONTAINERS_NEVER = "stopAppContainersNever";
	public static final String STOP_APP_CONTAINERS_PROMPT = "stopAppContainersPrompt";
	public static final String STOP_APP_CONTAINERS_DEFAULT = STOP_APP_CONTAINERS_PROMPT;
	
	private static final Map<OperatingSystem, String> installMap = new HashMap<OperatingSystem, String>();

	static {
		installMap.put(OperatingSystem.LINUX, "resources/codewind-installer-linux");
		installMap.put(OperatingSystem.MAC, "resources/codewind-installer-macos");
		installMap.put(OperatingSystem.WINDOWS, "resources/codewind-installer-win.exe");
	}
	
	private static final String INSTALLER_DIR = "installerWorkDir";
	private static final String INSTALL_DEV_CMD = "install-dev";
	private static final String INSTALL_CMD = "install";
	private static final String START_CMD = "start";
	private static final String STOP_CMD = "stop";
	private static final String STOP_ALL_CMD = "stop-all";
	private static final String STATUS_CMD = "status";
	private static final String REMOVE_CMD = "remove";
	
	private static final String INSTALL_DEV_VAR = "INSTALL_DEV";
	private static final String TAG_OPTION = "-t";
	private static final String DEFAULT_INSTALL_VERSION = "0.2";
	private static final String INSTALL_VERSION_VAR = "INSTALL_VERSION";
	private static String installCmd = null;
	private static String installVersion = null;
	private static String installExec = null;
	
	public enum InstallStatus {
		RUNNING(2),
		INSTALLED(1),
		NOT_INSTALLED(0),
		UNKNOWN(-1);
		
		private int code;
		
		private InstallStatus(int code) {
			this.code = code;
		}
		
		public static InstallStatus getStatus(int code) {
			for (InstallStatus status : InstallStatus.values()) {
				if (status.code == code) {
					return status;
				}
			}
			// This should not happen
			Logger.logError("Unrecognized installer status code: " + code);
			return UNKNOWN;
		}
		
		public boolean isInstalled() {
			return (this != NOT_INSTALLED && this != UNKNOWN);
		}
	}
	
	public static InstallStatus getInstallStatus() throws IOException, TimeoutException {
		ProcessResult result = statusCodewind();
		return InstallStatus.getStatus(result.getExitValue());
	}
	
	public static ProcessResult startCodewind(IProgressMonitor monitor) throws IOException, TimeoutException {
		SubMonitor mon = SubMonitor.convert(monitor, Messages.StartCodewindJobLabel, 100);
		Process process = null;
		try {
			CodewindManager.getManager().setInstallerStatus(InstallerStatus.STARTING);
			process = runInstaller(START_CMD, getVersion());
			ProcessResult result = ProcessHelper.waitForProcess(process, 500, 60, mon.split(90));
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
		    process = runInstaller(stopAll ? STOP_ALL_CMD : STOP_CMD, null);
		    ProcessResult result = ProcessHelper.waitForProcess(process, 500, 60, mon);
		    return result;
		} finally {
			if (process != null && process.isAlive()) {
				process.destroy();
			}
			CodewindManager.getManager().setInstallerStatus(null);
		}
	}
	
	public static ProcessResult installCodewind(IProgressMonitor monitor) throws IOException, TimeoutException {
		SubMonitor mon = SubMonitor.convert(monitor, Messages.InstallCodewindJobLabel, 100);
		Process process = null;
		try {
			CodewindManager.getManager().setInstallerStatus(InstallerStatus.INSTALLING);
		    process = runInstaller(getInstallCmd(), getVersion());
		    ProcessResult result = ProcessHelper.waitForProcess(process, 1000, 300, mon);
		    return result;
		} finally {
			if (process != null && process.isAlive()) {
				process.destroy();
			}
			CodewindManager.getManager().setInstallerStatus(null);
		}
	}
	
	public static ProcessResult removeCodewind(IProgressMonitor monitor) throws IOException, TimeoutException {
		SubMonitor mon = SubMonitor.convert(monitor, Messages.RemovingCodewindJobLabel, 100);
		Process process = null;
		try {
			CodewindManager.getManager().setInstallerStatus(InstallerStatus.UNINSTALLING);
		    process = runInstaller(REMOVE_CMD, null);
		    ProcessResult result = ProcessHelper.waitForProcess(process, 500, 60, mon);
		    return result;
		} finally {
			if (process != null && process.isAlive()) {
				process.destroy();
			}
			CodewindManager.getManager().setInstallerStatus(null);
		}
	}
	
	public static ProcessResult statusCodewind() throws IOException, TimeoutException {
		Process process = null;
		try {
			process = runInstaller(STATUS_CMD, null);
			ProcessResult result = ProcessHelper.waitForProcess(process, 500, 60, new NullProgressMonitor());
			return result;
		} finally {
			if (process != null && process.isAlive()) {
				process.destroy();
			}
		}
	}
	
	public static Process runInstaller(String cmd, String version) throws IOException {
		String installerPath = getInstallerExecutable();
		List<String> cmdList = new ArrayList<String>();
		cmdList.add(installerPath);
		cmdList.add(cmd);
		if (version != null) {
			cmdList.add(TAG_OPTION);
			cmdList.add(version);
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
	
	public static String getInstallerExecutable() throws IOException {
		if (installExec != null && (new File(installExec)).exists()) {
			return installExec;
		}
		
		// Get the current platform and choose the correct executable path
		OperatingSystem os = PlatformUtil.getOS(System.getProperty("os.name"));
		String relPath = installMap.get(os);
		if (relPath == null) {
			String msg = "Failed to get the relative path for the install executable";
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
			installExec = execPath;
			return installExec;
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
	
	private static String getInstallCmd() {
		if (installCmd == null) {
			String value = System.getenv(INSTALL_DEV_VAR);
			if ("true".equals(value)) {
				installCmd = INSTALL_DEV_CMD;
			} else {
				installCmd = INSTALL_CMD;
			}
		}
		return installCmd;
	}
	
	private static String getVersion() {
		if (INSTALL_DEV_CMD.equals(getInstallCmd())) {
			// No version if install-dev used
			return null;
		}
		if (installVersion == null) {
			String value = System.getenv(INSTALL_VERSION_VAR);
			if (value != null && !value.isEmpty()) {
				installVersion = value;
			} else {
				installVersion = DEFAULT_INSTALL_VERSION;
			}
		}
		return installVersion;
	}

}
