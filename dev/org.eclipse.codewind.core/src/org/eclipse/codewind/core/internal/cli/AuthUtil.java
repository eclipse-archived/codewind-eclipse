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

import org.eclipse.codewind.core.internal.Logger;
import org.eclipse.codewind.core.internal.ProcessHelper;
import org.eclipse.codewind.core.internal.ProcessHelper.ProcessResult;
import org.eclipse.codewind.core.internal.messages.Messages;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.json.JSONException;
import org.json.JSONObject;

public class AuthUtil {
	

	private static final String[] SECKEYRING_UPDATE_CMD = new String[] {"seckeyring",  "update"};
	
	private static final String[] SECTOKEN_GET_CMD = new String[] {"sectoken",  "get"};
	
	private static final String USERNAME_OPTION = "--username";
	private static final String PASSWORD_OPTION = "--password";
	
	private static final String STATUS_KEY = "status";
	private static final String STATUS_MSG_KEY = "status_message";
	
	private static final String STATUS_OK_VALUE = "OK";
	
	public static AuthToken genAuthToken(String username, String password, String conid, IProgressMonitor monitor) throws IOException, JSONException, TimeoutException {
		SubMonitor mon = SubMonitor.convert(monitor, Messages.AuthGenTaskLabel, 100);
		Process process = null;
		try {
			process = CLIUtil.runCWCTL(null, SECKEYRING_UPDATE_CMD, new String[] {USERNAME_OPTION, username, PASSWORD_OPTION, password, CLIUtil.CON_ID_OPTION, conid});
			ProcessResult result = ProcessHelper.waitForProcess(process, 500, 60, mon.split(50));
			if (result.getExitValue() != 0) {
				Logger.logError("Seckeyring update failed with rc: " + result.getExitValue() + " and error: " + result.getErrorMsg()); //$NON-NLS-1$ //$NON-NLS-2$
				throw new IOException(result.getErrorMsg());
			}
			if (result.getOutput() == null || result.getOutput().trim().isEmpty()) {
				// This should not happen
				Logger.logError("Seckeyring update had 0 return code but the output is empty"); //$NON-NLS-1$
				throw new IOException("The output from seckeyring update is empty."); //$NON-NLS-1$
			}
			JSONObject resultJson = new JSONObject(result.getOutput());
			if (!STATUS_OK_VALUE.equals(resultJson.getString(STATUS_KEY))) {
				String msg = "Seckeyring update failed for: " + conid + " with output: " + resultJson.getString(STATUS_MSG_KEY); //$NON-NLS-1$ //$NON-NLS-2$
				Logger.logError(msg);
				throw new IOException(msg);
			}
			
			return getAuthToken(username, conid, mon.split(50));
		} finally {
			if (process != null && process.isAlive()) {
				process.destroy();
			}
		}
	}
	
	public static AuthToken getAuthToken(String username, String conid, IProgressMonitor monitor) throws IOException, JSONException, TimeoutException {
		SubMonitor mon = SubMonitor.convert(monitor, Messages.AuthGetTaskLabel, 100);
		Process process = null;
		try {
			process = CLIUtil.runCWCTL(CLIUtil.GLOBAL_INSECURE, SECTOKEN_GET_CMD, new String[] {USERNAME_OPTION, username, CLIUtil.CON_ID_OPTION, conid});
			ProcessResult result = ProcessHelper.waitForProcess(process, 500, 60, mon.split(100));
			if (result.getExitValue() != 0) {
				Logger.logError("Sectoken get failed with rc: " + result.getExitValue() + " and error: " + result.getErrorMsg()); //$NON-NLS-1$ //$NON-NLS-2$
				throw new IOException(result.getErrorMsg());
			}
			if (result.getOutput() == null || result.getOutput().trim().isEmpty()) {
				// This should not happen
				Logger.logError("Sectoken get had 0 return code but the output is empty"); //$NON-NLS-1$
				throw new IOException("The output from sectoken get is empty."); //$NON-NLS-1$
			}
			return new AuthToken(new JSONObject(result.getOutput()));
		} finally {
			if (process != null && process.isAlive()) {
				process.destroy();
			}
		}
	}
}
