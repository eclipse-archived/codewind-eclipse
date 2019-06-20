/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.codewind.filewatchers.core;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Contains information on what directory to (recursively monitor), and any
 * filters that should be applied to ignore changes to files/directories with
 * specific names/paths. The field correspond to the array of projects JSON from
 * the 'GET api/v1/projects/watchlist' API. See docs for format restrictions for
 * these fields.
 */
public class ProjectToWatch {

	private final String projectId;

	/** Path (inside the container) to monitor */
	private final String pathToMonitor;

	private final List<String> ignoredPaths = new ArrayList<>();
	private final List<String> ignoredFilenames = new ArrayList<>();

	private final String projectWatchStateId;

	private boolean external = false;

	public ProjectToWatch(JSONObject json, boolean deleteChangeType) throws JSONException {

		// Delete event from WebSocket only has these fields.
		if (deleteChangeType) {
			this.projectId = json.getString("projectID");
			this.pathToMonitor = null;
			this.projectWatchStateId = null;
			return;
		}

		String projectType = json.optString("type");

		this.external = projectType != null ? projectType.equalsIgnoreCase("non-project") : false;

		this.projectId = json.getString("projectID");
		this.pathToMonitor = PathUtils.normalizeDriveLetter(json.getString("pathToMonitor"));

		this.projectWatchStateId = json.getString("projectWatchStateId");

		validatePathToMonitor();

		JSONArray ignoredPathsJson = json.optJSONArray("ignoredPaths");
		if (ignoredPathsJson != null && ignoredPathsJson.length() > 0) {
			for (int x = 0; x < ignoredPathsJson.length(); x++) {
				this.ignoredPaths.add(ignoredPathsJson.getString(x));
			}
		}

		JSONArray ignoredFilenamesJson = json.optJSONArray("ignoredFilenames");
		if (ignoredFilenamesJson != null && ignoredFilenamesJson.length() > 0) {
			for (int x = 0; x < ignoredFilenamesJson.length(); x++) {
				this.ignoredFilenames.add(ignoredFilenamesJson.getString(x));
			}
		}

	}

	public ProjectToWatch(String projectId, String pathToMonitorParam) {
		this.projectId = projectId;

		this.projectWatchStateId = null;

		pathToMonitorParam = PathUtils.normalizeDriveLetter(pathToMonitorParam);
		this.pathToMonitor = pathToMonitorParam;

		validatePathToMonitor();

	}

	private void validatePathToMonitor() {

		if (this.pathToMonitor.contains("\\")) {
			throw new IllegalArgumentException(
					"Path to monitor should not contain Windows-style path separators: " + this.pathToMonitor);
		}

		if (!this.pathToMonitor.startsWith("/")) {
			throw new IllegalArgumentException(
					"Path to monitor should always begin with a forward slash: " + this.pathToMonitor);
		}

		if (this.pathToMonitor.endsWith("/") || this.pathToMonitor.endsWith("\\")) {
			throw new IllegalArgumentException(
					"Path to monitor may not end with path separator: " + this.pathToMonitor);
		}
	}

	public String getProjectId() {
		return projectId;
	}

	public String getPathToMonitor() {
		return pathToMonitor;
	}

	public List<String> getIgnoredPaths() {
		return ignoredPaths;
	}

	public List<String> getIgnoredFilenames() {
		return ignoredFilenames;
	}

	public String getProjectWatchStateId() {
		return projectWatchStateId;
	}

	public boolean isExternal() {
		return external;
	}

	/**
	 * The watcher WebSocket will inform of us of project watch changes just like
	 * the GET watchlist API, except that the WebSocket JSON has an additional field
	 * 'changeType' which specifies how the watched project changed: for example, if
	 * a project is deleted or created, this field will indicate as such.
	 * 
	 * Absent the above field, this object has the same structure as its parent.
	 */
	public static class ProjectToWatchFromWebSocket extends ProjectToWatch {
		private final String changeType;

		public ProjectToWatchFromWebSocket(JSONObject obj, String changeType) throws JSONException {
			super(obj, changeType.equalsIgnoreCase("delete"));
			this.changeType = changeType;
		}

		public String getChangeType() {
			return changeType;
		}

	}
}
