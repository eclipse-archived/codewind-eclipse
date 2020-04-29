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
import org.eclipse.codewind.core.internal.constants.ProjectLanguage;
import org.eclipse.codewind.core.internal.constants.ProjectType;
import org.eclipse.codewind.core.internal.constants.StartMode;
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
	public void connectDebugger() {
		try {
			debugPFInfo = KubeUtil.startPortForward(this, getContainerDebugPort());
		} catch (Exception e) {
			Logger.logError("An error occurred trying to port forward the debug port for application: " + name, e); //$NON-NLS-1$
			CoreUtil.openDialog(CoreUtil.DialogType.ERROR, Messages.RemoteDebugErrorTitle, NLS.bind(Messages.RemoteDebugPortForwardErrorWithMsg, name, e.getMessage()));
			return;
		}
		if (debugPFInfo == null || debugPFInfo.localPort == -1) {
			Logger.logError("Port forwarding of the debug port returned -1 for the local port for application: " + name); //$NON-NLS-1$
			CoreUtil.openDialog(CoreUtil.DialogType.ERROR, Messages.RemoteDebugErrorTitle, NLS.bind(Messages.RemoteDebugPortForwardError, name));
			debugPFInfo = null;
			return;
		}
		Logger.log("Port forwarding was successful for the debug port of the " + name + " application: " + debugPFInfo.localPort + ":" + debugPFInfo.remotePort); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		CoreUtil.updateApplication(this);
		super.connectDebugger();
	}

	@Override
	public void clearDebugger() {
		super.clearDebugger();
		cleanupPortForwarding();
		CoreUtil.updateApplication(this);
	}
	
	private void cleanupPortForwarding() {
		if (debugPFInfo != null) {
			// Clear out the port forwarding info to indicate that it was ended internally
			PortForwardInfo info = debugPFInfo;
			debugPFInfo = null;
			try {
				Logger.log("Ending port forwarding for the " + name + " application: " + debugPFInfo.localPort + ":" + debugPFInfo.remotePort); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				KubeUtil.endPortForward(this, info);
			} catch (Exception e) {
				Logger.logError("An error occurred trying to terminate the debug port forward for: " + name, e); //$NON-NLS-1$
			}
		}
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
		}
		clearDebugger();
	}

}
