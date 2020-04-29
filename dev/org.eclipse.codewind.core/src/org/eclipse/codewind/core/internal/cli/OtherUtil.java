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

import org.eclipse.codewind.core.internal.Logger;
import org.eclipse.codewind.core.internal.ProcessHelper;
import org.eclipse.codewind.core.internal.ProcessHelper.ProcessResult;
import org.eclipse.codewind.core.internal.launch.UtilityLaunchConfigDelegate;
import org.eclipse.codewind.core.internal.messages.Messages;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.osgi.util.NLS;
import org.json.JSONException;
import org.json.JSONObject;

public class OtherUtil {

	private static final String[] LOGLEVELS_CMD = new String[] {"loglevels"};
	private static final String[] DIAGNOSTICS_CMD = new String[] {"diagnostics"};
	
	private static final String ECLIPSE_WORKSPACE_OPTION = "--eclipseWorkspaceDir";
	private static final String PROJECTS_OPTION = "--projects";
	
	public static LogLevels getLoglevels(String connectionName, String conid, IProgressMonitor monitor) throws IOException, JSONException, TimeoutException {
		SubMonitor mon = SubMonitor.convert(monitor, NLS.bind(Messages.FetchLogLevelsTaskLabel, connectionName), 100);
		Process process = null;
		try {
			process = CLIUtil.runCWCTL(CLIUtil.GLOBAL_JSON_INSECURE, LOGLEVELS_CMD, new String[] {CLIUtil.CON_ID_OPTION, conid});
			ProcessResult result = ProcessHelper.waitForProcess(process, 500, 300, mon);
			CLIUtil.checkResult(LOGLEVELS_CMD, result, true);
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
			CLIUtil.checkResult(LOGLEVELS_CMD, result, true);
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
	
	public static ILaunch startDiagnostics(String connectionName, String conid, boolean includeEclipseWorkspace, boolean includeProjectInfo, IProgressMonitor monitor) throws IOException, CoreException {
		List<String> options = new ArrayList<String>();
		options.add(CLIUtil.CON_ID_OPTION);
		options.add(conid);
		if (includeEclipseWorkspace) {
			options.add(ECLIPSE_WORKSPACE_OPTION);
			options.add(ResourcesPlugin.getWorkspace().getRoot().getLocation().toOSString());
		}
		if (includeProjectInfo) {
			options.add(PROJECTS_OPTION);
		}
		List<String> command = CLIUtil.getCWCTLCommandList(null, DIAGNOSTICS_CMD, options.toArray(new String[options.size()]), null);
		
		ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
		ILaunchConfigurationType launchConfigurationType = launchManager.getLaunchConfigurationType(UtilityLaunchConfigDelegate.LAUNCH_CONFIG_ID);
		ILaunchConfigurationWorkingCopy workingCopy = launchConfigurationType.newInstance((IContainer) null, connectionName);
		workingCopy.setAttribute(UtilityLaunchConfigDelegate.TITLE_ATTR, Messages.UtilGenDiagnosticsTitle);
		workingCopy.setAttribute(UtilityLaunchConfigDelegate.COMMAND_ATTR, command);
		ILaunchConfiguration launchConfig = workingCopy.doSave();
		return launchConfig.launch(ILaunchManager.RUN_MODE, monitor);
	}
}
