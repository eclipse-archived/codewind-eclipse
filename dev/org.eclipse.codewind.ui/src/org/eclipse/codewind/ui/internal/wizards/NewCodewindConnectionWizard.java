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

package org.eclipse.codewind.ui.internal.wizards;

import org.eclipse.codewind.ui.CodewindUIPlugin;
import org.eclipse.codewind.ui.internal.messages.Messages;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;

/**
 * This wizard, which can be launched through the MC Preferences page or from the New menu.
 */
public class NewCodewindConnectionWizard extends Wizard implements INewWizard {

	private NewCodewindConnectionPage newConnectionPage;
	
	public NewCodewindConnectionWizard() {
		setDefaultPageImageDescriptor(CodewindUIPlugin.getImageDescriptor(CodewindUIPlugin.CODEWIND_BANNER));
		setHelpAvailable(false);
		setNeedsProgressMonitor(true);		
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		// Empty
	}

	@Override
	public void addPages() {
		setWindowTitle(Messages.NewConnectionWizard_ShellTitle);
		newConnectionPage = new NewCodewindConnectionPage();
		addPage(newConnectionPage);
	}

	@Override
	public boolean canFinish() {
		return newConnectionPage.canFinish();
	}

	@Override
	public boolean performFinish() {
		if(!canFinish()) {
			return false;
		}

		Job job = new Job(NLS.bind(Messages.NewConnectionWizard_CreateJobTitle, newConnectionPage.getConnectionName())) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				return newConnectionPage.createConnection(monitor);
			}
		};
		job.schedule();

		return true;
	}
}
