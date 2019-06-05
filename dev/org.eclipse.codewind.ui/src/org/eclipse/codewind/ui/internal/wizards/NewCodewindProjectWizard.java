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

import org.eclipse.codewind.core.internal.Logger;
import org.eclipse.codewind.core.internal.CodewindApplication;
import org.eclipse.codewind.core.internal.connection.CodewindConnection;
import org.eclipse.codewind.core.internal.connection.CodewindConnectionManager;
import org.eclipse.codewind.core.internal.console.ProjectTemplateInfo;
import org.eclipse.codewind.core.internal.constants.ProjectType;
import org.eclipse.codewind.ui.CodewindUIPlugin;
import org.eclipse.codewind.ui.internal.actions.ImportProjectAction;
import org.eclipse.codewind.ui.internal.messages.Messages;
import org.eclipse.codewind.ui.internal.views.ViewHelper;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.osgi.util.NLS;
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
		setWindowTitle(Messages.NewProjectPage_ShellTitle);
		newProjectPage = new NewCodewindProjectPage(connection, templateList);
		addPage(newProjectPage);
	}

	@Override
	public boolean performCancel() {
		CodewindConnection newConnection = newProjectPage.getConnection();
		if (newConnection != null && CodewindConnectionManager.getActiveConnection(newConnection.baseUrl.toString()) == null) {
			newConnection.close();
		}
		return super.performCancel();
	}

	@Override
	public boolean performFinish() {
		if(!canFinish()) {
			return false;
		}

		ProjectTemplateInfo info = newProjectPage.getProjectTemplateInfo();
		String name = newProjectPage.getProjectName();
		CodewindConnection newConnection = newProjectPage.getConnection();
		if (info == null || name == null || newConnection == null) {
			Logger.logError("The connection, project type or name was null for the new project wizard"); //$NON-NLS-1$
			return false;
		}
		
		Job job = new Job("Creating Codewind project: " + name) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					newConnection.requestProjectCreate(info, name);
					String type = null;
					if (ProjectType.LANGUAGE_JAVA.equals(info.getLanguage())) {
						if (info.getLabel().toLowerCase().contains("spring")) {
							type = "spring";
						} else if (info.getLabel().toLowerCase().contains("microprofile")) {
							type = "liberty";
						} else {
							type = "docker";
						}
					} else if (ProjectType.LANGUAGE_NODEJS.equals(info.getLanguage())) {
						type = "nodejs";
					} else if (ProjectType.LANGUAGE_SWIFT.equals(info.getLanguage())) {
						type = "swift";
					} else {
						type = "docker";
					}
					newConnection.requestProjectBind(name, newConnection.getWorkspacePath() + "/" + name, info.getLanguage(), type);
					if (CodewindConnectionManager.getActiveConnection(newConnection.baseUrl.toString()) == null) {
						CodewindConnectionManager.add(newConnection);
					}
					newConnection.refreshApps(null);
					CodewindApplication app = newConnection.getAppByName(name);
					if (app != null) {
						ImportProjectAction.importProject(app);
					} else {
						Logger.logError("Could not get the application for import: " + name); //$NON-NLS-1$
					}
					ViewHelper.openCodewindExplorerView();
					ViewHelper.refreshCodewindExplorerView(newConnection);
					ViewHelper.expandConnection(newConnection);
					return Status.OK_STATUS;
				} catch (Exception e) {
					Logger.logError("An error occured trying to create a project with type: " + info.getUrl() + ", and name: " + name, e); //$NON-NLS-1$ //$NON-NLS-2$
					return new Status(IStatus.ERROR, CodewindUIPlugin.PLUGIN_ID, NLS.bind(Messages.NewProjectPage_ProjectCreateErrorMsg, name), e);
				}
			}
		};
		job.schedule();
		return true;
	}
}
