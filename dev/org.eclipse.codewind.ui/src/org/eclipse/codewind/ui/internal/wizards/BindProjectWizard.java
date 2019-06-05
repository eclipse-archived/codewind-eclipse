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

import org.eclipse.codewind.core.internal.Logger;
import org.eclipse.codewind.core.internal.connection.CodewindConnection;
import org.eclipse.codewind.ui.CodewindUIPlugin;
import org.eclipse.codewind.ui.internal.messages.Messages;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;

public class BindProjectWizard extends Wizard implements INewWizard {

	private ProjectSelectionPage projectPage;
	private LanguageSelectionPage languagePage;
	
	private final CodewindConnection connection;
	private IProject project = null;
	
	// If a connection is passed in and no project then the project selection page will be shown
	public BindProjectWizard(CodewindConnection connection) {
		super();
		this.connection = connection;
		init();
	}
	
	// If the project is passed in then the project selection page will not be shown
	public BindProjectWizard(CodewindConnection connection, IProject project) {
		super();
		this.connection = connection;
		this.project = project;
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
		if (project == null) {
			projectPage = new ProjectSelectionPage(this, connection);
			addPage(projectPage);
		}
		languagePage = new LanguageSelectionPage(connection, project);
		addPage(languagePage);
	}

	@Override
	public boolean canFinish() {
		boolean canFinish = languagePage.canFinish();
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
			project = projectPage.getProject();
		}

		Job job = new Job(NLS.bind(Messages.BindProjectWizardJobLabel, project.getName())) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					connection.requestProjectBind(project.getName(), project.getLocation().toFile().getAbsolutePath(), languagePage.getLanguage(), languagePage.getType());
					return Status.OK_STATUS;
				} catch (Exception e) {
					Logger.logError("An error occured trying to add the project to Codewind: " + project.getName(), e); //$NON-NLS-1$
					return new Status(IStatus.ERROR, CodewindUIPlugin.PLUGIN_ID, NLS.bind(Messages.BindProjectWizardError, project.getName()), e);
				}
			}
		};
		job.schedule();

		return true;
	}
	
	public void setProject(IProject project) {
		if (languagePage != null) {
			languagePage.setProject(project);
		}
	}
}
