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

package org.eclipse.codewind.core.internal.constants;

public class CoreConstants {

	private CoreConstants() {}

	public static final String

			BUILD_LOG_SHORTNAME = "build.log",

			// Version string returned by development builds of Codewind
			VERSION_LATEST = "latest",

			// Portal API endpoints
			APIPATH_READY = "ready",
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
			APIPATH_INJECT_METRICS = "metrics/inject",
			APIPATH_PROJECT_UNBIND = "unbind",
			APIPATH_PROJECT_TYPES = "project-types",
			APIPATH_IMAGEPUSHREGISTRY = "imagepushregistry",
			APIPATH_REGISTRYSECRETS = "registrysecrets",

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
			KEY_IS_HTTPS = "isHttps",
			KEY_CAPABILITIES_READY = "capabilitiesReady",

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
			KEY_ERROR = "error",
			KEY_APP_STATUS = "appStatus",
			KEY_DETAILED_APP_STATUS = "detailedAppStatus",
			KEY_MESSAGE = "message",
			KEY_NOTIFICATION_ID = "notificationID",
			KEY_LINK = "link",
			KEY_LINK_LABEL = "linkLabel",
			KEY_BUILD_STATUS = "buildStatus",
			KEY_DETAILED_BUILD_STATUS = "detailedBuildStatus",
			KEY_LAST_BUILD = "lastbuild",
			KEY_APP_IMAGE_LAST_BUILD = "appImageLastBuild",
			KEY_PORTS = "ports",
			KEY_EXPOSED_PORT = "exposedPort",
			KEY_INTERNAL_PORT = "internalPort",
			KEY_EXPOSED_DEBUG_PORT = "exposedDebugPort",
			KEY_INTERNAL_DEBUG_PORT = "internalDebugPort",
			KEY_AUTO_BUILD = "autoBuild",
			KEY_APP_BASE_URL = "appBaseURL",
			
			KEY_INJECT_METRICS_ENABLE = "enable",
			KEY_INJECTION = "injection",
			KEY_INJECTABLE = "injectable",
			KEY_INJECTED = "injected",
			KEY_METRICS_DASHBOARD = "metricsDashboard",
			KEY_METRICS_HOSTING = "hosting",
			VALUE_METRICS_HOSTING_PROJECT = "project",
			VALUE_METRICS_HOSTING_PERF_CONTAINER = "performanceContainer",
			KEY_METRICS_PATH = "path",
			KEY_PERF_DASHBOARD_PATH = "perfDashboardPath",
			
			VALUE_INFO = "INFO",
			VALUE_WARN = "WARN",
			VALUE_ERROR = "ERROR",

			KEY_LANGUAGE = "language",
			KEY_FRAMEWORK = "framework",
			KEY_EXTENSION = "extension",
			KEY_PROJECT_NAME = "projectName",
			KEY_PARENT_PATH = "parentPath",
			KEY_TEMPLATE_ID = "templateID",
			KEY_OP = "op",
			KEY_VALUE = "value",
			VALUE_OP_ENABLE = "enable",

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
			
			KEY_AUTO_GENERATE = "autoGenerate",
			
			// Registry
			KEY_IMAGE_PUSH_REGISTRY = "imagePushRegistry",
			KEY_ADDRESS = "address",
			KEY_USERNAME = "username",
			KEY_PASSWORD = "password",
			KEY_CREDENTIALS = "credentials",
			KEY_NAMESPACE = "namespace",
			KEY_OPERATION = "operation",
			VALUE_OP_TEST = "test",
			VALUE_OP_SET = "set",
			KEY_STATUS_CODE = "statusCode",
			
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
			VIEW_OVERVIEW = "overview",
			PERF_MONITOR = "performance/charts"

			;
}
