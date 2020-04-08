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

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeoutException;

import org.eclipse.codewind.core.CodewindCorePlugin;
import org.eclipse.codewind.core.internal.CodewindApplication;
import org.eclipse.codewind.core.internal.CodewindManager;
import org.eclipse.codewind.core.internal.CodewindManager.InstallerStatus;
import org.eclipse.codewind.core.internal.CoreUtil;
import org.eclipse.codewind.core.internal.FileUtil;
import org.eclipse.codewind.core.internal.Logger;
import org.eclipse.codewind.core.internal.cli.ProjectUtil;
import org.eclipse.codewind.core.internal.connection.CodewindConnection;
import org.eclipse.codewind.core.internal.connection.CodewindConnectionManager;
import org.eclipse.codewind.core.internal.connection.ProjectTemplateInfo;
import org.eclipse.codewind.ui.CodewindUIPlugin;
import org.eclipse.codewind.ui.internal.actions.CodewindInstall;
import org.eclipse.codewind.ui.internal.actions.ImportProjectAction;
import org.eclipse.codewind.ui.internal.actions.OpenAppOverviewAction;
import org.eclipse.codewind.ui.internal.messages.Messages;
import org.eclipse.codewind.ui.internal.prefs.RegistryManagementDialog;
import org.eclipse.codewind.ui.internal.views.ViewHelper;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;

public class NewCodewindProjectWizard extends Wizard implements INewWizard {

	private List<CodewindConnection> connections = null;
	private CodewindConnection connection = null;
	private ConnectionSelectionPage connectionPage = null;
	private NewCodewindProjectPage newProjectPage = null;
	
	public NewCodewindProjectWizard() {
		setDefaultPageImageDescriptor(CodewindUIPlugin.getImageDescriptor(CodewindUIPlugin.CODEWIND_BANNER));
		setHelpAvailable(false);
		setNeedsProgressMonitor(true);		
	}
	
	public NewCodewindProjectWizard(CodewindConnection connection) {
		this();
		this.connection = connection;
	}
	
	public NewCodewindProjectWizard(List<CodewindConnection> connections) {
		this();
		this.connections = connections;
	}

	@Override
	public void init(IWorkbench arg0, IStructuredSelection arg1) {
		// Empty
	}

	@Override
	public void addPages() {
		setWindowTitle(Messages.NewProjectPage_ShellTitle);
		// If there is only one connection then use it
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
		newProjectPage = new NewCodewindProjectPage(connection);
		addPage(newProjectPage);
		
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
			newProjectPage.setConnection(connection);
			return newProjectPage;
		}
		return null;
	}

	@Override
	public boolean canFinish() {
		if (newProjectPage != null) {
			return newProjectPage.canFinish();
		}
		return false;
	}

	@Override
	public boolean performFinish() {
		if(!canFinish()) {
			return false;
		}

		ProjectTemplateInfo info = newProjectPage.getProjectTemplateInfo();
		String projectName = newProjectPage.getProjectName();
		IPath location = newProjectPage.getLocationPath();
		if (location != null && location.equals(ResourcesPlugin.getWorkspace().getRoot().getLocation())) {
			location = location.append(projectName);
		}
		final IPath projectPath = location;
		if (info == null || projectName == null || projectPath == null) {
			Logger.logError("The project type, name or location was null for the new project wizard"); //$NON-NLS-1$
			return false;
		}
		
		Job job = new Job(NLS.bind(Messages.NewProjectPage_CreateJobLabel, projectName)) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					SubMonitor mon = SubMonitor.convert(monitor, 140);
					
					// Check for a push registry if Codewind style project
					if (!connection.isLocal() && info.isCodewindStyle() && !connection.requestHasPushRegistry()) {
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
					
					// Create and bind the project
					ProjectUtil.createProject(projectName, projectPath.toOSString(), info.getUrl(), connection.getConid(), mon.split(40));
					if (mon.isCanceled()) {
						cleanup(projectName, projectPath, connection);
						return Status.CANCEL_STATUS;
					}
					ProjectUtil.bindProject(projectName, projectPath.toOSString(), info.getLanguage(), info.getProjectType(), connection.getConid(), mon.split(40));
					if (mon.isCanceled()) {
						cleanup(projectName, projectPath, connection);
						return Status.CANCEL_STATUS;
					}
					mon.split(10);
					connection.refreshApps(null);
					if (mon.isCanceled()) {
						return Status.CANCEL_STATUS;
					}
					mon.split(10);
					CodewindApplication app = connection.getAppByName(projectName);
					if (app != null) {
						ImportProjectAction.importProject(app);
						if (CodewindCorePlugin.getDefault().getPreferenceStore().getBoolean(CodewindCorePlugin.AUTO_OPEN_OVERVIEW_PAGE)) {
							Display.getDefault().asyncExec(() -> OpenAppOverviewAction.openAppOverview(app));
						}
					} else {
						Logger.logError("Could not get the application for import: " + projectName); //$NON-NLS-1$
					}
					mon.worked(10);
					mon.done();
					ViewHelper.openCodewindExplorerView();
					CodewindUIPlugin.getUpdateHandler().updateConnection(connection);
					return Status.OK_STATUS;
				} catch (TimeoutException e) {
					Logger.logError("A timeout occurred trying to create a project with type: " + info.getUrl() + ", and name: " + projectName, e); //$NON-NLS-1$ //$NON-NLS-2$
					cleanup(projectName, projectPath, connection);
					return new Status(IStatus.ERROR, CodewindUIPlugin.PLUGIN_ID, NLS.bind(Messages.NewProjectPage_ProjectCreateTimeoutMsg, projectName), e);
				} catch (Exception e) {
					Logger.logError("An error occured trying to create a project with type: " + info.getUrl() + ", and name: " + projectName, e); //$NON-NLS-1$ //$NON-NLS-2$
					return new Status(IStatus.ERROR, CodewindUIPlugin.PLUGIN_ID, NLS.bind(Messages.NewProjectPage_ProjectCreateErrorMsg, projectName), e);
				}
			}
		};
		job.schedule();
		return true;
	}
	
	private void cleanup(String projectName, IPath projectPath, CodewindConnection connection) {
		Job job = new Job(NLS.bind(Messages.ProjectCleanupJobLabel, projectName)) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				SubMonitor mon = SubMonitor.convert(monitor, 30);
				mon.split(10);
				connection.refreshApps(null);
				CodewindApplication app = connection.getAppByName(projectName);
				if (app != null) {
					try {
						ProjectUtil.removeProject(app.name, app.projectID, true, mon.split(20));
					} catch (Exception e) {
						Logger.logError("An error occurred while trying to remove the project after project create terminated for: " + projectName, e);
						return new Status(IStatus.ERROR, CodewindUIPlugin.PLUGIN_ID, NLS.bind(Messages.ProjectCleanupError, projectName), null);
					}
				} else {
					mon.split(20);
					boolean success = false;
					for (int i = 0; i < 10 && !success; i++) {
						try {
							FileUtil.deleteDirectory(projectPath.toOSString(), true);
							success = true;
						} catch (IOException e) {
							mon.worked(10);
							mon.setWorkRemaining(20);
							try {
								Thread.sleep(1000);
							} catch (InterruptedException e1) {
								// Ignore
							}
						}
					}
					if (!success) {
						Logger.logError("An error occurred while trying to cleanup the project directory after project create terminated: " + projectPath.toOSString());
						return new Status(IStatus.ERROR, CodewindUIPlugin.PLUGIN_ID, NLS.bind(Messages.DirectoryCleanupError, projectPath.toOSString()), null);
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
			BindProjectWizard.setupLocalConnection(connection, this);
		} else {
			CodewindInstall.codewindInstallerDialog();
			return false;
		}

		// If the connection is still not set up then display an error dialog
		if (!connection.isConnected()) {
			CoreUtil.openDialog(true, Messages.NewProjectWizard_ErrorTitle, NLS.bind(Messages.NewProjectWizard_ConnectionError, connection.getName()));
			return false;
		}
		
		return true;
	}
	
	private boolean checkRemoteStatus() {
		// Try to connect if disconnected
		if (!connection.isConnected()) {
			BindProjectWizard.connectCodewind(connection, this);
		}
		
		// If still not connected then display an error dialog
		if (!connection.isConnected()) {
			CoreUtil.openDialog(true, Messages.NewProjectWizard_ErrorTitle, NLS.bind(Messages.NewProjectWizard_ConnectionError, connection.getName()));
			return false;
		}
		
		return true;
	}

}
