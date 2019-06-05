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

package org.eclipse.codewind.ui.internal.actions;

import org.eclipse.codewind.core.internal.CodewindApplication;
import org.eclipse.codewind.core.internal.Logger;
import org.eclipse.codewind.ui.CodewindUIPlugin;
import org.eclipse.codewind.ui.internal.editors.ApplicationOverviewEditorInput;
import org.eclipse.codewind.ui.internal.editors.ApplicationOverviewEditorPart;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;

/**
 * Action to open the application overview in a browser.
 */
public class OpenAppOverviewAction implements IObjectActionDelegate {

    protected CodewindApplication app;

    @Override
    public void selectionChanged(IAction action, ISelection selection) {
        if (!(selection instanceof IStructuredSelection)) {
            action.setEnabled(false);
            return;
        }

        IStructuredSelection sel = (IStructuredSelection) selection;
        if (sel.size() == 1) {
            Object obj = sel.getFirstElement();
            if (obj instanceof CodewindApplication) {
            	app = (CodewindApplication) obj;
            	action.setEnabled(true);
            	return;
            }
        }
        action.setEnabled(false);
    }

    @Override
    public void run(IAction action) {
        if (app == null) {
        	// should not be possible
        	Logger.logError("OpenAppOverviewAction ran but no application was selected"); //$NON-NLS-1$
			return;
		}
        
        IWorkbenchWindow workbenchWindow = CodewindUIPlugin.getDefault().getWorkbench().getActiveWorkbenchWindow();
		IWorkbenchPage page = workbenchWindow.getActivePage();
		
		try {
			ApplicationOverviewEditorInput input = new ApplicationOverviewEditorInput(app);
			IEditorPart part = page.openEditor(input, ApplicationOverviewEditorInput.EDITOR_ID);
			if (part instanceof ApplicationOverviewEditorPart) {
				((ApplicationOverviewEditorPart)part).update(app);
			} else {
				// This should not happen
				Logger.logError("Application overview editor part for the " + app.name + " application is the wrong type: " + part.getClass()); //$NON-NLS-1$  //$NON-NLS-2$
			}
		} catch (Exception e) {
			Logger.logError("An error occurred opening the editor for application: " + app.name, e); //$NON-NLS-1$
		}
    }

	@Override
	public void setActivePart(IAction arg0, IWorkbenchPart arg1) {
		// nothing
	}
}
