/*******************************************************************************
 * Copyright (c) 2018, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.codewind.ui.internal.views;

import java.util.HashMap;
import java.util.List;

import org.eclipse.codewind.core.internal.CodewindApplication;
import org.eclipse.codewind.core.internal.IUpdateHandler;
import org.eclipse.codewind.core.internal.connection.CodewindConnection;

/**
 * Update handler registered on the Codewind core plug-in in order to keep
 * the Codewind view up to date.  Listeners can also register to be kept up
 * to date.
 */
public class UpdateHandler implements IUpdateHandler {
	
	private HashMap<String, AppUpdateListener> appListeners = new HashMap<String, AppUpdateListener>();
	
	@Override
	public void updateAll() {
		ViewHelper.refreshCodewindExplorerView(null);
	}

	@Override
	public void updateConnection(CodewindConnection connection) {
		ViewHelper.refreshCodewindExplorerView(connection);
		ViewHelper.expandConnection(connection);
	}

	@Override
	public void updateApplication(CodewindApplication app) {
		ViewHelper.refreshCodewindExplorerView(app);
		ViewHelper.expandConnection(app.connection);
		synchronized(appListeners) {
			AppUpdateListener listener = appListeners.get(app.projectID);
			if (listener != null) {
				listener.update(app);
			}
		}
	}
	
	@Override
	public void removeConnection(List<CodewindApplication> apps) {
		ViewHelper.refreshCodewindExplorerView(null);
		synchronized(appListeners) {
			for (CodewindApplication app : apps) {
				AppUpdateListener listener = appListeners.get(app.projectID);
				if (listener != null) {
					listener.remove(app);
				}
			}
		}
	}

	@Override
	public void removeApplication(CodewindApplication app) {
		ViewHelper.refreshCodewindExplorerView(app.connection);
		ViewHelper.expandConnection(app.connection);
		synchronized(appListeners) {
			AppUpdateListener listener = appListeners.get(app.projectID);
			if (listener != null) {
				listener.remove(app);
			}
		}
	}

	public void addAppUpdateListener(String projectID, AppUpdateListener listener) {
		synchronized(appListeners) {
			appListeners.put(projectID, listener);
		}
	}
	
	public void removeAppUpdateListener(String projectID) {
		synchronized(appListeners) {
			appListeners.remove(projectID);
		}
	}
	
	public interface AppUpdateListener {
		public void update(CodewindApplication app);
		public void remove(CodewindApplication app);
	}

}
