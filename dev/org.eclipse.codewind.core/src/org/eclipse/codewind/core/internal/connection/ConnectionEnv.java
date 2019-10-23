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

import org.eclipse.codewind.core.internal.Logger;
import org.json.JSONObject;

public class ConnectionEnv extends JSONObjectResult {
	
	public static final String KEY_VERSION = "codewind_version"; //$NON-NLS-1$
	public static final String UNKNOWN_VERSION = "unknown"; //$NON-NLS-1$
	public static final String KEY_SOCKET_NAMESPACE = "socket_namespace"; //$NON-NLS-1$
	public static final String KEY_TEKTON_DASHBOARD_URL = "tekton_dashboard_url"; //$NON-NLS-1$
	public static final String VALUE_TEKTON_DASHBOARD_NOT_INSTALLED = "not-installed"; //$NON-NLS-1$
	public static final String VALUE_TEKTON_DASHBOARD_ERROR = "error"; //$NON-NLS-1$
	
	public ConnectionEnv(JSONObject env) {
		super(env, "connection environment");
	}
	
	public String getVersion() {
		String version = getString(KEY_VERSION);
		if (version == null) {
			version = UNKNOWN_VERSION;
		}
		return version;
	}
	
	public String getSocketNamespace() {
		return getString(KEY_SOCKET_NAMESPACE);
	}
	
	public URL getTektonDashboardURL() {
		String urlStr = getString(KEY_TEKTON_DASHBOARD_URL);
		if (urlStr == null || urlStr.isEmpty() || urlStr.equals(VALUE_TEKTON_DASHBOARD_NOT_INSTALLED) || urlStr.equals(VALUE_TEKTON_DASHBOARD_ERROR)) {
			return null;
		}
		try {
			return new URL(urlStr);
		} catch (Exception e) {
			Logger.logError("The Tekton dashboard URL is not valid: " + urlStr, e);
		}
		return null;
	}
}
