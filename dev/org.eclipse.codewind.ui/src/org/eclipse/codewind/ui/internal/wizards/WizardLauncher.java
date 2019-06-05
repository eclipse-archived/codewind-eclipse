/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.codewind.ui.internal.wizards;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;

/**
 * Static utilities to eliminate some boilerplate for launching wizards.
 */
public class WizardLauncher {

	private WizardLauncher() {}

	/**
	 * Useful when the wizard to be launched does not care about the workbench or selection.
	 * Do not use with the Link Wizard since it requires these.
	 */
	public static void launchWizardWithoutSelection(Wizard wizard) {
		WizardDialog dialog = new WizardDialog(Display.getDefault().getActiveShell(), wizard);
		dialog.create();
		dialog.open();
	}

	public static void launchWizard(INewWizard wizard, ISelection selection, IWorkbench workbench, Shell parentShell) {
		IStructuredSelection structuredSelection = null;
		if (selection instanceof IStructuredSelection) {
			structuredSelection = (IStructuredSelection) selection;
		}

		wizard.init(workbench, structuredSelection);

		WizardDialog dialog = new WizardDialog(parentShell, wizard);
		dialog.create();
		dialog.open();
	}
}
