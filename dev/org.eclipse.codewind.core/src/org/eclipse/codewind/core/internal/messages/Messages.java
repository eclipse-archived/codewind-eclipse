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

package org.eclipse.codewind.core.internal.messages;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.codewind.core.internal.messages.messages"; //$NON-NLS-1$

	public static String GenericUnknown;
	
	public static String CodewindLocalConnectionName;
	
	public static String Connection_ErrConnection_AlreadyExists;
	public static String Connection_ErrConnection_OldVersion;
	public static String Connection_ErrConnection_VersionUnknown;
	public static String Connection_ErrConnection_WorkspaceErr;
	public static String Connection_ErrContactingServerDialogMsg;
	public static String Connection_ErrContactingServerDialogTitle;
	public static String Connection_ErrGettingProjectListTitle;
	public static String Connection_ErrConnection_UpdateCacheException;
	public static String Connection_ErrConnection_CodewindNotReady;

	public static String ConnectionException_ConnectingToMCFailed;

	public static String ReconnectJob_ReconnectErrorDialogMsg;
	public static String ReconnectJob_ReconnectErrorDialogTitle;
	public static String ReconnectJob_ReconnectJobName;

	public static String DebugLaunchConfigName;
	public static String DebuggerConnectFailureDialogTitle;
	public static String DebuggerConnectFailureDialogMsg;

	public static String BuildConsoleName;
	public static String AppConsoleName;
	public static String LogFileConsoleName;
	public static String LogFileInitialMsg;

	public static String FileNotFoundTitle;
	public static String FileNotFoundMsg;

	public static String Socket_ErrRestartingProjectDialogMsg;
	public static String Socket_ErrRestartingProjectDialogTitle;
	
	public static String AppStatusStarting;
	public static String AppStatusStarted;
	public static String AppStatusStopping;
	public static String AppStatusStopped;
	public static String AppStatusUnknown;
	public static String AppStatusDebugging;
	
	public static String BuildStateQueued;
	public static String BuildStateInProgress;
	public static String BuildStateSuccess;
	public static String BuildStateFailed;
	public static String BuildStateUnknown;
	
	public static String DebugLaunchError;
	public static String ConnectDebugJob;
	
	public static String RefreshResourceJobLabel;
	public static String RefreshResourceError;
	
	public static String StartCodewindJobLabel;
	public static String InstallCodewindJobLabel;
	public static String StopCodewindJobLabel;
	public static String RemovingCodewindJobLabel;
	public static String CreateProjectTaskLabel;
	public static String ValidateProjectTaskLabel;
	public static String DeleteProjectJobLabel;
	public static String DeleteProjectError;
	
	public static String ProjectSettingsUpdateErrorTitle;
	
	public static String DockerTypeDisplayName;
	
	public static String ProcessHelperUnknownError;

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}
