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
	

	private static final String SECKEYRING_CMD = "seckeyring";
	private static final String UPDATE_OPTION = "update";
	
	private static final String SECTOKEN_CMD = "sectoken";
	private static final String GET_OPTION = "get";
	
	private static final String INSECURE_OPTION = "--insecure";
	private static final String USERNAME_OPTION = "--username";
	private static final String PASSWORD_OPTION = "--password";
	private static final String CON_ID_OPTION = "--conid";
	
	private static final String STATUS_KEY = "status";
	private static final String STATUS_MSG_KEY = "status_message";
	
	private static final String STATUS_OK_VALUE = "OK";
	
	public static AuthToken getAuthToken(String username, String password, String conid, IProgressMonitor monitor) throws IOException, JSONException, TimeoutException {
		SubMonitor mon = SubMonitor.convert(monitor, Messages.AuthorizingTaskLabel, 100);
		Process process = null;
		try {
			process = CLIUtil.runCWCTL(SECKEYRING_CMD, UPDATE_OPTION, USERNAME_OPTION, username, PASSWORD_OPTION, password, CON_ID_OPTION, conid);
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
			
			process = CLIUtil.runCWCTL(INSECURE_OPTION, SECTOKEN_CMD, GET_OPTION, USERNAME_OPTION, username, CON_ID_OPTION, conid);
			result = ProcessHelper.waitForProcess(process, 500, 60, mon.split(50));
			if (result.getExitValue() != 0) {
				Logger.logError("Sectoken get failed with rc: " + result.getExitValue() + " and error: " + result.getErrorMsg()); //$NON-NLS-1$ //$NON-NLS-2$
				throw new IOException(result.getErrorMsg());
			}
			if (result.getOutput() == null || result.getOutput().trim().isEmpty()) {
				// This should not happen
				Logger.logError("Sectoken get had 0 return code but the output is empty"); //$NON-NLS-1$
				throw new IOException("The output from sectoken get is empty."); //$NON-NLS-1$
			}
			resultJson = new JSONObject(result.getOutput());
			return new AuthToken(resultJson);
		} finally {
			if (process != null && process.isAlive()) {
				process.destroy();
			}
		}
	}
}
