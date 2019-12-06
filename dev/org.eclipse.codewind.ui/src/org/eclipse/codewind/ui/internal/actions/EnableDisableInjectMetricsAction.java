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

package org.eclipse.codewind.ui.internal.actions;

import org.eclipse.codewind.core.internal.CodewindApplication;
import org.eclipse.codewind.core.internal.CodewindEclipseApplication;
import org.eclipse.codewind.core.internal.Logger;
import org.eclipse.codewind.ui.CodewindUIPlugin;
import org.eclipse.codewind.ui.internal.messages.Messages;
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
 * Action for enabling/disabling inject metrics on a Codewind project.
 */
public class EnableDisableInjectMetricsAction implements IObjectActionDelegate {

    protected CodewindEclipseApplication app;

    @Override
    public void selectionChanged(IAction action, ISelection selection) {
        if (!(selection instanceof IStructuredSelection)) {
            action.setEnabled(false);
            return;
        }

        IStructuredSelection sel = (IStructuredSelection) selection;
        if (sel.size() == 1) {
            Object obj = sel.getFirstElement();
            if (obj instanceof CodewindEclipseApplication) {
            	app = (CodewindEclipseApplication)obj;
            	if (app.isAvailable() && app.canInjectMetrics()) {
	            	if (app.isInjectMetrics()) {
	                	action.setText(Messages.DisableInjectMetricsLabel);
	                } else {
	                	action.setText(Messages.EnableInjectMetricsLabel);
	                }
		            action.setEnabled(true);
	            	return;
            	}
            }
        }
        action.setEnabled(false);
    }

    @Override
    public void run(IAction action) {
        if (app == null) {
        	// should not be possible
        	Logger.logError(Messages.ErrorOnEnableDisableInjectMetrics + " ran but no application was selected"); //$NON-NLS-1$
			return;
		}
        
        enableDisableInjectMetrics(app, !app.isInjectMetrics());
    }
    
	public static void enableDisableInjectMetrics(CodewindApplication app, boolean enable) {
		Job job = new Job(NLS.bind(Messages.EnableDisableInjectMetricsJob, app.name)) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					app.connection.requestInjectMetrics(app.projectID, enable);
					app.setInjectMetrics(enable);
					return Status.OK_STATUS;
				} catch (Exception e) {
					Logger.logError("An error occurred changing inject metric setting for: " + app.name + ", with id: " + app.projectID, e); //$NON-NLS-1$ //$NON-NLS-2$
					return new Status(IStatus.ERROR, CodewindUIPlugin.PLUGIN_ID, NLS.bind(Messages.ErrorOnEnableDisableInjectMetrics, app.name), e);
				}
			}
		};
		job.schedule();
	}

	@Override
	public void setActivePart(IAction arg0, IWorkbenchPart arg1) {
		// nothing
	}
}
