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

import org.eclipse.codewind.core.CodewindCorePlugin;
import org.eclipse.codewind.core.internal.FileUtil;
import org.eclipse.codewind.core.internal.Logger;
import org.eclipse.codewind.core.internal.PlatformUtil;
import org.eclipse.codewind.core.internal.PlatformUtil.OperatingSystem;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

public class CLIUtil {
	
	// Common options
	public static final String JSON_OPTION = "--json";
		
	private static final String INSTALLER_DIR = "installerWorkDir";
	
	private static final Map<OperatingSystem, String> cwctlMap = new HashMap<OperatingSystem, String>();
	private static final Map<OperatingSystem, String> appsodyMap = new HashMap<OperatingSystem, String>();

	static {
		cwctlMap.put(OperatingSystem.LINUX, "binaries/linux/cwctl");
		cwctlMap.put(OperatingSystem.MAC, "binaries/darwin/cwctl");
		cwctlMap.put(OperatingSystem.WINDOWS, "binaries/windows/cwctl.exe");
	}
	
	static {
		appsodyMap.put(OperatingSystem.LINUX, "binaries/linux/appsody");
		appsodyMap.put(OperatingSystem.MAC, "binaries/darwin/appsody");
		appsodyMap.put(OperatingSystem.WINDOWS, "binaries/windows/appsody.exe");		
	}
	
	private static final CLIInfo codewindInfo = new CLIInfo("Codewind", cwctlMap);
	private static final CLIInfo appsodyInfo = new CLIInfo("Appsody", appsodyMap);
	
	private static final CLIInfo[] cliInfos = {codewindInfo, appsodyInfo};
	
	public static Process runCWCTL(String cmd, String... options) throws IOException {		
		// Make sure the executables are installed
		for (int i=0; i< cliInfos.length; i++) {
			if (cliInfos[i] != null)
				cliInfos[i].setInstallPath(getCLIExecutable(cliInfos[i]));
		}
		
		List<String> cmdList = new ArrayList<String>();
		cmdList.add(codewindInfo.getInstallPath());
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
	
	public static String getCWCTLExecutable() throws IOException {
		return getCLIExecutable(codewindInfo);
	}
	
	public static String getCLIExecutable(CLIInfo operation) throws IOException {
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
		String installerDir = getCLIInstallDir();
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
	
	private static String getCLIInstallDir() {
		IPath stateLoc = CodewindCorePlugin.getDefault().getStateLocation();
		return stateLoc.append(INSTALLER_DIR).toOSString();
	}
}
