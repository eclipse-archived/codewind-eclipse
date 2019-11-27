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
import java.util.List;
import java.util.concurrent.TimeoutException;

import org.eclipse.codewind.core.internal.ProcessHelper;
import org.eclipse.codewind.core.internal.ProcessHelper.ProcessResult;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.json.JSONException;
import org.json.JSONObject;

public class ConnectionUtil {

	private static final String CONNECTIONS_CMD = "connections";
	private static final String LIST_OPTION = "list";
	private static final String ADD_OPTION = "add";
	private static final String REMOVE_OPTION = "remove";
	private static final String UPDATE_OPTION = "update";
	
	private static final String LABEL_OPTION = "--label";
	private static final String URL_OPTION = "--url";
	private static final String USERNAME_OPTION = "--username";
	
	private static final String ID_KEY = "id";
	
	public static List<ConnectionInfo> listConnections(IProgressMonitor monitor) throws IOException, JSONException, TimeoutException {
		ProcessResult result = runConnectionCmd(new String[] {CONNECTIONS_CMD, LIST_OPTION}, null, null, true, monitor);
		JSONObject resultJson = new JSONObject(result.getOutput());
		return ConnectionInfo.getInfos(resultJson);
	}
	
	public static String addConnection(String name, String url, String username, IProgressMonitor monitor) throws IOException, JSONException, TimeoutException {
		ProcessResult result = runConnectionCmd(new String[] {CONNECTIONS_CMD, ADD_OPTION}, new String[] {LABEL_OPTION, name, URL_OPTION, url, USERNAME_OPTION, username}, null, true, monitor);
		JSONObject resultJson = new JSONObject(result.getOutput());
		return resultJson.getString(ID_KEY);
	}
	
	public static void removeConnection(String conid, IProgressMonitor monitor) throws IOException, JSONException, TimeoutException {
		runConnectionCmd(new String[] {CONNECTIONS_CMD, REMOVE_OPTION}, new String[] {CLIUtil.CON_ID_OPTION, conid}, null, false, monitor);
	}
	
	public static void updateConnection(String conid, String name, String url, String username, IProgressMonitor monitor) throws IOException, JSONException, TimeoutException {
		runConnectionCmd(new String[] {CONNECTIONS_CMD, UPDATE_OPTION}, new String[] {CLIUtil.CON_ID_OPTION, conid, LABEL_OPTION, name, URL_OPTION, url, USERNAME_OPTION, username}, null, false, monitor);
	}
	
	private static ProcessResult runConnectionCmd(String[] command, String[] options, String[] args, boolean checkOutput, IProgressMonitor monitor) throws IOException, JSONException, TimeoutException {
		SubMonitor mon = SubMonitor.convert(monitor, 100);
		Process process = null;
		try {
			process = CLIUtil.runCWCTL(new String[] {CLIUtil.INSECURE_OPTION, CLIUtil.JSON_OPTION}, command, options, args);
			ProcessResult result = ProcessHelper.waitForProcess(process, 500, 60, mon);
			CLIUtil.checkResult(command, result, checkOutput);
			return result;
		} finally {
			if (process != null && process.isAlive()) {
				process.destroy();
			}
		}
	}
}
