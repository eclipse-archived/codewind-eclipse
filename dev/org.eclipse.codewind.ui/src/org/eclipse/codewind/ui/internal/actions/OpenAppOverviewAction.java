/*******************************************************************************
 * Copyright (c) 2018, 2020 IBM Corporation and others.
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
import org.eclipse.codewind.ui.internal.messages.Messages;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.actions.SelectionProviderAction;

/**
 * Action to open the application overview in a browser.
 */
public class OpenAppOverviewAction extends SelectionProviderAction {

    protected CodewindApplication app;
    
	public OpenAppOverviewAction(ISelectionProvider selectionProvider) {
		super(selectionProvider, Messages.OpenAppOverviewAction_Label);
		setImageDescriptor(CodewindUIPlugin.getImageDescriptor(CodewindUIPlugin.CODEWIND_ICON));
		selectionChanged(getStructuredSelection());
	}

    @Override
    public void selectionChanged(IStructuredSelection sel) {
        if (sel.size() == 1) {
            Object obj = sel.getFirstElement();
            if (obj instanceof CodewindApplication) {
            	app = (CodewindApplication) obj;
            	setEnabled(true);
            	return;
            }
        }
        setEnabled(false);
    }

    @Override
    public void run() {
        if (app == null) {
        	// should not be possible
        	Logger.logError("OpenAppOverviewAction ran but no application was selected"); //$NON-NLS-1$
			return;
		}
        openAppOverview(app);
    }
    
    public static void openAppOverview(CodewindApplication app) {
    	IWorkbenchWindow workbenchWindow = CodewindUIPlugin.getDefault().getWorkbench().getActiveWorkbenchWindow();
		IWorkbenchPage page = workbenchWindow.getActivePage();
		
		try {
			ApplicationOverviewEditorInput input = new ApplicationOverviewEditorInput(app);
			IEditorPart part = page.openEditor(input, ApplicationOverviewEditorInput.EDITOR_ID);
			if (!(part instanceof ApplicationOverviewEditorPart)) {
				// This should not happen
				Logger.logError("Application overview editor part for the " + app.name + " application is the wrong type: " + part.getClass()); //$NON-NLS-1$  //$NON-NLS-2$
			}
		} catch (Exception e) {
			Logger.logError("An error occurred opening the editor for application: " + app.name, e); //$NON-NLS-1$
		}
    }
}
