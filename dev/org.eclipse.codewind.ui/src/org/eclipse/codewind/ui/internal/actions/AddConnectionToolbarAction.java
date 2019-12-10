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

import org.eclipse.codewind.core.internal.Logger;
import org.eclipse.codewind.ui.internal.wizards.NewCodewindConnectionWizard;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;

/**
 * Toolbar action to create a new project.
 */
public class AddConnectionToolbarAction implements IViewActionDelegate {
	
	@Override
	public void run(IAction action) {
		try {
			NewCodewindConnectionWizard wizard = new NewCodewindConnectionWizard();
			WizardDialog dialog = new WizardDialog(Display.getDefault().getActiveShell(), wizard);
			dialog.open();
		} catch (Exception e) {
			Logger.logError("An error occurred running the new connection toolbar action", e); //$NON-NLS-1$
		}
	}

	@Override
	public void selectionChanged(IAction action, ISelection sel) {
		// This action is always enabled
		action.setEnabled(true);
	}

	@Override
	public void init(IViewPart part) {
		// empty
	}
}
