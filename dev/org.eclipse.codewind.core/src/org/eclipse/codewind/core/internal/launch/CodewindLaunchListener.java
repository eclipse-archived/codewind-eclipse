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

import org.eclipse.codewind.core.internal.CodewindApplication;
import org.eclipse.codewind.core.internal.Logger;
import org.eclipse.codewind.core.internal.connection.CodewindConnection;
import org.eclipse.codewind.core.internal.connection.CodewindConnectionManager;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchesListener2;

public class CodewindLaunchListener implements ILaunchesListener2 {

	@Override
	public void launchesAdded(ILaunch[] launches) {
		// Ignore
	}

	@Override
	public void launchesChanged(ILaunch[] launches) {
		// Ignore
	}

	@Override
	public void launchesRemoved(ILaunch[] launches) {
		// Ignore
	}

	@Override
	public void launchesTerminated(ILaunch[] launches) {
		for (ILaunch launch : launches) {
			ILaunchConfiguration config = launch.getLaunchConfiguration();
			try {
				if (config.hasAttribute(CodewindLaunchConfigDelegate.CONNECTION_ID_ATTR) && config.hasAttribute(CodewindLaunchConfigDelegate.PROJECT_ID_ATTR)) {
					CodewindConnection conn = CodewindConnectionManager.getConnectionById(config.getAttribute(CodewindLaunchConfigDelegate.CONNECTION_ID_ATTR, ""));
					CodewindApplication app = conn == null ? null : conn.getAppByID(config.getAttribute(CodewindLaunchConfigDelegate.PROJECT_ID_ATTR, ""));
					if (app != null) {
						app.launchTerminated(launch);
					}
				}
			} catch (CoreException e) {
				Logger.logError("An error occurred trying to look up the application for a launch configuration", e);
			}
		}
	}

}
