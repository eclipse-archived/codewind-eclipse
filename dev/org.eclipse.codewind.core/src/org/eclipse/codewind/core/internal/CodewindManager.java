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

import org.eclipse.codewind.core.CodewindCorePlugin;
import org.eclipse.codewind.core.internal.cli.InstallStatus;
import org.eclipse.codewind.core.internal.cli.InstallUtil;
import org.eclipse.codewind.core.internal.connection.CodewindConnection;
import org.eclipse.codewind.core.internal.connection.CodewindConnectionManager;
import org.eclipse.codewind.core.internal.messages.Messages;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.osgi.util.NLS;
import org.json.JSONException;

public class CodewindManager {
	
	private static CodewindManager codewindManager;
	
	CodewindConnection localConnection = null;
	
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
		localConnection = CodewindObjectFactory.createLocalConnection(Messages.CodewindLocalConnectionName, null);
		CodewindConnectionManager.add(localConnection);
		refreshInstallStatus(new NullProgressMonitor());
		if (installStatus.isStarted()) {
			Job job = new Job(NLS.bind(Messages.Connection_JobLabel, localConnection.getName())) {
				@Override
				protected IStatus run(IProgressMonitor monitor) {
					try {
						localConnection.connect(monitor);
						return Status.OK_STATUS;
					} catch (Exception e) {
						Logger.logError("An error occurred trying to connect to the local Codewind instance at:" + localConnection.getBaseURI(), e); //$NON-NLS-1$
						return new Status(IStatus.ERROR, CodewindCorePlugin.PLUGIN_ID, NLS.bind(Messages.Connection_JobError, new String[] {localConnection.getName(), localConnection.getBaseURI().toString()}), e);
					}
				}
			};
			job.schedule();
		}
	}

	public static synchronized CodewindManager getManager() {
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
	
	public synchronized void refreshInstallStatus(IProgressMonitor monitor) {
		String urlStr = null;
		try {
			SubMonitor mon = SubMonitor.convert(monitor, 100);
			installStatus = InstallUtil.getInstallStatus(mon.split(60));
			if (installStatus.isStarted()) {
				urlStr = installStatus.getURL();
				if (!localConnection.isConnected()) {
					URI uri = new URI(urlStr);
					localConnection.setBaseURI(uri);
					localConnection.connect(mon.split(40));
				}
			} else {
				localConnection.disconnect();
				localConnection.setBaseURI(null);
			}
			return;
		} catch (IOException e) {
			Logger.logError("An error occurred trying to get the installer status", e); //$NON-NLS-1$
		} catch (TimeoutException e) {
			Logger.logError("Timed out trying to get the installer status", e); //$NON-NLS-1$
		} catch (JSONException e) {
			Logger.logError("The Codewind installer status format is not recognized", e); //$NON-NLS-1$
		} catch (URISyntaxException e) {
			Logger.logError("The Codewind installer status command returned an invalid url: " + urlStr, e);
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

	public boolean isSupportedVersion(String version) {
		return CodewindConnection.isSupportedVersion(version);
	}
	
	public synchronized CodewindConnection getLocalConnection() {
		return localConnection;
	}

	public void refresh(IProgressMonitor monitor) {
		refreshInstallStatus(monitor);
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
