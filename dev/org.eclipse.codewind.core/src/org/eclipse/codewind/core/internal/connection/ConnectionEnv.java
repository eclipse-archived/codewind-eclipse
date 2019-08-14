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

import java.net.URL;

import org.eclipse.codewind.core.internal.CoreUtil;
import org.eclipse.codewind.core.internal.Logger;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.json.JSONException;
import org.json.JSONObject;

public class ConnectionEnv {
	
	public static final String CODEWIND_WORKSPACE_PROPERTY = "org.eclipse.codewind.internal.workspace"; //$NON-NLS-1$
	public static final String KEY_VERSION = "codewind_version"; //$NON-NLS-1$
	public static final String UNKNOWN_VERSION = "unknown"; //$NON-NLS-1$
	public static final String KEY_WORKSPACE_LOC = "workspace_location"; //$NON-NLS-1$
	public static final String KEY_SOCKET_NAMESPACE = "socket_namespace"; //$NON-NLS-1$
	public static final String KEY_TEKTON_DASHBOARD_URL = "tekton_dashboard_url"; //$NON-NLS-1$
	
	private JSONObject env;
	private IPath workspacePath;
	
	public ConnectionEnv(JSONObject env) {
		this.env = env;
	}
	
	public String getVersion() {
		String version = getString(KEY_VERSION);
		if (version == null) {
			version = UNKNOWN_VERSION;
		}
		return version;
	}
	
	public IPath getWorkspacePath() {
		if (workspacePath == null) {
			// Try the internal system property first
			String path = System.getProperty(CODEWIND_WORKSPACE_PROPERTY, null);
			if (path != null && !path.isEmpty()) {
				workspacePath = new Path(path);
			} else {
				String workspaceLoc = getString(KEY_WORKSPACE_LOC);
				if (workspaceLoc == null) {
					return null;
				}
				if (CoreUtil.isWindows() && workspaceLoc.startsWith("/")) { //$NON-NLS-1$
					String device = workspaceLoc.substring(1, 2);
					workspaceLoc = device + ":" + workspaceLoc.substring(2); //$NON-NLS-1$
				}
				workspacePath = new Path(workspaceLoc);
			}
		}
		return workspacePath;
	}
	
	public String getSocketNamespace() {
		return getString(KEY_SOCKET_NAMESPACE);
	}
	
	public URL getTektonDashboardURL() {
		String urlStr = getString(KEY_TEKTON_DASHBOARD_URL);
		if (urlStr == null || urlStr.isEmpty()) {
			return null;
		}
		try {
			return new URL(urlStr);
		} catch (Exception e) {
			Logger.logError("The Tekton dashboard URL is not valid: " + urlStr, e);
		}
		return null;
	}
	
	private String getString(String key) {
		String value = null;
		if (env.has(key)) {
			try {
				value = env.getString(key);
			} catch (JSONException e) {
				Logger.logError("An error occurred retrieving the value from the connection environment object for key: " + key, e);
			}
		} else {
			Logger.logError("The connection environment object did not have the expected key: " + key);
		}
		return value;
	}

	@Override
	public String toString() {
		return env.toString();
	}

}
