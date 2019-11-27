/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.codewind.ui.internal.wizards;

import java.util.List;

import org.eclipse.codewind.core.CodewindCorePlugin;
import org.eclipse.codewind.core.internal.CodewindApplication;
import org.eclipse.codewind.core.internal.CodewindManager;
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
import org.eclipse.codewind.ui.internal.views.ViewHelper;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;

public class NewCodewindProjectWizard extends Wizard implements INewWizard {

	private CodewindConnection connection = null;
	private List<ProjectTemplateInfo> templateList = null;
	private NewCodewindProjectPage newProjectPage = null;
	
	public NewCodewindProjectWizard() {
		setDefaultPageImageDescriptor(CodewindUIPlugin.getImageDescriptor(CodewindUIPlugin.CODEWIND_BANNER));
		setHelpAvailable(false);
		setNeedsProgressMonitor(true);		
	}
	
	public NewCodewindProjectWizard(CodewindConnection connection, List<ProjectTemplateInfo> templateList) {
		this();
		this.connection = connection;
		this.templateList = templateList;
	}

	@Override
	public void init(IWorkbench arg0, IStructuredSelection arg1) {
		// Empty
	}

	@Override
	public void addPages() {
		if (CodewindManager.getManager().getInstallerStatus() != null) {
			// The installer is currently running
			CodewindInstall.installerActiveDialog(CodewindManager.getManager().getInstallerStatus());
			if (getContainer() != null) {
				getContainer().getShell().close();
			}
		} else if (CodewindManager.getManager().getInstallStatus().isInstalled()) {
			setWindowTitle(Messages.NewProjectPage_ShellTitle);
			newProjectPage = new NewCodewindProjectPage(connection, templateList);
			addPage(newProjectPage);
		} else {
			CodewindInstall.codewindInstallerDialog();
			if (getContainer() != null) {
				getContainer().getShell().close();
			}
		}
	}

	@Override
	public boolean performCancel() {
		if (newProjectPage != null) {
			CodewindConnection newConnection = newProjectPage.getConnection();
			if (newConnection != null && CodewindConnectionManager.getActiveConnection(newConnection.getBaseURI().toString()) == null) {
				newConnection.disconnect();
			}
		}
		return super.performCancel();
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
		CodewindConnection newConnection = newProjectPage.getConnection();
		if (info == null || projectName == null || projectPath == null || newConnection == null) {
			Logger.logError("The connection, project type, name or location was null for the new project wizard"); //$NON-NLS-1$
			return false;
		}
		
		Job job = new Job(NLS.bind(Messages.NewProjectPage_CreateJobLabel, projectName)) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					SubMonitor mon = SubMonitor.convert(monitor, 100);
					ProjectUtil.createProject(projectName, projectPath.toOSString(), info.getUrl(), newConnection.getConid(), mon.split(40));
					if (mon.isCanceled()) {
						return Status.CANCEL_STATUS;
					}
					ProjectUtil.bindProject(projectName, projectPath.toOSString(), info.getLanguage(), info.getProjectType(), newConnection.getConid(), mon.split(40));
					if (CodewindConnectionManager.getActiveConnection(newConnection.getBaseURI().toString()) == null) {
						CodewindConnectionManager.add(newConnection);
					}
					if (mon.isCanceled()) {
						return Status.CANCEL_STATUS;
					}
					newConnection.refreshApps(null);
					if (mon.isCanceled()) {
						return Status.CANCEL_STATUS;
					}
					mon.worked(10);
					CodewindApplication app = newConnection.getAppByName(projectName);
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
					ViewHelper.refreshCodewindExplorerView(newConnection);
					ViewHelper.expandConnection(newConnection);
					return Status.OK_STATUS;
				} catch (Exception e) {
					Logger.logError("An error occured trying to create a project with type: " + info.getUrl() + ", and name: " + projectName, e); //$NON-NLS-1$ //$NON-NLS-2$
					return new Status(IStatus.ERROR, CodewindUIPlugin.PLUGIN_ID, NLS.bind(Messages.NewProjectPage_ProjectCreateErrorMsg, projectName), e);
				}
			}
		};
		job.schedule();
		return true;
	}
}
