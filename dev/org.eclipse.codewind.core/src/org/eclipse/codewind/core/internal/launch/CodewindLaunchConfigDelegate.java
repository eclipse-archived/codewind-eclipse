/*******************************************************************************
 * Copyright (c) 2018, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.codewind.core.internal.launch;

import org.eclipse.codewind.core.internal.CodewindApplication;
import org.eclipse.codewind.core.internal.CodewindEclipseApplication;
import org.eclipse.codewind.core.internal.CoreUtil;
import org.eclipse.codewind.core.internal.Logger;
import org.eclipse.codewind.core.internal.connection.CodewindConnection;
import org.eclipse.codewind.core.internal.connection.CodewindConnectionManager;
import org.eclipse.codewind.core.internal.messages.Messages;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.jdt.launching.AbstractJavaLaunchConfigurationDelegate;

public class CodewindLaunchConfigDelegate extends AbstractJavaLaunchConfigurationDelegate {
	
	public static final String LAUNCH_CONFIG_ID = "org.eclipse.codewind.core.internal.launchConfigurationType";
	
	public static final String PROJECT_NAME_ATTR = "org.eclipse.codewind.core.internal.projectNameAttr";
	public static final String PROJECT_ID_ATTR = "org.eclipse.codewind.core.internal.projectIdAttr";
	public static final String CONNECTION_ID_ATTR = "org.eclipse.codewind.core.internal.connectionIdAttr";
	
	@Override
	public void launch(ILaunchConfiguration config, String launchMode, ILaunch launch, IProgressMonitor monitor)
			throws CoreException {

		try {
			launchInner(config, launchMode, launch, monitor);
		} catch (Exception e) {
			String msg = "An error occurred trying to connect the debugger for launch configuration: " + config.getName(); // $NON-NLS-1$
			Logger.logError(msg, e);
			monitor.setCanceled(true);
			getLaunchManager().removeLaunch(launch);
			if (e instanceof CoreException) {
				throw (CoreException) e;
			}
			abort(msg, e, IStatus.ERROR);  
		}
	}

	private void launchInner(ILaunchConfiguration config, String launchMode, ILaunch launch, IProgressMonitor monitor) throws Exception {
		
		CodewindEclipseApplication app = getApp(config);
		
		if (app.getDebugConnectHost() == null || app.getDebugConnectPort() <= 0) {
			String msg = "The debug connect host or debug connect port is not set up for application: " + app.name; // $NON-NLS-1$
			Logger.logError(msg);
			abort(msg, null, IStatus.ERROR);
		}
		
		// Make sure the saved launch is up to date in case this is a relaunch
		app.setLaunch(launch);
					
		setDefaultSourceLocator(launch, config);

		Logger.log("Connecting the debugger for project: " + app.name); //$NON-NLS-1$
		IDebugTarget debugTarget = CodewindDebugConnector.connectDebugger(launch, app, monitor);
		if (debugTarget != null) {
			Logger.log("Debugger connect success. Application should go into Debugging state soon."); //$NON-NLS-1$
			launch.addDebugTarget(debugTarget);
		} else if (!monitor.isCanceled()) {
			Logger.logError("Debugger connect timeout for project: " + app.name); //$NON-NLS-1$
			CoreUtil.openDialog(true, Messages.DebuggerConnectFailureDialogTitle, Messages.DebuggerConnectFailureDialogMsg);
			getLaunchManager().removeLaunch(launch);
		}

		monitor.done();
	}
	
	protected CodewindEclipseApplication getApp(ILaunchConfiguration config) throws Exception {
		String connId = config.getAttribute(CONNECTION_ID_ATTR, (String) null);
		String projectId = config.getAttribute(PROJECT_ID_ATTR, (String) null);
		if (connId == null || projectId == null) {
			Logger.logError("Expected attributes were not found for launch configuration: " + config.getName());
			return null;
		}
		CodewindConnection conn = CodewindConnectionManager.getConnectionById(connId);
		CodewindApplication app = conn == null ? null : conn.getAppByID(projectId);
		if (!(app instanceof CodewindEclipseApplication)) {
			String msg = "Could not find the application associated with launch configuration: " + config.getName(); // $NON-NLS-1$
			Logger.logError(msg);
			abort(msg, null, IStatus.ERROR);
		}
		return (CodewindEclipseApplication) app;
	}
	
	public static void setConfigAttributes(ILaunchConfigurationWorkingCopy config, CodewindApplication app) {
		config.setAttribute(PROJECT_NAME_ATTR, app.name);
		config.setAttribute(PROJECT_ID_ATTR, app.projectID);
		config.setAttribute(CONNECTION_ID_ATTR, app.connection.getConid());
	}
}
