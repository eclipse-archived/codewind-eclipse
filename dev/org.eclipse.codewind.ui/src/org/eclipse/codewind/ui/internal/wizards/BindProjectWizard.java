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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.codewind.core.CodewindCorePlugin;
import org.eclipse.codewind.core.internal.CodewindApplication;
import org.eclipse.codewind.core.internal.Logger;
import org.eclipse.codewind.core.internal.cli.ProjectUtil;
import org.eclipse.codewind.core.internal.connection.CodewindConnection;
import org.eclipse.codewind.core.internal.connection.CodewindConnectionManager;
import org.eclipse.codewind.core.internal.connection.ProjectTypeInfo;
import org.eclipse.codewind.core.internal.connection.ProjectTypeInfo.ProjectSubtypeInfo;
import org.eclipse.codewind.core.internal.constants.ProjectInfo;
import org.eclipse.codewind.core.internal.constants.ProjectType;
import org.eclipse.codewind.ui.CodewindUIPlugin;
import org.eclipse.codewind.ui.internal.actions.ImportProjectAction;
import org.eclipse.codewind.ui.internal.actions.OpenAppOverviewAction;
import org.eclipse.codewind.ui.internal.messages.Messages;
import org.eclipse.codewind.ui.internal.prefs.RegistryManagementDialog;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;

public class BindProjectWizard extends Wizard implements INewWizard {

	private ProjectSelectionPage projectPage;
	private ProjectValidationPage projectValidationPage;
	private ProjectTypeSelectionPage projectTypePage;
	
	private final CodewindConnection connection;
	private IProject project = null;
	private IPath projectPath = null;
	
	// If a connection is passed in and no project then the project selection page will be shown
	public BindProjectWizard(CodewindConnection connection) {
		this(connection, null);
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
		if (projectPath == null) {
			projectPage = new ProjectSelectionPage(this, connection);
			addPage(projectPage);
		}
		projectValidationPage = new ProjectValidationPage(this, connection, project);
		projectTypePage = new ProjectTypeSelectionPage(connection);
		addPage(projectValidationPage);
		addPage(projectTypePage);
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
		
		final List<CodewindApplication> existingDeployments = new ArrayList<CodewindApplication>();
		for (CodewindConnection conn : CodewindConnectionManager.activeConnections()) {
			if (conn.isConnected()) {
				CodewindApplication app = conn.getAppByLocation(projectPath);
				if (app != null && app.isEnabled()) {
					existingDeployments.add(app);
				}
			}
		}
		
		final ProjectDeployedDialog.Behaviour selectedBehaviour;
		if (!existingDeployments.isEmpty()) {
			ProjectDeployedDialog dialog = new ProjectDeployedDialog(getShell(), projectPath);
			if (dialog.open() == IStatus.OK) {
				selectedBehaviour = dialog.getSelectedBehaviour();
			} else {
				return false;
			}
		} else {
			selectedBehaviour = null;
		}
		
		// Use the detected type if the validation page is active
		final ProjectInfo projectInfo = projectValidationPage.isActivePage() ? projectValidationPage.getProjectInfo() : null;
		
		final ProjectTypeInfo type = projectTypePage.getType();
		final ProjectSubtypeInfo projectSubtype = projectTypePage.getSubtype();
		final String language = projectTypePage.getLanguage();		
		
		Job job = new Job(NLS.bind(Messages.BindProjectWizardJobLabel, new String[] {connection.getName(), name})) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					SubMonitor mon = SubMonitor.convert(monitor, 140);
					
					// Check for a push registry if Codewind style project
					if (!connection.isLocal() && ProjectType.isCodewindStyle(type.getId()) && !connection.requestHasPushRegistry()) {
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
					if (projectInfo != null) {
						ProjectUtil.bindProject(name, path, projectInfo.language.getId(), projectInfo.type.getId(), connection.getConid(), mon.split(30));
					} else {
						// call validate again with type and subtype hint
						// allows it to run extension commands if defined for that type and subtype
						if (projectSubtype != null) {
							ProjectUtil.validateProject(name, path, type + ":" + projectSubtype.id, connection.getConid(), mon.split(10));
						}
						mon.setWorkRemaining(40);
						ProjectUtil.bindProject(name, path, language, type.getId(), connection.getConid(), mon.split(20));
					}
					if (mon.isCanceled()) {
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
					return Status.OK_STATUS;
				} catch (Exception e) {
					Logger.logError("An error occured trying to add the project to Codewind: " + projectPath.toOSString(), e); //$NON-NLS-1$
					return new Status(IStatus.ERROR, CodewindUIPlugin.PLUGIN_ID, NLS.bind(Messages.BindProjectWizardError, projectPath.toOSString()), e);
				}
			}
		};
		job.schedule();

		return true;
	}
	
	public void setProjectInfo(ProjectInfo projectInfo) {
		if (projectTypePage != null) {
			projectTypePage.setProjectInfo(projectInfo);
		}
	}

	public void setProject(IProject project, IPath projectPath) {
		if (projectValidationPage != null) {
			projectValidationPage.setProject(project, projectPath, true);
		}
	}
}
