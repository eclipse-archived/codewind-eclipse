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

package org.eclipse.codewind.ui.internal.messages;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.codewind.ui.internal.messages.messages"; //$NON-NLS-1$

	public static String GenericNotAvailable;
	
	public static String ConnectionPrefsPage_AddBtn;
	public static String ConnectionPrefsPage_PageTitle;
	public static String ConnectionPrefsPage_RemoveBtn;
	public static String ConnectionPrefsPage_ShellTitle;
	public static String ConnectionPrefsPage_TableTitleLabel;
	public static String ConnectionPrefsPage_URLColumn;

	public static String PrefsParentPage_GeneralGroup;
	public static String PrefsParentPage_AutoOpenOverviewButton;
	public static String PrefsParentPage_EnableSupportFeatures;
	public static String PrefsParentPage_StartupShutdownGroup;
	public static String PrefsParentPage_InstallTimeout;
	public static String PrefsParentPage_UninstallTimeout;
	public static String PrefsParentPage_StartTimeout;
	public static String PrefsParentPage_StopTimeout;
	public static String PrefsParentPage_DebugGroup;
	public static String PrefsParentPage_StopAppsLabel;
	public static String PrefsParentPage_StopAppsAlways;
	public static String PrefsParentPage_StopAppsNever;
	public static String PrefsParentPage_StopAppsPrompt;
	public static String PrefsParentPage_DebugTimeoutLabel;
	public static String PrefsParentPage_ErrInvalidTimeout;

	public static String NewConnectionActionLabel;
	public static String NewConnectionPage_ShellTitle;
	public static String NewConnectionPage_WizardDescription;
	public static String NewConnectionPage_WizardTitle;
	public static String NewConnectionWizard_ShellTitle;
	public static String NewConnectionWizard_CreateJobTitle;
	
	public static String CodewindConnectionComposite_ConnNameLabel;
	public static String CodewindConnectionComposite_ConnDetailsGroup;
	public static String CodewindConnectionComposite_ConnDetailsInstructions;
	public static String CodewindConnectionComposite_UrlLabel;
	public static String CodewindConnectionComposite_UserLabel;
	public static String CodewindConnectionComposite_PasswordLabel;
	public static String CodewindConnectionComposite_TestConnButton;
	public static String CodewindConnectionComposite_RegDetailsGroup;
	public static String CodewindConnectionComposite_RegDetailsInstructions;
	public static String CodewindConnectionComposite_TestRegButton;
	public static String CodewindConnectionComposite_NoConnNameError;
	public static String CodewindConnectionComposite_ConnNameInUseError;
	public static String CodewindConnectionComposite_InvalidUrlError;
	public static String CodewindConnectionComposite_UrlInUseError;
	public static String CodewindConnectionComposite_MissingConnDetailsError;
	public static String CodewindConnectionComposite_NoPasswordForUpdateError;
	public static String CodewindConnectionComposite_TestConnMsg;
	public static String CodewindConnectionComposite_TestConnectionJobLabel;
	public static String CodewindConnectionComposite_ConnectSucceeded;
	public static String CodewindConnectionComposite_ErrCouldNotConnect;
	public static String CodewindConnectionComposite_ConnErrorTitle;
	public static String CodewindConnectionComposite_ConnErrorMsg;
	public static String CodewindConnectionComposite_ConnFailed;
	public static String CodewindConnectionComposite_MissingRegDetailsError;
	public static String CodewindConnectionComposite_RegErrorTitle;
	public static String CodewindConnectionComposite_RegErrorMsg;
	public static String CodewindConnectionComposite_RegFailed;
	
	public static String CodewindConnectionErrorTitle;
	public static String CodewindConnectionCreateError;
	public static String CodewindConnectionConnectError;
	public static String CodewindConnectionUpdateError;

	public static String OpenAppOverviewAction_Label;
	
	public static String OpenAppAction_Label;
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
	public static String CodewindErrorMsgWithDetails;
	public static String CodewindNotInstalledMsg;
	public static String CodewindWrongVersionQualifier;
	public static String CodewindWrongVersionMsg;
	public static String CodewindNotStartedMsg;
	public static String CodewindConnectionLabel;
	public static String CodewindConnected;
	public static String CodewindDisconnected;
	public static String CodewindDisconnectedDetails;
	public static String CodewindProjectNoStatus;
	public static String CodewindProjectDisabled;
	public static String CodewindConnectionNoProjects;
	public static String CodewindDescriptionContextRoot;
	
	public static String InstallerActionInstallLabel;
	public static String InstallerActionUninstallLabel;
	public static String InstallerActionUpdateLabel;
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
	public static String UpdatingCodewindJobLabel;
	public static String UpgradeWorkspaceJobLabel;
	public static String InstallingCodewindTask;
	public static String UninstallingCodewindTask;
	public static String InstallCodewindDialogTitle;
	public static String InstallCodewindDialogMessage;
	public static String UpgradeCodewindDialogMessage;
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
	public static String CodewindUpdateError;
	public static String CodewindStartTimeout;
	public static String CodewindStopTimeout;
	public static String CodewindInstallTimeout;
	public static String CodewindUninstallTimeout;
	public static String WorkspaceUpgradeError;
	public static String WorkspaceUpgradeTitle;
	
	public static String CodewindInstalledDialogTitle;
	public static String CodewindInstalledDialogMsg;
	public static String UpdateCodewindDialogTitle;
	public static String UpdateCodewindDialogMsg;
	
	public static String BindProjectErrorTitle;
	public static String BindProjectConnectionError;
	public static String BindProjectAlreadyExistsError;
	
	public static String BindProjectWizardTitle;
	public static String BindProjectWizardRemoveTask;
	public static String BindProjectWizardDisableTask;
	public static String BindProjectWizardJobLabel;
	public static String BindProjectWizardError;
	
	public static String SelectConnectionPageName;
	public static String SelectConnectionPageTitle;
	public static String SelectConnectionPageDescription;
	public static String SelectConnectionPageNoSelectionMsg;
	
	public static String MoveProjectJobLabel;
	public static String MoveProjectError;
	
	public static String ProjectDeployedDialogShell;
	public static String ProjectDeployedDialogTitle;
	public static String ProjectDeployedDialogMessage;
	public static String ProjectDeployedDialogGroupLabel;
	public static String ProjectDeployedDialogRemoveLabel;
	public static String ProjectDeployedDialogRemoveTooltip;
	public static String ProjectDeployedDialogDisableLabel;
	public static String ProjectDeployedDialogDisableTooltip;
	public static String ProjectDeployedDialogMaintainLabel;
	public static String ProjectDeployedDialogMaintainTooltip;
	
	public static String SelectProjectTypePageName;
	public static String SelectProjectTypePageTitle;
	public static String SelectProjectTypePageDescription;
	public static String SelectProjectTypePageProjectTypeLabel;
	public static String SelectProjectTypePageLanguageLabel;
	public static String SelectProjectTypePageSubtypeLabel;
	public static String SelectProjectTypeErrorLabel;
	public static String SelectProjectTypeManageRepoLabel;
	public static String SelectProjectTypeManageRepoLink;
	public static String SelectProjectTypeManageRepoTooltip;
	public static String SelectProjectTypeGatherTypesTask;
	public static String SelectProjectTypeRefreshTypesTask;
	public static String SelectProjectTypeRefreshTypesError;
	public static String SelectProjectTypeNoProjectTypes;
	
	public static String SelectProjectPageName;
	public static String SelectProjectPageTitle;
	public static String SelectProjectPageDescription;
	public static String SelectProjectPageWorkspaceProject;
	public static String SelectProjectPageFilesystemProject;
	public static String SelectProjectPageFilterText;
	public static String SelectProjectPageNoWorkspaceProjects;
	public static String SelectProjectPagePathLabel;
	public static String SelectProjectPageBrowseButton;
	public static String SelectProjectPageFilesystemDialogTitle;
	public static String SelectProjectPageNoExistError;
	public static String SelectProjectPageLocationError;
	public static String SelectProjectPageCWProjectExistsError;
	public static String SelectProjectPageProjectExistsError;
	
	public static String ProjectValidationPageName;
	public static String ProjectValidationPageTitle;
	public static String ProjectValidationPageDescription;
	public static String ProjectValidationPageMsg;
	public static String ProjectValidationPageFailMsg;
	public static String ProjectValidationPageTypeLabel;
	public static String ProjectValidationPageLanguageLabel;
	public static String ProjectValidationTask;
	
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

	public static String EnableInjectMetricsLabel;
	public static String DisableInjectMetricsLabel;
	public static String EnableDisableInjectMetricsJob;
	public static String ErrorOnEnableDisableInjectMetrics;
	
	public static String ConnectActionLabel;
	public static String DisconnectActionLabel;
	public static String ConnectJobLabel;
	public static String DisconnectJobLabel;
	public static String ConnectJobError;
	public static String DisconnectJobError;
	
	public static String RemoveConnectionActionLabel;
	public static String RemoveConnectionActionConfirmTitle;
	public static String RemoveConnectionActionConfirmMsg;
	public static String EditConnectionActionLabel;
	
	public static String ShowLogFilesMenu;
	public static String ShowLogFileJobLabel;
	public static String ShowLogFileError;
	public static String HideLogFileJobLabel;
	public static String HideLogFileError;
	public static String ShowAllLogFilesAction;
	public static String ShowAllLogFilesJobLabel;
	public static String ShowAllLogFilesError;
	public static String HideAllLogFilesAction;
	public static String HideAllLogFilesJobLabel;
	public static String HideAllLogFilesError;
	public static String ErrorOnShowLogFileDialogTitle;
	public static String ShowOnContentChangeAction;
	
	public static String ActionNewConnection;
	
	public static String ActionOpenMetricsDashboard;
	
	public static String ActionOpenPerfDashboard;
	
	public static String ActionOpenTektonDashboard;
	public static String ActionOpenTektonDashboardErrorDialogTitle;
	public static String ActionOpenTektonDashboardNotInstalled;
	public static String ActionOpenTektonDashboardOtherError;
	
	public static String ActionOpenContainerShell;
	public static String ActionOpenContainerShellMissingDepsTitle;
	public static String ActionOpenContainerShellMissingDepsMsg;
	public static String ContainerShellTitle;

	public static String ValidateLabel;
	public static String AttachDebuggerLabel;
	public static String LaunchDebugSessionLabel;
	public static String refreshResourceJobLabel;
	public static String RefreshResourceError;
	public static String RefreshCodewindJobLabel;
	public static String RefreshConnectionJobLabel;
	public static String RefreshProjectJobLabel;
	
	public static String ImportProjectActionLabel;
	public static String ImportProjectError;
	public static String StartBuildActionLabel;
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
	
	public static String NoDebugSetupDialogTitle;
	public static String NoDebugSetupDialogMsg;
	public static String NoDebugSetupDialogToggle;
	
	public static String BrowserTooltipApp;
	public static String BrowserTooltipAppMonitor;
	public static String BrowserTooltipPerformanceMonitor;
	public static String BrowserTooltipTektonDashboard;
	
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
	public static String NewProjectWizard_ErrorTitle;
	public static String NewProjectWizard_ConnectionError;
	public static String NewProjectPage_ShellTitle;
	public static String NewProjectPage_WizardDescription;
	public static String NewProjectPage_WizardTitle;
	public static String NewProjectPage_LocationGroupLabel;
	public static String NewProjectPage_LocationDefaultButton;
	public static String NewProjectPage_LocationTextLabel;
	public static String NewProjectPage_LocationBrowseButton;
	public static String NewProjectPage_TemplateGroupLabel;
	public static String NewProjectPage_ProjectTypeGroup;
	public static String NewProjectPage_FilterMessage;
	public static String NewProjectPage_TemplateColumn;
	public static String NewProjectPage_TypeColumn;
	public static String NewProjectPage_LanguageColumn;
	public static String NewProjectPage_DescriptionLabel;
	public static String NewProjectPage_StyleLabel;
	public static String NewProjectPage_SourceLabel;
	public static String NewProjectPage_DetailsNone;
	public static String NewProjectPage_ImportLabel;
	public static String NewProjectPage_ProjectNameLabel;
	public static String NewProjectPage_ManageRepoLabel;
	public static String NewProjectPage_ManageRepoLink;
	public static String NewProjectPage_ManageRepoTooltip;
	public static String NewProjectPage_GetTemplatesTask;
	public static String NewProjectPage_RefreshTemplatesTask;
	public static String NewProjectPage_RefreshTemplatesError;
	public static String NewProjectPage_ProjectExistsError;
	public static String NewProjectPage_EclipseProjectExistsError;
	public static String NewProjectPage_EmptyProjectName;
	public static String NewProjectPage_InvalidProjectName;
	public static String NewProjectPage_CreateJobLabel;
	public static String NewProjectPage_ProjectCreateErrorTitle;
	public static String NewProjectPage_ProjectCreateErrorMsg;
	public static String NewProjectPage_CodewindConnectError;
	public static String NewProjectPage_TemplateListError;
	public static String NewProjectPage_EmptyTemplateList;
	public static String NewProjectPage_NoLocationError;
	public static String NewProjectPage_NoTemplateSelected;
	public static String ProjectLocationInCodewindDataDirError;
	
	public static String ManageRegistriesLinkLabel;
	public static String ManageRegistriesLinkText;
	public static String ManageRegistriesLinkTooltip;
	public static String ManageRegistriesLinkTooltipLocal;
	public static String NoPushRegistryTitle;
	public static String NoPushRegistryMessage;
	public static String NoPushRegistryError;
	
	public static String AppOverviewEditorCreateError;
	public static String AppOverviewEditorPartName;
	public static String AppOverviewEditorProjectInfoSection;
	public static String AppOverviewEditorTypeEntry;
	public static String AppOverviewEditorLanguageEntry;
	public static String AppOverviewEditorLocationEntry;
	public static String AppOverviewEditorAppUrlEntry;
	public static String AppOverviewEditorHostAppPortEntry;
	public static String AppOverviewEditorHostDebugPortEntry;
	public static String AppOverviewEditorNotDebugging;
	public static String AppOverviewEditorProjectIdEntry;
	public static String AppOverviewEditorContainerIdEntry;
	public static String AppOverviewEditorPodNameEntry;
	public static String AppOverviewEditorNamespaceEntry;
	public static String AppOverviewEditorStatusEntry;
	public static String AppOverviewEditorStatusEnabled;
	public static String AppOverviewEditorStatusDisabled;
	public static String AppOverviewEditorProjectStatusSection;
	public static String AppOverviewEditorAppPortEntry;
	public static String AppOverviewEditorDebugPortEntry;
	public static String AppOverviewEditorEditProjectSettings;
	public static String AppOverviewEditorProjectSettingsInfo;
	public static String AppOverviewEditorAppInfoSection;
	public static String AppOverviewEditorAutoBuildEntry;
	public static String AppOverviewEditorAutoBuildOn;
	public static String AppOverviewEditorAutoBuildOff;
	public static String AppOverviewEditorInjectMetricsEntry;
	public static String AppOverviewEditorInjectMetricsOn;
	public static String AppOverviewEditorInjectMetricsOff;
	public static String AppOverviewEditorInjectMetricsUnavailable;
	public static String AppOverviewEditorLastBuildEntry;
	public static String AppOverviewEditorLastImageBuildEntry;
	public static String AppOverviewEditorProjectNeverBuilt;
	public static String AppOverviewEditorImageNeverBuilt;
	public static String AppOverviewEditorRefreshButton;
	public static String AppOverviewEditorNotAvailable;
	public static String AppOverviewEditorDebugNotSupported;
	public static String AppOverviewEditorNoDebugRemote;
	public static String AppOverviewEditorOpenSettingsErrorTitle;
	public static String AppOverviewEditorOpenSettingsErrorMsg;
	public static String AppOverviewEditorOpenSettingsNotFound;
	public static String AppOverviewEditorNoConnection;
	public static String AppOverviewEditorNoApplication;
	public static String AppOverviewEditorAppStatusEntry;
	public static String AppOverviewEditorBuildStatusEntry;
	
	public static String StopAllDialog_Title;
	public static String StopAllDialog_Message;
	public static String StopAllDialog_ToggleMessage;
	
	public static String GenericActionNotSupported;
	public static String AppMonitorNotSupported;
	public static String PerfDashboardNotSupported;
	
	public static String RepoMgmtActionLabel;
	
	public static String RepoMgmtDialogTitle;
	public static String RepoMgmtDialogMessage;
	
	public static String RepoMgmtDescription;
	public static String RepoMgmtLearnMoreLink;
	public static String RepoMgmtAddButton;
	public static String RepoMgmtRemoveButton;
	public static String RepoMgmtDescriptionLabel;
	public static String RepoMgmtStylesLabel;
	public static String RepoMgmtUrlLabel;
	public static String RepoMgmtUpdateError;
	public static String RepoMgmtRemoveFailed;
	public static String RepoMgmtUpdateFailed;
	public static String RepoMgmtAddFailed;
	
	public static String AddRepoDialogShell;
	public static String AddRepoDialogTitle;
	public static String AddRepoDialogMessage;
	public static String AddRepoDialogNameLabel;
	public static String AddRepoDialogDescriptionLabel;
	public static String AddRepoDialogUrlLabel;
	public static String AddRepoDialogNoName;
	public static String AddRepoDialogNoDescription;
	public static String AddRepoDialogNoUrl;
	
	public static String RepoUpdateTask;
	public static String RepoUpdateErrorTitle;
	public static String RepoListErrorTitle;
	public static String RepoListErrorMsg;
	
	public static String EditConnectionDialogShell;
	public static String EditConnectionDialogTitle;
	public static String EditConnectionDialogMessage;
	public static String UpdateConnectionJobLabel;
	
	public static String RegMgmtActionLabel;
	
	public static String RegMgmtDialogTitle;
	public static String RegMgmtDialogMessage;
	public static String RegMgmtDialogLocalMessage;
	
	public static String RegMgmtDescription;
	public static String RegMgmtLocalDescription;
	public static String RegMgmtLearnMoreLink;
	public static String RegMgmtAddButton;
	public static String RegMgmtSetPushButton;
	public static String RegMgmtRemoveButton;
	public static String RegMgmtAddressColumn;
	public static String RegMgmtUsernameColumn;
	public static String RegMgmtNamespaceColumn;
	public static String RegMgmtPushRegColumn;
	public static String RegMgmtPushRegSet;
	
	public static String RegMgmtAddDialogShell;
	public static String RegMgmtAddDialogTitle;
	public static String RegMgmtAddDialogMessage;
	public static String RegMgmtAddDialogAddressLabel;
	public static String RegMgmtAddDialogUsernameLabel;
	public static String RegMgmtAddDialogPasswordLabel;
	public static String RegMgmtAddDialogPushRegLabel;
	public static String RegMgmtAddDialogNamespaceLabel;
	public static String RegMgmtAddDialogNoAddress;
	public static String RegMgmtAddDialogAddressInUse;
	public static String RegMgmtAddDialogNoUsername;
	public static String RegMgmtAddDialogNoPassword;
	public static String RegMgmtAddDialogNoNamespace;
	public static String RegMgmtUpdateError;
	public static String RegMgmtRemoveFailed;
	public static String RegMgmtUpdateFailed;
	public static String RegMgmtAddFailed;
	public static String RegMgmtSetPushRegFailed;
	
	public static String RegMgmtNamespaceDialogShell;
	public static String RegMgmtNamespaceDialogTitle;
	public static String RegMgmtNamespaceDialogMessage;
	
	public static String RegUpdateTask;
	public static String RegUpdateErrorTitle;
	public static String RegListErrorTitle;
	public static String RegListErrorMsg;
	
	public static String LogLevelAction;
	public static String LogLevelsFetchTaskLabel;
	public static String LogLevelsFetchErrorTitle;
	public static String LogLevelsFetchError;
	public static String LogLevelSetJobLabel;
	public static String LogLevelSetError;
	public static String LogLevelDialogShell;
	public static String LogLevelDialogTitle;
	public static String LogLevelDialogMessage;
	public static String LogLevelDialogLogLabel;
	public static String LogLevelDialogLogDefault;
	
	public static String GetStartedIntroMessage;
	
	public static String WelcomePageAction;
	public static String WelcomePageEditorPartName;
	public static String WelcomePageEditorCreateError;
	public static String WelcomePageEditorFormTitle;
	public static String WelcomePageEditorFormMessage;
	public static String WelcomePageWelcomeHeader;
	public static String WelcomePageWelcomeText;
	public static String WelcomePageCodewindExplorerLink;
	public static String WelcomePageQuickStartHeader;
	public static String WelcomePageQuickStartText;
	public static String WelcomePageQuickStartLocalButton;
	public static String WelcomePageQuickStartRemoteButton;
	public static String WelcomePageQuickStartSetUpLabel;
	public static String WelcomePageQuickStartProjectLabel;
	public static String WelcomePageQuickStartStep;
	public static String WelcomePageQuickStartOr;
	public static String WelcomePageQuickStartInstallDockerButton;
	public static String WelcomePageQuickStartInstallImagesButton;
	public static String WelcomePageQuickStartNewConnectionButton;
	public static String WelcomePageQuickStartNoConnectionTitle;
	public static String WelcomePageQuickStartNoConnectionMsg;
	public static String WelcomePageQuickStartNewProjectButton;
	public static String WelcomePageQuickStartAddProjectButton;
	public static String WelcomePageLearnHeader;
	public static String WelcomePageLearnCommandsLabel;
	public static String WelcomePageLearnCommandsTooltip;
	public static String WelcomePageLearnCommandsText;
	public static String WelcomePageLearnCommandsLink;
	public static String WelcomePageLearnCommandsLinkTooltip;
	public static String WelcomePageLearnDocsLabel;
	public static String WelcomePageLearnDocsTooltip;
	public static String WelcomePageLearnDocsText;
	public static String WelcomePageLearnDocsTemplatesLink;
	public static String WelcomePageLearnDocsTemplatesTooltip;
	public static String WelcomePageLearnDocsRemoteLink;
	public static String WelcomePageLearnDocsRemoteTooltip;
	public static String WelcomePageLearnExtensionsLabel;
	public static String WelcomePageLearnExtensionsText;
	public static String WelcomePageLearnExtensionsOpenAPILink;
	public static String WelcomePageLearnExtensionsOpenAPITooltip;
	public static String WelcomePageLearnExtensionsDockerLink;
	public static String WelcomePageLearnExtensionsDockerTooltip;
	
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {}
}
