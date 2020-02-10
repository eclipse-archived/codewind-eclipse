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

package org.eclipse.codewind.core.internal.connection;

import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.eclipse.codewind.core.internal.CodewindApplication;
import org.eclipse.codewind.core.internal.CodewindApplicationFactory;
import org.eclipse.codewind.core.internal.CoreUtil;
import org.eclipse.codewind.core.internal.FileUtil;
import org.eclipse.codewind.core.internal.HttpUtil;
import org.eclipse.codewind.core.internal.HttpUtil.HttpResult;
import org.eclipse.codewind.core.internal.Logger;
import org.eclipse.codewind.core.internal.cli.AuthToken;
import org.eclipse.codewind.core.internal.cli.CLIUtil;
import org.eclipse.codewind.core.internal.cli.InstallUtil;
import org.eclipse.codewind.core.internal.connection.ConnectionEnv.TektonDashboard;
import org.eclipse.codewind.core.internal.console.ProjectLogInfo;
import org.eclipse.codewind.core.internal.constants.CoreConstants;
import org.eclipse.codewind.core.internal.messages.Messages;
import org.eclipse.codewind.filewatchers.eclipse.CodewindFilewatcherdConnection;
import org.eclipse.codewind.filewatchers.eclipse.ICodewindProjectTranslator;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.osgi.util.NLS;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Represents a connection to a Codewind instance
 */
public class CodewindConnection {

	public static final String CODEWIND_WORKSPACE_PROPERTY = "org.eclipse.codewind.internal.workspace"; //$NON-NLS-1$

	private String name;
	private URI baseUri;
	private String conid;
	private ConnectionEnv env = null;
	private String connectionErrorMsg = null;

	private CodewindSocket socket;
	private CodewindFilewatcherdConnection filewatcher;
	
	private volatile boolean isConnected = false;

	private Map<String, CodewindApplication> appMap = new LinkedHashMap<String, CodewindApplication>();

	public CodewindConnection(String name, URI uri, String conid) {
		setName(name);
		setBaseURI(uri);
		this.conid = conid;
	}
	
	public void connect(IProgressMonitor monitor) throws IOException, URISyntaxException, JSONException {
		if (isConnected) {
			return;
		}
		
		SubMonitor mon = SubMonitor.convert(monitor, 100);
		mon.setTaskName(NLS.bind(Messages.Connection_TaskLabel, this.baseUri));
		if (!waitForReady(mon.split(20))) {
			if (mon.isCanceled()) {
				return;
			}
			Logger.logError("Timed out waiting for Codewind to go into ready state.");
			onInitFail(Messages.Connection_ErrConnection_CodewindNotReady);
		}
		
		mon.split(25);
		env = new ConnectionEnv(getEnvData(this.baseUri));
		Logger.log("Codewind version is: " + env.getVersion());	// $NON-NLS-1$
		if (!isSupportedVersion(env.getVersion())) {
			Logger.logError("The detected version of Codewind is not supported: " + env.getVersion() + ", url: " + baseUri);	// $NON-NLS-1$	// $NON-NLS-2$
			onInitFail(NLS.bind(Messages.Connection_ErrConnection_OldVersion, env.getVersion(), InstallUtil.DEFAULT_INSTALL_VERSION));
		}
		if (mon.isCanceled()) {
			return;
		}

		socket = new CodewindSocket(this);
		if(!socket.blockUntilFirstConnection(mon.split(35))) {
			Logger.logError("Socket failed to connect: " + socket.socketUri);
			disconnect();
			throw new CodewindConnectionException(socket.socketUri);
		}
		if (mon.isCanceled()) {
			socket.close();
			return;
		}
		
		File cwctl = new File(CLIUtil.getCWCTLExecutable());
		filewatcher = new CodewindFilewatcherdConnection(baseUri.toString(), cwctl, new ICodewindProjectTranslator() {
			@Override
			public Optional<String> getProjectId(IProject project) {
				if (project != null) {
					CodewindApplication app = getAppByLocation(project.getLocation());
					if (app != null) {
						return Optional.of(app.projectID);
					}
				}
				return Optional.empty();
			}
		}, getAuthManager());
		
		if (mon.isCanceled()) {
			disconnect();
			return;
		}
		
		isConnected = true;

		Logger.log("Connected to: " + this); //$NON-NLS-1$
		
		mon.split(20);
		refreshApps(null);
	}

	public String getSocketNamespace() {
		return env.getSocketNamespace();
	}
	
	public CodewindSocket getSocket() {
		return socket;
	}

	private void onInitFail(String msg) throws ConnectException {
		Logger.log("Initializing Codewind connection failed: " + msg); //$NON-NLS-1$
		disconnect();
		throw new ConnectException(msg);
	}
	
	public void disconnect() {
		Logger.log("Disconnecting connection: " + this); //$NON-NLS-1$
		isConnected = false;
		if (socket != null) {
			socket.close();
		}
		if (filewatcher != null) {
			filewatcher.dispose();
		}
		for (CodewindApplication app : appMap.values()) {
			app.dispose();
		}
		appMap.clear();
	}

	public String getName() {
		if (name == null) {
			return "";
		}
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public URI getBaseURI() {
		return this.baseUri;
	}
	
	public void setBaseURI(URI uri) {
		if (uri != null && !uri.toString().endsWith("/")) { //$NON-NLS-1$
			this.baseUri = uri.resolve("/"); //$NON-NLS-1$
		} else {
			this.baseUri = uri;
		}
	}
	
	public String getConid() {
		return conid;
	}
	
	public void setUsername(String username) {
		// Do nothing - override as needed
	}
	
	public String getUsername() {
		// Override as needed
		return null;
	}
	
	public void setAuthToken(AuthToken authToken) {
		// Do nothing - override as needed
	}
	
	AuthToken getAuthToken(boolean update) throws IOException, JSONException {
		// Override as needed
		return null;
	}
	
	AuthManager getAuthManager() {
		// Override as needed
		return null;
	}

	private JSONObject getEnvData(URI baseUrl) throws JSONException, IOException {
		final URI envUrl = baseUrl.resolve(CoreConstants.APIPATH_ENV);

		String envResponse = null;
		try {
			HttpResult result = HttpUtil.get(envUrl, getAuthToken(false));
			if (hasAuthFailure(result)) {
				result = HttpUtil.get(envUrl, getAuthToken(true));
			}
			envResponse = result.response;
		} catch (IOException e) {
			Logger.logError("Error contacting Environment endpoint", e); //$NON-NLS-1$
			throw e;
		}

		return new JSONObject(envResponse);
	}
	
	public String getVersion(URI baseURI) {
		try {
			ConnectionEnv env = new ConnectionEnv(getEnvData(baseURI));
			return env.getVersion();
		} catch (Exception e) {
			Logger.logError("An error occurred trying to get the Codewind version.", e);
		}
		return null;
	}
	
	public static int compareVersions(String versionA, String versionB) throws NumberFormatException {
		if (versionA.equals(versionB)) {
			return 0;
		}
		if (CoreConstants.VERSION_LATEST.equals(versionA) || "x.x.dev".equals(versionA) || ConnectionEnv.UNKNOWN_VERSION.equals(versionB)) {
			return 1;
		}
		if (CoreConstants.VERSION_LATEST.equals(versionB) || "x.x.dev".equals(versionB) || ConnectionEnv.UNKNOWN_VERSION.equals(versionA)) {
			return -1;
		}
		
		String[] digitsA = versionA.split("\\.");
		String[] digitsB = versionB.split("\\.");
		
		for (int i = 0; i < digitsA.length; i++) {
			int valueA = Integer.parseInt(digitsA[i]);
			if (i >= digitsB.length) {
				// If versionA is longer than versionB and the extra digits are
				// non-zero then versionA is greater
				if (valueA != 0) {
					return 1;
				}
			} else {
				// If valueA is greater than valueB return 1
				// If valueA is less than valueB return -1.
				// If they are the same, keep going.
				int valueB = Integer.parseInt(digitsB[i]);
				if (valueA > valueB) {
					return 1;
				} else if (valueA < valueB) {
					return -1;
				}
			}
		}
		// If valueB is longer and the extra digits are not all zero, return -1
		if (digitsB.length > digitsA.length) {
			for (int i = digitsA.length; i < digitsB.length; i++) {
				int valueB = Integer.parseInt(digitsB[i]);
				if (valueB != 0) {
					return -1;
				}
			}
		}
		return 0;
	}
	
	public static boolean isSupportedVersion(String versionStr) {
		try {
			return compareVersions(versionStr, InstallUtil.DEFAULT_INSTALL_VERSION) >= 0;
		} catch (NumberFormatException e) {
			Logger.logError("Invalid version: " + versionStr, e);
			return false;
		}
	}

	public String getConnectionErrorMsg() {
		return this.connectionErrorMsg;
	}

	/**
	 * Refresh this connection's apps using the Codewind project list endpoint.
	 * If projectID is not null then only refresh the corresponding application.
	 */
	public void refreshApps(String projectID) {

		final URI projectsURL = baseUri.resolve(CoreConstants.APIPATH_PROJECT_LIST);

		try {
			HttpResult result = HttpUtil.get(projectsURL, getAuthToken(false));
			if (hasAuthFailure(result)) {
				result = HttpUtil.get(projectsURL, getAuthToken(true));
			}
			CodewindApplicationFactory.getAppsFromProjectsJson(this, result.response, projectID);
			Logger.log("App list update success"); //$NON-NLS-1$
		}
		catch(Exception e) {
			CoreUtil.openDialog(true, Messages.Connection_ErrGettingProjectListTitle, e.getMessage());
		}
	}
	
	public void addApp(CodewindApplication app) {
		synchronized(appMap) {
			appMap.put(app.projectID, app);
		}
	}

	public List<CodewindApplication> getApps() {
		synchronized(appMap) {
			return new ArrayList<CodewindApplication>(appMap.values());
		}
	}
	
	public Set<String> getAppIds() {
		synchronized(appMap) {
			return new HashSet<String>(appMap.keySet());
		}
	}

	public void removeApp(String projectID) {
		CodewindApplication app = null;
		synchronized(appMap) {
			app = appMap.remove(projectID);
		}
		if (app != null) {
			Logger.log("Removing the " + app.name + " application with id: " + projectID);
			CoreUtil.removeApplication(app);
			app.dispose();
		} else {
			Logger.log("No application found for deleted project: " + projectID); //$NON-NLS-1$
		}
	}

	/**
	 * @return The app with the given ID, if it exists in this Codewind instance, else null.
	 */
	public CodewindApplication getAppByID(String projectID) {
		synchronized(appMap) {
			return appMap.get(projectID);
		}
	}

	/*
	 * Only use this when searching for the application using the
	 * Codewind application name (not the Eclipse project name).
	 * Otherwise use getAppByLocation.
	 */
	public CodewindApplication getAppByName(String name) {
		synchronized(appMap) {
			for (CodewindApplication app : getApps()) {
				if (app.name.equals(name)) {
					return app;
				}
			}
		}
		Logger.log("No application found for name " + name); //$NON-NLS-1$
		return null;
	}
	
	public CodewindApplication getAppByLocation(IPath location) {
		if (location == null) {
			return null;
		}
		String canonicalLocation = FileUtil.getCanonicalPath(location.toOSString());
		synchronized(appMap) {
			for (CodewindApplication app : getApps()) {
				if (FileUtil.getCanonicalPath(app.fullLocalPath.toOSString()).equals(canonicalLocation)) {
					return app;
				}
			}
		}
		Logger.log("No application found for location: " + location); //$NON-NLS-1$
		return null;
	}
	
	public boolean waitForReady(IProgressMonitor monitor) throws IOException, JSONException {
		SubMonitor mon = SubMonitor.convert(monitor, 100);
		IOException exception = null;
		for (int i = 0; i < 10; i++) {
			try {
				mon.split(10);
				if (requestCodewindReady(500, 500)) {
					return true;
				}
				if (mon.isCanceled()) {
					return false;
				}
				Thread.sleep(500);
				if (mon.isCanceled()) {
					return false;
				}
			} catch (IOException e) {
				exception = e;
			} catch (InterruptedException e) {
				// Ignore
			}
		}
		if (exception != null) {
			throw exception;
		}
		return false;
	}
	
	public boolean requestCodewindReady(int connectTimeoutMS, int readTimeoutMS) throws IOException, JSONException {
		String endpoint = CoreConstants.APIPATH_READY;
		URI uri = baseUri.resolve(endpoint);
		HttpResult result = HttpUtil.get(uri, getAuthToken(false), connectTimeoutMS, readTimeoutMS);
		if (hasAuthFailure(result)) {
			result = HttpUtil.get(uri, getAuthToken(true), connectTimeoutMS, readTimeoutMS);
		}
		checkResult(result, uri, true);
		return "true".equals(result.response);
	}

	public void requestProjectRestart(CodewindApplication app, String launchMode)
			throws JSONException, IOException {

		String restartEndpoint = CoreConstants.APIPATH_PROJECT_LIST + "/" 	//$NON-NLS-1$
				+ app.projectID + "/" 										//$NON-NLS-1$
				+ CoreConstants.APIPATH_RESTART;

        URI url = baseUri.resolve(restartEndpoint);

		JSONObject restartProjectPayload = new JSONObject();
		restartProjectPayload.put(CoreConstants.KEY_START_MODE, launchMode);

		// This initiates the restart
		HttpResult result = HttpUtil.post(url, getAuthToken(false), restartProjectPayload);
		if (hasAuthFailure(result)) {
			result = HttpUtil.post(url, getAuthToken(true), restartProjectPayload);
		}
		if (!result.isGoodResponse) {
			final String msg = String.format("Received bad response from server %d with error message %s", //$NON-NLS-1$
					result.responseCode, result.error);
			throw new IOException(msg);
		}
		app.invalidatePorts();
	}
	
	public void requestProjectOpenClose(CodewindApplication app, boolean enable)
			throws JSONException, IOException {
		
		String action = enable ? CoreConstants.APIPATH_OPEN : CoreConstants.APIPATH_CLOSE;

		String restartEndpoint = CoreConstants.APIPATH_PROJECT_LIST + "/" 	//$NON-NLS-1$
				+ app.projectID + "/" 										//$NON-NLS-1$
				+ action;

		URI url = baseUri.resolve(restartEndpoint);

		// This initiates the restart
		HttpResult result = HttpUtil.put(url, getAuthToken(false));
		if (hasAuthFailure(result)) {
			result = HttpUtil.put(url, getAuthToken(true));
		}
		if (!result.isGoodResponse) {
			final String msg = String.format("Received bad response from server %d with error message %s", //$NON-NLS-1$
					result.responseCode, result.error);
			throw new IOException(msg);
		}
	}

	/**
	 * Get the project status endpoint, and filter the response for the JSON corresponding to the given project.
	 * @return
	 * 	The JSON containing the status info for the given project,
	 * 	or null if the project is not found in the status info.
	 */
	public JSONObject requestProjectStatus(CodewindApplication app) throws IOException, JSONException {
		final URI statusUrl = baseUri.resolve(CoreConstants.APIPATH_PROJECT_LIST);

		HttpResult result = HttpUtil.get(statusUrl, getAuthToken(false));
		if (hasAuthFailure(result)) {
			result = HttpUtil.get(statusUrl, getAuthToken(true));
		}

		if (!result.isGoodResponse) {
			final String msg = String.format("Received bad response from server %d with error message %s", //$NON-NLS-1$
					result.responseCode, result.error);
			throw new IOException(msg);
		}
		else if (result.response == null) {
			// I don't think this will ever happen.
			throw new IOException("Server returned good response code, but null response when getting initial state"); //$NON-NLS-1$
		}

		JSONArray allProjectStatuses = new JSONArray(result.response);
		for (int i = 0; i < allProjectStatuses.length(); i++) {
			JSONObject projectStatus = allProjectStatuses.getJSONObject(i);
			if (projectStatus.getString(CoreConstants.KEY_PROJECT_ID).equals(app.projectID)) {
				// Success - found the project of interest
				return projectStatus;
			}
		}

		Logger.log("Didn't find status info for project " + app.name); //$NON-NLS-1$
		return null;
	}
	
	public JSONObject requestProjectMetricsStatus(CodewindApplication app) throws IOException, JSONException {
		String endpoint = CoreConstants.APIPATH_PROJECT_LIST + "/" 	//$NON-NLS-1$
				+ app.projectID + "/" 								//$NON-NLS-1$
				+ CoreConstants.APIPATH_METRICS_STATUS;

		URI uri = baseUri.resolve(endpoint);
		HttpResult result = HttpUtil.get(uri, getAuthToken(false));
		if (hasAuthFailure(result)) {
			result = HttpUtil.get(uri, getAuthToken(true));
		}
		checkResult(result, uri, true);
		return new JSONObject(result.response);
	}

	/**
	 * Request a build on an application
	 * @param app The app to build
	 */
	public void requestProjectBuild(CodewindApplication app, String action) throws JSONException, IOException {
		String buildEndpoint = CoreConstants.APIPATH_PROJECT_LIST + "/" //$NON-NLS-1$
				+ app.projectID + "/" //$NON-NLS-1$
				+ CoreConstants.APIPATH_BUILD;

		URI url = baseUri.resolve(buildEndpoint);

		JSONObject buildPayload = new JSONObject();
		buildPayload.put(CoreConstants.KEY_ACTION, action);

		// This initiates the build
		HttpResult result = HttpUtil.post(url, getAuthToken(false), buildPayload);
		if (hasAuthFailure(result)) {
			result = HttpUtil.post(url, getAuthToken(true), buildPayload);
		}
		checkResult(result, url, false);
	}
	
	public List<ProjectLogInfo> requestProjectLogs(CodewindApplication app) throws JSONException, IOException {
		List<ProjectLogInfo> logList = new ArrayList<ProjectLogInfo>();

		String endpoint = CoreConstants.APIPATH_PROJECT_LIST + "/"	//$NON-NLS-1$
				+ app.projectID + "/"								//$NON-NLS-1$
				+ CoreConstants.APIPATH_LOGS;
		
		URI uri = baseUri.resolve(endpoint);
		HttpResult result = HttpUtil.get(uri, getAuthToken(false));
		if (hasAuthFailure(result)) {
			result = HttpUtil.get(uri, getAuthToken(true));
		}
		checkResult(result, uri, true);
        
		JSONObject logs = new JSONObject(result.response);
		JSONArray buildLogs = logs.getJSONArray(CoreConstants.KEY_LOG_BUILD);
		logList.addAll(getLogs(buildLogs, CoreConstants.KEY_LOG_BUILD));
		JSONArray appLogs = logs.getJSONArray(CoreConstants.KEY_LOG_APP);
		logList.addAll(getLogs(appLogs, CoreConstants.KEY_LOG_APP));
		return logList;
	}
	
	public static List<ProjectLogInfo> getLogs(JSONArray logs, String type) throws JSONException {
		List<ProjectLogInfo> logList = new ArrayList<ProjectLogInfo>();
		if (logs != null) {
			for (int i = 0; i < logs.length(); i++) {
				JSONObject log = logs.getJSONObject(i);
				if (log.has(CoreConstants.KEY_LOG_NAME)) {
					String logName = log.getString(CoreConstants.KEY_LOG_NAME);
					if ("-".equals(logName)) {
						continue;
					}
					String workspacePath = null;
					if (log.has(CoreConstants.KEY_LOG_WORKSPACE_PATH)) {
						workspacePath = log.getString(CoreConstants.KEY_LOG_WORKSPACE_PATH);
					}
					ProjectLogInfo logInfo = new ProjectLogInfo(type, logName, workspacePath);
					logList.add(logInfo);
				} else {
					Logger.log("An item in the log list does not have the key: " + CoreConstants.KEY_LOG_NAME);
				}
			}
		}
		return logList;
	}
	
	public void requestEnableLogStream(CodewindApplication app, ProjectLogInfo logInfo) throws IOException, JSONException {
		String endpoint = CoreConstants.APIPATH_PROJECT_LIST + "/" 	//$NON-NLS-1$
				+ app.projectID + "/" 								//$NON-NLS-1$
				+ CoreConstants.APIPATH_LOGS + "/"					//$NON-NLS-1$
				+ logInfo.type + "/"								//$NON-NLS-1$
				+ logInfo.logName;
		
		URI uri = baseUri.resolve(endpoint);
		HttpResult result = HttpUtil.post(uri, getAuthToken(false));
		if (hasAuthFailure(result)) {
			result = HttpUtil.post(uri, getAuthToken(true));
		}
        checkResult(result, uri, false);
	}
	
	public void requestDisableLogStream(CodewindApplication app, ProjectLogInfo logInfo) throws IOException, JSONException {
		String endpoint = CoreConstants.APIPATH_PROJECT_LIST + "/" 	//$NON-NLS-1$
				+ app.projectID + "/" 								//$NON-NLS-1$
				+ CoreConstants.APIPATH_LOGS + "/"					//$NON-NLS-1$
				+ logInfo.type + "/"								//$NON-NLS-1$
				+ logInfo.logName;
		
		URI uri = baseUri.resolve(endpoint);
		HttpResult result = HttpUtil.delete(uri, getAuthToken(false));
		if (hasAuthFailure(result)) {
			result = HttpUtil.delete(uri, getAuthToken(true));
		}
        checkResult(result, uri, false);
	}
	
	public void requestValidate(CodewindApplication app) throws JSONException, IOException {
		String endpoint = CoreConstants.APIPATH_PROJECT_LIST + "/"	//$NON-NLS-1$
					+ app.projectID + "/"	//$NON-NLS-1$
					+ CoreConstants.APIPATH_VALIDATE;
		
		URI url = baseUri.resolve(endpoint);
		
		JSONObject buildPayload = new JSONObject();
		buildPayload.put(CoreConstants.KEY_PROJECT_TYPE, app.projectType.getId());
		
		HttpResult result = HttpUtil.post(url, getAuthToken(false), buildPayload);
		if (hasAuthFailure(result)) {
			result = HttpUtil.post(url, getAuthToken(true), buildPayload);
		}
		if (!result.isGoodResponse) {
			final String msg = String.format("Received bad response from server %d with error message %s", //$NON-NLS-1$
					result.responseCode, result.error);
			throw new IOException(msg);
		}
	}
	
	public void requestValidateGenerate(CodewindApplication app) throws JSONException, IOException {
		String endpoint = CoreConstants.APIPATH_PROJECT_LIST + "/"	//$NON-NLS-1$
				+ app.projectID + "/"	//$NON-NLS-1$
				+ CoreConstants.APIPATH_VALIDATE_GENERATE;
		
		URI url = baseUri.resolve(endpoint);
		
		JSONObject buildPayload = new JSONObject();
		buildPayload.put(CoreConstants.KEY_PROJECT_TYPE, app.projectType.getId());
		buildPayload.put(CoreConstants.KEY_AUTO_GENERATE, true);
		
		HttpResult result = HttpUtil.post(url, getAuthToken(false), buildPayload);
		if (hasAuthFailure(result)) {
			result = HttpUtil.post(url, getAuthToken(true), buildPayload);
		}
		if (!result.isGoodResponse) {
			final String msg = String.format("Received bad response from server %d with error message %s", //$NON-NLS-1$
					result.responseCode, result.error);
			throw new IOException(msg);
		}
		
		// Perform validation again to clear the errors/warnings that have been fixed
		requestValidate(app);
	}
	
	public JSONObject requestProjectCapabilities(CodewindApplication app) throws IOException, JSONException {
		final URI statusUrl = baseUri.resolve(CoreConstants.APIPATH_PROJECT_LIST + "/" + app.projectID + "/" + CoreConstants.APIPATH_CAPABILITIES);

		HttpResult result = HttpUtil.get(statusUrl, getAuthToken(false));
		if (hasAuthFailure(result)) {
			result = HttpUtil.get(statusUrl, getAuthToken(true));
		}

		if (!result.isGoodResponse) {
			final String msg = String.format("Received bad response from server %d with error message %s", //$NON-NLS-1$
					result.responseCode, result.error);
			throw new IOException(msg);
		} else if (result.response == null) {
			// I don't think this will ever happen.
			throw new IOException("Server returned good response code, but empty content when getting project capabilities"); //$NON-NLS-1$
		}

		JSONObject capabilities = new JSONObject(result.response);
		return capabilities;
	}
	
	public List<ProjectTypeInfo> requestProjectTypes() throws IOException, JSONException {
		List<ProjectTypeInfo> projectTypes = new ArrayList<ProjectTypeInfo>();
		final URI uri = baseUri.resolve(CoreConstants.APIPATH_BASE + "/" + CoreConstants.APIPATH_PROJECT_TYPES);
		HttpResult result = HttpUtil.get(uri, getAuthToken(false));
		if (hasAuthFailure(result)) {
			result = HttpUtil.get(uri, getAuthToken(true));
		}
		checkResult(result, uri, true);
		
		JSONArray array = new JSONArray(result.response);
		for (int i = 0; i < array.length(); i++) {
			projectTypes.add(new ProjectTypeInfo(array.getJSONObject(i)));
		}
		return projectTypes;
	}
	
	public List<RegistryInfo> requestRegistryList() throws IOException, JSONException {
		List<RegistryInfo> regList = new ArrayList<RegistryInfo>();
		URI uri = baseUri.resolve(CoreConstants.APIPATH_BASE + "/" + CoreConstants.APIPATH_REGISTRYSECRETS);
		HttpResult result = HttpUtil.get(uri, getAuthToken(false));
		if (hasAuthFailure(result)) {
			result = HttpUtil.get(uri, getAuthToken(true));
		}
		checkResult(result, uri, true);
		
		JSONArray array = new JSONArray(result.response);
		for (int i = 0; i < array.length(); i++) {
			regList.add(new RegistryInfo(array.getJSONObject(i)));
		}
		
		return regList;
	}
	
	public void requestAddRegistry(String address, String username, String password) throws IOException, JSONException {
		final URI uri = baseUri.resolve(CoreConstants.APIPATH_BASE + "/" + CoreConstants.APIPATH_REGISTRYSECRETS);
		
		JSONObject credentials = new JSONObject();
		credentials.put(CoreConstants.KEY_USERNAME, username);
		credentials.put(CoreConstants.KEY_PASSWORD, password);
		String encoding = Base64.getEncoder().encodeToString(credentials.toString().getBytes("UTF-8"));
		JSONObject payload = new JSONObject();
		payload.put(CoreConstants.KEY_ADDRESS, address);
		payload.put(CoreConstants.KEY_CREDENTIALS, encoding);
		
		HttpResult result = HttpUtil.post(uri, getAuthToken(false), payload);
		if (hasAuthFailure(result)) {
			result = HttpUtil.post(uri, getAuthToken(true), payload);
		}
		checkResult(result, uri, true);
	}
	
	public void requestRemoveRegistry(String address, String username) throws IOException, JSONException {
		final URI uri = baseUri.resolve(CoreConstants.APIPATH_BASE + "/" + CoreConstants.APIPATH_REGISTRYSECRETS);
		
		JSONObject payload = new JSONObject();
		payload.put(CoreConstants.KEY_ADDRESS, address);
//		payload.put(CoreConstants.KEY_USERNAME, username);
		
		HttpResult result = HttpUtil.delete(uri, getAuthToken(false), payload);
		if (hasAuthFailure(result)) {
			result = HttpUtil.delete(uri, getAuthToken(true), payload);
		}
		checkResult(result, uri, true);
	}

	public void requestSetPushRegistry(String address, String namespace) throws IOException, JSONException {
		final URI uri = baseUri.resolve(CoreConstants.APIPATH_BASE + "/" + CoreConstants.APIPATH_IMAGEPUSHREGISTRY);
		JSONObject payload = new JSONObject();
		payload.put(CoreConstants.KEY_ADDRESS, address);
		payload.put(CoreConstants.KEY_NAMESPACE, namespace);
		payload.put(CoreConstants.KEY_OPERATION, CoreConstants.VALUE_OP_SET);
		
		HttpResult result = HttpUtil.post(uri, getAuthToken(false), payload);
		if (hasAuthFailure(result)) {
			result = HttpUtil.post(uri, getAuthToken(true), payload);
		}
		checkResult(result, uri, false);
	}
	
	public ImagePushRegistryInfo requestGetPushRegistry() throws IOException, JSONException {
		final URI uri = baseUri.resolve(CoreConstants.APIPATH_BASE + "/" + CoreConstants.APIPATH_IMAGEPUSHREGISTRY);
		HttpResult result = HttpUtil.get(uri, getAuthToken(false));
		if (hasAuthFailure(result)) {
			result = HttpUtil.get(uri, getAuthToken(true));
		}
		checkResult(result, uri, true);
		
		JSONObject obj = new JSONObject(result.response);
		if (obj.has(CoreConstants.KEY_IMAGE_PUSH_REGISTRY) && obj.getBoolean(CoreConstants.KEY_IMAGE_PUSH_REGISTRY)) {
			return new ImagePushRegistryInfo(obj);
		}
		return null;
	}
	
	public boolean requestHasPushRegistry() throws IOException, JSONException {
		return requestGetPushRegistry() != null;
	}
	
	public void requestDeletePushRegistry(String address) throws IOException, JSONException {
		final URI uri = baseUri.resolve(CoreConstants.APIPATH_BASE + "/" + CoreConstants.APIPATH_IMAGEPUSHREGISTRY);
		JSONObject payload = new JSONObject();
		payload.put(CoreConstants.KEY_ADDRESS, address);
		
		HttpResult result = HttpUtil.delete(uri, getAuthToken(false), payload);
		if (hasAuthFailure(result)) {
			result = HttpUtil.delete(uri, getAuthToken(true), payload);
		}
		checkResult(result, uri, false);
	}
	
	public void requestInjectMetrics(String projectID, boolean enable) throws IOException, JSONException {
		String endpoint = CoreConstants.APIPATH_PROJECT_LIST + "/"	//$NON-NLS-1$
				+ projectID + "/"	//$NON-NLS-1$
				+ CoreConstants.APIPATH_INJECT_METRICS;
		
		URI uri = baseUri.resolve(endpoint);
		JSONObject buildPayload = new JSONObject();
		buildPayload.put(CoreConstants.KEY_INJECT_METRICS_ENABLE, enable);
		
		HttpResult result = HttpUtil.post(uri, getAuthToken(false), buildPayload);
		if (hasAuthFailure(result)) {
			result = HttpUtil.post(uri, getAuthToken(true), buildPayload);
		}
		checkResult(result, uri, false);
		CoreUtil.updateConnection(this);
	}
	
	private boolean hasAuthFailure(HttpResult result) {
		return result.responseCode == 302;
	}

	private void checkResult(HttpResult result, URI uri, boolean checkContent) throws IOException {
		if (!result.isGoodResponse) {
			final String msg = String.format("Received bad response code %d for uri %s with error message %s", //$NON-NLS-1$
					result.responseCode, uri, result.error);
			throw new IOException(msg);
		} else if (checkContent && result.response == null) {
			// I don't think this will ever happen.
			throw new IOException("Server returned good response code, but the content of the result is null for uri: " + uri); //$NON-NLS-1$
		}
	}
	
	public boolean isConnected() {
		return isConnected;
	}

	/**
	 * Called by the CodewindSocket when the socket.io connection goes down.
	 */
	public synchronized void onConnectionError() {
		Logger.log("Connection to " + baseUri + " lost"); //$NON-NLS-1$ //$NON-NLS-2$
		isConnected = false;
		synchronized(appMap) {
			appMap.clear();
		}
		// Update everything as Codewind might be down as well
		CoreUtil.updateAll();
	}

	/**
	 * Called by the CodewindSocket when the socket.io connection is working.
	 */
	public synchronized void clearConnectionError() {
		Logger.log("Connection to " + baseUri + " restored"); //$NON-NLS-1$ //$NON-NLS-2$
		
		// Reset any cached information in case it has changed
		try {
			String oldSocketNS = env.getSocketNamespace();
			env = new ConnectionEnv(getEnvData(baseUri));
			if (!isSupportedVersion(env.getVersion())) {
				Logger.logError("The detected version of Codewind after reconnect is not supported: " + env.getVersion());
				this.connectionErrorMsg = NLS.bind(Messages.Connection_ErrConnection_OldVersion, env.getVersion(), InstallUtil.DEFAULT_INSTALL_VERSION);
				CoreUtil.updateConnection(this);
				return;
			}
			
			String socketNS = env.getSocketNamespace();
			if ((socketNS != null && !socketNS.equals(oldSocketNS)) || (oldSocketNS != null && !oldSocketNS.equals(socketNS))) {
				// The socket namespace has changed so need to recreate the socket
				socket.close();
				socket = new CodewindSocket(this);
				if(!socket.blockUntilFirstConnection(new NullProgressMonitor())) {
					// Still not connected
					Logger.logError("Failed to create a new socket with updated URI: " + socket.socketUri);
					// Clear the message so that it just shows the basic disconnected message
					this.connectionErrorMsg = null;
					CoreUtil.updateAll();
					return;
				}
			}
		} catch (Exception e) {
			Logger.logError("An exception occurred while trying to update the connection information", e);
			this.connectionErrorMsg = Messages.Connection_ErrConnection_UpdateCacheException;
			CoreUtil.updateAll();
			return;
		}
		
		this.connectionErrorMsg = null;
		isConnected = true;
		refreshApps(null);
		CoreUtil.updateAll();
	}

	@Override
	public String toString() {
		return String.format("%s @ name=%s baseUrl=%s conid=%s", //$NON-NLS-1$
				CodewindConnection.class.getSimpleName(), name, baseUri == null ? "unknown" : baseUri, conid == null ? "<none>" : conid);
	}

	public TektonDashboard getTektonDashboard() {
		return env.getTektonDashboard(); 
	}
	
	public URI getNewProjectURI() {
		return getProjectURI(CoreConstants.QUERY_NEW_PROJECT);
	}
	
	public URI getImportProjectURI() {
		return getProjectURI(CoreConstants.QUERY_IMPORT_PROJECT);
	}
	
	private URI getProjectURI(String projectQuery) {
		try {
			URI uri = baseUri;
			String query = projectQuery + "=" + CoreConstants.VALUE_TRUE;
			uri = new URI(uri.getScheme(), uri.getAuthority(), uri.getPath(), query, uri.getFragment());
			return uri;
		} catch (Exception e) {
			Logger.logError("Failed to get the project URI for the query: " + projectQuery, e);  //$NON-NLS-1$
		}
		return null;
	}

	public boolean isLocal() {
		return false;
	}
}
