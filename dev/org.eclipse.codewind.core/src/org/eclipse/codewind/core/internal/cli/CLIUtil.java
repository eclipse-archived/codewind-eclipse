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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.eclipse.codewind.core.CodewindCorePlugin;
import org.eclipse.codewind.core.internal.CoreUtil;
import org.eclipse.codewind.core.internal.FileUtil;
import org.eclipse.codewind.core.internal.Logger;
import org.eclipse.codewind.core.internal.PlatformUtil;
import org.eclipse.codewind.core.internal.PlatformUtil.OperatingSystem;
import org.eclipse.codewind.core.internal.ProcessHelper.ProcessResult;
import org.eclipse.core.runtime.IPath;
import org.json.JSONException;
import org.json.JSONObject;

public class CLIUtil {
	
	public static final String CODEWIND_STORE_DIR = ".codewind";
	
	// Global options
	public static final String JSON_OPTION = "--json";
	public static final String INSECURE_OPTION = "--insecure";
	public static final String[] GLOBAL_JSON = new String[] {JSON_OPTION};
	public static final String[] GLOBAL_INSECURE = new String[] {INSECURE_OPTION};
	public static final String[] GLOBAL_JSON_INSECURE = new String[] {JSON_OPTION, INSECURE_OPTION};
	
	// Common options
	public static final String CON_ID_OPTION = "--conid";
	
	// Common keys
	public static final String ERROR_KEY = "error";
	public static final String ERROR_DESCRIPTION_KEY = "error_description";
		
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
	
	public static Process runCWCTL(String[] globalOptions, String[] cmd, String[] options) throws IOException {
		return runCWCTL(globalOptions, cmd, options, null);
	}
	
	public static Process runCWCTL(String[] globalOptions, String[] cmd, String[] options, String[] args) throws IOException {
		String[] command = getCWCTLCommand(globalOptions, cmd, options, args);
		ProcessBuilder builder = new ProcessBuilder(command);
		if (PlatformUtil.getOS() == PlatformUtil.OperatingSystem.MAC) {
			String pathVar = System.getenv("PATH");
			pathVar = "/usr/local/bin:" + pathVar;
			Map<String, String> env = builder.environment();
			env.put("PATH", pathVar);
		}
		return builder.start();
	}
	
	public static List<String> getCWCTLCommandList(String[] globalOptions, String[] cmd, String[] options, String[] args) throws IOException {
		// Make sure the executables are installed
		for (int i=0; i< cliInfos.length; i++) {
			if (cliInfos[i] != null)
				cliInfos[i].setInstallPath(getCLIExecutable(cliInfos[i]));
		}
		
		List<String> cmdList = new ArrayList<String>();
		cmdList.add(codewindInfo.getInstallPath());
		addOptions(cmdList, globalOptions);
		addOptions(cmdList, cmd);
		addOptions(cmdList, options);
		addOptions(cmdList, args);
		return cmdList;
	}
	
	public static String[] getCWCTLCommand(String[] globalOptions, String[] cmd, String[] options, String[] args) throws IOException {
		List<String> cmdList = getCWCTLCommandList(globalOptions, cmd, options, args);
		return cmdList.toArray(new String[cmdList.size()]);
	}
	
	private static void addOptions(List<String> cmdList, String[] options) {
		if (options != null) {
			for (String opt : options) {
				cmdList.add(opt);
			}
		}
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
			URL url = CodewindCorePlugin.getDefault().getBundle().getEntry(relPath);
			stream = url.openStream();
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
		IPath userHome = CoreUtil.getUserHome();
		if (userHome != null) {
			return userHome.append(CODEWIND_STORE_DIR).append(InstallUtil.getDefaultInstallVersion()).toOSString();
		}
		IPath stateLoc = CodewindCorePlugin.getDefault().getStateLocation();
		return stateLoc.append(INSTALLER_DIR).toOSString();
	}
	
	public static void checkResult(String[] command, ProcessResult result, boolean checkOutput) throws IOException {
		// Check for json error output (may still get a 0 return code in this case). If it is in the expected format
		// then use this for the error message, otherwise fall back to using the system error if not empty or the
		// system output.
		// Expected format:
		//    {"error":"con_not_found","error_description":"Connection AGALJKAFD not found"}
		try {
			if (result.getOutput() != null && !result.getOutput().isEmpty()) {
				JSONObject obj = new JSONObject(result.getOutput());
				if (obj.has(ERROR_KEY) && obj.has(ERROR_DESCRIPTION_KEY)) {
					String msg = String.format("The cwctl '%s' command failed with error: %s", CoreUtil.formatString(command, " "), obj.getString(ERROR_DESCRIPTION_KEY)); //$NON-NLS-1$
					Logger.logError(msg);
					throw new IOException(obj.getString(ERROR_DESCRIPTION_KEY));
				}
			}
		} catch (JSONException e) {
			// Ignore
		}
		
		if (result.getExitValue() != 0) {
			String msg;
			String error = result.getError() != null && !result.getError().isEmpty() ? result.getError() : result.getOutput();
			if (error == null || error.isEmpty()) {
				msg = String.format("The cwctl '%s' command exited with return code %d", CoreUtil.formatString(command, " "), result.getExitValue()); //$NON-NLS-1$
			} else {
				msg = String.format("The cwctl '%s' command exited with return code %d and error: %s", CoreUtil.formatString(command, " "), result.getExitValue(), error); //$NON-NLS-1$
			}
			Logger.logError(msg);
			throw new IOException(msg);
		} else if (checkOutput && (result.getOutput() == null || result.getOutput().isEmpty())) {
			String msg = String.format("The cwctl '%s' command exited with return code 0 but the output was empty", CoreUtil.formatString(command, " "));  //$NON-NLS-1$
			Logger.logError(msg);
			throw new IOException(msg);
		}
		
		Logger.log(String.format("Result of the cwctl '%s' command: \n%s", CoreUtil.formatString(command, " "), Optional.ofNullable(result.getOutput()).orElse("<empty>")));
	}
}
