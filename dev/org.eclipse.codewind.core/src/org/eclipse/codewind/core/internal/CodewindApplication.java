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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import org.eclipse.codewind.core.internal.HttpUtil.HttpResult;
import org.eclipse.codewind.core.internal.connection.CodewindConnection;
import org.eclipse.codewind.core.internal.console.ProjectLogInfo;
import org.eclipse.codewind.core.internal.constants.AppStatus;
import org.eclipse.codewind.core.internal.constants.BuildStatus;
import org.eclipse.codewind.core.internal.constants.CoreConstants;
import org.eclipse.codewind.core.internal.constants.ProjectCapabilities;
import org.eclipse.codewind.core.internal.constants.ProjectLanguage;
import org.eclipse.codewind.core.internal.constants.ProjectType;
import org.eclipse.codewind.core.internal.constants.StartMode;
import org.eclipse.core.runtime.IPath;
import org.json.JSONObject;

/**
 * Represents a Codewind Application / Project
 */
public class CodewindApplication {

	public final CodewindConnection connection;
	public final String projectID, name, host;
	public final IPath fullLocalPath;
	public final ProjectType projectType;
	public final ProjectLanguage projectLanguage;

	
	private String contextRoot;	// can be null
	private StartMode startMode;
	private AppStatus appStatus;
	private String appStatusDetails;
	private BuildStatus buildStatus;
	private String buildDetails;
	private boolean autoBuild = true;
	private boolean injectMetrics = false;
	private boolean enabled = true;
	private String containerId;
	private boolean capabilitiesReady = false;
	private ProjectCapabilities projectCapabilities;
	private String action;
	private List<ProjectLogInfo> logInfos = new ArrayList<ProjectLogInfo>();
	private boolean metricsAvailable = false;
	private boolean hasConfirmedMetrics = false; 		// see confirmMetricsAvailable
	private long lastBuild = -1;
	private long lastImageBuild = -1;
	private boolean isHttps = false;
	private boolean deleteContents = false;
	private final Vector<String> activeNotificationIDs = new Vector<String>();
	

	// Must be updated whenever httpPort changes. Can be null
	private URL baseUrl;
	
	// Application base url
	private String appBaseUrl;
	
	// Full application url ((appBaseUrl if set or baseUrl) + context root)
	private URL rootUrl;

	// These are set by the CodewindSocket so we have to make sure the reads and writes are synchronized
	// An httpPort of -1 indicates the app is not started - could be building or disabled.
	private int httpPort = -1, debugPort = -1;
	
	// Container ports
	private String containerAppPort = null, containerDebugPort = null;

	CodewindApplication(CodewindConnection connection, String id, String name, 
			ProjectType projectType, ProjectLanguage projectLanguage, IPath localPath)
					throws MalformedURLException {

		this.connection = connection;
		this.projectID = id;
		this.name = name;
		this.projectType = projectType;
		this.projectLanguage = projectLanguage;
		this.host = connection.getBaseURI().getHost();

		this.fullLocalPath = localPath;

		this.startMode = StartMode.RUN;
		this.appStatus = AppStatus.UNKNOWN;
		this.buildStatus = BuildStatus.UNKOWN;
	}

	private void setUrls() throws MalformedURLException {
		if (httpPort == -1) {
			Logger.log("Un-setting baseUrl because httpPort is not valid"); //$NON-NLS-1$
			baseUrl = null;
			rootUrl = null;
			return;
		}

		String httpStr = getIsHttps() ? "https" : "http";
		baseUrl = new URL(httpStr, host, httpPort, ""); //$NON-NLS-1$ //$NON-NLS-2$
		
		// The root URL is the app base URL plus the context root if there is one
		rootUrl = getAppBaseUrl();
		if (contextRoot != null && !contextRoot.isEmpty()) {
			rootUrl = new URL(rootUrl, contextRoot);
		}
	}

	public synchronized void setAppStatus(String appStatus, String appStatusDetails) {
		if (appStatus != null) {
			this.appStatus = AppStatus.get(appStatus);
			if (appStatusDetails == null || appStatusDetails.trim().isEmpty()) {
				this.appStatusDetails = null;
			} else {
				this.appStatusDetails = appStatusDetails;
			}
		}
	}
	
	public synchronized void setBuildStatus(String buildStatus, String buildDetails) {
		if (buildStatus != null) {
			BuildStatus newStatus = BuildStatus.get(buildStatus);
			boolean hasChanged = newStatus != this.buildStatus;
			this.buildStatus = newStatus;
			if (buildDetails != null && buildDetails.trim().isEmpty()) {
				this.buildDetails = null;
			} else {
				this.buildDetails = buildDetails;
			}
			if (hasChanged && newStatus.isComplete()) {
				buildComplete();
			}
		}
	}
	
	public synchronized void setAppBaseUrl(String appBaseUrl) {
		this.appBaseUrl = appBaseUrl;
		try {
			setUrls();
		} catch (MalformedURLException e) {
			Logger.logError("An error occurred updating the application base url to: " + appBaseUrl, e);
		}
	}
	
	public synchronized void setContextRoot(String contextRoot) {
		this.contextRoot = contextRoot;
		try {
			setUrls();
		} catch (MalformedURLException e) {
			Logger.logError("An error occurred updating the base url with the new context root: " + contextRoot, e);
		}
	}
	
	public synchronized String getContextRoot() {
		return this.contextRoot;
	}
	
	public synchronized void setStartMode(StartMode startMode) {
		this.startMode = startMode;
	}
	
	public synchronized void setAutoBuild(boolean enabled) {
		this.autoBuild = enabled;
		CoreUtil.updateApplication(this);
	}

	public synchronized void setInjectMetrics(boolean enabled) {
		this.injectMetrics = enabled;
		CoreUtil.updateApplication(this);
	}
	
	public synchronized void setEnabled(boolean enabled) {
		boolean reenabled = enabled && !this.enabled;
		this.enabled = enabled;
		if (reenabled) {
			connection.refreshApps(projectID);
			CoreUtil.updateApplication(this);
		} else if (!enabled) {
			// Reset fields that are only valid when the app is enabled
			setHttpPort(-1);
			setDebugPort(-1);
			setContainerId(null);
		}
	}
	
	public synchronized void setContainerId(String id) {
		this.containerId = id;
	}
	
	public synchronized void setAction(String action) {
		this.action = action;
	}
	
	public synchronized void addLogInfos(List<ProjectLogInfo> newLogInfos) {
		if (newLogInfos == null || newLogInfos.isEmpty()) {
			Logger.logError("Trying to add empty log infos to project: " + name);
			return;
		}
		if (this.logInfos == null || this.logInfos.isEmpty()) {
			this.logInfos = newLogInfos;
			return;
		}
		for (ProjectLogInfo newLogInfo : newLogInfos) {
			boolean found = false;
			for (ProjectLogInfo logInfo : this.logInfos) {
				// There should not be more than one log with the same name for a project
				if (logInfo.logName.equals(newLogInfo.logName)) {
					found = true;
					break;
				}
			}
			if (!found) {
				this.logInfos.add(newLogInfo);
			}
		}
	}
	
	public synchronized void setLogInfos(List<ProjectLogInfo> logInfos) {
		if (logInfos == null) {
			Logger.logError("The logs should not be set to null for project: " + name);
			return;
		}
		this.logInfos = logInfos;
	}
	
	public synchronized void setMetricsAvailable(boolean value) {
		metricsAvailable = value;
	}
	
	/**
	 * Can return null if this project hasn't started yet (ie httpPort == -1)
	 */
	public URL getBaseUrl() {
		return baseUrl;
	}
	
	public URL getAppBaseUrl() throws MalformedURLException {
		// If the app url was set in the project info, use it
		if (appBaseUrl != null && !appBaseUrl.isEmpty()) {
			return new URL(appBaseUrl);
		}
		return baseUrl;
	}
	
	/**
	 * Can return null if this project hasn't started yet (ie httpPort == -1)
	 */
	public URL getRootUrl() {
		return rootUrl;
	}
	
	public URL getMetricsUrl() {
		try {
			if ((!this.injectMetrics) && this.metricsAvailable) {
				return new URL(getAppBaseUrl(), projectLanguage.getMetricsRoot());
			} else {
				return (connection.getBaseURI().resolve(CoreConstants.PERF_METRICS_DASH + "/" + projectLanguage.getId() + "?theme=dark&projectID=" + projectID)).toURL();
			}
		} catch (MalformedURLException e) {
			Logger.logError("An error occurred trying to construct the application metrics URL", e);
		}
		return null;
	}
	
	/**
	 * For extension projects, the metricsAvailable may not be incorrectly 'true'.
	 * So after the application is running, GET that page to make sure. If it fails, we set metricsAvailable to false.
	 * 
	 * Workaround for https://github.com/eclipse/codewind/issues/258
	 */
	public synchronized void confirmMetricsAvailable() {
		if (this.hasConfirmedMetrics) {
			return;
		}
		this.hasConfirmedMetrics = true;

		// Only extension projects which report they DO support metrics require this extra check; 
		// for normal projects the metricsAvailable is accurate.
		if (!this.metricsAvailable || !this.projectType.isExtension()) {
			return;
		}
		
		try {
			URL metricsUrl = this.getMetricsUrl();
			if (metricsUrl == null) {
				// we should not have made it this far
				return;
			}
			HttpResult getMetricsResult = HttpUtil.get(metricsUrl.toURI());
			this.metricsAvailable = getMetricsResult.isGoodResponse;
		}
		catch (IOException | URISyntaxException e) {
			Logger.logError("An error occurred trying to confirm the application metrics status", e);
		}		
	}
	
	public synchronized AppStatus getAppStatus() {
		return appStatus;
	}
	
	public synchronized String getAppStatusDetails() {
		return appStatusDetails;
	}
	
	public synchronized BuildStatus getBuildStatus() {
		return buildStatus;
	}
	
	public synchronized String getBuildDetails() {
		return buildDetails;
	}
	
	public synchronized int getHttpPort() {
		return httpPort;
	}

	public synchronized int getDebugPort() {
		return debugPort;
	}

	public synchronized StartMode getStartMode() {
		return startMode;
	}
	
	public synchronized boolean isAutoBuild() {
		return autoBuild;
	}

	public synchronized boolean isInjectMetrics() {
		return injectMetrics;
	}
	
	public synchronized boolean isEnabled() {
		return enabled;
	}
	
	public synchronized String getContainerId() {
		return containerId;
	}
	
	public boolean isActive() {
		return getAppStatus() == AppStatus.STARTING || getAppStatus() == AppStatus.STARTED;
	}

	public boolean isRunning() {
		return rootUrl != null;
	}
	
	public boolean isDeleting() {
		return CoreConstants.VALUE_ACTION_DELETING.equals(action);
	}
	
	public boolean isImporting() {
		// The action value is called "validating" but really this means the project is importing
		return CoreConstants.VALUE_ACTION_VALIDATING.equals(action);
	}
	
	public boolean isAvailable() {
		return isEnabled() && !isImporting();
	}
	
	public List<ProjectLogInfo> getLogInfos() {
		return logInfos;
	}

	public boolean hasBuildLog() {
		return (projectType != ProjectType.TYPE_NODEJS);
	}
	
	public synchronized boolean getMetricsAvailable() {
		return metricsAvailable;
	}
	
	public synchronized boolean hasAppMonitor() {
		// Metrics available is really: metrics are in the package.json/pom.xml/package.swift
		// thus it can be false even when injectMetrics is true so need to check both
		return projectLanguage.alwaysHasAppMonitor() || getMetricsAvailable() || injectMetrics;
	}
	
	public synchronized boolean hasPerfDashboard() {
		// Metrics available is really: metrics are in the package.json/pom.xml/package.swift
		// thus it can be false even when injectMetrics is true so need to check both
		return getMetricsAvailable() || injectMetrics;
	}
	
	public synchronized void setLastBuild(long timestamp) {
		lastBuild = timestamp;
	}
	
	public synchronized long getLastBuild() {
		return lastBuild;
	}
	
	public synchronized void setLastImageBuild(long timestamp) {
		lastImageBuild = timestamp;
	}
	
	public synchronized long getLastImageBuild() {
		return lastImageBuild;
	}

	public synchronized void setHttpPort(int httpPort) {
		Logger.log("Set HTTP port for " + rootUrl + " to " + httpPort); //$NON-NLS-1$ //$NON-NLS-2$
		this.httpPort = httpPort;
		try {
			setUrls();
		} catch (MalformedURLException e) {
			Logger.logError(e);
		}
	}

	public synchronized void setDebugPort(int debugPort) {
		Logger.log("Set debug port for " + rootUrl + " to " + debugPort); //$NON-NLS-1$ //$NON-NLS-2$
		this.debugPort = debugPort;
	}

	/**
	 * Invalidate fields that can change when the application is restarted.
	 * On restart success, these will be updated by the Socket handler for that event.
	 * This is done because the application will wait for the ports to be
	 * set to something other than -1 before trying to connect.
	 */
	public synchronized void invalidatePorts() {
		Logger.log("Invalidate ports for " + name); //$NON-NLS-1$
		httpPort = -1;
		debugPort = -1;
	}
	
	public synchronized void setContainerAppPort(String port) {
		this.containerAppPort = port;
	}
	
	public synchronized String getContainerAppPort() {
		return this.containerAppPort;
	}
	
	public synchronized void setContainerDebugPort(String port) {
		this.containerDebugPort = port;
	}
	
	public synchronized String getContainerDebugPort() {
		return this.containerDebugPort;
	}
	
	public synchronized void setIsHttps(boolean value) {
		isHttps = value;
		try {
			setUrls();
		} catch (MalformedURLException e) {
			Logger.logError("An error occurred updating isHttps to: " + value, e);
		}
	}
	
	public synchronized boolean getIsHttps() {
		return isHttps;
	}
	
	public synchronized void setDeleteContents(boolean value) {
		deleteContents = value;
	}
	
	public synchronized boolean getDeleteContents() {
		return deleteContents;
	}
	
	public synchronized void setCapabilitiesReady(boolean capabilitiesReady) {
		this.capabilitiesReady = capabilitiesReady;
	}
	
	public synchronized boolean getCapabilitiesReady() {
		return capabilitiesReady;
	}

	/**
	 * Get the capabilities of a project.  Cache them because they should not change
	 * and since they are used to decide which menu items are shown/enabled this method
	 * needs to be fast.
	 */
	public ProjectCapabilities getProjectCapabilities() {
		if (projectCapabilities == null && capabilitiesReady) {
			try {
				JSONObject obj = connection.requestProjectCapabilities(this);
				projectCapabilities = new ProjectCapabilities(obj);
			} catch (Exception e) {
				Logger.logError("Failed to get the project capabilities for application: " + name, e); //$NON-NLS-1$
			}
		}
		if (projectCapabilities == null) {
			return ProjectCapabilities.emptyCapabilities;
		}
		return projectCapabilities;
	}
	
	public void clearDebugger() {
		// Override as needed
	}
	
	public void connectDebugger() {
		// Override as needed
	}
	
	public void reconnectDebugger() {
		// override as needed
	}

	public void dispose() {
		// Override as needed
	}
	
	public void resetValidation() {
		// Override as needed
	}
	
	public void validationError(String filePath, String message, String quickFixId, String quickFixDescription) {
		// Override as needed
	}
	
	public void validationWarning(String filePath, String message, String quickFixId, String quickFixDescription) {
		// Override as needed
	}
	
	public boolean supportsDebug() {
		// Override as needed
		return false;
	}
	
	public void buildComplete() {
		// Override to perform actions when a build has completed
	}
	
	public boolean canInjectMetrics() {
		List<ProjectType> projectTypesWithMetricInjection = Arrays.asList(
				ProjectType.TYPE_LIBERTY,
				ProjectType.TYPE_SPRING,
				ProjectType.TYPE_NODEJS
		);
		return projectTypesWithMetricInjection.contains(projectType) ||
				(ProjectType.TYPE_DOCKER.equals(projectType) &&
				 ProjectLanguage.LANGUAGE_JAVA.equals(projectLanguage));
	}
	
	public boolean hasNotificationID(String id) {
		Logger.log(String.format("The %s notification id for the %s application is contained: %b", id, name, activeNotificationIDs.contains(id))); //$NON-NLS-1$
		return activeNotificationIDs.contains(id);
	}
	
	// Call hasNotificationID first before adding
	public void addNotificationID(String id) {
		Logger.log(String.format("Adding notification id %s to the %s application", id, name)); //$NON-NLS-1$
		activeNotificationIDs.add(id);
	}
	
	public void clearNotificationIDs() {
		Logger.log(String.format("Clearing notification ids for the %s application", name)); //$NON-NLS-1$
		activeNotificationIDs.clear();
	}

	@Override
	public String toString() {
		return String.format("%s@%s id=%s name=%s type=%s loc=%s", //$NON-NLS-1$
				CodewindApplication.class.getSimpleName(), rootUrl.toString(),
				projectID, name, projectType, fullLocalPath.toOSString());
	}
}
