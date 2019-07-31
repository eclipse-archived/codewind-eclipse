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

	public static String PrefsParentPage_StopAppsLabel;
	public static String PrefsParentPage_StopAppsAlways;
	public static String PrefsParentPage_StopAppsNever;
	public static String PrefsParentPage_StopAppsPrompt;
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
	
	public static String CodewindLabel;
	public static String CodewindNotInstalledQualifier;
	public static String CodewindNotStartedQualifier;
	public static String CodewindRunningQualifier;
	public static String CodewindInstallingQualifier;
	public static String CodewindUninstallingQualifier;
	public static String CodewindStartingQualifier;
	public static String CodewindStoppingQualifier;
	public static String CodewindErrorQualifier;
	public static String CodewindErrorMsg;
	public static String CodewindNotInstalledMsg;
	public static String CodewindNotStartedMsg;
	public static String CodewindLocalProjects;
	public static String CodewindConnectionLabel;
	public static String CodewindDisconnected;
	public static String CodewindProjectDisabled;
	public static String CodewindConnectionNoProjects;
	public static String CodewindDescriptionContextRoot;
	
	public static String InstallerActionInstallLabel;
	public static String InstallerActionUninstallLabel;
	public static String InstallerActionStartLabel;
	public static String InstallerActionStopLabel;
	
	public static String BindActionLabel;
	public static String UnbindActionLabel;
	public static String UnbindActionTitle;
	public static String UnbindActionMessage;
	public static String UnbindActionMultipleMessage;
	public static String UnbindActionDeleteContentsLabel;
	public static String UnbindActionLocationLabel;
	public static String UnbindActionMultipleLocationLabel;
	public static String UnbindActionJobTitle;
	public static String UnbindActionMultipleJobTitle;
	public static String UnbindActionError;
	
	public static String InstallCodewindJobLabel;
	public static String StartingCodewindJobLabel;
	public static String StoppingCodewindJobLabel;
	public static String RemovingCodewindJobLabel;
	public static String InstallingCodewindTask;
	public static String UninstallingCodewindTask;
	public static String InstallCodewindDialogTitle;
	public static String InstallCodewindDialogMessage;
	public static String InstallCodewindNewProjectMessage;
	public static String InstallCodewindAddProjectMessage;
	public static String InstallCodewindInstallingMessage;
	public static String InstallCodewindUninstallingMessage;
	public static String InstallCodewindFailNoMessage;
	public static String CodewindStartFail;
	public static String CodewindStopFail;
	public static String CodewindInstallFail;
	public static String CodewindUninstallFail;
	public static String CodewindStartError;
	public static String CodewindStopError;
	public static String CodewindInstallError;
	public static String CodewindUninstallError;
	public static String CodewindStartTimeout;
	public static String CodewindStopTimeout;
	public static String CodewindInstallTimeout;
	public static String CodewindUninstallTimeout;
	
	public static String BindProjectErrorTitle;
	public static String BindProjectConnectionError;
	public static String BindProjectAlreadyExistsError;
	public static String BindProjectBadLocationError;
	
	public static String BindProjectWizardTitle;
	public static String BindProjectWizardJobLabel;
	public static String BindProjectWizardError;
	
	public static String SelectProjectTypePageName;
	public static String SelectProjectTypePageTitle;
	public static String SelectProjectTypePageDescription;
	public static String SelectProjectTypePageProjectTypeLabel;
	public static String SelectProjectTypePageLanguageLabel;
	public static String SelectProjectTypeErrorLabel;
	
	public static String SelectProjectPageName;
	public static String SelectProjectPageTitle;
	public static String SelectProjectPageDescription;
	public static String SelectProjectPageChooseProjectLabel;
	public static String SelectProjectPageFilterText;
	
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
	public static String refreshResourceJobLabel;
	public static String RefreshResourceError;
	public static String RefreshCodewindJobLabel;
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
	public static String NewProjectPage_TemplateColumn;
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
	
	public static String AppOverviewEditorCreateError;
	public static String AppOverviewEditorPartName;
	public static String AppOverviewEditorGeneralSection;
	public static String AppOverviewEditorTypeEntry;
	public static String AppOverviewEditorLanguageEntry;
	public static String AppOverviewEditorLocationEntry;
	public static String AppOverviewEditorAppUrlEntry;
	public static String AppOverviewEditorHostAppPortEntry;
	public static String AppOverviewEditorHostDebugPortEntry;
	public static String AppOverviewEditorProjectIdEntry;
	public static String AppOverviewEditorContainerIdEntry;
	public static String AppOverviewEditorStatusEntry;
	public static String AppOverviewEditorStatusEnabled;
	public static String AppOverviewEditorStatusDisabled;
	public static String AppOverviewEditorProjectSettingsSection;
	public static String AppOverviewEditorContextRootEntry;
	public static String AppOverviewEditorAppPortEntry;
	public static String AppOverviewEditorDebugPortEntry;
	public static String AppOverviewEditorEditProjectSettings;
	public static String AppOverviewEditorBuildSection;
	public static String AppOverviewEditorAutoBuildEntry;
	public static String AppOverviewEditorAutoBuildOn;
	public static String AppOverviewEditorAutoBuildOff;
	public static String AppOverviewEditorLastBuildEntry;
	public static String AppOverviewEditorLastImageBuildEntry;
	public static String AppOverviewEditorProjectNeverBuilt;
	public static String AppOverviewEditorImageNeverBuilt;
	public static String AppOverviewEditorRefreshButton;
	public static String AppOverviewEditorNotAvailable;
	public static String AppOverviewEditorDebugNotSupported;
	public static String AppOverviewEditorOpenSettingsErrorTitle;
	public static String AppOverviewEditorOpenSettingsErrorMsg;
	public static String AppOverviewEditorOpenSettingsNotFound;
	
	public static String StopAllDialog_Title;
	public static String StopAllDialog_Message;
	public static String StopAllDialog_ToggleMessage;
	
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {}
}
