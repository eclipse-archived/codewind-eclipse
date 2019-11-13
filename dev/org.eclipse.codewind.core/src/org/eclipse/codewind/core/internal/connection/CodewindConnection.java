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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Base64.Encoder;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.zip.Deflater;

import org.eclipse.codewind.core.internal.CodewindApplication;
import org.eclipse.codewind.core.internal.CodewindApplicationFactory;
import org.eclipse.codewind.core.internal.CoreUtil;
import org.eclipse.codewind.core.internal.HttpUtil;
import org.eclipse.codewind.core.internal.HttpUtil.HttpResult;
import org.eclipse.codewind.core.internal.Logger;
import org.eclipse.codewind.core.internal.cli.AuthToken;
import org.eclipse.codewind.core.internal.cli.CLIUtil;
import org.eclipse.codewind.core.internal.cli.ConnectionUtil;
import org.eclipse.codewind.core.internal.cli.InstallUtil;
import org.eclipse.codewind.core.internal.connection.ConnectionEnv.TektonDashboard;
import org.eclipse.codewind.core.internal.console.ProjectLogInfo;
import org.eclipse.codewind.core.internal.constants.CoreConstants;
import org.eclipse.codewind.core.internal.constants.ProjectType;
import org.eclipse.codewind.core.internal.messages.Messages;
import org.eclipse.codewind.filewatchers.eclipse.CodewindFilewatcherdConnection;
import org.eclipse.codewind.filewatchers.eclipse.ICodewindProjectTranslator;
import org.eclipse.core.resources.IProject;
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
	private AuthToken authToken;
	private ConnectionEnv env = null;
	private String connectionErrorMsg = null;

	private CodewindSocket socket;
	private CodewindFilewatcherdConnection filewatcher;
	
	private volatile boolean isConnected = false;

	private Map<String, CodewindApplication> appMap = new LinkedHashMap<String, CodewindApplication>();

	public CodewindConnection(String name, URI uri, String conid, AuthToken authToken) {
		setName(name);
		setBaseURI(uri);
		this.conid = conid;
		this.authToken = authToken;
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
		env = new ConnectionEnv(getEnvData(this.baseUri, authToken));
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
					CodewindApplication app = getAppByName(project.getName());
					if (app != null) {
						return Optional.of(app.projectID);
					}
				}
				return Optional.empty();
			}
		});
		if (mon.isCanceled()) {
			close();
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

	/**
	 * Call this when the connection is removed.
	 */
	public void close() {
		disconnect();
		Logger.log("Removing connection: " + this); //$NON-NLS-1$
		if (conid != null) {
			try {
				ConnectionUtil.removeConnection(name, conid, new NullProgressMonitor());
			} catch (Exception e) {
				Logger.logError("An error occurred trying to de-register the connection: " + this); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
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
	
	public void setAuthToken(AuthToken authToken) {
		this.authToken = authToken;
	}

	private static JSONObject getEnvData(URI baseUrl, AuthToken auth) throws JSONException, IOException {
		final URI envUrl = baseUrl.resolve(CoreConstants.APIPATH_ENV);

		String envResponse = null;
		try {
			envResponse = HttpUtil.get(envUrl, auth).response;
		} catch (IOException e) {
			Logger.logError("Error contacting Environment endpoint", e); //$NON-NLS-1$
			throw e;
		}

		return new JSONObject(envResponse);
	}
	
	public static String getVersion(URI baseURI, AuthToken auth) {
		try {
			ConnectionEnv env = new ConnectionEnv(getEnvData(baseURI, auth));
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
		if (CoreConstants.VERSION_LATEST.equals(versionA) || ConnectionEnv.UNKNOWN_VERSION.equals(versionB)) {
			return 1;
		}
		if (CoreConstants.VERSION_LATEST.equals(versionB) || ConnectionEnv.UNKNOWN_VERSION.equals(versionA)) {
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
			String projectsResponse = HttpUtil.get(projectsURL, authToken).response;
			CodewindApplicationFactory.getAppsFromProjectsJson(this, projectsResponse, projectID);
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
	
	public boolean waitForReady(IProgressMonitor monitor) throws IOException {
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
	
	public boolean requestCodewindReady(int connectTimeoutMS, int readTimeoutMS) throws IOException {
		String endpoint = CoreConstants.APIPATH_READY;
		URI uri = baseUri.resolve(endpoint);
		HttpResult result = HttpUtil.get(uri, authToken, connectTimeoutMS, readTimeoutMS);
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
		HttpResult result = HttpUtil.post(url, authToken, restartProjectPayload);
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
		HttpResult result = HttpUtil.put(url, authToken);
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

		HttpResult result = HttpUtil.get(statusUrl, authToken);

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
		HttpResult result = HttpUtil.get(uri, authToken);
		checkResult(result, uri, true);
		return new JSONObject(result.response);
	}

	/**
	 * Request a build on an application
	 * @param app The app to build
	 */
	public void requestProjectBuild(CodewindApplication app, String action) throws JSONException, IOException {

		// Synchronise the source by clearing the old version and uploading the latest.
		long syncTime = System.currentTimeMillis();
		String[] fileList = requestUploadsRecursively(app.projectID, app.fullLocalPath.toOSString(), app.getLastSync());
		requestProjectClear(app, fileList);
		Logger.log("Sync complete for " + app.name + " to "+  app.connection.baseUri + " in " +  (System.currentTimeMillis() - syncTime) +"ms");
		app.setLastSync(syncTime);

		String buildEndpoint = CoreConstants.APIPATH_PROJECT_LIST + "/" //$NON-NLS-1$
				+ app.projectID + "/" //$NON-NLS-1$
				+ CoreConstants.APIPATH_BUILD;

		URI url = baseUri.resolve(buildEndpoint);

		JSONObject buildPayload = new JSONObject();
		buildPayload.put(CoreConstants.KEY_ACTION, action);

		// This initiates the build
		HttpUtil.post(url, authToken, buildPayload);
	}
	
	public List<ProjectLogInfo> requestProjectLogs(CodewindApplication app) throws JSONException, IOException {
		List<ProjectLogInfo> logList = new ArrayList<ProjectLogInfo>();

		String endpoint = CoreConstants.APIPATH_PROJECT_LIST + "/"	//$NON-NLS-1$
				+ app.projectID + "/"								//$NON-NLS-1$
				+ CoreConstants.APIPATH_LOGS;
		
		URI uri = baseUri.resolve(endpoint);
		HttpResult result = HttpUtil.get(uri, authToken);
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
	
	public void requestEnableLogStream(CodewindApplication app, ProjectLogInfo logInfo) throws IOException {
		String endpoint = CoreConstants.APIPATH_PROJECT_LIST + "/" 	//$NON-NLS-1$
				+ app.projectID + "/" 								//$NON-NLS-1$
				+ CoreConstants.APIPATH_LOGS + "/"					//$NON-NLS-1$
				+ logInfo.type + "/"								//$NON-NLS-1$
				+ logInfo.logName;
		
		URI uri = baseUri.resolve(endpoint);
		HttpResult result = HttpUtil.post(uri, authToken);
        checkResult(result, uri, false);
	}
	
	public void requestDisableLogStream(CodewindApplication app, ProjectLogInfo logInfo) throws IOException {
		String endpoint = CoreConstants.APIPATH_PROJECT_LIST + "/" 	//$NON-NLS-1$
				+ app.projectID + "/" 								//$NON-NLS-1$
				+ CoreConstants.APIPATH_LOGS + "/"					//$NON-NLS-1$
				+ logInfo.type + "/"								//$NON-NLS-1$
				+ logInfo.logName;
		
		URI uri = baseUri.resolve(endpoint);
		HttpResult result = HttpUtil.delete(uri, authToken);
        checkResult(result, uri, false);
	}
	
	public void requestValidate(CodewindApplication app) throws JSONException, IOException {
		String endpoint = CoreConstants.APIPATH_PROJECT_LIST + "/"	//$NON-NLS-1$
					+ app.projectID + "/"	//$NON-NLS-1$
					+ CoreConstants.APIPATH_VALIDATE;
		
		URI url = baseUri.resolve(endpoint);
		
		JSONObject buildPayload = new JSONObject();
		buildPayload.put(CoreConstants.KEY_PROJECT_TYPE, app.projectType.getId());
		
		HttpResult result = HttpUtil.post(url, authToken, buildPayload);
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
		
		HttpResult result = HttpUtil.post(url, authToken, buildPayload);
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

		HttpResult result = HttpUtil.get(statusUrl, authToken);

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
	
	public List<ProjectTemplateInfo> requestProjectTemplates(boolean enabledOnly) throws IOException, JSONException, URISyntaxException {
		List<ProjectTemplateInfo> templates = new ArrayList<ProjectTemplateInfo>();
		URI uri = baseUri.resolve(CoreConstants.APIPATH_BASE + "/" + CoreConstants.APIPATH_TEMPLATES);
		if (enabledOnly) {
			String query = CoreConstants.QUERY_SHOW_ENABLED_ONLY + "=true";
			uri = new URI(uri.getScheme(), uri.getAuthority(), uri.getPath(), query, uri.getFragment());
		}
		HttpResult result = HttpUtil.get(uri, authToken);
		checkResult(result, uri, false);
		
		// A response code of 204 means there are no available templates so just return the empty template list
		if (result.responseCode == 204) {
			return templates;
		}
		JSONArray templateArray = new JSONArray(result.response);
		for (int i = 0; i < templateArray.length(); i++) {
			templates.add(new ProjectTemplateInfo(templateArray.getJSONObject(i)));
		}
		return templates;
	}
	
	public List<RepositoryInfo> requestRepositories() throws IOException, JSONException {
		List<RepositoryInfo> repos = new ArrayList<RepositoryInfo>();
		final URI uri = baseUri.resolve(CoreConstants.APIPATH_BASE + "/" + CoreConstants.APIPATH_REPOSITORIES);
		HttpResult result = HttpUtil.get(uri, authToken);
		checkResult(result, uri, true);
		
		JSONArray repoArray = new JSONArray(result.response);
		for (int i = 0; i < repoArray.length(); i++) {
			repos.add(new RepositoryInfo(repoArray.getJSONObject(i)));
		}
		return repos;
	}
	
	public void requestRepoEnable(String repoUrl, boolean enable) throws IOException, JSONException {
		final URI uri = baseUri.resolve(CoreConstants.APIPATH_BASE + "/" + CoreConstants.APIPATH_BATCH_REPOSITORIES);
		JSONArray payload = new JSONArray();
		JSONObject obj = new JSONObject();
		obj.put(CoreConstants.KEY_OP, CoreConstants.VALUE_OP_ENABLE);
		obj.put(CoreConstants.KEY_URL, repoUrl);
		obj.put(CoreConstants.KEY_VALUE, enable ? "true" : "false");
		payload.put(obj);
		
		HttpResult result = HttpUtil.patch(uri, payload);
		checkResult(result, uri, false);
	}
	
	public void requestRepoAdd(String url, String name, String description) throws IOException, JSONException {
		final URI uri = baseUri.resolve(CoreConstants.APIPATH_BASE + "/" + CoreConstants.APIPATH_REPOSITORIES);
		JSONObject payload = new JSONObject();
		payload.put(RepositoryInfo.URL_KEY, url);
		if (name != null && !name.isEmpty()) {
			payload.put(RepositoryInfo.NAME_KEY, name);
		}
		if (description != null && !description.isEmpty()) {
			payload.put(RepositoryInfo.DESCRIPTION_KEY, description);
		}
		payload.put(RepositoryInfo.ENABLED_KEY, true);
		
		HttpResult result = HttpUtil.post(uri, authToken, payload);
		checkResult(result, uri, false);
	}
	
	public void requestRepoRemove(String url) throws IOException, JSONException {
		final URI uri = baseUri.resolve(CoreConstants.APIPATH_BASE + "/" + CoreConstants.APIPATH_REPOSITORIES);
		JSONObject payload = new JSONObject();
		payload.put(RepositoryInfo.URL_KEY, url);
		
		HttpResult result = HttpUtil.delete(uri, authToken, payload);
		checkResult(result, uri, false);
	}

	public void requestProjectBind(String name, String path, String language, String projectType)
			throws JSONException, IOException {

		String bindStartEndpoint = CoreConstants.APIPATH_PROJECT_LIST + "/" + CoreConstants.APIPATH_PROJECT_REMOTE_BIND_START;
		URI bindStartUri = baseUri.resolve(bindStartEndpoint);
		long initialSyncTime = System.currentTimeMillis();

		JSONObject bindStartPayload = new JSONObject();
		bindStartPayload.put(CoreConstants.KEY_NAME, name);
		bindStartPayload.put(CoreConstants.KEY_PATH, CoreUtil.getContainerPath(path));
		bindStartPayload.put(CoreConstants.KEY_LANGUAGE, language);
		if (projectType == null) {
			projectType = ProjectType.getTypeFromLanguage(language).getId();
		}
		if (projectType != null) {
			bindStartPayload.put(CoreConstants.KEY_PROJECT_TYPE, projectType);
		}
		bindStartPayload.put(CoreConstants.KEY_AUTO_BUILD, true);

		HttpResult bindStartResult = HttpUtil.post(bindStartUri, authToken, bindStartPayload, 300);
		checkResult(bindStartResult, bindStartUri, false);

		JSONObject projectInf = new JSONObject(bindStartResult.response);
		String projectID = projectInf.getString(CoreConstants.KEY_PROJECT_ID);

		requestUploadsRecursively(projectID, path, 0);
		String bindEndEndpoint = CoreConstants.APIPATH_PROJECT_LIST + "/" + projectID + "/" + CoreConstants.APIPATH_PROJECT_REMOTE_BIND_END;
		URI bindEndUri = baseUri.resolve(bindEndEndpoint);

		HttpResult bindEndResult = HttpUtil.post(bindEndUri, authToken);
		checkResult(bindEndResult, bindEndUri, false);

		CodewindApplication newApp = getAppByID(projectID);
		if (newApp != null) {
			newApp.setLastSync(initialSyncTime);
		}
		Logger.log("Initial project upload complete for " + name + " to "+  baseUri + " in " +  (System.currentTimeMillis() - initialSyncTime) +"ms");

		CoreUtil.updateConnection(this);
	}
	
	public String[] requestUploadsRecursively(String projectId, String path, long lastSyncMs) throws JSONException, IOException {

		List<Path> fileList = new LinkedList<Path>();
		Path basePath = Paths.get(path);
		Files.walk(basePath).forEach((Path fullPath) -> {
			Path relative = basePath.relativize(fullPath);
			fileList.add(relative);
			try {
				if (Files.getLastModifiedTime(fullPath).toMillis() > lastSyncMs && !Files.isDirectory(fullPath)) {
					requestUpload(projectId, fullPath, relative.toString());
				}
			} catch (JSONException | IOException e) {
				Logger.logError(e);
			}
		});
		String[] fileArray = fileList.stream().map(Path::toString).toArray(String[]::new);
		return fileArray;
	}

	/**
	 * Request a clear of the source tree (prior to upload).
	 * @param app The app to clear
	 */
	public void requestProjectClear(CodewindApplication app, String[] fileList)
			throws JSONException, IOException {

		String clearEndpoint = CoreConstants.APIPATH_PROJECT_LIST + "/" 	//$NON-NLS-1$
				+ app.projectID + "/" 									//$NON-NLS-1$
				+ CoreConstants.APIPATH_PROJECT_CLEAR;

		JSONObject payload = new JSONObject();
		payload.put(CoreConstants.KEY_FILE_LIST, fileList);

		URI url = baseUri.resolve(clearEndpoint);

		// This initiates the build
		HttpUtil.post(url, authToken, payload);
	}

	public void requestUpload(String projectId, Path fullPath, String relativePath) throws JSONException, IOException {

		// Read the file and convert the content to JSON.
		byte[] fileContent = Files.readAllBytes(fullPath);
		String jsonContent = JSONObject.quote(new String(fileContent, "UTF-8"));

		// zlib compress the content
		Deflater fileDeflater = new Deflater();
		byte[] jsonBytes = jsonContent.getBytes("UTF-8");
		fileDeflater.setInput(jsonBytes);
		fileDeflater.finish();
		byte[] buffer = new byte[jsonBytes.length];
		ByteArrayOutputStream compressedStream = new ByteArrayOutputStream(jsonBytes.length);
		while (!fileDeflater.finished()) {
			int bytesCompressed = fileDeflater.deflate(buffer);
			compressedStream.write(buffer, 0, bytesCompressed);
		}

		// base64 encode the compressed content
		Encoder encoder = Base64.getEncoder();
		String base64Compressed = encoder.encodeToString(compressedStream.toByteArray());

		JSONObject body = new JSONObject();
		body.put(CoreConstants.KEY_DIRECTORY, false);
		body.put(CoreConstants.KEY_PATH, relativePath.replace('\\', '/'));
		body.put(CoreConstants.KEY_MSG, base64Compressed);

		String endpoint = CoreConstants.APIPATH_PROJECT_LIST + "/" + projectId + "/"
				+ CoreConstants.APIPATH_PROJECT_UPLOAD;
		URI uri = baseUri.resolve(endpoint);
		HttpResult result = HttpUtil.put(uri, authToken, body, 300);
		checkResult(result, uri, false);
	}

	public void requestProjectUnbind(String projectID) throws IOException {
		String endpoint = CoreConstants.APIPATH_PROJECT_LIST + "/" + projectID + "/" + CoreConstants.APIPATH_PROJECT_UNBIND;
		URI uri = baseUri.resolve(endpoint);
		HttpResult result = HttpUtil.post(uri, authToken);
		checkResult(result, uri, false);
		CoreUtil.updateConnection(this);
	}
	
	public List<ProjectTypeInfo> requestProjectTypes() throws IOException, JSONException {
		List<ProjectTypeInfo> projectTypes = new ArrayList<ProjectTypeInfo>();
		final URI uri = baseUri.resolve(CoreConstants.APIPATH_BASE + "/" + CoreConstants.APIPATH_PROJECT_TYPES);
		HttpResult result = HttpUtil.get(uri, authToken);
		checkResult(result, uri, true);
		
		JSONArray array = new JSONArray(result.response);
		for (int i = 0; i < array.length(); i++) {
			projectTypes.add(new ProjectTypeInfo(array.getJSONObject(i)));
		}
		return projectTypes;
	}
	
	public String requestRegistryTest(String registry) throws IOException, JSONException {
		final URI uri = baseUri.resolve(CoreConstants.APIPATH_BASE + "/" + CoreConstants.APIPATH_REGISTRY);
		JSONObject payload = new JSONObject();
		payload.put(CoreConstants.KEY_DEPLOYMENT_REGISTRY, registry);
		payload.put(CoreConstants.KEY_OPERATION, CoreConstants.VALUE_OP_TEST);
		
		HttpResult result = HttpUtil.post(uri, authToken, payload);
		checkResult(result, uri, true);
		
		JSONObject resultObj = new JSONObject(result.response);
		if (resultObj.getBoolean(CoreConstants.KEY_DEPLOYMENT_REGISTRY_TEST)) {
			return null;
		}
		String msg = resultObj.getString(CoreConstants.KEY_MSG);
		return msg;
	}
	
	public void requestRegistrySet(String registry) throws IOException, JSONException {
		final URI uri = baseUri.resolve(CoreConstants.APIPATH_BASE + "/" + CoreConstants.APIPATH_REGISTRY);
		JSONObject payload = new JSONObject();
		payload.put(CoreConstants.KEY_DEPLOYMENT_REGISTRY, registry);
		payload.put(CoreConstants.KEY_OPERATION, CoreConstants.VALUE_OP_SET);
		
		HttpResult result = HttpUtil.post(uri, authToken, payload);
		checkResult(result, uri, false);
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
			env = new ConnectionEnv(getEnvData(baseUri, authToken));
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

	public void requestProjectDelete(String projectId)
			throws JSONException, IOException {

		String endpoint = CoreConstants.APIPATH_PROJECT_LIST + "/" + projectId;

		URI uri = baseUri.resolve(endpoint);

		HttpResult result = HttpUtil.delete(uri, authToken);
		checkResult(result, uri, false);
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
	
	public URL getAppMonitorURL(CodewindApplication app) {
		return getAppViewURL(app, CoreConstants.VIEW_MONITOR);
	}

	public URL getAppViewURL(CodewindApplication app, String view) {
		try {
			URI uri = baseUri;
			String query = CoreConstants.QUERY_PROJECT + "=" + app.projectID;
			query = query + "&" + CoreConstants.QUERY_VIEW + "=" + view;
			uri = new URI(uri.getScheme(), uri.getAuthority(), uri.getPath(), query, uri.getFragment());
			return uri.toURL();
		} catch (Exception e) {
			Logger.logError("Failed to get the URL for the " + view + " view and the " + app.name + "application.", e);  //$NON-NLS-1$  //$NON-NLS-2$  //$NON-NLS-3$ 
		}
		return null;
	}
	
	public URL getPerformanceMonitorURL(CodewindApplication app) {
		try {
			URI uri = baseUri;
			uri = uri.resolve(CoreConstants.PERF_MONITOR);
			String query = CoreConstants.QUERY_PROJECT + "=" + app.projectID;
			uri = new URI(uri.getScheme(), uri.getAuthority(), uri.getPath(), query, uri.getFragment());
			return uri.toURL();
		} catch (Exception e) {
			Logger.logError("Failed to get the performance monitor URL for the " + app.name + "application.", e);  //$NON-NLS-1$  //$NON-NLS-2$  //$NON-NLS-3$ 
		}
		return null;
	}
	
	public boolean isLocal() {
		return false;
	}
}
