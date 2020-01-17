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
import java.util.concurrent.TimeoutException;

import org.eclipse.codewind.core.internal.Logger;
import org.eclipse.codewind.core.internal.ProcessHelper;
import org.eclipse.codewind.core.internal.ProcessHelper.ProcessResult;
import org.eclipse.codewind.core.internal.messages.Messages;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.osgi.util.NLS;
import org.json.JSONException;
import org.json.JSONObject;

public class OtherUtil {

	private static final String[] LOGLEVELS_CMD = new String[] {"loglevels"};
	
	public static LogLevels getLoglevels(String connectionName, String conid, IProgressMonitor monitor) throws IOException, JSONException, TimeoutException {
		SubMonitor mon = SubMonitor.convert(monitor, NLS.bind(Messages.FetchLogLevelsTaskLabel, connectionName), 100);
		Process process = null;
		try {
			process = CLIUtil.runCWCTL(CLIUtil.GLOBAL_JSON_INSECURE, LOGLEVELS_CMD, new String[] {CLIUtil.CON_ID_OPTION, conid});
			ProcessResult result = ProcessHelper.waitForProcess(process, 500, 300, mon);
			if (result.getExitValue() != 0) {
				Logger.logError("Get loglevels failed with rc: " + result.getExitValue() + " and error: " + result.getErrorMsg()); //$NON-NLS-1$ //$NON-NLS-2$
				throw new IOException(result.getErrorMsg());
			}
			if (result.getOutput() == null || result.getOutput().trim().isEmpty()) {
				// This should not happen
				Logger.logError("Get loglevels had 0 return code but the output is empty"); //$NON-NLS-1$
				throw new IOException("The output from loglevels is empty."); //$NON-NLS-1$
			}
			JSONObject resultJson = new JSONObject(result.getOutput());
			return new LogLevels(resultJson);
		} finally {
			if (process != null && process.isAlive()) {
				process.destroy();
			}
		}
	}
	
	public static void setLoglevels(String connectionName, String level, String conid, IProgressMonitor monitor) throws IOException, JSONException, TimeoutException {
		SubMonitor mon = SubMonitor.convert(monitor, NLS.bind(Messages.SetLogLevelTaskLabel, new String[] {connectionName, level}), 100);
		Process process = null;
		try {
			process = CLIUtil.runCWCTL(CLIUtil.GLOBAL_JSON_INSECURE, LOGLEVELS_CMD, new String[] {CLIUtil.CON_ID_OPTION, conid}, new String[] {level});
			ProcessResult result = ProcessHelper.waitForProcess(process, 500, 300, mon);
			if (result.getExitValue() != 0) {
				Logger.logError("Set loglevels failed with rc: " + result.getExitValue() + " and error: " + result.getErrorMsg()); //$NON-NLS-1$ //$NON-NLS-2$
				throw new IOException(result.getErrorMsg());
			}
			if (result.getOutput() == null || result.getOutput().trim().isEmpty()) {
				// This should not happen
				Logger.logError("Set loglevels had 0 return code but the output is empty"); //$NON-NLS-1$
				throw new IOException("The output from loglevels is empty."); //$NON-NLS-1$
			}
			JSONObject resultJson = new JSONObject(result.getOutput());
			LogLevels logLevels = new LogLevels(resultJson);
			if (!logLevels.getCurrentLevel().equals(level)) {
				String msg = "The current log level is not what was requested, requested: " + level + ", actual: " + logLevels.getCurrentLevel(); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				Logger.logError(msg);
				throw new IOException(msg); //$NON-NLS-1$
			}
		} finally {
			if (process != null && process.isAlive()) {
				process.destroy();
			}
		}
	}
}
