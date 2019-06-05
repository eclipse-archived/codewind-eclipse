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

import org.eclipse.codewind.core.internal.connection.CodewindConnection;
import org.eclipse.codewind.ui.CodewindUIPlugin;
import org.eclipse.codewind.ui.internal.messages.Messages;
import org.eclipse.codewind.ui.internal.views.ViewHelper;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;

/**
 * This wizard, which can be launched through the MC Preferences page or from the New menu.
 */
public class NewCodewindConnectionWizard extends Wizard implements INewWizard {

	private NewCodewindConnectionPage newConnectionPage;

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {

		setDefaultPageImageDescriptor(CodewindUIPlugin.getImageDescriptor(CodewindUIPlugin.CODEWIND_BANNER));

		// TODO help
		setHelpAvailable(false);
		setNeedsProgressMonitor(true);
	}

	@Override
	public void addPages() {
		setWindowTitle(Messages.NewConnectionWizard_ShellTitle);
		newConnectionPage = new NewCodewindConnectionPage();
		addPage(newConnectionPage);
	}

	@Override
	public boolean canFinish() {
		return newConnectionPage.getConnection() != null;
	}

	@Override
	public boolean performCancel() {
		CodewindConnection connection = newConnectionPage.getConnection();
		if (connection != null) {
			connection.close();
		}
		return true;
	}

	@Override
	public boolean performFinish() {
		if(!canFinish()) {
			return false;
		}

		newConnectionPage.performFinish();

		ViewHelper.openCodewindExplorerView();
		ViewHelper.refreshCodewindExplorerView(null);
		ViewHelper.expandConnection(newConnectionPage.getConnection());

		return true;
	}
}
