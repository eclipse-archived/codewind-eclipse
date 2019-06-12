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

import org.eclipse.codewind.core.internal.CodewindEclipseApplication;
import org.eclipse.codewind.core.internal.CoreUtil;
import org.eclipse.codewind.core.internal.Logger;
import org.eclipse.codewind.ui.CodewindUIPlugin;
import org.eclipse.codewind.ui.internal.messages.Messages;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.actions.SelectionProviderAction;

/**
 * Action for unbinding a Codewind project.
 */
public class UnbindProjectAction extends SelectionProviderAction {
	
	CodewindEclipseApplication app;
	
	public UnbindProjectAction(ISelectionProvider selectionProvider) {
		super(selectionProvider, Messages.UnbindActionLabel);
		selectionChanged(getStructuredSelection());
	}

	@Override
	public void selectionChanged(IStructuredSelection sel) {
		if (sel.size() == 1) {
			Object obj = sel.getFirstElement();
			if (obj instanceof CodewindEclipseApplication) {
				app = (CodewindEclipseApplication) obj;
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
			Logger.logError("UnbindProjectAction ran but no application was selected"); //$NON-NLS-1$
			return;
		}
		
		if (CoreUtil.openConfirmDialog(Messages.UnbindActionTitle, NLS.bind(Messages.UnbindActionMessage, app.name))) {
			Job job = new Job(NLS.bind(Messages.UnbindActionJobTitle, app.name)) {
    			@Override
    			protected IStatus run(IProgressMonitor monitor) {
    				try {
    					app.connection.requestProjectUnbind(app.projectID);
    					return Status.OK_STATUS;
    				} catch (Exception e) {
    					Logger.logError("Error requesting application remove: " + app.name, e); //$NON-NLS-1$
    					return new Status(IStatus.ERROR, CodewindUIPlugin.PLUGIN_ID, NLS.bind(Messages.UnbindActionError, app.name), e);
    				}
    			}
    		};
    		job.schedule();
		}
	}
}
