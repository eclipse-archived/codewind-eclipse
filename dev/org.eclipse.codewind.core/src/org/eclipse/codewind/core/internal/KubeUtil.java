/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.codewind.core.internal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.codewind.core.internal.launch.CodewindLaunchConfigDelegate;
import org.eclipse.codewind.core.internal.launch.UtilityLaunchConfigDelegate;
import org.eclipse.codewind.core.internal.messages.Messages;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.osgi.util.NLS;

public class KubeUtil {
	
	private static final String KUBECTL_CMD = "kubectl"; //$NON-NLS-1$
	private static final String KUBECTL_EXE = "kubectl.exe"; //$NON-NLS-1$
	private static final String OC_CMD = "oc"; //$NON-NLS-1$
	private static final String OC_EXE = "oc.exe"; //$NON-NLS-1$
	
	private static String kubeCommand = null;
	
	public static String getCommand() {
		if (kubeCommand != null) {
			return kubeCommand;
		}
		boolean isWindows = CoreUtil.isWindows();
		String exec = CoreUtil.getExecutablePath(isWindows ? KUBECTL_EXE : KUBECTL_CMD);
		if (exec == null) {
			exec = CoreUtil.getExecutablePath(isWindows ? OC_EXE : OC_CMD);
		}
		kubeCommand = exec;
		return kubeCommand;
	}
	
	public static PortForwardInfo startPortForward(CodewindApplication app, int port) throws Exception {
		// Check the app
		if (app.getPodName() == null || app.getNamespace() == null) {
			// This should not happen
			Logger.logError("Trying to port forward for the " + app.name + " project but the pod name or the namespace is null");
			return null;
		}

		// Get the command
		String processPath = KubeUtil.getCommand();
		if (processPath == null) {
			Logger.logError("Port forwarding cannot be initiated because neither of the kubectl or oc commands could be found on the path");
			throw new IOException(Messages.ErrorNoKubectlMsg);
		}

		// Find a free port
		int localPort = PlatformUtil.findFreePort();
		if (localPort == -1) {
			Logger.logError("Could not find a free port to use for port forwarding for project: " + app.name);
			return null;
		}

		String portMapping = localPort + ":" + port; //$NON-NLS-1$
		List<String> commandList = new ArrayList<String>();
		commandList.add(processPath);
		commandList.add("port-forward");
		commandList.add("-n");
		commandList.add(app.getNamespace());
		commandList.add(app.getPodName());
		commandList.add(portMapping);
		String title = NLS.bind(Messages.PortForwardTitle, portMapping);

		ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
		ILaunchConfigurationType launchConfigurationType = launchManager.getLaunchConfigurationType(UtilityLaunchConfigDelegate.LAUNCH_CONFIG_ID);
		ILaunchConfigurationWorkingCopy workingCopy = launchConfigurationType.newInstance((IContainer) null, app.name);
		workingCopy.setAttribute(CodewindLaunchConfigDelegate.CONNECTION_ID_ATTR, app.connection.getConid());
		workingCopy.setAttribute(CodewindLaunchConfigDelegate.PROJECT_ID_ATTR, app.projectID);
		workingCopy.setAttribute(UtilityLaunchConfigDelegate.TITLE_ATTR, title);
		workingCopy.setAttribute(UtilityLaunchConfigDelegate.COMMAND_ATTR, commandList);
		CodewindLaunchConfigDelegate.setConfigAttributes(workingCopy, app);
		ILaunchConfiguration launchConfig = workingCopy.doSave();
		ILaunch launch = launchConfig.launch(ILaunchManager.RUN_MODE, new NullProgressMonitor());

		return new PortForwardInfo(localPort, port, launch);
	}
	
	public static void endPortForward(CodewindApplication app, PortForwardInfo pfInfo) throws Exception {
		if (pfInfo != null && pfInfo.launch != null) {
			if (!pfInfo.launch.isTerminated()) {
				pfInfo.launch.terminate();
			}
		}
	}
	
	public static class PortForwardInfo {
		public final int localPort;
		public final int remotePort;
		public final ILaunch launch;
		
		public PortForwardInfo(int localPort, int remotePort, ILaunch launch) {
			this.localPort = localPort;
			this.remotePort = remotePort;
			this.launch = launch;
		}
	}

}
