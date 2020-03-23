/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

import org.eclipse.codewind.core.internal.ProcessHelper;
import org.eclipse.codewind.core.internal.ProcessHelper.ProcessResult;
import org.eclipse.codewind.core.internal.connection.RegistryInfo;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.json.JSONArray;
import org.json.JSONException;

public class RegistryUtil {
	
	private static final String REGISTRYSECRETS_CMD = "registrysecrets";
	private static final String[] REG_SECRET_LIST_CMD = new String[] {REGISTRYSECRETS_CMD, "list"};
	private static final String[] REG_SECRET_ADD_CMD = new String[] {REGISTRYSECRETS_CMD, "add"};
	private static final String[] REG_SECRET_REMOVE_CMD = new String[] {REGISTRYSECRETS_CMD, "remove"};
	
	private static final String ADDRESS_OPTION = "--address";
	private static final String USERNAME_OPTION = "--username";
	private static final String PASSWORD_OPTION = "--password";

	public static List<RegistryInfo> listRegistrySecrets(String conid, IProgressMonitor monitor) throws IOException, JSONException, TimeoutException {
		SubMonitor mon = SubMonitor.convert(monitor, 100);
		Process process = null;
		try {
			process = CLIUtil.runCWCTL(CLIUtil.GLOBAL_JSON_INSECURE, REG_SECRET_LIST_CMD, new String[] {CLIUtil.CON_ID_OPTION, conid});
			ProcessResult result = ProcessHelper.waitForProcess(process, 500, 60, mon.split(100));
			CLIUtil.checkResult(REG_SECRET_LIST_CMD, result, true);
			JSONArray registryArray = new JSONArray(result.getOutput().trim());
			List<RegistryInfo> registries = new ArrayList<RegistryInfo>();
			for (int i = 0; i < registryArray.length(); i++) {
				registries.add(new RegistryInfo(registryArray.getJSONObject(i)));
			}
			return registries;
		} finally {
			if (process != null && process.isAlive()) {
				process.destroy();
			}
		}
	}
	
	public static void addRegistrySecret(String address, String username, String password, String conid, IProgressMonitor monitor) throws IOException, JSONException, TimeoutException {
		runRegistrySecretCmd(REG_SECRET_ADD_CMD, new String[] {ADDRESS_OPTION, address, USERNAME_OPTION, username, PASSWORD_OPTION, password, CLIUtil.CON_ID_OPTION, conid}, null, monitor);
	}
	
	public static void removeRegistrySecret(String address, String conid, IProgressMonitor monitor) throws IOException, JSONException, TimeoutException {
		runRegistrySecretCmd(REG_SECRET_REMOVE_CMD, new String[] {ADDRESS_OPTION, address, CLIUtil.CON_ID_OPTION, conid}, null, monitor);
	}
	
	private static void runRegistrySecretCmd(String[] command, String[] options, String[] args, IProgressMonitor monitor) throws IOException, JSONException, TimeoutException {
		SubMonitor mon = SubMonitor.convert(monitor, 100);
		Process process = null;
		try {
			process = CLIUtil.runCWCTL(CLIUtil.GLOBAL_JSON_INSECURE, command, options, args);
			ProcessResult result = ProcessHelper.waitForProcess(process, 500, 60, mon.split(100));
			CLIUtil.checkResult(command, result, false);
		} finally {
			if (process != null && process.isAlive()) {
				process.destroy();
			}
		}
	}
}
