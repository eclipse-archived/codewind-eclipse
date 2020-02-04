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

package org.eclipse.codewind.ui.internal.actions;

import org.eclipse.codewind.core.internal.Logger;
import org.eclipse.codewind.ui.internal.wizards.BindProjectWizard;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

public class BindProjectAction implements IObjectActionDelegate {
	
	private IProject project;

	@Override
	public void run(IAction action) {
		if (project == null) {
			// Should not happen
			Logger.logError("BindProjectAction ran but no project was selected"); //$NON-NLS-1$
			return;
		}

		BindProjectWizard wizard = new BindProjectWizard(null, project);
		WizardDialog dialog = new WizardDialog(Display.getDefault().getActiveShell(), wizard);
		dialog.open();
	}
	
	@Override
	public void selectionChanged(IAction action, ISelection selection) {
        if (!(selection instanceof IStructuredSelection)) {
            action.setEnabled(false);
            return;
        }

        IStructuredSelection sel = (IStructuredSelection) selection;
        if (sel.size() == 1) {
            Object obj = sel.getFirstElement();
            if (obj instanceof IProject) {
            	project = (IProject)obj;
            	action.setEnabled(project.isAccessible());
            	return;
            }
        }
        
        action.setEnabled(false);
	}

	@Override
	public void setActivePart(IAction action, IWorkbenchPart part) {
		// empty
	}
}
