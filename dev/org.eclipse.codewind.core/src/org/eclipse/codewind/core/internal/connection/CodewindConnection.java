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
import java.net.ConnectException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.codewind.core.internal.CodewindApplication;
import org.eclipse.codewind.core.internal.CodewindApplicationFactory;
import org.eclipse.codewind.core.internal.CoreUtil;
import org.eclipse.codewind.core.internal.HttpUtil;
import org.eclipse.codewind.core.internal.HttpUtil.HttpResult;
import org.eclipse.codewind.core.internal.InstallUtil;
import org.eclipse.codewind.core.internal.Logger;
import org.eclipse.codewind.core.internal.console.ProjectLogInfo;
import org.eclipse.codewind.core.internal.constants.CoreConstants;
import org.eclipse.codewind.core.internal.constants.ProjectType;
import org.eclipse.codewind.core.internal.messages.Messages;
import org.eclipse.codewind.filewatchers.eclipse.CodewindFilewatcherdConnection;
import org.eclipse.codewind.filewatchers.eclipse.ICodewindProjectTranslator;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.osgi.util.NLS;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Represents a connection to a Codewind instance
 */
public class CodewindConnection {

	public static final String CODEWIND_WORKSPACE_PROPERTY = "org.eclipse.codewind.internal.workspace"; //$NON-NLS-1$
	private static final String BRANCH_VERSION = "\\d{4}_M\\d{1,2}_\\D";
	private static final Pattern pattern = Pattern.compile(BRANCH_VERSION);

	public final URI baseUrl;
	private ConnectionEnv env = null;
	private String connectionErrorMsg = null;

	private CodewindSocket socket;
	private CodewindFilewatcherdConnection filewatcher;
	
	private volatile boolean isConnected = true;

	private Map<String, CodewindApplication> appMap = new LinkedHashMap<String, CodewindApplication>();

	public static URI buildUrl(String host, int port) throws URISyntaxException {
		return new URI("http", null, host, port, null, null, null); //$NON-NLS-1$
	}

	public CodewindConnection (URI uri) throws IOException, URISyntaxException, JSONException {
		if (!uri.toString().endsWith("/")) { //$NON-NLS-1$
			uri = uri.resolve("/"); //$NON-NLS-1$
		}
		this.baseUrl = uri;

		if (CodewindConnectionManager.getActiveConnection(uri.toString()) != null) {
			onInitFail(NLS.bind(Messages.Connection_ErrConnection_AlreadyExists, baseUrl));
		}
		
		env = new ConnectionEnv(getEnvData(this.baseUrl));
		Logger.log("Codewind version is: " + env.getVersion());			// $NON-NLS-1$

		if (env.getWorkspacePath() == null) {
			// Can't recover from this
			// This should never happen since we have already determined it is a supported version of Codewind.
			onInitFail(Messages.Connection_ErrConnection_WorkspaceErr);
		}
		
		socket = new CodewindSocket(this);
		if(!socket.blockUntilFirstConnection()) {
			close();
			throw new CodewindConnectionException(socket.socketUri);
		}

		refreshApps(null);
		
		filewatcher = new CodewindFilewatcherdConnection(baseUrl.toString(), new ICodewindProjectTranslator() {
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

		Logger.log("Created " + this); //$NON-NLS-1$
	}
	
	public String getSocketNamespace() {
		return env.getSocketNamespace();
	}
	
	public CodewindSocket getSocket() {
		return socket;
	}

	private void onInitFail(String msg) throws ConnectException {
		Logger.log("Initializing Codewind connection failed: " + msg); //$NON-NLS-1$
		close();
		throw new ConnectException(msg);
	}

	/**
	 * Call this when the connection is removed.
	 */
	public void close() {
		Logger.log("Closing " + this); //$NON-NLS-1$
		if (socket != null) {
			socket.close();
		}
		if (filewatcher != null) {
			filewatcher.dispose();
		}
		for (CodewindApplication app : appMap.values()) {
			app.dispose();
		}
	}
	
	private static JSONObject getEnvData(URI baseUrl) throws JSONException, IOException {
		final URI envUrl = baseUrl.resolve(CoreConstants.APIPATH_ENV);

		String envResponse = null;
		try {
			envResponse = HttpUtil.get(envUrl).response;
		} catch (IOException e) {
			Logger.logError("Error contacting Environment endpoint", e); //$NON-NLS-1$
			throw e;
		}

		return new JSONObject(envResponse);
	}
	
	public static String getVersion(URI baseURI) {
		try {
			ConnectionEnv env = new ConnectionEnv(getEnvData(baseURI));
			return env.getVersion();
		} catch (Exception e) {
			Logger.logError("An error occurred trying to get the Codewind version.", e);
		}
		return null;
	}

	public static boolean isSupportedVersion(String versionStr) {
		if (ConnectionEnv.UNKNOWN_VERSION.equals(versionStr)) {
			return false;
		}

		if (CoreConstants.VERSION_LATEST.equals(versionStr)) {
			// Development build - possible other values to check for?
			return true;
		}
		
		Matcher matcher = pattern.matcher(versionStr);
		if (matcher.matches()) {
			return true;
		}

		try {
			String[] expectedDigits = InstallUtil.DEFAULT_INSTALL_VERSION.split("\\.");
			String[] actualDigits = versionStr.split("\\.");
			
			for (int i = 0; i < expectedDigits.length; i++) {
				int expectedVal = Integer.parseInt(expectedDigits[i]);
				if (i >= actualDigits.length) {
					// It is ok if the expected is longer than the actual
					// as long as all of the extra digits are 0, if not
					// then return false
					if (expectedVal != 0) {
						return false;
					}
				} else {
					// If the value is less than expected, return false.
					// If the value is greater than expected, return true.
					// If they are the same, keep going.
					int actualVal = Integer.parseInt(actualDigits[i]);
					if (actualVal < expectedVal) {
						return false;
					} else if (actualVal > expectedVal) {
						return true;
					}
				}
			}
			return true;
		} catch(NumberFormatException e) {
			Logger.logError("Couldn't parse version number from " + versionStr); //$NON-NLS-1$
			return false;
		}
	}
	
	public boolean checkVersion(int requiredVersion, String requiredVersionBr) {
		String versionStr = env.getVersion();
		
		if (ConnectionEnv.UNKNOWN_VERSION.equals(versionStr)) {
			return false;
		}
		
		if (CoreConstants.VERSION_LATEST.equals(versionStr)) {
			// Development build - possible other values to check for?
			return true;
		}
		
		Matcher matcher = pattern.matcher(versionStr);
		if (matcher.matches()) {
			String actualYear = versionStr.substring(0, 4);
			String requiredYear = requiredVersionBr.substring(0, 4);
			try {
				if (Integer.parseInt(actualYear) >= (Integer.parseInt(requiredYear))) {
					int index = versionStr.lastIndexOf('_');
					String actualIteration = versionStr.substring(6, index);
					index = requiredVersionBr.lastIndexOf('_');
					String requiredIteration = requiredVersionBr.substring(6, index);
					if (Integer.parseInt(actualIteration) >= Integer.parseInt(requiredIteration)) {
						return true;
					}
				}
			} catch (NumberFormatException e) {
				Logger.logError("Failed to parse the actual version: " + versionStr + ", or the required version: " + requiredVersionBr);
			}
			return false;
		}
		
		try {
			return Integer.parseInt(versionStr) >= requiredVersion;
		} catch(NumberFormatException e) {
			Logger.logError("Couldn't parse version number from " + versionStr); //$NON-NLS-1$
		}
		
		return false;
	}
	
	public String getConnectionErrorMsg() {
		return this.connectionErrorMsg;
	}

	/**
	 * Refresh this connection's apps using the Codewind project list endpoint.
	 * If projectID is not null then only refresh the corresponding application.
	 */
	public void refreshApps(String projectID) {

		final URI projectsURL = baseUrl.resolve(CoreConstants.APIPATH_PROJECT_LIST);

		try {
			String projectsResponse = HttpUtil.get(projectsURL).response;
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
			CoreUtil.removeApplication(app);
			app.dispose();
		} else {
			Logger.logError("No application found for project being deleted: " + projectID); //$NON-NLS-1$
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

	public void requestProjectRestart(CodewindApplication app, String launchMode)
			throws JSONException, IOException {

		String restartEndpoint = CoreConstants.APIPATH_PROJECT_LIST + "/" 	//$NON-NLS-1$
				+ app.projectID + "/" 										//$NON-NLS-1$
				+ CoreConstants.APIPATH_RESTART;

        URI url = baseUrl.resolve(restartEndpoint);

		JSONObject restartProjectPayload = new JSONObject();
		restartProjectPayload.put(CoreConstants.KEY_START_MODE, launchMode);

		// This initiates the restart
		HttpResult result = HttpUtil.post(url, restartProjectPayload);
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

		URI url = baseUrl.resolve(restartEndpoint);

		// This initiates the restart
		HttpResult result = HttpUtil.put(url);
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
		final URI statusUrl = baseUrl.resolve(CoreConstants.APIPATH_PROJECT_LIST);

		HttpResult result = HttpUtil.get(statusUrl);

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

		URI uri = baseUrl.resolve(endpoint);
		HttpResult result = HttpUtil.get(uri);
		checkResult(result, uri, true);
		return new JSONObject(result.response);
	}

	/**
	 * Request a build on an application
	 * @param app The app to build
	 */
	public void requestProjectBuild(CodewindApplication app, String action)
			throws JSONException, IOException {

		String buildEndpoint = CoreConstants.APIPATH_PROJECT_LIST + "/" 	//$NON-NLS-1$
				+ app.projectID + "/" 									//$NON-NLS-1$
				+ CoreConstants.APIPATH_BUILD;

		URI url = baseUrl.resolve(buildEndpoint);

		JSONObject buildPayload = new JSONObject();
		buildPayload.put(CoreConstants.KEY_ACTION, action);

		// This initiates the build
		HttpUtil.post(url, buildPayload);
	}
	
	public List<ProjectLogInfo> requestProjectLogs(CodewindApplication app) throws JSONException, IOException {
		List<ProjectLogInfo> logList = new ArrayList<ProjectLogInfo>();

		String endpoint = CoreConstants.APIPATH_PROJECT_LIST + "/"	//$NON-NLS-1$
				+ app.projectID + "/"								//$NON-NLS-1$
				+ CoreConstants.APIPATH_LOGS;
		
		URI uri = baseUrl.resolve(endpoint);
		HttpResult result = HttpUtil.get(uri);
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
		
		URI uri = baseUrl.resolve(endpoint);
		HttpResult result = HttpUtil.post(uri);
        checkResult(result, uri, false);
	}
	
	public void requestDisableLogStream(CodewindApplication app, ProjectLogInfo logInfo) throws IOException {
		String endpoint = CoreConstants.APIPATH_PROJECT_LIST + "/" 	//$NON-NLS-1$
				+ app.projectID + "/" 								//$NON-NLS-1$
				+ CoreConstants.APIPATH_LOGS + "/"					//$NON-NLS-1$
				+ logInfo.type + "/"								//$NON-NLS-1$
				+ logInfo.logName;
		
		URI uri = baseUrl.resolve(endpoint);
		HttpResult result = HttpUtil.delete(uri);
        checkResult(result, uri, false);
	}
	
	public void requestValidate(CodewindApplication app) throws JSONException, IOException {
		boolean projectIdInPath = checkVersion(1901, "2019_M1_E");
		
		String endpoint;
		if (projectIdInPath) {
			endpoint = CoreConstants.APIPATH_PROJECT_LIST + "/"	//$NON-NLS-1$
					+ app.projectID + "/"	//$NON-NLS-1$
					+ CoreConstants.APIPATH_VALIDATE;
		} else {
			endpoint = CoreConstants.APIPATH_BASE	+ "/"	//$NON-NLS-1$
					+ CoreConstants.APIPATH_VALIDATE;
					
		}
		
		URI url = baseUrl.resolve(endpoint);
		
		JSONObject buildPayload = new JSONObject();
		if (!projectIdInPath) {
			buildPayload.put(CoreConstants.KEY_PROJECT_ID, app.projectID);
		}
		buildPayload.put(CoreConstants.KEY_PROJECT_TYPE, app.projectType.getId());
		
		HttpResult result = HttpUtil.post(url, buildPayload);
		if (!result.isGoodResponse) {
			final String msg = String.format("Received bad response from server %d with error message %s", //$NON-NLS-1$
					result.responseCode, result.error);
			throw new IOException(msg);
		}
	}
	
	public void requestValidateGenerate(CodewindApplication app) throws JSONException, IOException {
		boolean projectIdInPath = checkVersion(1901, "2019_M1_E");
		
		String endpoint;
		if (projectIdInPath) {
			endpoint = CoreConstants.APIPATH_PROJECT_LIST + "/"	//$NON-NLS-1$
					+ app.projectID + "/"	//$NON-NLS-1$
					+ CoreConstants.APIPATH_VALIDATE_GENERATE;
		} else {
			endpoint = CoreConstants.APIPATH_BASE	+ "/"	//$NON-NLS-1$
					+ CoreConstants.APIPATH_VALIDATE_GENERATE;
					
		}
		
		URI url = baseUrl.resolve(endpoint);
		
		JSONObject buildPayload = new JSONObject();
		if (!projectIdInPath) {
			buildPayload.put(CoreConstants.KEY_PROJECT_ID, app.projectID);
		}
		buildPayload.put(CoreConstants.KEY_PROJECT_TYPE, app.projectType.getId());
		buildPayload.put(CoreConstants.KEY_AUTO_GENERATE, true);
		
		HttpResult result = HttpUtil.post(url, buildPayload);
		if (!result.isGoodResponse) {
			final String msg = String.format("Received bad response from server %d with error message %s", //$NON-NLS-1$
					result.responseCode, result.error);
			throw new IOException(msg);
		}
		
		// Perform validation again to clear the errors/warnings that have been fixed
		requestValidate(app);
	}
	
	public JSONObject requestProjectCapabilities(CodewindApplication app) throws IOException, JSONException {
		final URI statusUrl = baseUrl.resolve(CoreConstants.APIPATH_PROJECT_LIST + "/" + app.projectID + "/" + CoreConstants.APIPATH_CAPABILITIES);

		HttpResult result = HttpUtil.get(statusUrl);

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
		URI uri = baseUrl.resolve(CoreConstants.APIPATH_BASE + "/" + CoreConstants.APIPATH_TEMPLATES);
		if (enabledOnly) {
			String query = CoreConstants.QUERY_SHOW_ENABLED_ONLY + "=true";
			uri = new URI(uri.getScheme(), uri.getAuthority(), uri.getPath(), query, uri.getFragment());
		}
		HttpResult result = HttpUtil.get(uri);
		checkResult(result, uri, true);
		
		JSONArray templateArray = new JSONArray(result.response);
		for (int i = 0; i < templateArray.length(); i++) {
			templates.add(new ProjectTemplateInfo(templateArray.getJSONObject(i)));
		}
		return templates;
	}
	
	public List<RepositoryInfo> requestRepositories() throws IOException, JSONException {
		List<RepositoryInfo> repos = new ArrayList<RepositoryInfo>();
		final URI uri = baseUrl.resolve(CoreConstants.APIPATH_BASE + "/" + CoreConstants.APIPATH_REPOSITORIES);
		HttpResult result = HttpUtil.get(uri);
		checkResult(result, uri, true);
		
		JSONArray repoArray = new JSONArray(result.response);
		for (int i = 0; i < repoArray.length(); i++) {
			repos.add(new RepositoryInfo(repoArray.getJSONObject(i)));
		}
		return repos;
	}
	
	public void requestRepoEnable(String repoUrl, boolean enable) throws IOException, JSONException {
		final URI uri = baseUrl.resolve(CoreConstants.APIPATH_BASE + "/" + CoreConstants.APIPATH_BATCH_REPOSITORIES);
		JSONArray payload = new JSONArray();
		JSONObject obj = new JSONObject();
		obj.put(CoreConstants.KEY_OP, CoreConstants.VALUE_OP_ENABLE);
		obj.put(CoreConstants.KEY_URL, repoUrl);
		obj.put(CoreConstants.KEY_VALUE, enable ? "true" : "false");
		payload.put(obj);
		
		HttpResult result = HttpUtil.patch(uri, payload);
		checkResult(result, uri, false);
	}
	
	public void requestRepoAdd(String description, String url) throws IOException, JSONException {
		final URI uri = baseUrl.resolve(CoreConstants.APIPATH_BASE + "/" + CoreConstants.APIPATH_REPOSITORIES);
		JSONObject payload = new JSONObject();
		payload.put(RepositoryInfo.DESCRIPTION_KEY, description);
		payload.put(RepositoryInfo.URL_KEY, url);
		payload.put(RepositoryInfo.ENABLED_KEY, true);
		
		HttpResult result = HttpUtil.post(uri, payload);
		checkResult(result, uri, false);
	}
	
	public void requestRepoRemove(String url) throws IOException, JSONException {
		final URI uri = baseUrl.resolve(CoreConstants.APIPATH_BASE + "/" + CoreConstants.APIPATH_REPOSITORIES);
		JSONObject payload = new JSONObject();
		payload.put(RepositoryInfo.URL_KEY, url);
		
		HttpResult result = HttpUtil.delete(uri, payload);
		checkResult(result, uri, false);
	}

	public void requestProjectBind(String name, String path, String language, String projectType)
			throws JSONException, IOException {

		String endpoint = CoreConstants.APIPATH_PROJECT_LIST + "/" + CoreConstants.APIPATH_PROJECT_BIND;

		URI uri = baseUrl.resolve(endpoint);

		JSONObject payload = new JSONObject();
		payload.put(CoreConstants.KEY_NAME, name);
		payload.put(CoreConstants.KEY_PATH, CoreUtil.getContainerPath(path));
		payload.put(CoreConstants.KEY_LANGUAGE, language);
		if (projectType == null) {
			projectType = ProjectType.getTypeFromLanguage(language).getId();
		}
		if (projectType != null) {
			payload.put(CoreConstants.KEY_PROJECT_TYPE, projectType);
		}
		payload.put(CoreConstants.KEY_AUTO_BUILD, true);

		HttpResult result = HttpUtil.post(uri, payload, 300);
		checkResult(result, uri, false);
		CoreUtil.updateConnection(this);
	}
	
	public void requestProjectUnbind(String projectID) throws IOException {
		String endpoint = CoreConstants.APIPATH_PROJECT_LIST + "/" + projectID + "/" + CoreConstants.APIPATH_PROJECT_UNBIND;
		URI uri = baseUrl.resolve(endpoint);
		HttpResult result = HttpUtil.post(uri);
		checkResult(result, uri, false);
		CoreUtil.updateConnection(this);
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
		Logger.log("Connection to " + baseUrl + " lost"); //$NON-NLS-1$ //$NON-NLS-2$
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
		Logger.log("Connection to " + baseUrl + " restored"); //$NON-NLS-1$ //$NON-NLS-2$
		
		// Reset any cached information in case it has changed
		try {
			String oldSocketNS = env.getSocketNamespace();
			env = new ConnectionEnv(getEnvData(baseUrl));
//			if (UNKNOWN_VERSION.equals(versionStr)) {
//				Logger.logError("Failed to get the Codewind version after reconnect");
//				this.connectionErrorMsg = NLS.bind(Messages.Connection_ErrConnection_VersionUnknown, CoreConstants.REQUIRED_CODEWIND_VERSION);
//				CoreUtil.updateConnection(this);
//				return;
//			}
//			if (!isSupportedVersion(version)) {
//				Logger.logError("The detected version of Codewind after reconnect is not supported: " + version);
//				this.connectionErrorMsg = NLS.bind(Messages.Connection_ErrConnection_OldVersion, versionStr, CoreConstants.REQUIRED_CODEWIND_VERSION);
//				CoreUtil.updateConnection(this);
//				return;
//			}
			if (env.getWorkspacePath() == null) {
				// This should not happen since the version was ok
				Logger.logError("Failed to get the local workspace path after reconnect");
				this.connectionErrorMsg = Messages.Connection_ErrConnection_WorkspaceErr;
				CoreUtil.updateAll();
				return;
			}
			
			String socketNS = env.getSocketNamespace();
			if ((socketNS != null && !socketNS.equals(oldSocketNS)) || (oldSocketNS != null && !oldSocketNS.equals(socketNS))) {
				// The socket namespace has changed so need to recreate the socket
				socket.close();
				socket = new CodewindSocket(this);
				if(!socket.blockUntilFirstConnection()) {
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
		return String.format("%s @ baseUrl=%s workspacePath=%s numApps=%d", //$NON-NLS-1$
				CodewindConnection.class.getSimpleName(), baseUrl, env.getWorkspacePath(), appMap.size());
	}

	// Note that toPrefsString and fromPrefsString are used to save and load connections from the preferences store
	// in CodewindConnectionManager, so be careful modifying these.

	public String toPrefsString() {
		// No newlines allowed!
		return baseUrl.toString();
	}
	
	public void requestProjectDelete(String projectId)
			throws JSONException, IOException {

		String endpoint = CoreConstants.APIPATH_PROJECT_LIST + "/" + projectId;

		URI uri = baseUrl.resolve(endpoint);

		HttpResult result = HttpUtil.delete(uri);
		checkResult(result, uri, false);
	}

	public IPath getWorkspacePath() {
		return env.getWorkspacePath();
	}
	
	public URL getTektonDashboardURL() {
		return env.getTektonDashboardURL(); 
	}
	
	public URI getNewProjectURI() {
		return getProjectURI(CoreConstants.QUERY_NEW_PROJECT);
	}
	
	public URI getImportProjectURI() {
		return getProjectURI(CoreConstants.QUERY_IMPORT_PROJECT);
	}
	
	private URI getProjectURI(String projectQuery) {
		try {
			URI uri = baseUrl;
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
			URI uri = baseUrl;
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
			URI uri = baseUrl;
			uri = uri.resolve(CoreConstants.PERF_MONITOR);
			String query = CoreConstants.QUERY_PROJECT + "=" + app.projectID;
			uri = new URI(uri.getScheme(), uri.getAuthority(), uri.getPath(), query, uri.getFragment());
			return uri.toURL();
		} catch (Exception e) {
			Logger.logError("Failed to get the performance monitor URL for the " + app.name + "application.", e);  //$NON-NLS-1$  //$NON-NLS-2$  //$NON-NLS-3$ 
		}
		return null;
	}
}
