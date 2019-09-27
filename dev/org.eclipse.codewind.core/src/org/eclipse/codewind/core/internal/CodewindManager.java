/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
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
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.TimeoutException;

import org.eclipse.codewind.core.internal.connection.CodewindConnection;
import org.eclipse.codewind.core.internal.connection.CodewindConnectionManager;
import org.eclipse.codewind.core.internal.messages.Messages;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.json.JSONException;

public class CodewindManager {
	
	private static CodewindManager codewindManager;
	
	CodewindConnection localConnection = null;
	URI localURI = null;
	
	// Keep track of the install status and if the installer is currently running.
	// If the installer is running, this is the status that should be reported, if
	// not the install status should be reported (installerStatus will be null).
	InstallStatus installStatus = InstallStatus.UNKNOWN;
	InstallerStatus installerStatus = null;
	
	public enum InstallerStatus {
		INSTALLING,
		UNINSTALLING,
		STARTING,
		STOPPING
	};
	
	private CodewindManager() {
		refreshInstallStatus();
		if (getInstallStatus().isStarted()) {
			createLocalConnection();
			if (localConnection != null) {
				localConnection.refreshApps(null);
			}
		}
	}

	public static CodewindManager getManager() {
		if (codewindManager == null) {
			codewindManager = new CodewindManager();
		}
		return codewindManager;
	}
	
	/**
	 * Get the current install status for Codewind
	 */
	public InstallStatus getInstallStatus() {
		return installStatus;
	}
	
	public synchronized void refreshInstallStatus() {
		String url = null;
		try {
			installStatus = InstallUtil.getInstallStatus();
			if (installStatus.isStarted()) {
				url = installStatus.getURL();
				localURI = new URI(url);
			} else {
				removeLocalConnection();
				localURI = null;
			}
			return;
		} catch (IOException e) {
			Logger.logError("An error occurred trying to get the installer status", e); //$NON-NLS-1$
		} catch (TimeoutException e) {
			Logger.logError("Timed out trying to get the installer status", e); //$NON-NLS-1$
		} catch (JSONException e) {
			Logger.logError("The Codewind installer status format is not recognized", e); //$NON-NLS-1$
		} catch (URISyntaxException e) {
			Logger.logError("The Codewind installer status command returned an invalid url: " + url, e);
		}
		installStatus = InstallStatus.UNKNOWN;
	}
	
	public InstallerStatus getInstallerStatus() {
		return installerStatus;
	}
	
	public void setInstallerStatus(InstallerStatus status) {
		this.installerStatus = status;
		CoreUtil.updateAll();
	}
	
	public URI getLocalURI() {
		return localURI;
	}

	public boolean isSupportedVersion(String version) {
		return CodewindConnection.isSupportedVersion(version);
	}
	
	public synchronized CodewindConnection getLocalConnection() {
		return localConnection;
	}

	public synchronized CodewindConnection createLocalConnection() {
		if (localConnection != null) {
			return localConnection;
		}
		try {
			CodewindConnection connection = CodewindObjectFactory.createCodewindConnection(Messages.CodewindLocalConnectionName, getLocalURI(), true);
			connection.connect(new NullProgressMonitor());
			localConnection = connection;
			CodewindConnectionManager.add(connection);
			return connection;
		} catch(Exception e) {
			Logger.log("Attempting to connect to Codewind failed: " + e.getMessage()); //$NON-NLS-1$
		}
		return null;
	}
	
	public synchronized void removeLocalConnection() {
		if (localConnection != null) {
			localConnection.close();
			CodewindConnectionManager.removeConnection(localConnection.baseUrl.toString());
			localConnection = null;
		}
	}
	
	public void refresh() {
		refreshInstallStatus();
		for (CodewindConnection conn : CodewindConnectionManager.activeConnections()) {
			if (conn.isConnected()) {
				conn.refreshApps(null);
			}
		}
	}
	
	public boolean hasActiveApplications() {
		for (CodewindConnection conn : CodewindConnectionManager.activeConnections()) {
			for (CodewindApplication app : conn.getApps()) {
				if (app.isAvailable()) {
					return true;
				}
			}
		}
		return false;
	}
}
