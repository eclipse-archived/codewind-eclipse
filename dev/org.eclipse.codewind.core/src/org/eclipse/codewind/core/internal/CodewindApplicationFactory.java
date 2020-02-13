/*******************************************************************************
 * Copyright (c) 2018, 2020 IBM Corporation and others.
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
import org.eclipse.codewind.core.internal.constants.ProjectLanguage;
import org.eclipse.codewind.core.internal.constants.ProjectType;
import org.eclipse.codewind.core.internal.constants.StartMode;
import org.eclipse.core.runtime.Path;
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
						Logger.log("The application is no longer in the project list so removing: " + id);
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
			JSONObject extension = null;
			if (appJso.has(CoreConstants.KEY_EXTENSION)) {
				extension = appJso.getJSONObject(CoreConstants.KEY_EXTENSION);
			}

			ProjectType type = ProjectType.TYPE_UNKNOWN;
			ProjectLanguage language = ProjectLanguage.LANGUAGE_UNKNOWN;
			try {
				String typeStr = appJso.getString(CoreConstants.KEY_PROJECT_TYPE);
				String languageStr = appJso.getString(CoreConstants.KEY_LANGUAGE);
				type = ProjectType.getType(typeStr, extension);
				language = ProjectLanguage.getLanguage(languageStr);
			} catch(JSONException e) {
				Logger.logError(e.getMessage() + " in: " + appJso); //$NON-NLS-1$
			}

			String localPath = appJso.getString(CoreConstants.KEY_LOC_DISK);
			localPath = CoreUtil.getHostPath(localPath);
			
			CodewindApplication app = CodewindObjectFactory.createCodewindApplication(connection, id, name, type, language, new Path(localPath));
			
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
				String detail = null;
				if (appJso.has(CoreConstants.KEY_DETAILED_APP_STATUS)) {
					JSONObject detailObj = appJso.getJSONObject(CoreConstants.KEY_DETAILED_APP_STATUS);
					if (detailObj != null && detailObj.has(CoreConstants.KEY_MESSAGE)) {
						detail = detailObj.getString(CoreConstants.KEY_MESSAGE);
						String notificationID = getStringValue(detailObj, CoreConstants.KEY_NOTIFICATION_ID);
						if (notificationID == null) {
							// If there is no notification id then clear the list
							app.clearNotificationIDs();
						} else if (!app.hasNotificationID(notificationID)) {
							// If there is a new notifiction id then need to notify the user
							
							// First store the notification id so the user does not get the same notification twice
							app.addNotificationID(notificationID);
							
							// Notify the user in a dialog with a link to more information if available
							CoreUtil.DialogType type = CoreUtil.DialogType.ERROR;
							if (detailObj.has(CoreConstants.KEY_SEVERITY)) {
								String severity = detailObj.getString(CoreConstants.KEY_SEVERITY);
								if (CoreConstants.VALUE_WARN.equals(severity)) {
									type = CoreUtil.DialogType.WARN;
								} else if (CoreConstants.VALUE_INFO.equals(severity)) {
									type = CoreUtil.DialogType.INFO;
								}
							}
							// Get the link and link label
							String link = null, linkLabel = null;
							if (detailObj.has(CoreConstants.KEY_LINK) && detailObj.has(CoreConstants.KEY_LINK_LABEL)) {
								link = detailObj.getString(CoreConstants.KEY_LINK);
								linkLabel = detailObj.getString(CoreConstants.KEY_LINK_LABEL);
							}
							if (link != null && !link.isEmpty() && linkLabel != null && !linkLabel.isEmpty()) {
								CoreUtil.openDialogWithLink(type, app.name, detail, linkLabel, link);
							} else {
								CoreUtil.openDialog(type, app.name, detail);
							}
						}
					}
				}
				app.setAppStatus(appStatus, detail);
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

			if (appJso.has(CoreConstants.KEY_LAST_BUILD)) {
				long timestamp = appJso.getLong(CoreConstants.KEY_LAST_BUILD);
				app.setLastBuild(timestamp);
			}
			
			if (appJso.has(CoreConstants.KEY_APP_IMAGE_LAST_BUILD)) {
				String timestamp = appJso.getString(CoreConstants.KEY_APP_IMAGE_LAST_BUILD);
				try {
					app.setLastImageBuild(Long.parseLong(timestamp));
				} catch (NumberFormatException e) {
					Logger.logError("Error parsing the app image last build value: " + timestamp, e);
				}
			}
			
			// Set the application base URL
			String appBaseUrl = null;
			if (appJso.has(CoreConstants.KEY_APP_BASE_URL)) {
				appBaseUrl = appJso.getString(CoreConstants.KEY_APP_BASE_URL);
			}
			app.setAppBaseUrl(appBaseUrl);
			
			// Set isHttps
			boolean isHttps = false;
			if (appJso.has(CoreConstants.KEY_IS_HTTPS)) {
				isHttps = appJso.getBoolean(CoreConstants.KEY_IS_HTTPS);
			}
			app.setIsHttps(isHttps);
			
			// Get the container id
			app.setContainerId(getStringValue(appJso, CoreConstants.KEY_CONTAINER_ID));
			
			// Get the pod information
			app.setPodInfo(getStringValue(appJso, CoreConstants.KEY_POD_NAME), getStringValue(appJso, CoreConstants.KEY_NAMESPACE));
			
			// Get the ports if they are available
			try {
				JSONObject portsObj = null;
				if (appJso.has(CoreConstants.KEY_PORTS) && (appJso.get(CoreConstants.KEY_PORTS) instanceof JSONObject)) {
					portsObj = appJso.getJSONObject(CoreConstants.KEY_PORTS);
				}
	
				int httpPortNum = -1;
				if (portsObj != null && portsObj.has(CoreConstants.KEY_EXPOSED_PORT)) {
					String httpPort = portsObj.getString(CoreConstants.KEY_EXPOSED_PORT);
					if (httpPort != null && !httpPort.isEmpty()) {
						httpPortNum = CoreUtil.parsePort(httpPort);
					}
				}
				app.setHttpPort(httpPortNum);
				
				String internalAppPort = null;
				if (portsObj != null && portsObj.has(CoreConstants.KEY_INTERNAL_PORT)) {
					internalAppPort = portsObj.getString(CoreConstants.KEY_INTERNAL_PORT);
				}
				app.setContainerAppPort(internalAppPort);

				int debugPortNum = -1;
				if (portsObj != null && portsObj.has(CoreConstants.KEY_EXPOSED_DEBUG_PORT)) {
					String debugPort = portsObj.getString(CoreConstants.KEY_EXPOSED_DEBUG_PORT);
					if (debugPort != null && !debugPort.isEmpty()) {
						debugPortNum = CoreUtil.parsePort(debugPort);
					}
				}
				app.setDebugPort(debugPortNum);
				
				String internalDebugPort = null;
				if (portsObj != null && portsObj.has(CoreConstants.KEY_INTERNAL_DEBUG_PORT)) {
					internalDebugPort = portsObj.getString(CoreConstants.KEY_INTERNAL_DEBUG_PORT);
				}
				app.setContainerDebugPort(internalDebugPort);
			} catch (Exception e) {
				Logger.logError("Failed to get the ports for application: " + app.name, e); //$NON-NLS-1$
			}
			
			// Set the context root
			String contextRoot = null;
			if (appJso.has(CoreConstants.KEY_CONTEXT_ROOT)) {
				contextRoot = appJso.getString(CoreConstants.KEY_CONTEXT_ROOT);
			} else if (appJso.has(CoreConstants.KEY_CONTEXTROOT)) {
				contextRoot = appJso.getString(CoreConstants.KEY_CONTEXTROOT);
			}
			app.setContextRoot(contextRoot);
			
			// Set the start mode
			app.setStartMode(StartMode.get(appJso));
			
			// Set auto build
			if (appJso.has(CoreConstants.KEY_AUTO_BUILD)) {
				app.setAutoBuild(appJso.getBoolean(CoreConstants.KEY_AUTO_BUILD));
			}
			
			// Set capabilities ready
			if (appJso.has(CoreConstants.KEY_CAPABILITIES_READY)) {
				app.setCapabilitiesReady(appJso.getBoolean(CoreConstants.KEY_CAPABILITIES_READY));
			}
			
			// Set inject metrics info
			if (appJso.has(CoreConstants.KEY_INJECTION)) {
				JSONObject injectObj = appJso.getJSONObject(CoreConstants.KEY_INJECTION);
				app.setMetricsInjectionInfo(injectObj.getBoolean(CoreConstants.KEY_INJECTABLE), injectObj.getBoolean(CoreConstants.KEY_INJECTED));
			}
			
			// Set metrics dashboard info
			if (appJso.has(CoreConstants.KEY_METRICS_DASHBOARD)) {
				JSONObject metricsObj = appJso.getJSONObject(CoreConstants.KEY_METRICS_DASHBOARD);
				app.setMetricsDashboardInfo(getStringValue(metricsObj, CoreConstants.KEY_METRICS_HOSTING), getStringValue(metricsObj, CoreConstants.KEY_METRICS_PATH));
			}
			
			// Set perf dashboard info
			if (appJso.has(CoreConstants.KEY_PERF_DASHBOARD_PATH)) {
				app.setPerfDashboardInfo(getStringValue(appJso, CoreConstants.KEY_PERF_DASHBOARD_PATH));
			}
		} catch(JSONException e) {
			Logger.logError("Error parsing project json: " + appJso, e); //$NON-NLS-1$
		}
		
		try {
			if (appJso.has(CoreConstants.KEY_LOGS) && appJso.getJSONObject(CoreConstants.KEY_LOGS).length() > 0) {
				// Set the log information
				List<ProjectLogInfo> logInfos = app.connection.requestProjectLogs(app);
				app.setLogInfos(logInfos);
			}
		} catch (Exception e) {
			Logger.logError("An error occurred while updating the log information for project: " + app.name, e);
		}
	}
	
	private static String getStringValue(JSONObject obj, String key) throws JSONException {
		if (!obj.has(key) || obj.isNull(key)) {
			return null;
		}
		String value = obj.getString(key);
		if (value == null || value.isEmpty()) {
			return null;
		}
		return value;
	}
}
