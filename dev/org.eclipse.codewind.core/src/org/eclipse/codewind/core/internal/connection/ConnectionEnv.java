/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
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
	public static final String KEY_NAMESPACE = "namespace"; //$NON-NLS-1$
	public static final String KEY_TEKTON_DASHBOARD = "tekton_dashboard"; //$NON-NLS-1$
	
	private TektonDashboard tektonDashboard;
	
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
	
	public String getNamespace() {
		return getString(KEY_NAMESPACE);
	}
	
	public TektonDashboard getTektonDashboard() {
		if (tektonDashboard == null) {
			JSONObject jsonObject = getObject(KEY_TEKTON_DASHBOARD);
			tektonDashboard = new TektonDashboard(jsonObject);
		}
		return tektonDashboard;
	}
	
	public class TektonDashboard extends JSONObjectResult {
		
		public static final String KEY_TEKTON_DASHBOARD_STATUS = "status"; //$NON-NLS-1$
		public static final String KEY_TEKTON_DASHBOARD_MESSAGE = "message"; //$NON-NLS-1$
		public static final String KEY_TEKTON_DASHBOARD_URL = "url"; //$NON-NLS-1$
		public static final String NOT_INSTALLED_MSG = "not-installed"; //$NON-NLS-1$
		
		protected TektonDashboard(JSONObject tektonDashboard) {
			super(tektonDashboard, "tekton dashboard");
		}
		
		public boolean hasTektonDashboard() {
			return getBoolean(KEY_TEKTON_DASHBOARD_STATUS);
		}
		
		public boolean isNotInstalled() {
			return NOT_INSTALLED_MSG.equals(getTektonMessage());
		}
		
		public String getTektonMessage() {
			return getString(KEY_TEKTON_DASHBOARD_MESSAGE);
		}
		
		public URL getTektonUrl() {
			String urlStr = getString(KEY_TEKTON_DASHBOARD_URL);
			if (urlStr == null || urlStr.isEmpty()) {
				return null;
			}
			try {
				return new URL("http://" + urlStr);
			} catch (Exception e) {
				Logger.logError("The Tekton dashboard URL is not valid: " + urlStr, e);
			}
			return null;
		}
		
	}
}
