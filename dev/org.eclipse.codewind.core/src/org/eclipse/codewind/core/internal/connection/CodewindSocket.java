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

package org.eclipse.codewind.core.internal.connection;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.codewind.core.internal.Logger;
import org.eclipse.codewind.core.internal.CoreUtil;
import org.eclipse.codewind.core.internal.CodewindApplication;
import org.eclipse.codewind.core.internal.console.ProjectLogInfo;
import org.eclipse.codewind.core.internal.console.SocketConsole;
import org.eclipse.codewind.core.internal.constants.CoreConstants;
import org.eclipse.codewind.core.internal.constants.ProjectType;
import org.eclipse.codewind.core.internal.constants.StartMode;
import org.eclipse.codewind.core.internal.messages.Messages;
import org.eclipse.osgi.util.NLS;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

/**
 * Wrapper for a SocketIO client socket, which connects to Codewind and listens for project state changes,
 * then updates the corresponding CodewindApplication's state.
 * One of these exists for each CodewindConnection. That connection is stored here so we can access
 * its applications.
 */
public class CodewindSocket {
	
	private final CodewindConnection connection;

	public final Socket socket;

	public final URI socketUri;

	private boolean hasLostConnection = false;

	private volatile boolean hasConnected = false;

	private Set<SocketConsole> socketConsoles = new HashSet<>();
	
	// Track the previous Exception so we don't spam the logs with the same connection failure message
	private Exception previousException;

	// SocketIO Event names
	private static final String
			EVENT_PROJECT_CREATION = "projectCreation",				//$NON-NLS-1$
			EVENT_PROJECT_CHANGED = "projectChanged", 				//$NON-NLS-1$
			EVENT_PROJECT_STATUS_CHANGE = "projectStatusChanged", 	//$NON-NLS-1$
			EVENT_PROJECT_RESTART = "projectRestartResult", 		//$NON-NLS-1$
			EVENT_PROJECT_CLOSED = "projectClosed", 				//$NON-NLS-1$
			EVENT_PROJECT_DELETION = "projectDeletion", 			//$NON-NLS-1$
			EVENT_PROJECT_VALIDATED = "projectValidated",			//$NON-NLS-1$
			EVENT_LOG_UPDATE = "log-update",						//$NON-NLS-1$
			EVENT_PROJECT_LOGS_LIST_CHANGED = "projectLogsListChanged",		//$NON-NLS-1$
			EVENT_PROJECT_SETTINGS_CHANGED = "projectSettingsChanged";	//$NON-NLS-1$

	public CodewindSocket(CodewindConnection connection) throws URISyntaxException {
		this.connection = connection;
		
		URI uri = connection.baseUrl;
		if (connection.getSocketNamespace() != null) {
			uri = uri.resolve(connection.getSocketNamespace());
		}
		socketUri = uri;

		socket = IO.socket(socketUri);
		
		socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
			@Override
			public void call(Object... arg0) {
				Logger.log("SocketIO connect success @ " + socketUri); //$NON-NLS-1$

				if (!hasConnected) {
					hasConnected = true;
				}
				if (hasLostConnection) {
					connection.clearConnectionError();
					previousException = null;
				}
			}
		})
		.on(Socket.EVENT_CONNECT_ERROR, new Emitter.Listener() {
			@Override
			public void call(Object... arg0) {
				if (arg0[0] instanceof Exception) {
					Exception e = (Exception) arg0[0];
					if (previousException == null || !e.getMessage().equals(previousException.getMessage())) {
						previousException = e;
						Logger.logError("SocketIO Connect Error @ " + socketUri, e); //$NON-NLS-1$
					}
				}
				connection.onConnectionError();
				hasLostConnection = true;
			}
		})
		.on(Socket.EVENT_ERROR, new Emitter.Listener() {
			@Override
			public void call(Object... arg0) {
				if (arg0[0] instanceof Exception) {
					Exception e = (Exception) arg0[0];
					Logger.logError("SocketIO Error @ " + socketUri, e); //$NON-NLS-1$
				}
			}
		})
		.on(Socket.EVENT_MESSAGE, new Emitter.Listener() {
			@Override
			public void call(Object... arg0) {
				// Don't think this is ever used
				Logger.log("SocketIO EVENT_MESSAGE " + arg0[0].toString()); //$NON-NLS-1$
			}
		})
		.on(EVENT_PROJECT_CREATION, new Emitter.Listener() {
			@Override
			public void call(Object... arg0) {
				Logger.log(EVENT_PROJECT_CREATION + ": " + arg0[0].toString()); //$NON-NLS-1$

				try {
					JSONObject event = new JSONObject(arg0[0].toString());
					onProjectCreation(event);
				} catch (JSONException e) {
					Logger.logError("Error parsing JSON: " + arg0[0].toString(), e); //$NON-NLS-1$
				}
			}
		})
		.on(EVENT_PROJECT_CHANGED, new Emitter.Listener() {
			@Override
			public void call(Object... arg0) {
				Logger.log(EVENT_PROJECT_CHANGED + ": " + arg0[0].toString()); //$NON-NLS-1$

				try {
					JSONObject event = new JSONObject(arg0[0].toString());
					onProjectChanged(event);
				} catch (JSONException e) {
					Logger.logError("Error parsing JSON: " + arg0[0].toString(), e); //$NON-NLS-1$
				}
			}
		})
		.on(EVENT_PROJECT_SETTINGS_CHANGED, new Emitter.Listener() {
			@Override
			public void call(Object... arg0) {
				Logger.log(EVENT_PROJECT_SETTINGS_CHANGED + ": " + arg0[0].toString()); //$NON-NLS-1$

				try {
					JSONObject event = new JSONObject(arg0[0].toString());
					onProjectSettingsChanged(event);
				} catch (JSONException e) {
					Logger.logError("Error parsing JSON: " + arg0[0].toString(), e); //$NON-NLS-1$
				}
			}
		})
		.on(EVENT_PROJECT_STATUS_CHANGE, new Emitter.Listener() {
			@Override
			public void call(Object... arg0) {
				Logger.log(EVENT_PROJECT_STATUS_CHANGE + ": " + arg0[0].toString()); //$NON-NLS-1$

				try {
					JSONObject event = new JSONObject(arg0[0].toString());
					onProjectStatusChanged(event);
				} catch (JSONException e) {
					Logger.logError("Error parsing JSON: " + arg0[0].toString(), e); //$NON-NLS-1$
				}
			}
		})
		.on(EVENT_PROJECT_RESTART, new Emitter.Listener() {
			@Override
			public void call(Object... arg0) {
				Logger.log(EVENT_PROJECT_RESTART + ": " + arg0[0].toString()); //$NON-NLS-1$

				try {
					JSONObject event = new JSONObject(arg0[0].toString());
					onProjectRestart(event);
				} catch (JSONException e) {
					Logger.logError("Error parsing JSON: " + arg0[0].toString(), e); //$NON-NLS-1$
				}
			}
		})
		.on(EVENT_PROJECT_CLOSED, new Emitter.Listener() {
			@Override
			public void call(Object... arg0) {
				Logger.log(EVENT_PROJECT_CLOSED + ": " + arg0[0].toString()); //$NON-NLS-1$

				try {
					JSONObject event = new JSONObject(arg0[0].toString());
					onProjectClosed(event);
				} catch (JSONException e) {
					Logger.logError("Error parsing JSON: " + arg0[0].toString(), e); //$NON-NLS-1$
				}
			}
		})
		.on(EVENT_PROJECT_DELETION, new Emitter.Listener() {
			@Override
			public void call(Object... arg0) {
				Logger.log(EVENT_PROJECT_DELETION + ": " + arg0[0].toString()); //$NON-NLS-1$

				try {
					JSONObject event = new JSONObject(arg0[0].toString());
					onProjectDeletion(event);
				} catch (JSONException e) {
					Logger.logError("Error parsing JSON: " + arg0[0].toString(), e); //$NON-NLS-1$
				}
			}
		})
		.on(EVENT_PROJECT_LOGS_LIST_CHANGED, new Emitter.Listener() {
			@Override
			public void call(Object... arg0) {
				Logger.log(EVENT_PROJECT_LOGS_LIST_CHANGED + ": " + arg0[0].toString()); //$NON-NLS-1$

				try {
					JSONObject event = new JSONObject(arg0[0].toString());
					onProjectLogsListChanged(event);
				} catch (JSONException e) {
					Logger.logError("Error parsing JSON: " + arg0[0].toString(), e); //$NON-NLS-1$
				}
			}
		})
		.on(EVENT_LOG_UPDATE, new Emitter.Listener() {
			@Override
			public void call(Object... arg0) {
				// can't print this whole thing because the logs strings flood the output
				Logger.log(EVENT_LOG_UPDATE);

				try {
					JSONObject event = new JSONObject(arg0[0].toString());
					onLogUpdate(event);
				} catch (JSONException e) {
					Logger.logError("Error parsing JSON: " + arg0[0].toString(), e); //$NON-NLS-1$
				}
			}
		})
		.on(EVENT_PROJECT_VALIDATED, new Emitter.Listener() {
			@Override
			public void call(Object... arg0) {
				Logger.log(EVENT_PROJECT_VALIDATED + ": " + arg0[0].toString()); //$NON-NLS-1$

				try {
					JSONObject event = new JSONObject(arg0[0].toString());
					onValidationEvent(event);
				} catch (JSONException e) {
					Logger.logError("Error parsing JSON: " + arg0[0].toString(), e); //$NON-NLS-1$
				}
			}
		});

		socket.connect();

		Logger.log("Created CodewindSocket connected to " + socketUri); //$NON-NLS-1$
	}
	
	public void close() {
		if (socket != null) {
			if (socket.connected()) {
				socket.disconnect();
			}
			socket.close();
		}
	}
	
	private void onProjectCreation(JSONObject event) throws JSONException {
		String projectID = event.getString(CoreConstants.KEY_PROJECT_ID);
		connection.refreshApps(projectID);
		CodewindApplication app = connection.getAppByID(projectID);
		if (app != null) {
			app.setEnabled(true);
		} else {
			Logger.logError("No application found matching the project id for the project creation event: " + projectID); //$NON-NLS-1$
		}
		CoreUtil.updateConnection(connection);
	}

	private void onProjectChanged(JSONObject event) throws JSONException {
		String projectID = event.getString(CoreConstants.KEY_PROJECT_ID);
		CodewindApplication app = connection.getAppByID(projectID);
		if (app == null) {
			Logger.logError("No application found matching the project id for the project changed event: " + projectID); //$NON-NLS-1$
			return;
		}
		
		app.setEnabled(true);
		
		// Update container id
		String containerId = null;
		if (event.has(CoreConstants.KEY_CONTAINER_ID)) {
		    containerId = event.getString(CoreConstants.KEY_CONTAINER_ID);
		}
		app.setContainerId(containerId);
	
        // Update ports
        JSONObject portsObj = event.getJSONObject(CoreConstants.KEY_PORTS);

        if (portsObj != null && portsObj.has(CoreConstants.KEY_EXPOSED_PORT)) {
        	int port = CoreUtil.parsePort(portsObj.getString(CoreConstants.KEY_EXPOSED_PORT));
    		app.setHttpPort(port);
        } else {
        	Logger.logError("No http port on project changed event for: " + app.name); //$NON-NLS-1$
        }

		if (portsObj != null && portsObj.has(CoreConstants.KEY_EXPOSED_DEBUG_PORT)) {
			int debugPort = CoreUtil.parsePort(portsObj.getString(CoreConstants.KEY_EXPOSED_DEBUG_PORT));
			app.setDebugPort(debugPort);
			if (StartMode.DEBUG_MODES.contains(app.getStartMode()) && debugPort != -1) {
				app.reconnectDebugger();
			}
		} else {
			app.setDebugPort(-1);
		}
		
		if (event.has(CoreConstants.KEY_AUTO_BUILD)) {
			boolean autoBuild = event.getBoolean(CoreConstants.KEY_AUTO_BUILD);
			app.setAutoBuild(autoBuild);
		}
		
		CoreUtil.updateApplication(app);
	}
	
	private void onProjectSettingsChanged(JSONObject event) throws JSONException {
		String projectID = event.getString(CoreConstants.KEY_PROJECT_ID);
		CodewindApplication app = connection.getAppByID(projectID);
		if (app == null) {
			Logger.logError("No application found matching the project id for the project settings changed event: " + projectID); //$NON-NLS-1$
			return;
		}
		
		app.setEnabled(true);
		
		// Update context root
		if (event.has(CoreConstants.KEY_CONTEXT_ROOT)) {
			app.setContextRoot(event.getString(CoreConstants.KEY_CONTEXT_ROOT));
		}
		
		// TODO: need to update ports?
		
		CoreUtil.updateApplication(app);
	}

	private void onProjectStatusChanged(JSONObject event) throws JSONException {
		String projectID = event.getString(CoreConstants.KEY_PROJECT_ID);
		CodewindApplication app = connection.getAppByID(projectID);
		if (app == null) {
			// Likely a new project is being created
			connection.refreshApps(projectID);
			CoreUtil.updateConnection(connection);
			return;
		}
		
		app.setEnabled(true);
		
		if (event.has(CoreConstants.KEY_APP_STATUS)) {
			String appStatus = event.getString(CoreConstants.KEY_APP_STATUS);
			app.setAppStatus(appStatus);
		}

		// Update build status if the project is not started or starting.
		if (event.has(CoreConstants.KEY_BUILD_STATUS)) {
			String buildStatus = event.getString(CoreConstants.KEY_BUILD_STATUS);
			String detail = ""; //$NON-NLS-1$
			if (event.has(CoreConstants.KEY_DETAILED_BUILD_STATUS)) {
				detail = event.getString(CoreConstants.KEY_DETAILED_BUILD_STATUS);
			}
			app.setBuildStatus(buildStatus, detail);
		}
		
		CoreUtil.updateApplication(app);
	}

	private void onProjectRestart(JSONObject event) throws JSONException {
		String projectID = event.getString(CoreConstants.KEY_PROJECT_ID);
		CodewindApplication app = connection.getAppByID(projectID);
		if (app == null) {
			Logger.logError("No application found matching the project id for the project restart event: " + projectID); //$NON-NLS-1$
			return;
		}
		
		app.setEnabled(true);
		
		String status = event.getString(CoreConstants.KEY_STATUS);
		if (!CoreConstants.REQUEST_STATUS_SUCCESS.equalsIgnoreCase(status)) {
			Logger.logError("Project restart failed on the application: " + event.toString()); //$NON-NLS-1$
			CoreUtil.openDialog(true,
					Messages.Socket_ErrRestartingProjectDialogTitle,
					NLS.bind(Messages.Socket_ErrRestartingProjectDialogMsg,
							app.name, status));
			return;
		}

		// This event should always have a 'ports' sub-object
		JSONObject portsObj = event.getJSONObject(CoreConstants.KEY_PORTS);

		// The ports object should always have an http port
		if (portsObj != null && portsObj.has(CoreConstants.KEY_EXPOSED_PORT)) {
			int port = CoreUtil.parsePort(portsObj.getString(CoreConstants.KEY_EXPOSED_PORT));
			app.setHttpPort(port);
		} else {
			Logger.logError("No http port on project restart event for: " + app.name); //$NON-NLS-1$
		}

		// Debug port will be missing if the restart was into Run mode.
		int debugPort = -1;
		if (portsObj != null && portsObj.has(CoreConstants.KEY_EXPOSED_DEBUG_PORT)) {
			debugPort = CoreUtil.parsePort(portsObj.getString(CoreConstants.KEY_EXPOSED_DEBUG_PORT));
		}
		app.setDebugPort(debugPort);
		
		StartMode startMode = StartMode.get(event);
		app.setStartMode(startMode);
		
		// Update the application
		CoreUtil.updateApplication(app);
		
		// Make sure no old debugger is running
		app.clearDebugger();
		
		if (StartMode.DEBUG_MODES.contains(startMode) && debugPort != -1) {
			app.connectDebugger();
		}
	}
	
	private void onProjectClosed(JSONObject event) throws JSONException {
		String projectID = event.getString(CoreConstants.KEY_PROJECT_ID);
		CodewindApplication app = connection.getAppByID(projectID);
		if (app == null) {
			Logger.logError("No application found for project being closed: " + projectID); //$NON-NLS-1$
			return;
		}
		app.setEnabled(false);
		CoreUtil.updateConnection(connection);
	}

	private void onProjectDeletion(JSONObject event) throws JSONException {
		String projectID = event.getString(CoreConstants.KEY_PROJECT_ID);
		CodewindApplication app = connection.removeApp(projectID);
		if (app == null) {
			Logger.logError("No application found for project being deleted: " + projectID); //$NON-NLS-1$
			return;
		}
		CoreUtil.updateConnection(connection);
		app.dispose();
	}

	public void registerSocketConsole(SocketConsole console) {
		Logger.log("Register socketConsole for project: " + console.app.name); //$NON-NLS-1$
		this.socketConsoles.add(console);
	}

	public void deregisterSocketConsole(SocketConsole console) {
		this.socketConsoles.remove(console);
	}

	private void onLogUpdate(JSONObject event) throws JSONException {
		String projectID = event.getString(CoreConstants.KEY_PROJECT_ID);
		String type = event.getString(CoreConstants.KEY_LOG_TYPE);
		String logName = event.getString(CoreConstants.KEY_LOG_NAME);
		Logger.log("Update the " + logName + " log for project: " + projectID); //$NON-NLS-1$ //$NON-NLS-2$

		for (SocketConsole console : this.socketConsoles) {
			if (console.app.projectID.equals(projectID) && console.logInfo.isThisLogInfo(type, logName)) {
				try {
					String logContents = event.getString(CoreConstants.KEY_LOGS);
					boolean reset = event.getBoolean(CoreConstants.KEY_LOG_RESET);
					console.update(logContents, reset);
				}
				catch(IOException e) {
					Logger.logError("Error updating console " + console.getName(), e);	// $NON-NLS-1$
				}
			}
		}
	}
	
	private void onProjectLogsListChanged(JSONObject event) throws JSONException {
		String projectID = event.getString(CoreConstants.KEY_PROJECT_ID);
		CodewindApplication app = connection.getAppByID(projectID);
		if (app == null) {
			// Likely a new project is being created
			connection.refreshApps(projectID);
			CoreUtil.updateConnection(connection);
			return;
		}

		String type;
		if (event.has(CoreConstants.KEY_LOG_BUILD)) {
			type = CoreConstants.KEY_LOG_BUILD;
			JSONArray logs = event.getJSONArray(CoreConstants.KEY_LOG_BUILD);
			List<ProjectLogInfo> logInfos = CodewindConnection.getLogs(logs, type);
			app.addLogInfos(logInfos);
		}
		if (event.has(CoreConstants.KEY_LOG_APP)) {
			type = CoreConstants.KEY_LOG_APP;
			JSONArray logs = event.getJSONArray(CoreConstants.KEY_LOG_APP);
			List<ProjectLogInfo> logInfos = CodewindConnection.getLogs(logs, type);
			app.addLogInfos(logInfos);
		}
	}
	
	private void onValidationEvent(JSONObject event) throws JSONException {
		String projectID = event.getString(CoreConstants.KEY_PROJECT_ID);
		CodewindApplication app = connection.getAppByID(projectID);
		if (app == null) {
			Logger.logError("No application found for project: " + projectID); //$NON-NLS-1$
			return;
		}
		
		// Clear out any old validation objects
		app.resetValidation();
		
		// If the validation is successful then just return
		String status = event.getString(CoreConstants.KEY_VALIDATION_STATUS);
		if (CoreConstants.VALUE_STATUS_SUCCESS.equals(status)) {
			// Nothing to do
			return;
		}
		
		// If the validation is not successful, create validation objects for each problem
		if (event.has(CoreConstants.KEY_VALIDATION_RESULTS)) {
			JSONArray results = event.getJSONArray(CoreConstants.KEY_VALIDATION_RESULTS);
			for (int i = 0; i < results.length(); i++) {
				JSONObject result = results.getJSONObject(i);
				String severity = result.getString(CoreConstants.KEY_SEVERITY);
				String filename = result.getString(CoreConstants.KEY_FILENAME);
				String filepath = result.getString(CoreConstants.KEY_FILEPATH);
				String type = null;
				if (result.has(CoreConstants.KEY_TYPE)) {
					type = result.getString(CoreConstants.KEY_TYPE);
				}
				String details = result.getString(CoreConstants.KEY_DETAILS);
				String quickFixId = null;
				String quickFixDescription = null;
				if (result.has(CoreConstants.KEY_QUICKFIX) && supportsQuickFix(app, type, filename)) {
					JSONObject quickFix = result.getJSONObject(CoreConstants.KEY_QUICKFIX);
					quickFixId = quickFix.getString(CoreConstants.KEY_FIXID);
					quickFixDescription = quickFix.getString(CoreConstants.KEY_DESCRIPTION);
				}
				if (CoreConstants.VALUE_SEVERITY_WARNING.equals(severity)) {
					app.validationWarning(filepath, details, quickFixId, quickFixDescription);
				} else {
					app.validationError(filepath, details, quickFixId, quickFixDescription);
				}
			}
		} else {
			Logger.log("Validation event indicates failure but no validation results,"); //$NON-NLS-1$
		}
	}
	
	private boolean supportsQuickFix(CodewindApplication app, String type, String filename) {
		// The regenerate job only works in certain cases so only show the quickfix in the working cases
		if (!CoreConstants.VALUE_TYPE_MISSING.equals(type) || app.projectType.isType(ProjectType.TYPE_DOCKER)) {
			return false;
		}
		if (CoreConstants.DOCKERFILE.equals(filename)) {
			return true;
		}
		if (app.projectType.isType(ProjectType.TYPE_LIBERTY) && CoreConstants.DOCKERFILE_BUILD.equals(filename)) {
			return true;
		}
		return false;
	}

	boolean blockUntilFirstConnection() {
		final int delay = 100;
		final int timeout = 2500;
		int waited = 0;
		while(!hasConnected && waited < timeout) {
			try {
				Thread.sleep(delay);
				waited += delay;

				if (waited % (5 * delay) == 0) {
					Logger.log("Waiting for CodewindSocket initial connection"); //$NON-NLS-1$
				}
			}
			catch(InterruptedException e) {
				Logger.logError(e);
			}
		}
		Logger.log("CodewindSocket initialized in time ? " + hasConnected); //$NON-NLS-1$
		return hasConnected;
	}
}
