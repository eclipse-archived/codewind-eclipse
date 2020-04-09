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

import org.eclipse.codewind.core.internal.CodewindEclipseApplication;
import org.eclipse.codewind.core.internal.Logger;
import org.eclipse.codewind.core.internal.constants.AppStatus;
import org.eclipse.codewind.ui.internal.messages.Messages;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.actions.SelectionProviderAction;

/**
 * Action for attaching the debugger.  This action is only enabled if the
 * application is in debug mode and is starting or started and a debugger
 * is not already attached.
 */
public class AttachDebuggerAction extends SelectionProviderAction {
	
	CodewindEclipseApplication app;
	
	public AttachDebuggerAction(ISelectionProvider selectionProvider) {
        super(selectionProvider, Messages.AttachDebuggerLabel);
        selectionChanged(getStructuredSelection());
    }

    @Override
    public void selectionChanged(IStructuredSelection sel) {
		if (sel.size() == 1) {
			Object obj = sel.getFirstElement();
			if (obj instanceof CodewindEclipseApplication) {
            	app = (CodewindEclipseApplication) obj;
            	if (app.projectLanguage.isJavaScript()) {
            		this.setText(Messages.LaunchDebugSessionLabel);
            	} else {
            		this.setText(Messages.AttachDebuggerLabel);
            	}
            	if (app.isAvailable() && app.readyForDebugSession() &&
            			(app.getAppStatus() == AppStatus.STARTED || app.getAppStatus() == AppStatus.STARTING)) {
            		setEnabled(app.canAttachDebugger());
            		return;
            	}
            }
		}
		setEnabled(false);
    }

    @Override
    public void run() {
    	if (app == null) {
			// should not be possible
			Logger.logError("AttachDebuggerAction ran but no application was selected"); //$NON-NLS-1$
			return;
		}

		app.attachDebugger();
    }
    
    public boolean showAction() {
    	// Don't show the action if the app does not support debug
    	return (app != null && app.isAvailable() && app.canInitiateDebugSession());
    }

}
