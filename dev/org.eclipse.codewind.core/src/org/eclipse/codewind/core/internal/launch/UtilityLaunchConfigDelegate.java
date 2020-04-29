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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.codewind.core.CodewindCorePlugin;
import org.eclipse.codewind.core.internal.CoreUtil;
import org.eclipse.codewind.core.internal.Logger;
import org.eclipse.codewind.core.internal.messages.Messages;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.LaunchConfigurationDelegate;
import org.eclipse.debug.core.model.RuntimeProcess;
import org.eclipse.osgi.util.NLS;

public class UtilityLaunchConfigDelegate extends LaunchConfigurationDelegate {

	public static final String LAUNCH_CONFIG_ID = "org.eclipse.codewind.core.internal.utilityLaunchConfigurationType";

	public static final String TITLE_ATTR = "org.eclipse.codewind.core.internal.titleAttr";
	public static final String COMMAND_ATTR = "org.eclipse.codewind.core.internal.commandAttr";

	@Override
	public void launch(ILaunchConfiguration config, String launchMode, ILaunch launch, IProgressMonitor monitor)
			throws CoreException {

		String[] command = new String[0];
		SubMonitor mon = SubMonitor.convert(monitor, 100);

		try {
			List<String> commandList = config.getAttribute(COMMAND_ATTR, (List<String>) null);
			if (commandList == null || commandList.isEmpty()) {
				// This should not happen
				Logger.logError("There was a missing or invalid command attribute for the utility launch");
				return;
			}
			command = commandList.toArray(new String[commandList.size()]);
			String title = config.getAttribute(TITLE_ATTR, (String) null);
			mon.worked(30);

			ProcessBuilder builder = new ProcessBuilder(command);
			Process p = builder.start();
			mon.worked(30);

			Map<String, String> attributes = new HashMap<String, String>();
			attributes.put(IProcess.ATTR_PROCESS_TYPE, "codewind.utility");
			IProcess process = new RuntimeProcess(launch, p, title, attributes);
			launch.addProcess(process);
			mon.worked(40);
		} catch (Exception e) {
			monitor.setCanceled(true);
			DebugPlugin.getDefault().getLaunchManager().removeLaunch(launch);
			if (e instanceof CoreException) {
				throw (CoreException) e;
			}
			IStatus status = new Status(IStatus.ERROR, CodewindCorePlugin.PLUGIN_ID, NLS.bind(Messages.UtilityLaunchError, CoreUtil.formatString(command, " ")), e); //$NON-NLS-1$
			throw new CoreException(status);
		}
	}
}
