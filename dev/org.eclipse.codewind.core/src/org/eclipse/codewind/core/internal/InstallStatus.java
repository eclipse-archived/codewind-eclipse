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

import org.eclipse.codewind.core.internal.connection.CodewindConnection;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class InstallStatus {
	
	public static final String STATUS_KEY = "status";
	public static final String URL_KEY = "url";
	public static final String INSTALLED_VERSIONS_KEY = "installed-versions";
	public static final String STARTED_KEY = "started";
	
	public static final InstallStatus UNKNOWN = new InstallStatus(Status.UNKNOWN);

	private Status status;
	private String url;
	private JSONArray installedVersions;
	private JSONArray startedVersions;
	private String supportedVersion;
	
	private enum Status {
		UNINSTALLED("uninstalled"),
		STOPPED("stopped"),
		STARTED("started"),
		UNKNOWN("unknown");

		private String value;

		private Status(String value) {
			this.value = value;
		}

		public static Status getStatus(String statusStr) {
			for (Status status : Status.values()) {
				if (status.value.equals(statusStr)) {
					return status;
				}
			}
			// This should not happen
			Logger.logError("Unrecognized installer status: " + statusStr);
			return UNKNOWN;
		}

		public boolean isInstalled() {
			return (this != UNINSTALLED && this != UNKNOWN);
		}
	}
   
	public InstallStatus(JSONObject statusObj) {
		try {
			status = Status.getStatus(statusObj.getString(STATUS_KEY));
			if (status.isInstalled()) {
				installedVersions = statusObj.getJSONArray(INSTALLED_VERSIONS_KEY);
				for (int i = 0; i < installedVersions.length(); i++) {
					try {
						if (CodewindConnection.isSupportedVersion(installedVersions.getString(i))) {
							supportedVersion = installedVersions.getString(i);
						}
					} catch (NumberFormatException e) {
						Logger.logError("The Codewind installer status contained an invalid version", e); //$NON-NLS-1$
					}
				}
			}
			if (status == Status.STARTED) {
				startedVersions = statusObj.getJSONArray(STARTED_KEY);
				url = statusObj.getString(URL_KEY);
				if (!url.endsWith("/")) {
					url = url + "/";
				}
			}
		} catch (JSONException e) {
			Logger.logError("The Codewind installer status format is not recognized", e); //$NON-NLS-1$
			status = Status.UNKNOWN;
			url = null;
		}
	}
	
	private InstallStatus(Status status) {
		this.status = status;
	}
	
	public boolean isUnknown() {
		return status == Status.UNKNOWN;
	}
	
	public boolean isInstalled() {
		return supportedVersion != null;
	}
	
	public boolean isStarted() {
		try {
			if (supportedVersion != null && status == Status.STARTED) {
				for (int i = 0; i < startedVersions.length(); i++) {
					if (supportedVersion.equals(startedVersions.getString(i))) {
						return true;
					}
				}
			}
		} catch (JSONException e) {
			Logger.logError("The Codewind installer status format is not recognized", e); //$NON-NLS-1$
		}
		return false;
	}
	
	public String getVersion() {
		return supportedVersion;
	}
	
	public String getURL() {
		return url;
	}
	
	public boolean hasInstalledVersions() {
		return (installedVersions != null && installedVersions.length() > 0);
	}
	
	public boolean hasStartedVersions() {
		return (startedVersions != null && startedVersions.length() > 0);
	}
	
	public String getInstalledVersions() {
		return getVersionList(installedVersions);
	}
	
	public String getStartedVersions() {
		return getVersionList(startedVersions);
	}
	
	public String getVersionList(JSONArray versions) {
		StringBuilder builder = new StringBuilder();
		boolean start = true;
		for (int i = 0; i < versions.length(); i++) {
			try {
				if (!start) {
					builder.append(", ");
				}
				builder.append(versions.getString(i));
				start = false;
			} catch (JSONException e) {
				Logger.logError("The Codewind installer status format is not recognized", e); //$NON-NLS-1$
			}
		}
		return builder.toString();
	}
}
