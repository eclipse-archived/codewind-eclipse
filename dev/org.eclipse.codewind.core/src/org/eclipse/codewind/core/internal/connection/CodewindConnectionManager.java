/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
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

import org.eclipse.codewind.core.CodewindCorePlugin;
import org.eclipse.codewind.core.internal.Logger;
import org.eclipse.codewind.core.internal.CoreUtil;
import org.eclipse.codewind.core.internal.CodewindObjectFactory;;

/**
 * Singleton class to keep track of the list of current Codewind connections,
 * and manage persisting them to and from the Preferences.
 */
public class CodewindConnectionManager {
	
	public static final String DEFAULT_CONNECTION_URL = "http://localhost:9090";

	// Singleton instance. Never access this directly. Use the instance() method.
	private static CodewindConnectionManager instance;

	public static final String CONNECTION_LIST_PREFSKEY = "mcc-connections"; //$NON-NLS-1$

	private List<CodewindConnection> connections = new ArrayList<>();
	// this list tracks the URLs of connections that have never successfully connected
	private List<String> brokenConnections = new ArrayList<>();

	private CodewindConnectionManager() {
		instance = this;
		loadFromPreferences();

		// Add a preference listener to reload the cached list of connections each time it's modified.
//		CodewindCorePlugin.getDefault().getPreferenceStore()
//			.addPropertyChangeListener(new IPropertyChangeListener() {
//				@Override
//				public void propertyChange(PropertyChangeEvent event) {
//				    if (event.getProperty() == CodewindConnectionManager.CONNECTION_LIST_PREFSKEY) {
//				    	// MCLogger.log("Loading prefs in MCCM");
//				        loadFromPreferences();
//				    }
//				}
//			});
	}

	private static CodewindConnectionManager instance() {
		if (instance == null) {
			instance = new CodewindConnectionManager();
		}
		return instance;
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
		Logger.log("Added a new connection: " + connection.baseUrl); //$NON-NLS-1$
		instance().writeToPreferences();
	}

	/**
	 * @return An <b>unmodifiable</b> copy of the list of existing MC connections.
	 */
	public synchronized static List<CodewindConnection> activeConnections() {
		return Collections.unmodifiableList(instance().connections);
	}

	public synchronized static CodewindConnection getActiveConnection(String baseUrl) {
		for(CodewindConnection mcc : activeConnections()) {
			if(mcc.baseUrl.toString().equals(baseUrl)) {
				return mcc;
			}
		}
		return null;
	}

	public synchronized static int activeConnectionsCount() {
		return instance().connections.size();
	}

	/**
	 * Try to remove the given connection. Removal will fail if the connection is still in use (ie. has a linked app).
	 * @return
	 * 	true if the connection was removed,
	 * 	false if not because it didn't exist.
	 */
	synchronized static boolean remove(String baseUrl) {
		boolean removeResult = false;

		CodewindConnection connection = CodewindConnectionManager.getActiveConnection(baseUrl.toString());
		if (connection != null) {
			connection.close();
			removeResult = instance().connections.remove(connection);
		}
		else {
			removeResult = instance().brokenConnections.remove(baseUrl);
		}

		if (!removeResult) {
			Logger.logError("Tried to remove connection " + baseUrl + ", but it didn't exist"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		instance().writeToPreferences();
		CoreUtil.updateAll();
		return removeResult;
	}

	/**
	 * Deletes all of the instance's connections. Does NOT write to preferences after doing so.
	 */
	public synchronized static void clear() {
		Logger.log("Clearing " + instance().connections.size() + " connections"); //$NON-NLS-1$ //$NON-NLS-2$

		Iterator<CodewindConnection> it = instance().connections.iterator();

		while(it.hasNext()) {
			CodewindConnection connection = it.next();
			connection.close();
			it.remove();
		}
	}

	/**
	 * @return An <b>unmodifiable</b> copy of the list of broken MC Connection URLs.
	 */
	public synchronized static List<String> brokenConnections() {
		return Collections.unmodifiableList(instance().brokenConnections);
	}

	public synchronized static String getBrokenConnection(String url) {
		for (String brokenConnectionUrl : brokenConnections()) {
			if (brokenConnectionUrl.toString().equals(url)) {
				return brokenConnectionUrl;
			}
		}
		return null;
	}

	// Preferences serialization
	private void writeToPreferences() {
		StringBuilder prefsBuilder = new StringBuilder();

		for (CodewindConnection mcc : activeConnections()) {
			prefsBuilder.append(mcc.toPrefsString()).append('\n');
		}
		for (String mcc : brokenConnections()) {
			prefsBuilder.append(mcc).append('\n');
		}

		Logger.log("Writing connections to preferences: " + prefsBuilder.toString()); //$NON-NLS-1$

		CodewindCorePlugin.getDefault().getPreferenceStore()
				.setValue(CONNECTION_LIST_PREFSKEY, prefsBuilder.toString());
	}

	private void loadFromPreferences() {
		clear();

		String storedConnections = CodewindCorePlugin.getDefault()
				.getPreferenceStore()
				.getString(CONNECTION_LIST_PREFSKEY).trim();

		Logger.log("Reading connections from preferences: \"" + storedConnections + "\""); //$NON-NLS-1$ //$NON-NLS-2$

		for(String line : storedConnections.split("\n")) { //$NON-NLS-1$
			line = line.trim();
			if(line.isEmpty()) {
				continue;
			}

			try {
				// Assume all connections are active. If they are broken they will be handled in the catch below.
				URI uri = new URI(line);
				CodewindConnection connection = CodewindObjectFactory.createCodewindConnection(uri);
				add(connection);
			}
			catch (CodewindConnectionException mce) {
				// The MC instance we wanted to connect to is down.
				brokenConnections.add(mce.connectionUrl.toString());
				CodewindReconnectJob.createAndStart(mce.connectionUrl);
			}
			catch (Exception e) {
				Logger.logError("Error loading connection from preferences", e); //$NON-NLS-1$
			}
		}

	}

	public static boolean removeConnection(String connectionUrl) {
		CodewindConnectionManager.remove(connectionUrl);
		return true;
	}
	
	public static CodewindConnection createConnection(String uriStr) throws Exception {
		return CodewindObjectFactory.createCodewindConnection(new URI(uriStr));
	}

}
