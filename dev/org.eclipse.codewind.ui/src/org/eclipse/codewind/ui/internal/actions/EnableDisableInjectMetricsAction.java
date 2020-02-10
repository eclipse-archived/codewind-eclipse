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
import org.eclipse.codewind.core.internal.CodewindEclipseApplication;
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
 * Action for enabling/disabling inject metrics on a Codewind project.
 */
public class EnableDisableInjectMetricsAction extends SelectionProviderAction {

    protected CodewindEclipseApplication app;
    
    public EnableDisableInjectMetricsAction(ISelectionProvider selectionProvider) {
		super(selectionProvider, Messages.EnableInjectMetricsLabel);
		selectionChanged(getStructuredSelection());
	}

    @Override
    public void selectionChanged(IStructuredSelection sel) {
        if (sel.size() == 1) {
            Object obj = sel.getFirstElement();
            if (obj instanceof CodewindEclipseApplication) {
            	app = (CodewindEclipseApplication)obj;
            	if (app.isAvailable() && app.canInjectMetrics()) {
	            	if (app.isMetricsInjected()) {
	                	setText(Messages.DisableInjectMetricsLabel);
	                } else {
	                	setText(Messages.EnableInjectMetricsLabel);
	                }
		            setEnabled(true);
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
        	Logger.logError(Messages.ErrorOnEnableDisableInjectMetrics + " ran but no application was selected"); //$NON-NLS-1$
			return;
		}
        
        enableDisableInjectMetrics(app, !app.isMetricsInjected());
    }
    
	public static void enableDisableInjectMetrics(CodewindApplication app, boolean enable) {
		Job job = new Job(NLS.bind(Messages.EnableDisableInjectMetricsJob, app.name)) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					app.connection.requestInjectMetrics(app.projectID, enable);
					app.connection.refreshApps(app.projectID);
					return Status.OK_STATUS;
				} catch (Exception e) {
					Logger.logError("An error occurred changing inject metric setting for: " + app.name + ", with id: " + app.projectID, e); //$NON-NLS-1$ //$NON-NLS-2$
					return new Status(IStatus.ERROR, CodewindUIPlugin.PLUGIN_ID, NLS.bind(Messages.ErrorOnEnableDisableInjectMetrics, app.name), e);
				}
			}
		};
		job.schedule();
	}
	
	public boolean showAction() {
		// Don't show the action if the app does not support inject metrics
    	return (app != null && app.canInjectMetrics());
	}

}
