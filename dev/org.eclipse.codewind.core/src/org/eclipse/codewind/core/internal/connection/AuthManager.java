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
import java.util.concurrent.TimeoutException;

import org.eclipse.codewind.core.internal.Logger;
import org.eclipse.codewind.core.internal.cli.AuthToken;
import org.eclipse.codewind.core.internal.cli.AuthUtil;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.json.JSONException;

public class AuthManager {
	
	private final CodewindConnection connection;
	private AuthToken token;
	
	AuthManager(CodewindConnection connection, AuthToken token) {
		this.connection = connection;
		this.token = token;
	}
	
	AuthToken getToken(boolean update, IProgressMonitor monitor) throws IOException, JSONException  {
		boolean needsUpdate = token == null || token.aboutToExpire() || update;
		if (needsUpdate) {
			updateToken(monitor);
		}
		return token;
	}
	
	synchronized void setToken(AuthToken token) {
		this.token = token;
	}
	
	synchronized void updateToken(IProgressMonitor monitor) throws IOException, JSONException {
		if (token != null && token.recentlyCreated()) {
			return;
		}
		try {
			token = AuthUtil.getAuthToken(connection.getUsername(), connection.getConid(), monitor);
		} catch (TimeoutException e) {
			throw new IOException("Timed out trying to update the token for connection: " + connection.getName());
		}
	}
	 
	AuthToken getTokenNonBlocking() {
		return token;
	}
	
	void updateTokenNonBlocking() {
		Runnable runnable = () -> {
			try {
				updateToken(new NullProgressMonitor());
			} catch (Exception e) {
				Logger.logError("An error occurred trying to update the token for connection: " + connection.getName(), e);
			}
		};
		Thread thread = new Thread(runnable);
		thread.setDaemon(true);
		thread.run();
	}

}
