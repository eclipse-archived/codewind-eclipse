/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *	 IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.codewind.ui.internal.actions;

import org.eclipse.codewind.core.internal.CodewindManager;
import org.eclipse.codewind.core.internal.InstallStatus;
import org.eclipse.codewind.core.internal.Logger;
import org.eclipse.codewind.ui.CodewindUIPlugin;
import org.eclipse.codewind.ui.internal.wizards.NewCodewindConnectionWizard;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.actions.SelectionProviderAction;

/**
 * Action to create a new project.
 */
public class AddConnectionAction extends SelectionProviderAction {

	public AddConnectionAction(ISelectionProvider selectionProvider) {
		super(selectionProvider, "New Connection");
		setImageDescriptor(CodewindUIPlugin.getDefaultIcon());
		selectionChanged(getStructuredSelection());
	}


	@Override
	public void selectionChanged(IStructuredSelection sel) {
		if (sel.size() == 1) {
			Object obj = sel.getFirstElement();
			if (obj instanceof CodewindManager) {
				InstallStatus status = CodewindManager.getManager().getInstallStatus();
				if (status.isInstalled()) {
					setEnabled(true);
					return;
				}
			}
		}
		setEnabled(false);
	}

	@Override
	public void run() {
		try {
			NewCodewindConnectionWizard wizard = new NewCodewindConnectionWizard();
			WizardDialog dialog = new WizardDialog(Display.getDefault().getActiveShell(), wizard);
			if (dialog.open() == Window.CANCEL) {
				return;
			}
		} catch (Exception e) {
			Logger.logError("An error occurred running the new connection action", e); //$NON-NLS-1$
		}
	}
}
