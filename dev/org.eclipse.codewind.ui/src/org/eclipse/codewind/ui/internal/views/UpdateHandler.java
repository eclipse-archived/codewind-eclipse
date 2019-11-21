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
import org.eclipse.codewind.core.internal.connection.CodewindConnectionManager;

/**
 * Update handler registered on the Codewind core plug-in in order to keep
 * the Codewind view up to date.  Listeners can also register to be kept up
 * to date.
 */
public class UpdateHandler implements IUpdateHandler {
	
	private HashMap<AppKey, AppUpdateListener> appListeners = new HashMap<AppKey, AppUpdateListener>();
	
	@Override
	public void updateAll() {
		ViewHelper.refreshCodewindExplorerView(null);
		updateApps();
	}

	@Override
	public void updateConnection(CodewindConnection connection) {
		ViewHelper.refreshCodewindExplorerView(connection);
		ViewHelper.expandConnection(connection);
		updateApps(connection);
	}
	
	private void updateApps() {
		CodewindConnectionManager.activeConnections().stream().forEach(conn -> updateApps(conn));
	}
	
	private void updateApps(CodewindConnection conn) {
		if (conn != null) {
			conn.getApps().stream().forEach(app -> updateApplication(app));
		}
	}

	@Override
	public void updateApplication(CodewindApplication app) {
		ViewHelper.refreshCodewindExplorerView(app);
		ViewHelper.expandConnection(app.connection);
		synchronized(appListeners) {
			AppUpdateListener listener = appListeners.get(new AppKey(app));
			if (listener != null) {
				listener.update();
			}
		}
	}
	
	@Override
	public void removeConnection(List<CodewindApplication> apps) {
		ViewHelper.refreshCodewindExplorerView(null);
		synchronized(appListeners) {
			for (CodewindApplication app : apps) {
				AppUpdateListener listener = appListeners.get(new AppKey(app));
				if (listener != null) {
					listener.remove();
				}
			}
		}
	}

	@Override
	public void removeApplication(CodewindApplication app) {
		ViewHelper.refreshCodewindExplorerView(app.connection);
		ViewHelper.expandConnection(app.connection);
		synchronized(appListeners) {
			AppUpdateListener listener = appListeners.get(new AppKey(app));
			if (listener != null) {
				listener.remove();
			}
		}
	}

	public void addAppUpdateListener(String connectionId, String projectID, AppUpdateListener listener) {
		synchronized(appListeners) {
			appListeners.put(new AppKey(connectionId, projectID), listener);
		}
	}
	
	public void removeAppUpdateListener(String connectionId, String projectID) {
		synchronized(appListeners) {
			appListeners.remove(new AppKey(connectionId, projectID));
		}
	}
	
	public interface AppUpdateListener {
		public void update();
		public void remove();
	}
	
	private class AppKey {
		public final String connectionId;
		public final String projectId;
		
		public AppKey(CodewindApplication app) {
			this(app.connection.getConid(), app.projectID);
		}
		
		public AppKey(String connectionId, String projectID) {
			this.connectionId = connectionId;
			this.projectId = projectID;
		}
		
		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof AppKey)) {
				return false;
			}
			if (obj == this) {
				return true;
			}
			AppKey key = (AppKey)obj;
			return this.connectionId.equals(key.connectionId) && this.projectId.equals(key.projectId);
		}

		@Override
		public int hashCode() {
			return connectionId.hashCode() * projectId.hashCode();
		}
	}

}
