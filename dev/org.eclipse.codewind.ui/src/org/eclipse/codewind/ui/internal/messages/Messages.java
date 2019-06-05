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

package org.eclipse.codewind.ui.internal.messages;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.codewind.ui.internal.messages.messages"; //$NON-NLS-1$

	public static String ConnectionPrefsPage_AddBtn;
	public static String ConnectionPrefsPage_PageTitle;
	public static String ConnectionPrefsPage_RemoveBtn;
	public static String ConnectionPrefsPage_ShellTitle;
	public static String ConnectionPrefsPage_TableTitleLabel;
	public static String ConnectionPrefsPage_URLColumn;

	public static String PrefsParentPage_DebugTimeoutLabel;
	public static String PrefsParentPage_ErrInvalidDebugTimeout;

	public static String NewConnectionPage_ConnectSucceeded;
	public static String NewConnectionPage_ErrAConnectionAlreadyExists;
	public static String NewConnectionPage_ErrCouldNotConnectToMC;
	public static String NewConnectionPage_HostnameLabel;
	public static String NewConnectionPage_NotValidPortNum;
	public static String NewConnectionPage_OnlyLocalhostSupported;
	public static String NewConnectionPage_PortLabel;
	public static String NewConnectionPage_ShellTitle;
	public static String NewConnectionPage_TestConnectionBtn;
	public static String NewConnectionPage_TestToProceed;
	public static String NewConnectionPage_WizardDescription;
	public static String NewConnectionPage_WizardTitle;
	public static String NewConnectionWizard_ShellTitle;
	
	public static String ConnectionErrorTitle;
	public static String ConnectionAlreadyExistsError;
	public static String ConnectingJobLabel;
	public static String DetectingConnectionTask;
	public static String ConnectingTask;
	public static String StartCodewindErrorWithMsg;
	public static String StartCodewindError;
	public static String StartCodewindTimeout;
	public static String StartCodewindNotActive;
	public static String StartCodewindConnectionError;

	public static String OpenAppAction_CantOpenNotRunningAppMsg;
	public static String OpenAppAction_CantOpenNotRunningAppTitle;

	public static String StartBuildAction_AppMissingTitle;
	public static String StartBuildAction_AppMissingMsg;
	public static String StartBuildAction_AlreadyBuildingTitle;
	public static String StartBuildAction_AlreadyBuildingMsg;
	
	public static String CodewindConnectionLabel;
	public static String CodewindDisconnected;
	public static String CodewindProjectDisabled;
	public static String CodewindConnectionNoProjects;
	
	public static String ActivateActionLabel;
	public static String ActivateActionJobLabel;
	public static String DeactivateActionLabel;
	public static String DeactivateActionJobLabel;
	public static String DeactivateActionErrorWithMsg;
	public static String DeactivateActionError;
	public static String DeactivateActionTimeout;
	public static String BindActionLabel;
	public static String UnbindActionLabel;
	public static String UnbindActionError;
	
	public static String BindProjectErrorTitle;
	public static String BindProjectConnectionError;
	public static String BindProjectAlreadyExistsError;
	public static String BindProjectBadLocationError;
	
	public static String BindProjectWizardTitle;
	public static String BindProjectWizardJobLabel;
	public static String BindProjectWizardError;
	
	public static String SelectLanguagePageName;
	public static String SelectLanguagePageTitle;
	public static String SelectLanguagePageDescription;
	public static String SelectLanguagePageLanguageLabel;
	public static String SelectLanguagePageProjectTypeLabel;
	public static String SelectProjectPageFilterText;
	
	public static String SelectProjectPageName;
	public static String SelectProjectPageTitle;
	public static String SelectProjectPageDescription;
	public static String SelectProjectPageChooseProjectLabel;
	
	public static String RestartInDebugMode;
	public static String RestartInRunMode;
	public static String ErrorOnRestartDialogTitle;
	
	public static String EnableProjectLabel;
	public static String DisableProjectLabel;
	public static String EnableDisableProjectJob;
	public static String ErrorOnEnableDisableProject;
	
	public static String EnableAutoBuildLabel;
	public static String DisableAutoBuildLabel;
	public static String EnableDisableAutoBuildJob;
	public static String ErrorOnEnableDisableAutoBuild;
	
	public static String ShowLogFilesMenu;
	public static String ShowAllLogFilesAction;
	public static String HideAllLogFilesAction;
	public static String ErrorOnShowLogFileDialogTitle;
	public static String ShowOnContentChangeAction;
	
	public static String ActionNewConnection;
	
	public static String ActionOpenAppMonitor;
	
	public static String ActionOpenPerformanceMonitor;

	public static String ValidateLabel;
	public static String AttachDebuggerLabel;
	public static String LaunchDebugSessionLabel;
	public static String DeleteProjectLabel;
	public static String DeleteProjectTitle;
	public static String DeleteProjectMessage;
	public static String DeleteProjectJobTitle;
	public static String DeleteProjectErrorMsg;
	public static String refreshResourceJobLabel;
	public static String RefreshResourceError;
	public static String RefreshConnectionJobLabel;
	public static String RefreshProjectJobLabel;
	
	public static String ImportProjectError;
	public static String StartBuildError;
	
	public static String DialogYesButton;
	public static String DialogNoButton;
	public static String DialogCancelButton;
	
	public static String ProjectNotImportedDialogTitle;
	public static String ProjectNotImportedDialogMsg;

	public static String ProjectClosedDialogTitle;
	public static String ProjectClosedDialogMsg;
	public static String ProjectOpenJob;
	public static String ProjectOpenError;
	
	public static String BrowserTooltipApp;
	public static String BrowserTooltipAppMonitor;
	public static String BrowserTooltipPerformanceMonitor;
	
	public static String NodeJsBrowserDialogCopyToClipboardButton;
	public static String NodeJsBrowserDialogOpenChromeButton;
	public static String NodeJsBrowserDialogPasteMessage;
	public static String NodeJSOpenBrowserTitle;
	public static String NodeJSOpenBrowserDesc;
	public static String NodeJSOpenBrowserJob;
	public static String NodeJSDebugURLError;
	
	public static String BrowserSelectionTitle;
	public static String BrowserSelectionDescription;
	public static String BrowserSelectionLabel;
	public static String BrowserSelectionAlwaysUseMsg;
	public static String BrowserSelectionManageButtonText;
	public static String BrowserSelectionListLabel;
	public static String BrowserSelectionNoBrowserSelected;
	
	public static String NewProjectAction_Label;
	public static String NewProjectWizard_ShellTitle;
	public static String NewProjectPage_ShellTitle;
	public static String NewProjectPage_WizardDescription;
	public static String NewProjectPage_WizardTitle;
	public static String NewProjectPage_TemplateGroupLabel;
	public static String NewProjectPage_ProjectTypeGroup;
	public static String NewProjectPage_FilterMessage;
	public static String NewProjectPage_TypeColumn;
	public static String NewProjectPage_LanguageColumn;
	public static String NewProjectPage_DescriptionLabel;
	public static String NewProjectPage_DescriptionNone;
	public static String NewProjectPage_ImportLabel;
	public static String NewProjectPage_ProjectNameLabel;
	public static String NewProjectPage_ProjectExistsError;
	public static String NewProjectPage_EclipseProjectExistsError;
	public static String NewProjectPage_EmptyProjectName;
	public static String NewProjectPage_InvalidProjectName;
	public static String NewProjectPage_ProjectCreateErrorTitle;
	public static String NewProjectPage_ProjectCreateErrorMsg;
	public static String NewProjectPage_CodewindConnectError;
	public static String NewProjectPage_TemplateListError;
	
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {}
}
