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
	
	private HashMap<AppKey, AppUpdateListener> appListeners = new HashMap<AppKey, AppUpdateListener>();
	
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

	public void addAppUpdateListener(CodewindConnection conn, String projectID, AppUpdateListener listener) {
		synchronized(appListeners) {
			appListeners.put(new AppKey(conn, projectID), listener);
		}
	}
	
	public void removeAppUpdateListener(CodewindConnection conn, String projectID) {
		synchronized(appListeners) {
			appListeners.remove(new AppKey(conn, projectID));
		}
	}
	
	public interface AppUpdateListener {
		public void update();
		public void remove();
	}
	
	private class AppKey {
		public final String connectionURI;
		public final String projectID;
		
		public AppKey(CodewindApplication app) {
			this(app.connection, app.projectID);
		}
		
		public AppKey(CodewindConnection conn, String projectID) {
			this.connectionURI = conn.baseUrl.toString();
			this.projectID = projectID;
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
			return this.connectionURI.equals(key.connectionURI) && this.projectID.equals(key.projectID);
		}

		@Override
		public int hashCode() {
			return connectionURI.hashCode() * projectID.hashCode();
		}
	}

}
