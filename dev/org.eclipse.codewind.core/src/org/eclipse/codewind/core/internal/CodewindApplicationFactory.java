/*******************************************************************************
 * Copyright (c) 2018, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.codewind.core.internal;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.codewind.core.internal.connection.CodewindConnection;
import org.eclipse.codewind.core.internal.console.ProjectLogInfo;
import org.eclipse.codewind.core.internal.constants.CoreConstants;
import org.eclipse.codewind.core.internal.constants.ProjectType;
import org.eclipse.codewind.core.internal.constants.StartMode;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class CodewindApplicationFactory {
	
	/**
	 * Process the json for all projects, create or update applications as needed.
	 */
	public static void getAppsFromProjectsJson(CodewindConnection connection, String projectsJson) {
		getAppsFromProjectsJson(connection, projectsJson, null);
	}
	
	/**
	 * Process the json for the given projectID or all projects if projectID is null.
	 */
	public static void getAppsFromProjectsJson(CodewindConnection connection,
			String projectsJson, String projectID) {

		try {
			Logger.log(projectsJson);
			JSONArray appArray = new JSONArray(projectsJson);
			Set<String> idSet = new HashSet<String>();
	
			for(int i = 0; i < appArray.length(); i++) {
				JSONObject appJso = appArray.getJSONObject(i);
				try {
					String id = appJso.getString(CoreConstants.KEY_PROJECT_ID);
					idSet.add(id);
					// If a project id was passed in then only process the JSON object for that project
					if (projectID == null || projectID.equals(id)) {
						synchronized(CodewindApplicationFactory.class) {
							CodewindApplication app = connection.getAppByID(id);
							if (app != null) {
								updateApp(app, appJso);
								if (app.isDeleting()) {
									// Remove the app from the list
									connection.removeApp(id);
								}
							} else {
								app = createApp(connection, appJso);
								if (app != null && !app.isDeleting()) {
									connection.addApp(app);
								}
							}
						}
					}
				} catch (Exception e) {
					Logger.logError("Error parsing project json: " + appJso, e); //$NON-NLS-1$
				}
			}
			
			// If refreshing all of the projects, remove any projects that are not in the list returned by Codewind.
			// This will only happen if something goes wrong and no delete event is received from Codewind for a
			// project.
			if (projectID == null) {
				for (String id : connection.getAppIds()) {
					if (!idSet.contains(id)) {
						connection.removeApp(id);
					}
				}
			}
		} catch (Exception e) {
			Logger.logError("Error parsing json for project array.", e); //$NON-NLS-1$
		}
	}
	
	/**
	 * Use the static information in the JSON object to create the application.
	 */
	public static CodewindApplication createApp(CodewindConnection connection, JSONObject appJso) {
		try {
			// MCLogger.log("app: " + appJso.toString());
			String name = appJso.getString(CoreConstants.KEY_NAME);
			String id = appJso.getString(CoreConstants.KEY_PROJECT_ID);

			ProjectType type = ProjectType.UNKNOWN_TYPE;
			try {
				String typeStr = appJso.getString(CoreConstants.KEY_PROJECT_TYPE);
				String languageStr = appJso.getString(CoreConstants.KEY_LANGUAGE);
				type = new ProjectType(typeStr, languageStr);
			}
			catch(JSONException e) {
				Logger.logError(e.getMessage() + " in: " + appJso); //$NON-NLS-1$
			}

			String loc = appJso.getString(CoreConstants.KEY_LOC_DISK);
			
			CodewindApplication app = CodewindObjectFactory.createCodewindApplication(connection, id, name, type, loc);
			
			updateApp(app, appJso);
			return app;
		} catch(JSONException e) {
			Logger.logError("Error parsing project json: " + appJso, e); //$NON-NLS-1$
		} catch (Exception e) {
			Logger.logError("Error creating new application for project.", e); //$NON-NLS-1$
		}
		return null;
	}
	
	/**
	 * Update the application with the dynamic information in the JSON object.
	 */
	public static void updateApp(CodewindApplication app, JSONObject appJso) {
		try {
			// Set the action
			if (appJso.has(CoreConstants.KEY_ACTION)) {
				String action = appJso.getString(CoreConstants.KEY_ACTION);
				app.setAction(action);
				if (CoreConstants.VALUE_ACTION_DELETING.equals(action)) {
					// No point in updating any further since this app should be removed from the list
					return;
				}
			} else {
				app.setAction(null);
			}
			
			// Set the app state
			if (appJso.has(CoreConstants.KEY_OPEN_STATE)) {
				String state = appJso.getString(CoreConstants.KEY_OPEN_STATE);
				if (CoreConstants.VALUE_STATE_CLOSED.equals(state)) {
					app.setEnabled(false);
					return;
				}
			}
			app.setEnabled(true);
						
			// Set the app status
			if (appJso.has(CoreConstants.KEY_APP_STATUS)) {
				String appStatus = appJso.getString(CoreConstants.KEY_APP_STATUS);
				if (appStatus != null) {
					app.setAppStatus(appStatus);
				}
			}
			
			// Set the build status
			if (appJso.has(CoreConstants.KEY_BUILD_STATUS)) {
				String buildStatus = appJso.getString(CoreConstants.KEY_BUILD_STATUS);
				String detail = ""; //$NON-NLS-1$
				if (appJso.has(CoreConstants.KEY_DETAILED_BUILD_STATUS)) {
					detail = appJso.getString(CoreConstants.KEY_DETAILED_BUILD_STATUS);
				}
				app.setBuildStatus(buildStatus, detail);
			}
			
			// Get the container id
			String containerId = null;
			if (appJso.has(CoreConstants.KEY_CONTAINER_ID)) {
			    containerId = appJso.getString(CoreConstants.KEY_CONTAINER_ID);
			}
			app.setContainerId(containerId);
			
			// Get the ports if they are available
			try {
				if (appJso.has(CoreConstants.KEY_PORTS) && (appJso.get(CoreConstants.KEY_PORTS) instanceof JSONObject)) {
					JSONObject portsObj = appJso.getJSONObject(CoreConstants.KEY_PORTS);
	
					int httpPortNum = -1;
					if (portsObj != null && portsObj.has(CoreConstants.KEY_EXPOSED_PORT)) {
						String httpPort = portsObj.getString(CoreConstants.KEY_EXPOSED_PORT);
						if (httpPort != null && !httpPort.isEmpty()) {
							httpPortNum = CoreUtil.parsePort(httpPort);
						}
					}
					if (httpPortNum != -1) {
						app.setHttpPort(httpPortNum);
					}
	
					int debugPortNum = -1;
					if (portsObj != null && portsObj.has(CoreConstants.KEY_EXPOSED_DEBUG_PORT)) {
						String debugPort = portsObj.getString(CoreConstants.KEY_EXPOSED_DEBUG_PORT);
						if (debugPort != null && !debugPort.isEmpty()) {
							debugPortNum = CoreUtil.parsePort(debugPort);
						}
					}
					app.setDebugPort(debugPortNum);

				} else {
					Logger.logError("No ports object on project info for application: " + app.name); //$NON-NLS-1$
				}
			} catch (Exception e) {
				Logger.logError("Failed to get the ports for application: " + app.name, e); //$NON-NLS-1$
			}
			
			// Set the context root
			String contextRoot = null;
			if (appJso.has(CoreConstants.KEY_CONTEXTROOT)) {
				contextRoot = appJso.getString(CoreConstants.KEY_CONTEXTROOT);
			} else if (appJso.has(CoreConstants.KEY_CUSTOM)) {
				JSONObject custom = appJso.getJSONObject(CoreConstants.KEY_CUSTOM);
				if (custom.has(CoreConstants.KEY_CONTEXTROOT)) {
					contextRoot = custom.getString(CoreConstants.KEY_CONTEXTROOT);
				}
			}
			app.setContextRoot(contextRoot);
			
			// Set the start mode
			StartMode startMode = StartMode.get(appJso);
			app.setStartMode(startMode);
			
			// Set auto build
			if (appJso.has(CoreConstants.KEY_AUTO_BUILD)) {
				boolean autoBuild = appJso.getBoolean(CoreConstants.KEY_AUTO_BUILD);
				app.setAutoBuild(autoBuild);
			}
		} catch(JSONException e) {
			Logger.logError("Error parsing project json: " + appJso, e); //$NON-NLS-1$
		}
		
		try {
			// Set the log information
			List<ProjectLogInfo> logInfos = app.connection.requestProjectLogs(app);
			app.setLogInfos(logInfos);
		} catch (Exception e) {
			Logger.logError("An error occurred while updating the log information for project: " + app.name, e);
		}
		
		// Check for metrics support
		boolean metricsAvailable = true;
		try {
			JSONObject obj = app.connection.requestProjectMetricsStatus(app);
			if (obj != null && obj.has(CoreConstants.KEY_METRICS_AVAILABLE)) {
				metricsAvailable = obj.getBoolean(CoreConstants.KEY_METRICS_AVAILABLE);
			}
		} catch (Exception e) {
			Logger.logError("An error occurred checking if metrics are available: " + app.name, e);
		}
		app.setMetricsAvailable(metricsAvailable);
		
	}
}
