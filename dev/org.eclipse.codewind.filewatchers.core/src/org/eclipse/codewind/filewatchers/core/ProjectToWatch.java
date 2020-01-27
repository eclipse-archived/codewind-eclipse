/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation
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
import java.util.Optional;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Contains information on what directory to (recursively monitor), and any
 * filters that should be applied (eg to ignore changes to files/directories
 * with specific names/paths).
 *
 * The fields correspond to the array of projects JSON from the 'GET
 * api/v1/projects/watchlist' API. See docs for format restrictions for these
 * fields.
 */
public class ProjectToWatch {

	private static final FWLogger log = FWLogger.getInstance();

	private final String projectId;

	/** Path (inside the container) to monitor */
	private final String pathToMonitor;

	private final List<String> ignoredPaths = new ArrayList<>();
	private final List<String> ignoredFilenames = new ArrayList<>();

	private final List<String> filesToWatch = new ArrayList<>();

	private final String projectWatchStateId;

	/** null if project time is not specified, a >0 value otherwise. */
	private final Long projectCreationTimeInAbsoluteMsecs;

	private boolean external = false;

	public ProjectToWatch(JSONObject json, boolean deleteChangeType) throws JSONException {

		// Delete event from WebSocket only has these fields.
		if (deleteChangeType) {
			this.projectId = json.getString("projectID");
			this.pathToMonitor = null;
			this.projectWatchStateId = null;
			this.projectCreationTimeInAbsoluteMsecs = null;
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

		long pct = json.optLong("projectCreationTime", 0l);

		this.projectCreationTimeInAbsoluteMsecs = pct != 0 ? pct : null;

		// Added as part of codewind/1399
		JSONArray refPaths = json.optJSONArray("refPaths");
		if (refPaths != null) {
			for (int x = 0; x < refPaths.length(); x++) {
				JSONObject pathObj = refPaths.getJSONObject(x);
				String fromPath = pathObj.optString("from");
				if (fromPath != null && !fromPath.trim().isEmpty()) {
					this.filesToWatch.add(fromPath);
				} else {
					log.logSevere("'from' field of refPaths could not be found or was null");
				}
			}
		}

	}

	/** This should ONLY be called from the clone method below. */
	private ProjectToWatch(ProjectToWatch old, Long projectCreationTimeInAbsoluteMsecsParam) {

		this.external = old.external;

		this.projectId = old.projectId;
		this.pathToMonitor = old.pathToMonitor;

		this.projectWatchStateId = old.projectWatchStateId;

		validatePathToMonitor();

		this.ignoredPaths.addAll(old.ignoredPaths);

		this.ignoredFilenames.addAll(old.ignoredFilenames);

		// Replace the old value, with specified parameter.
		this.projectCreationTimeInAbsoluteMsecs = projectCreationTimeInAbsoluteMsecsParam;

		this.filesToWatch.addAll(old.filesToWatch);
	}

	/**
	 * Create a clone of this object, but use the given
	 * projectCreationTimeInAbsoluteMsecsParam in that field, replacing the value of
	 * the current object.
	 */
	public ProjectToWatch cloneWithNewProjectCreationTime(Long projectCreationTimeInAbsoluteMsecsParam) {

		return new ProjectToWatch(this, projectCreationTimeInAbsoluteMsecsParam);

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

	public List<String> getFilesToWatch() {
		return filesToWatch;
	}

	public Optional<Long> getProjectCreationTimeInAbsoluteMsecs() {
		return Optional.ofNullable(projectCreationTimeInAbsoluteMsecs);
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
