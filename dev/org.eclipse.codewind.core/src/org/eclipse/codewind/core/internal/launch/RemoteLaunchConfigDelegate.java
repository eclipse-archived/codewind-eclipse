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

package org.eclipse.codewind.core.internal.launch;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.codewind.core.internal.KubeUtil;
import org.eclipse.codewind.core.internal.KubeUtil.PortForwardInfo;
import org.eclipse.codewind.core.internal.Logger;
import org.eclipse.codewind.core.internal.PlatformUtil;
import org.eclipse.codewind.core.internal.RemoteEclipseApplication;
import org.eclipse.codewind.core.internal.messages.Messages;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IDebugEventSetListener;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.RuntimeProcess;
import org.eclipse.osgi.util.NLS;

public class RemoteLaunchConfigDelegate extends CodewindLaunchConfigDelegate {
	
	public static final String LAUNCH_CONFIG_ID = "org.eclipse.codewind.core.internal.remoteLaunchConfigurationType";

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

	private void launchInner(ILaunchConfiguration config, String launchMode, ILaunch launch, IProgressMonitor monitor)
			throws Exception {

		RemoteEclipseApplication app = (RemoteEclipseApplication) getApp(config);

		if (app.getContainerDebugPort() <= 0) {
			String msg = "The container debug port is not set up for application: " + app.name; // $NON-NLS-1$
			Logger.logError(msg);
			abort(msg, null, IStatus.ERROR);
		}

		// Find a free port
		int localPort = PlatformUtil.findFreePort();
		if (localPort <= 0) {
			String msg = "Could not find a free port for port forwarding the debug port for launch config: " + config.getName(); // $NON-NLS-1$
			Logger.logError(msg);
			abort(msg, null, IStatus.ERROR);
		}

		// Start the port forward
		PortForwardInfo pfInfo = KubeUtil.startPortForward(app, localPort, app.getContainerDebugPort());
		app.setDebugPFInfo(pfInfo);
		Map<String, String> attributes = new HashMap<String, String>();
		attributes.put(IProcess.ATTR_PROCESS_TYPE, "codewind.utility");
		String title = NLS.bind(Messages.PortForwardTitle, localPort + ":" + app.getContainerDebugPort());
		launch.addProcess(new RuntimeProcess(launch, pfInfo.process, title, attributes));

		addDebugEventListener(launch);

		// Launch the debug session
		super.launch(config, launchMode, launch, monitor);
	}
	
	public static void addDebugEventListener(final ILaunch launch) {
		// Add the debug listener
		DebugPlugin.getDefault().addDebugEventListener(new IDebugEventSetListener() {
			@Override
			public void handleDebugEvents(DebugEvent[] events) {
				for (DebugEvent event : events) {
					if (event.getKind() == DebugEvent.TERMINATE && event.getSource() instanceof IDebugTarget
							&& ((IDebugTarget) event.getSource()).getLaunch() == launch) {
						// Remove this listener
						DebugPlugin.getDefault().removeDebugEventListener(this);

						// Make sure the port forward is terminated
						Arrays.stream(launch.getProcesses()).filter(process -> !process.isTerminated()).forEach(process -> {
							try {
								process.terminate();
							} catch (DebugException e) {
								Logger.logError("An error occurred trying to terminate the process: " + process.getLabel(), e);
							}
						});

						// No need to process the rest of the events
						break;
					}
				}
			}
		});
	}
	
}
