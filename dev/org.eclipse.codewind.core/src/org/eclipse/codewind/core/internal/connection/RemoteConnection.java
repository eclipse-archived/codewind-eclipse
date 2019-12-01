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

package org.eclipse.codewind.core.internal.connection;

import java.io.IOException;
import java.net.URI;

import org.eclipse.codewind.core.internal.cli.AuthToken;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.json.JSONException;

public class RemoteConnection extends CodewindConnection {
	
	private String username = null;
	private final AuthManager authManager;
	
	public RemoteConnection(String name, URI uri, String conid, String username, AuthToken authToken) {
		super(name, uri, conid);
		this.username = username;
		authManager = new AuthManager(this, authToken);
	}
	
	@Override
	AuthToken getAuthToken(boolean update) throws IOException, JSONException {
		return authManager.getToken(update, new NullProgressMonitor());
	}
	
	@Override
	public void setAuthToken(AuthToken authToken) {
		authManager.setToken(authToken);
	}
	
	@Override
	public void setUsername(String username) {
		this.username = username;
	}
	
	@Override
	public String getUsername() {
		return username;
	}
	

}
