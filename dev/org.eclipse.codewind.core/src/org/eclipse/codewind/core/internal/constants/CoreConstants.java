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

package org.eclipse.codewind.core.internal.constants;

public class CoreConstants {

	private CoreConstants() {}

	public static final String

			BUILD_LOG_SHORTNAME = "build.log",

			// Version string returned by development builds of Codewind
			VERSION_LATEST = "latest",

			// Portal API endpoints
			APIPATH_BASE = "api/v1",
			APIPATH_PROJECT_LIST = "api/v1/projects",
			APIPATH_ENV = "api/v1/environment",
			APIPATH_VALIDATE = "validate",
			APIPATH_VALIDATE_GENERATE = "validate/generate",
			APIPATH_RESTART = "restart",
			APIPATH_BUILD = "build",
			APIPATH_CLOSE = "close",
			APIPATH_OPEN = "open",
			APIPATH_CAPABILITIES = "capabilities",
			APIPATH_LOGS = "logs",
			APIPATH_METRICS_STATUS = "metrics/status",
			APIPATH_TEMPLATES = "templates",
			APIPATH_PROJECT_BIND = "bind",
			APIPATH_PROJECT_UNBIND = "unbind",

			// JSON keys
			KEY_PROJECT_ID = "projectID",
			KEY_NAME = "name",
			KEY_PATH = "path",
			KEY_PROJECT_PATH = "projectPath",
			KEY_PROJECT_TYPE = "projectType",
			KEY_LOC_DISK = "locOnDisk",
			KEY_CONTEXTROOT = "contextroot",
			KEY_CONTEXT_ROOT = "contextRoot",
			KEY_CONTAINER_ID = "containerId",
			KEY_CUSTOM = "custom",

			KEY_BUILD_LOG = "build-log",
			KEY_BUILD_LOG_LAST_MODIFIED = "build-log-last-modified",
			KEY_LOGS = "logs",
			KEY_LOG_BUILD = "build",
			KEY_LOG_APP = "app",
			KEY_LOG_FILE = "file",
			KEY_LOG_TYPE = "logType",
			KEY_LOG_NAME = "logName",
			KEY_LOG_WORKSPACE_PATH = "workspaceLogPath",
			KEY_LOG_RESET = "reset",

			KEY_OPEN_STATE = "state",
			// VALUE_STATE_OPEN = "open",
			VALUE_STATE_CLOSED = "closed",

			KEY_STATUS = "status",
			KEY_RESULT = "result",
			KEY_APP_STATUS = "appStatus",
			KEY_BUILD_STATUS = "buildStatus",
			KEY_DETAILED_BUILD_STATUS = "detailedBuildStatus",
			KEY_PORTS = "ports",
			KEY_EXPOSED_PORT = "exposedPort",
			KEY_EXPOSED_DEBUG_PORT = "exposedDebugPort",
			KEY_AUTO_BUILD = "autoBuild",

			KEY_ENV_WORKSPACE_LOC = "workspace_location",
			KEY_ENV_VERSION = "microclimate_version",
			KEY_ENV_SOCKET_NAMESPACE = "socket_namespace",

			KEY_LANGUAGE = "language",
			KEY_FRAMEWORK = "framework",
			KEY_EXTENSION = "extension",
			KEY_PROJECT_NAME = "projectName",
			KEY_PARENT_PATH = "parentPath",
			KEY_URL = "url",
			KEY_TEMPLATE_ID = "templateID",

			KEY_START_MODE = "startMode",
			KEY_ACTION = "action",
			VALUE_ACTION_BUILD = "build",
			VALUE_ACTION_ENABLEAUTOBUILD = "enableautobuild",
			VALUE_ACTION_DISABLEAUTOBUILD = "disableautobuild",
			VALUE_ACTION_DELETING = "deleting",
			VALUE_ACTION_VALIDATING = "validating",
			
			KEY_VALIDATION_STATUS = "validationStatus",
			KEY_VALIDATION_RESULTS = "validationResults",
			KEY_SEVERITY = "severity",
			KEY_FILENAME = "filename",
			KEY_FILEPATH = "filepath",
			KEY_TYPE = "type",
			KEY_LABEL = "label",
			KEY_DETAILS = "details",
			KEY_QUICKFIX = "quickfix",
			KEY_FIXID = "fixID",
			KEY_DESCRIPTION = "description",
			VALUE_STATUS_SUCCESS = "success",
			VALUE_STATUS_FAILED = "failed",
			VALUE_SEVERITY_ERROR = "error",
			VALUE_SEVERITY_WARNING = "warning",
			VALUE_TYPE_MISSING = "missing",
			VALUE_TYPE_INVALID = "invalid",
			
			KEY_CAPABILIITES = "capabilities",
			KEY_START_MODES = "startModes",
			KEY_CONTROL_COMMANDS = "controlCommands",
			KEY_METRICS_AVAILABLE = "metricsAvailable",
			
			KEY_AUTO_GENERATE = "autoGenerate",

			// JSON attribute values
			REQUEST_STATUS_SUCCESS = "success",
			
			// Codewind files
			DOCKERFILE = "Dockerfile",
			DOCKERFILE_BUILD = "Dockerfile-build",
			
			QUERY_NEW_PROJECT = "new-project",
			QUERY_IMPORT_PROJECT = "import-project",
			VALUE_TRUE = "true",
			
			QUERY_PROJECT = "project",
			QUERY_VIEW = "view",
			VIEW_MONITOR = "monitor",
			VIEW_OVERVIEW = "overview"

			;

	public static final int
			REQUIRED_CODEWIND_VERSION = 1812;

}
