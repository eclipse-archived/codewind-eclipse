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

import org.eclipse.codewind.core.CodewindCorePlugin;
import org.eclipse.codewind.core.internal.CodewindApplication;
import org.eclipse.codewind.core.internal.InstallUtil;
import org.eclipse.codewind.core.internal.Logger;
import org.eclipse.codewind.core.internal.connection.CodewindConnection;
import org.eclipse.codewind.core.internal.connection.ProjectTypeInfo.ProjectSubtypeInfo;
import org.eclipse.codewind.ui.CodewindUIPlugin;
import org.eclipse.codewind.ui.internal.actions.ImportProjectAction;
import org.eclipse.codewind.ui.internal.actions.OpenAppOverviewAction;
import org.eclipse.codewind.ui.internal.messages.Messages;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;

public class BindProjectWizard extends Wizard implements INewWizard {

	private ProjectSelectionPage projectPage;
	private ProjectTypeSelectionPage projectTypePage;
	
	private final CodewindConnection connection;
	private IPath projectPath = null;
	
	// If a connection is passed in and no project then the project selection page will be shown
	public BindProjectWizard(CodewindConnection connection) {
		super();
		this.connection = connection;
		init();
	}
	
	// If the project is passed in then the project selection page will not be shown
	public BindProjectWizard(CodewindConnection connection, IPath projectPath) {
		super();
		this.connection = connection;
		this.projectPath = projectPath;
		init();
	}
	
	private void init() {
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
		projectTypePage = new ProjectTypeSelectionPage(connection, projectPath);
		addPage(projectTypePage);
	}

	@Override
	public boolean canFinish() {
		boolean canFinish = projectTypePage.canFinish();
		if (projectPage != null) {
			canFinish &= projectPage.canFinish();
		}
		return canFinish;
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
			projectPath = projectPage.getProjectPath();
		}

		final String name = projectPath.lastSegment();
		
		final String type = projectTypePage.getType().getId();
		ProjectSubtypeInfo projectSubtype = projectTypePage.getSubtype();
		final String subtype = (projectSubtype == null) ? null : projectSubtype.id;
		final String language = projectTypePage.getLanguage();		
		
		Job job = new Job(NLS.bind(Messages.BindProjectWizardJobLabel, name)) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					String path = projectPath.toFile().getAbsolutePath();
					// call validate again with type and subtype hint
					// allows it to run extension commands if defined for that type and subtype
					if (subtype != null) {
						InstallUtil.validateProject(name, path, type + ":" + subtype, monitor);
					}
					connection.requestProjectBind(name, path, language, type);
					connection.refreshApps(null);
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
	
	public void setProjectPath(IPath projectPath) {
		if (projectTypePage != null) {
			projectTypePage.setProjectPath(projectPath);
		}
	}
}
