/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *	 IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.codewind.core.internal.cli;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.codewind.core.internal.Logger;
import org.eclipse.codewind.core.internal.connection.JSONObjectResult;
import org.eclipse.codewind.core.internal.connection.LocalConnection;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ConnectionInfo extends JSONObjectResult {
	
	private static final String SCHEMAVERSION_KEY = "schemaversion";
	private static final String CONNECTIONS_KEY = "connections";
	private static final String ID_KEY = "id";
	private static final String LABEL_KEY = "label";
	private static final String URL_KEY = "url";
	private static final String AUTH_KEY = "auth";
	private static final String REALM_KEY = "realm";
	private static final String CLIENTID_KEY = "clientid";
	private static final String USERNAME_KEY = "username";
	
	public ConnectionInfo(JSONObject connectionInfo) {
		super(connectionInfo, "connection info");
	}
	
	public String getId() {
		return getString(ID_KEY);
	}
	
	public boolean isLocal() {
		return LocalConnection.DEFAULT_ID.equals(getId());
	}
	
	public String getLabel() {
		return getString(LABEL_KEY);
	}
	
	public String getURL() {
		return getString(URL_KEY);
	}
	
	public String getAuth() {
		return getString(AUTH_KEY);
	}
	
	public String getRealm() {
		return getString(REALM_KEY);
	}
	
	public String getClientid() {
		return getString(CLIENTID_KEY);
	}
	
	public String getUsername() {
		return getString(USERNAME_KEY);
	}
	
	public static List<ConnectionInfo> getInfos(JSONObject connectionsObj) {
		List<ConnectionInfo> connections = new ArrayList<ConnectionInfo>();
		try {
			if (connectionsObj.has(SCHEMAVERSION_KEY) && connectionsObj.getInt(SCHEMAVERSION_KEY) == 1 && connectionsObj.has(CONNECTIONS_KEY)) {
				JSONArray array = connectionsObj.getJSONArray(CONNECTIONS_KEY);
				for (int i = 0; i < array.length(); i++) {
					connections.add(new ConnectionInfo(array.getJSONObject(i)));
				}
			}
		} catch (JSONException e) {
			Logger.logError("The output for the connection list is not valid", e);
		}
		return connections;
	}

}
