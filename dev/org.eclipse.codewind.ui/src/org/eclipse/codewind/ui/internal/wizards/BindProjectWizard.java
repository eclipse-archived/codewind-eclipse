/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.codewind.ui.internal.wizards;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

import org.eclipse.codewind.core.CodewindCorePlugin;
import org.eclipse.codewind.core.internal.CodewindApplication;
import org.eclipse.codewind.core.internal.CodewindManager;
import org.eclipse.codewind.core.internal.CodewindManager.InstallerStatus;
import org.eclipse.codewind.core.internal.CoreUtil;
import org.eclipse.codewind.core.internal.Logger;
import org.eclipse.codewind.core.internal.ProcessHelper.ProcessResult;
import org.eclipse.codewind.core.internal.cli.InstallStatus;
import org.eclipse.codewind.core.internal.cli.InstallUtil;
import org.eclipse.codewind.core.internal.cli.ProjectUtil;
import org.eclipse.codewind.core.internal.connection.CodewindConnection;
import org.eclipse.codewind.core.internal.connection.CodewindConnectionManager;
import org.eclipse.codewind.core.internal.connection.ProjectTypeInfo;
import org.eclipse.codewind.core.internal.connection.ProjectTypeInfo.ProjectSubtypeInfo;
import org.eclipse.codewind.core.internal.constants.ProjectInfo;
import org.eclipse.codewind.core.internal.constants.ProjectType;
import org.eclipse.codewind.ui.CodewindUIPlugin;
import org.eclipse.codewind.ui.internal.actions.CodewindInstall;
import org.eclipse.codewind.ui.internal.actions.ImportProjectAction;
import org.eclipse.codewind.ui.internal.actions.OpenAppOverviewAction;
import org.eclipse.codewind.ui.internal.messages.Messages;
import org.eclipse.codewind.ui.internal.prefs.RegistryManagementDialog;
import org.eclipse.codewind.ui.internal.views.ViewHelper;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;

public class BindProjectWizard extends Wizard implements INewWizard {

	private ConnectionSelectionPage connectionPage;
	private ProjectSelectionPage projectPage;
	private ProjectValidationPage projectValidationPage;
	private ProjectTypeSelectionPage projectTypePage;
	
	private List<CodewindConnection> connections;
	private CodewindConnection connection;
	private IProject project = null;
	private IPath projectPath = null;
	
	// If a connection is passed in and no project then the project selection page will be shown
	public BindProjectWizard(CodewindConnection connection) {
		this(connection, null);
	}
	
	// If a list of connections is passed in, let the user select from this list
	public BindProjectWizard(List<CodewindConnection> connections) {
		this(null, null);
		this.connections = connections;
	}
	
	// If the project is passed in then the project selection page will not be shown
	public BindProjectWizard(CodewindConnection connection, IProject project) {
		super();
		this.connection = connection;
		this.project = project;
		if (project != null) {
			this.projectPath = project.getLocation();
		}
		setNeedsProgressMonitor(true);
		setDefaultPageImageDescriptor(CodewindUIPlugin.getImageDescriptor(CodewindUIPlugin.CODEWIND_BANNER));
		setHelpAvailable(false);
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		// Empty
	}

	@Override
	public void addPages() {
		setWindowTitle(Messages.BindProjectWizardTitle);
		if (connection == null) {
			if (connections == null) {
				connections = CodewindConnectionManager.activeConnections();
			}
			if (connections.size() == 1) {
				connection = connections.get(0);
			} else {
				connectionPage = new ConnectionSelectionPage(connections);
				addPage(connectionPage);
			}
		}
		if (connection != null && ((connection.isLocal() && !checkInstallStatus()) || (!connection.isLocal() && !checkRemoteStatus()))) {
			if (getContainer() != null) {
				getContainer().getShell().close();
			}
			return;
		}
		if (projectPath == null) {
			projectPage = new ProjectSelectionPage(connection);
			addPage(projectPage);
		}
		projectValidationPage = new ProjectValidationPage(connection, project);
		projectTypePage = new ProjectTypeSelectionPage(connection);
		addPage(projectValidationPage);
		addPage(projectTypePage);
	}

	@Override
	public IWizardPage getNextPage(IWizardPage page) {
		if (connectionPage != null && connectionPage.isActivePage()) {
			connection = connectionPage.getConnection();
			if ((connection.isLocal() && !checkInstallStatus()) || (!connection.isLocal() && !checkRemoteStatus())) {
				if (getContainer() != null) {
					getContainer().getShell().close();
				}
				return null;
			}
			projectValidationPage.setConnection(connection);
			return projectValidationPage;
		} else if (projectPage != null && projectPage.isActivePage()) {
			projectValidationPage.setProject(projectPage.getProject(), projectPage.getProjectPath());
			return projectValidationPage;
		} else if (projectValidationPage.isActivePage()) {
			projectTypePage.initPage(connection, projectValidationPage.getProjectInfo());
			return projectTypePage;
		}
		return null;
	}

	@Override
	public boolean canFinish() {
		// The user must select a project
		if (projectPage != null && !projectPage.canFinish()) {
			return false;
		}
		// If the validation page is active and can finish then return true, the user does not need
		// to go to the type selection page unless they want to
		if (projectValidationPage.isActivePage() && projectValidationPage.canFinish()) {
			return true;
		}
		// Finally, check if the type selection page can finish
		return projectTypePage.isActivePage() && projectTypePage.canFinish();
	}

	@Override
	public boolean performCancel() {
		return true;
	}

	@Override
	public boolean performFinish() {
		if(!canFinish()) {
			return false;
		}
		
		if (projectPage != null) {
			project = projectPage.getProject();
			projectPath = projectPage.getProjectPath();
		}

		final String name = project != null ? project.getName() : projectPath.lastSegment();
		
		// Check if this application is already deployed on another connection
		final List<CodewindApplication> existingDeployments = new ArrayList<CodewindApplication>();
		for (CodewindConnection conn : CodewindConnectionManager.activeConnections()) {
			if (conn.isConnected()) {
				CodewindApplication app = conn.getAppByLocation(projectPath);
				if (app != null && app.isEnabled()) {
					existingDeployments.add(app);
				}
			}
		}
		
		// If the application is deployed on another connection, ask the user what they want to do
		final ProjectDeployedDialog.Behaviour selectedBehaviour;
		if (!existingDeployments.isEmpty()) {
			ProjectDeployedDialog dialog = new ProjectDeployedDialog(getShell(), projectPath, connection, existingDeployments);
			if (dialog.open() == IStatus.OK) {
				selectedBehaviour = dialog.getSelectedBehaviour();
			} else {
				return false;
			}
		} else {
			selectedBehaviour = null;
		}
		
		// Use the detected type if the validation page is active otherwise use the type from the project type selection page
		final ProjectInfo detectedProjectInfo = projectValidationPage.isActivePage() ? projectValidationPage.getProjectInfo() : null;
		final ProjectTypeInfo projectTypeInfo = projectTypePage.getProjectTypeInfo();
		final String typeId = detectedProjectInfo != null ? detectedProjectInfo.type.getId() : projectTypeInfo.getId();
		
		Job job = new Job(NLS.bind(Messages.BindProjectWizardJobLabel, new String[] {connection.getName(), name})) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					SubMonitor mon = SubMonitor.convert(monitor, 140);
					
					// Check for a push registry if Codewind style project
					if (!connection.isLocal() && ProjectType.isCodewindStyle(typeId) && !connection.requestHasPushRegistry()) {
						Display.getDefault().syncExec(new Runnable() {
							@Override
							public void run() {
								if (MessageDialog.openConfirm(getShell(), Messages.NoPushRegistryTitle, Messages.NoPushRegistryMessage)) {
									RegistryManagementDialog.open(getShell(), connection, mon.split(40));
								} else {
									mon.setCanceled(true);
								}
							}
						});
						if (mon.isCanceled()) {
							return Status.CANCEL_STATUS;
						}
						if (!connection.requestHasPushRegistry()) {
							return new Status(IStatus.ERROR, CodewindUIPlugin.PLUGIN_ID, Messages.NoPushRegistryError, null);
						}
					}
					mon.setWorkRemaining(100);
					
					// Perform selected action if project already bound to another connection
					if (selectedBehaviour != null) {
						switch (selectedBehaviour) {
							case REMOVE:
								mon.setTaskName(Messages.BindProjectWizardRemoveTask);
								for (CodewindApplication app : existingDeployments) {
									try {
										ProjectUtil.removeProject(app.name, app.projectID, mon.split(10));
									} catch (Exception e) {
										Logger.logError("An error occurred trying to unbind the " + app.name + " project from connection: " + app.connection.getBaseURI()); //$NON-NLS-1$ //$NON-NLS-2$
									}
									if (mon.isCanceled()) {
										return Status.CANCEL_STATUS;
									}
								}
								break;
							case DISABLE:
								mon.setTaskName(Messages.BindProjectWizardDisableTask);
								mon.split(10 * existingDeployments.size());
								for (CodewindApplication app : existingDeployments) {
									try {
										app.connection.requestProjectOpenClose(app, false);
									} catch (Exception e) {
										Logger.logError("An error occurred trying to disable the " + app.name + " project on connection: " + app.connection.getBaseURI()); //$NON-NLS-1$ //$NON-NLS-2$
									}
									if (mon.isCanceled()) {
										return Status.CANCEL_STATUS;
									}
									mon.worked(10);
								}
								break;
							case MAINTAIN:
							default:
								// Do nothing
								break;
						}
					}
					mon.setWorkRemaining(50);
					
					// Bind the project to the connection
					String path = projectPath.toFile().getAbsolutePath();
					if (detectedProjectInfo != null) {
						// The detected project type is being used
						ProjectUtil.bindProject(name, path, detectedProjectInfo.language.getId(), detectedProjectInfo.type.getId(), connection.getConid(), mon.split(30));
					} else {
						// The user chose a different project type than what was detected
						final ProjectSubtypeInfo projectSubtypeInfo = projectTypePage.getProjectSubtypeInfo();
						final String language = projectTypePage.getLanguage();
						
						// If the subtype is not null then need to call validate again with the type and subtype hint
						// Allows it to run any extension commands that are defined for that type and subtype
						if (projectSubtypeInfo != null) {
							ProjectUtil.validateProject(name, path, projectTypeInfo.getId() + ":" + projectSubtypeInfo.id, connection.getConid(), mon.split(10));
						}
						mon.setWorkRemaining(40);
						ProjectUtil.bindProject(name, path, language, projectTypeInfo.getId(), connection.getConid(), mon.split(20));
					}
					if (mon.isCanceled()) {
						cleanup(name, connection);
						return Status.CANCEL_STATUS;
					}
					mon.split(10);
					connection.refreshApps(null);
					if (mon.isCanceled()) {
						return Status.CANCEL_STATUS;
					}
					mon.split(10);
					CodewindApplication app = connection.getAppByName(name);
					if (app != null) {
						IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(name);
						if (project == null || !project.exists()) {
							ImportProjectAction.importProject(app);
						}
						if (CodewindCorePlugin.getDefault().getPreferenceStore().getBoolean(CodewindCorePlugin.AUTO_OPEN_OVERVIEW_PAGE)) {
							Display.getDefault().asyncExec(() -> OpenAppOverviewAction.openAppOverview(app));
						}
					}
					mon.done();
					ViewHelper.openCodewindExplorerView();
					CodewindUIPlugin.getUpdateHandler().updateConnection(connection);
					return Status.OK_STATUS;
				} catch (TimeoutException e) {
					Logger.logError("A timeout occurred trying to add the " + name + " project to connection: " + connection.getName(), e); //$NON-NLS-1$ //$NON-NLS-2$
					cleanup(name, connection);
					return new Status(IStatus.ERROR, CodewindUIPlugin.PLUGIN_ID, NLS.bind(Messages.BindProjectWizardTimeout, projectPath.toOSString()), e);
				} catch (Exception e) {
					Logger.logError("An error occured trying to add the project to Codewind: " + projectPath.toOSString(), e); //$NON-NLS-1$
					return new Status(IStatus.ERROR, CodewindUIPlugin.PLUGIN_ID, NLS.bind(Messages.BindProjectWizardError, projectPath.toOSString()), e);
				}
			}
		};
		job.schedule();

		return true;
	}
	
	public static void cleanup(String projectName, CodewindConnection connection) {
		Job job = new Job(NLS.bind(Messages.ProjectCleanupJobLabel, projectName)) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				SubMonitor mon = SubMonitor.convert(monitor, 30);
				mon.split(10);
				connection.refreshApps(null);
				CodewindApplication app = connection.getAppByName(projectName);
				if (app != null) {
					try {
						ProjectUtil.removeProject(app.name, app.projectID, mon.split(20));
					} catch (Exception e) {
						Logger.logError("An error occurred while trying to remove the project after bind project terminated for: " + projectName, e);
						return new Status(IStatus.ERROR, CodewindUIPlugin.PLUGIN_ID, NLS.bind(Messages.ProjectCleanupError, projectName), null);
					}
				}
				return Status.OK_STATUS;
			}
		};
		job.schedule();
	}
	
	private boolean checkInstallStatus() {
		// If the installer is currently running, show a dialog to the user
		final InstallerStatus installerStatus = CodewindManager.getManager().getInstallerStatus();
		if (installerStatus != null) {
			Display.getDefault().asyncExec(new Runnable() {
				@Override
				public void run() {
					CodewindInstall.installerActiveDialog(installerStatus);
				}
			});
			return false;
		}
		
		// Install or start Codewind if necessary
		if (CodewindManager.getManager().getInstallStatus().isInstalled()) {
			setupLocalConnection(connection, this);
		} else {
			CodewindInstall.codewindInstallerDialog(project);
			return false;
		}

		// If the connection is still not set up then display an error dialog
		if (!connection.isConnected()) {
			CoreUtil.openDialog(true, Messages.BindProjectErrorTitle, NLS.bind(Messages.BindProjectConnectionError, connection.getName()));
			return false;
		}
		
		// Check for project errors (project is already deployed on the connection)
		String projectError = getProjectError(connection, project);
		if (projectError != null) {
			CoreUtil.openDialog(true, Messages.BindProjectErrorTitle, projectError);
			return false;
		}
		
		return true;
	}
	
	private boolean checkRemoteStatus() {
		// Try to connect if disconnected
		if (!connection.isConnected()) {
			connectCodewind(connection, this);
		}
		
		// If still not connected then display an error dialog
		if (!connection.isConnected()) {
			CoreUtil.openDialog(true, Messages.BindProjectErrorTitle, NLS.bind(Messages.BindProjectConnectionError, connection.getName()));
			return false;
		}
		
		// Check for project errors (project is already deployed on the connection)
		String projectError = getProjectError(connection, project);
		if (projectError != null) {
			CoreUtil.openDialog(true, Messages.BindProjectErrorTitle, projectError);
			return false;
		}
		
		return true;
	}

	public static void setupLocalConnection(CodewindConnection conn, Wizard wizard) {
		if (conn.isConnected()) {
			return;
		}
		InstallStatus status = CodewindManager.getManager().getInstallStatus();
		if (status.isStarted()) {
			if (!conn.isConnected()) {
				// This should not happen since the connection should be established as part of the start action
				Logger.logError("Local Codewind is started but the connection has not been established or is down");
			}
			return;
		}
		if (!status.isInstalled()) {
			Logger.logError("In BindProjectWizard run method and Codewind is not installed or has unknown status."); //$NON-NLS-1$
			return;
		}
		
		IRunnableWithProgress runnable = new IRunnableWithProgress() {
			@Override
			public void run(IProgressMonitor monitor) throws InvocationTargetException {
				try {
					ProcessResult result = InstallUtil.startCodewind(status.getVersion(), monitor);
					if (result.getExitValue() != 0) {
						Logger.logError("Installer start failed with return code: " + result.getExitValue() + ", output: " + result.getOutput() + ", error: " + result.getError()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						String errorText = result.getError() != null && !result.getError().isEmpty() ? result.getError() : result.getOutput();
						throw new InvocationTargetException(null, "There was a problem trying to start Codewind: " + errorText); //$NON-NLS-1$
					}
					ViewHelper.refreshCodewindExplorerView(conn);
				} catch (TimeoutException e) {
					throw new InvocationTargetException(e, "Codewind did not start in the expected time: " + e.getMessage()); //$NON-NLS-1$
				} catch (Exception e) {
					throw new InvocationTargetException(e, "An error occurred trying to start Codewind: " + e.getMessage()); //$NON-NLS-1$
				}
			}
		};
		try {
			if (wizard.getPageCount() > 0 && wizard.getContainer() != null) {
				wizard.getContainer().run(true, true, runnable);
			} else {
				PlatformUI.getWorkbench().getProgressService().busyCursorWhile(runnable);
			}
		} catch (InvocationTargetException e) {
			Logger.logError("An error occurred trying to start Codewind", e); //$NON-NLS-1$
		} catch (InterruptedException e) {
			Logger.logError("Codewind start was interrupted", e); //$NON-NLS-1$
		}
	}
	
	public static void connectCodewind(CodewindConnection conn, Wizard wizard) {
		IRunnableWithProgress runnable = new IRunnableWithProgress() {
			@Override
			public void run(IProgressMonitor monitor) throws InvocationTargetException {
				try {
					conn.connect(monitor);
					ViewHelper.refreshCodewindExplorerView(conn);
				} catch (Exception e) {
					throw new InvocationTargetException(e, "An error occurred trying to connect to " + conn.getName() + ": " + e.getMessage()); //$NON-NLS-1$  //$NON-NLS-2$
				}
			}
		};
		try {
			if (wizard.getPageCount() > 0 && wizard.getContainer() != null) {
				wizard.getContainer().run(true, true, runnable);
			} else {
				PlatformUI.getWorkbench().getProgressService().busyCursorWhile(runnable);
			}
		} catch (InvocationTargetException e) {
			Logger.logError("An error occurred trying to connect to: " + conn.getName(), e); //$NON-NLS-1$
		} catch (InterruptedException e) {
			Logger.logError("Codewind connect was interrupted for: " + conn.getName(), e); //$NON-NLS-1$
		}
	}
	
	private String getProjectError(CodewindConnection connection, IProject project) {
		if (connection == null || project == null) {
			return null;
		}
		if (connection.getAppByLocation(project.getLocation()) != null) {
			return NLS.bind(Messages.BindProjectAlreadyExistsError,  new String[] {project.getName(), connection.getName()});
		}
		return null;
	}

}
