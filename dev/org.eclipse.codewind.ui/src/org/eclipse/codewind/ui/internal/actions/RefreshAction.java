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
import org.eclipse.codewind.core.internal.CodewindManager;
import org.eclipse.codewind.core.internal.Logger;
import org.eclipse.codewind.core.internal.connection.CodewindConnection;
import org.eclipse.codewind.core.internal.connection.LocalConnection;
import org.eclipse.codewind.ui.internal.messages.Messages;
import org.eclipse.codewind.ui.internal.views.ViewHelper;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

/**
 * Refresh action for a Codewind connection or application.  This retrieves the
 * latest information for the object from Microcliamte and updates the view.
 */
public class RefreshAction implements IObjectActionDelegate {

    protected Object codewindObject;

    @Override
    public void selectionChanged(IAction action, ISelection selection) {
        if (!(selection instanceof IStructuredSelection)) {
            action.setEnabled(false);
            return;
        }

        IStructuredSelection sel = (IStructuredSelection) selection;
        if (sel.size() == 1) {
            Object obj = sel.getFirstElement();
            if (obj instanceof CodewindManager || obj instanceof CodewindConnection || obj instanceof CodewindApplication) {
            	codewindObject = obj;
            	action.setEnabled(true);
            	return;
            }
        }
        action.setEnabled(false);
    }

    @Override
    public void run(IAction action) {
    	if (codewindObject instanceof CodewindManager) {
    		Job job = new Job(Messages.RefreshCodewindJobLabel) {
    			@Override
    			protected IStatus run(IProgressMonitor monitor) {
    				IStatus status = ((CodewindManager)codewindObject).refresh(monitor);
		        	ViewHelper.refreshCodewindExplorerView(codewindObject);
		        	return status;
    			}
    		};
    		job.schedule();
    	} else if (codewindObject instanceof LocalConnection) {
    		final LocalConnection connection = (LocalConnection) codewindObject;
    		Job job = new Job(NLS.bind(Messages.RefreshConnectionJobLabel, connection.getName())) {
    			@Override
    			protected IStatus run(IProgressMonitor monitor) {
    				CodewindManager.getManager().refreshInstallStatus(monitor);
    				if (connection.isConnected()) {
    					connection.refreshApps(null);
    				}
		        	ViewHelper.refreshCodewindExplorerView(connection);
		        	return Status.OK_STATUS;
    			}
    		};
    		job.schedule();
    	} else if (codewindObject instanceof CodewindConnection) {
        	final CodewindConnection connection = (CodewindConnection) codewindObject;
        	Job job = new Job(NLS.bind(Messages.RefreshConnectionJobLabel, connection.getName())) {
    			@Override
    			protected IStatus run(IProgressMonitor monitor) {
		        	connection.refreshApps(null);
		        	ViewHelper.refreshCodewindExplorerView(connection);
		        	return Status.OK_STATUS;
    			}
    		};
    		job.schedule();
        } else if (codewindObject instanceof CodewindApplication) {
        	final CodewindApplication app = (CodewindApplication) codewindObject;
        	Job job = new Job(NLS.bind(Messages.RefreshProjectJobLabel, app.name)) {
    			@Override
    			protected IStatus run(IProgressMonitor monitor) {
    				app.connection.refreshApps(app.projectID);
    				ViewHelper.refreshCodewindExplorerView(app);
		        	return Status.OK_STATUS;
    			}
    		};
    		job.schedule();
        } else {
        	// Should not happen
        	Logger.logError("RefreshAction ran but no Codewind object was selected"); //$NON-NLS-1$
        }
    }

	@Override
	public void setActivePart(IAction arg0, IWorkbenchPart arg1) {
		// nothing
	}
}
