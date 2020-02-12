/*******************************************************************************
 * Copyright (c) 2018, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.codewind.core.internal.connection;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.codewind.core.CodewindCorePlugin;
import org.eclipse.codewind.core.internal.CodewindManager;
import org.eclipse.codewind.core.internal.CodewindObjectFactory;
import org.eclipse.codewind.core.internal.CoreUtil;
import org.eclipse.codewind.core.internal.Logger;
import org.eclipse.codewind.core.internal.cli.AuthToken;
import org.eclipse.codewind.core.internal.cli.AuthUtil;
import org.eclipse.codewind.core.internal.cli.ConnectionInfo;
import org.eclipse.codewind.core.internal.cli.ConnectionUtil;
import org.eclipse.codewind.core.internal.messages.Messages;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.osgi.util.NLS;

/**
 * Singleton class to keep track of the list of current Codewind connections,
 * and manage persisting them to and from the Preferences.
 */
public class CodewindConnectionManager {
	
	public static final String RESTORE_CONNECTIONS_FAMILY = CodewindCorePlugin.PLUGIN_ID + ".restoreConnectionsFamily";;
	
	// Singleton instance. Never access this directly. Use the instance() method.
	private static CodewindConnectionManager instance;

	private List<CodewindConnection> connections = new ArrayList<>();
	private CodewindConnection localConnection = null;

	private CodewindConnectionManager() {
		instance = this;
		restoreConnections();
	}

	private static CodewindConnectionManager instance() {
		if (instance == null) {
			instance = new CodewindConnectionManager();
		}
		return instance;
	}

	public synchronized static CodewindConnection getLocalConnection() {
		return instance().localConnection;
	}

	/**
	 * Adds the given connection to the list of connections.
	 */
	public synchronized static void add(CodewindConnection connection) {
		if (connection == null) {
			Logger.logError("Null connection passed to be added"); //$NON-NLS-1$
			return;
		}

		instance().connections.add(connection);
		Logger.log("Added a new connection: " + connection.getBaseURI()); //$NON-NLS-1$
	}

	/**
	 * @return An <b>unmodifiable</b> copy of the list of existing MC connections.
	 */
	public synchronized static List<CodewindConnection> activeConnections() {
		return Collections.unmodifiableList(instance().connections);
	}

	public synchronized static CodewindConnection getActiveConnection(String baseUrl) {
		for(CodewindConnection conn : activeConnections()) {
			if(conn.getBaseURI() != null && conn.getBaseURI().toString().equals(baseUrl)) {
				return conn;
			}
		}
		return null;
	}
	
	public synchronized static List<CodewindConnection> activeRemoteConnections() {
		return activeConnections().stream().filter(conn -> !conn.isLocal()).collect(Collectors.toList());
	}
	
	public synchronized static CodewindConnection getConnectionById(String id) {
		for(CodewindConnection conn : activeConnections()) {
			if(conn.getBaseURI() != null && conn.getConid().equals(id)) {
				return conn;
			}
		}
		return null;
	}
	
	public synchronized static CodewindConnection getActiveConnectionByName(String name) {
		for(CodewindConnection conn : activeConnections()) {
			if(name != null && name.equals(conn.getName())) {
				return conn;
			}
		}
		return null;
	}

	public synchronized static int activeConnectionsCount() {
		return instance().connections.size();
	}

	/**
	 * Try to remove the given connection.
	 * @return
	 * 	true if the connection was removed,
	 * 	false if not because it didn't exist.
	 */
	public synchronized static boolean remove(String baseUrl) {
		boolean removeResult = false;

		CodewindConnection connection = CodewindConnectionManager.getActiveConnection(baseUrl.toString());
		if (connection != null) {
			connection.disconnect();
			removeResult = instance().connections.remove(connection);
			CoreUtil.removeConnection(connection);
			if (!connection.isLocal() && connection.getConid() != null) {
				try {
					ConnectionUtil.removeConnection(connection.getConid(), new NullProgressMonitor());
				} catch (Exception e) {
					Logger.logError("An error occurred trying to de-register the connection: " + connection.getName()); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
		}

		if (!removeResult) {
			Logger.logError("Tried to remove connection " + baseUrl + ", but it didn't exist"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		
		CoreUtil.updateAll();
		return removeResult;
	}

	/**
	 * Deletes all of the instance's connections. Called when the plugin is stopped.
	 */
	public synchronized static void clear() {
		Logger.log("Clearing " + instance().connections.size() + " connections"); //$NON-NLS-1$ //$NON-NLS-2$

		Iterator<CodewindConnection> it = instance().connections.iterator();

		while(it.hasNext()) {
			CodewindConnection connection = it.next();
			connection.disconnect();
			it.remove();
		}
	}
	
	private void restoreConnections() {
		Job job = new Job(Messages.ConnectionManager_RestoreJobLabel) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				SubMonitor mon = SubMonitor.convert(monitor, 100);
				
				// Make sure the local connection is first in the list
				localConnection = CodewindObjectFactory.createLocalConnection(null);
				connections.add(localConnection);
				try {
					// This will connect if Codewind is running
					CodewindManager.getManager().refreshInstallStatus(mon.split(20));
				} catch (Exception e) {
					Logger.logError("An error occurred trying to connect to the local Codewind instance", e); //$NON-NLS-1$
				}
				
				// Add the rest of the connections, skipping local
				try {
					List<ConnectionInfo> infos = ConnectionUtil.listConnections(mon.split(20));
					MultiStatus multiStatus = new MultiStatus(CodewindCorePlugin.PLUGIN_ID, 0, null, null);
					if (infos.size() > 1) {
						mon.setWorkRemaining(100 * (infos.size() - 1));
					}
					for (ConnectionInfo info : infos) {
						try {
							if (!info.isLocal()) {
								URI uri = new URI(info.getURL());
								AuthToken auth = null;
								try {
									auth = AuthUtil.getAuthToken(info.getUsername(), info.getId(), mon.split(20));
								} catch (Exception e) {
									Logger.logError("An error occurred trying to get the authorization token for: " + info.getId(), e); //$NON-NLS-1$
								}
								if (mon.isCanceled()) {
									return Status.CANCEL_STATUS;
								}
								CodewindConnection conn = CodewindObjectFactory.createRemoteConnection(info.getLabel(), uri, info.getId(), info.getUsername(), auth);
								connections.add(conn);
								if (auth != null) {
									conn.connect(mon.split(80));
								}
								if (mon.isCanceled()) {
									return Status.CANCEL_STATUS;
								}
							}
						} catch (Exception e) {
							IStatus status = new Status(IStatus.ERROR, CodewindCorePlugin.PLUGIN_ID, NLS.bind(Messages.ConnectionManager_RestoreConnError, new String[] {info.getLabel(), info.getURL()}), e);
							multiStatus.add(status);
						} finally {
							CoreUtil.updateAll();
						}
					}
					return multiStatus;
				} catch (Exception e) {
					Logger.logError("An error occurred trying to restore the connections", e); //$NON-NLS-1$
					return new Status(IStatus.ERROR, CodewindCorePlugin.PLUGIN_ID, Messages.ConnectionManager_RestoreGeneralError, e);
				}
			}

			@Override
			public boolean belongsTo(Object family) {
				return RESTORE_CONNECTIONS_FAMILY.equals(family);
			}
		};
		job.schedule();
	}
}
