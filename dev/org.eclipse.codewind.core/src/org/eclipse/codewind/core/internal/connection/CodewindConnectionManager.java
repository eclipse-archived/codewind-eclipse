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
import org.eclipse.codewind.core.internal.CodewindApplication;
import org.eclipse.codewind.core.internal.CodewindObjectFactory;
import org.eclipse.codewind.core.internal.CoreUtil;
import org.eclipse.codewind.core.internal.Logger;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Singleton class to keep track of the list of current Codewind connections,
 * and manage persisting them to and from the Preferences.
 */
public class CodewindConnectionManager {
	
	// Singleton instance. Never access this directly. Use the instance() method.
	private static CodewindConnectionManager instance;

	public static final String CONNECTION_LIST_PREFSKEY = "codewindConnections"; //$NON-NLS-1$
	public static final String NAME_KEY = "name"; //$NON-NLS-1$
	public static final String URI_KEY = "uri"; //$NON-NLS-1$
	
	
	private List<CodewindConnection> connections = new ArrayList<>();

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
		Logger.log("Added a new connection: " + connection.getBaseURI()); //$NON-NLS-1$
		instance().writeToPreferences();
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
	synchronized static boolean remove(String baseUrl) {
		boolean removeResult = false;

		CodewindConnection connection = CodewindConnectionManager.getActiveConnection(baseUrl.toString());
		if (connection != null) {
			List<CodewindApplication> apps = connection.getApps();
			connection.close();
			removeResult = instance().connections.remove(connection);
			CoreUtil.removeConnection(apps);
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

	// Preferences serialization
	private void writeToPreferences() {
		JSONArray jsonArray = new JSONArray();
		
		for (CodewindConnection conn : activeConnections()) {
			if (!conn.isLocal()) {
				try {
					JSONObject obj = new JSONObject();
					obj.put(NAME_KEY, conn.getName());
					obj.put(URI_KEY, conn.getBaseURI());
					jsonArray.put(obj);
				} catch (JSONException e) {
					Logger.logError("An error occurred trying to add connection to the preferences: " + conn.getBaseURI(), e);
				}
			}
		}

		Logger.log("Writing connections to preferences: " + jsonArray.toString()); //$NON-NLS-1$

		CodewindCorePlugin.getDefault().getPreferenceStore()
				.setValue(CONNECTION_LIST_PREFSKEY, jsonArray.toString());
	}

	private void loadFromPreferences() {
		clear();

		String storedConnections = CodewindCorePlugin.getDefault()
				.getPreferenceStore()
				.getString(CONNECTION_LIST_PREFSKEY).trim();
		
		if (storedConnections == null || storedConnections.isEmpty()) {
			Logger.log("The preferences do not contain any connections");
			return;
		}

		Logger.log("Reading connections from preferences: \"" + storedConnections + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		
		try {
			JSONArray array = new JSONArray(storedConnections);
			for (int i = 0; i < array.length(); i++) {
				try {
					JSONObject obj = array.getJSONObject(i);
					String name = obj.getString(NAME_KEY);
					String uriStr = obj.getString(URI_KEY);
					URI uri = new URI(uriStr);
					CodewindConnection connection = CodewindObjectFactory.createCodewindConnection(name, uri, false);
					connection.connect(new NullProgressMonitor());
					CodewindConnectionManager.add(connection);
				} catch (CodewindConnectionException e) {
					Logger.logError("Fatal error trying to create connection for url: " + e.connectionUrl, e);
				}
			}
		} catch (Exception e) {
			Logger.logError("Error loading connection from preferences", e); //$NON-NLS-1$
		}
	}

	public static boolean removeConnection(String connectionUrl) {
		CodewindConnectionManager.remove(connectionUrl);
		return true;
	}
}
