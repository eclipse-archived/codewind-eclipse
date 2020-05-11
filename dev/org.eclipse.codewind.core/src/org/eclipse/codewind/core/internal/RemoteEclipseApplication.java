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

import java.net.MalformedURLException;

import org.eclipse.codewind.core.CodewindCorePlugin;
import org.eclipse.codewind.core.internal.KubeUtil.PortForwardInfo;
import org.eclipse.codewind.core.internal.connection.CodewindConnection;
import org.eclipse.codewind.core.internal.constants.AppStatus;
import org.eclipse.codewind.core.internal.constants.ProjectLanguage;
import org.eclipse.codewind.core.internal.constants.ProjectType;
import org.eclipse.codewind.core.internal.constants.StartMode;
import org.eclipse.codewind.core.internal.launch.RemoteLaunchConfigDelegate;
import org.eclipse.codewind.core.internal.messages.Messages;
import org.eclipse.core.runtime.IPath;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;

public class RemoteEclipseApplication extends CodewindEclipseApplication {
	
	private static final String NOTIFY_PORT_FORWARD_TERMINATED_PREFSKEY = "portForwardTerminated";
	
	private PortForwardInfo debugPFInfo = null;
	
	RemoteEclipseApplication(CodewindConnection connection, String id, String name,
			ProjectType projectType, ProjectLanguage language, IPath localPath)
					throws MalformedURLException {
		super(connection, id, name, projectType, language, localPath);
	}
	
	@Override
	public boolean readyForDebugSession() {
		return StartMode.DEBUG_MODES.contains(getStartMode()) && getContainerDebugPort() != -1;
	}

	@Override
	public String getLaunchConfigId() {
		return RemoteLaunchConfigDelegate.LAUNCH_CONFIG_ID;
	}

	@Override
	public void clearDebugger() {
		super.clearDebugger();
		if (debugPFInfo != null) {
			debugPFInfo.terminate();
			debugPFInfo = null;
		}
		CoreUtil.updateApplication(this);
	}

	public synchronized void setDebugPFInfo(PortForwardInfo info) {
		debugPFInfo = info;
	}
	
	@Override
	public synchronized int getDebugConnectPort() {
		return debugPFInfo == null ? -1 : debugPFInfo.localPort;
	}

	@Override
	public synchronized String getDebugConnectHost() {
		return "localhost";
	}
	
	@Override
	public void launchTerminated(ILaunch launch) {
		if (debugPFInfo != null && launch == debugPFInfo.launch) {
			// The user terminated the port forwarding
			IPreferenceStore prefs = CodewindCorePlugin.getDefault().getPreferenceStore();
			if (!prefs.getBoolean(NOTIFY_PORT_FORWARD_TERMINATED_PREFSKEY)) {
				Display.getDefault().asyncExec(() -> {
					MessageDialogWithToggle portForwardEndDialog = MessageDialogWithToggle.openInformation(Display.getDefault().getActiveShell(),
							Messages.PortForwardTerminateTitle,
							NLS.bind(Messages.PortForwardTerminateMsg, new String[] {name, connection.getName(), Integer.toString(debugPFInfo.remotePort)}),
							Messages.PortForwardTerminateToggleMsg, false, null, null);
					prefs.setValue(NOTIFY_PORT_FORWARD_TERMINATED_PREFSKEY, portForwardEndDialog.getToggleState());
				});
			}
			debugPFInfo = null;
			CoreUtil.updateApplication(this);
		} else if (launch == getLaunch()) {
			// Make sure the port forward process is terminated
			if (debugPFInfo != null) {
				debugPFInfo.terminate();
				debugPFInfo = null;
			}
			CoreUtil.updateApplication(this);
		}
	}

	@Override
	public synchronized void setAppStatus(String appStatus, String appStatusDetails) {
		if (appStatus != null) {
			AppStatus oldStatus = getAppStatus();
			super.setAppStatus(appStatus, appStatusDetails);
			// Reconnect the debugger if necessary
			if (getAppStatus() == AppStatus.STARTING && oldStatus != AppStatus.STARTING
					&& StartMode.DEBUG_MODES.contains(getStartMode()) && getContainerDebugPort() != -1) {
				reconnectDebugger();
			}
		}
	}
}
