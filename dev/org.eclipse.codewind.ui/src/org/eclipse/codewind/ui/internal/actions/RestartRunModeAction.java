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
import org.eclipse.codewind.core.internal.cli.ProjectUtil;
import org.eclipse.codewind.core.internal.constants.AppStatus;
import org.eclipse.codewind.core.internal.constants.StartMode;
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
 * Action to restart a Codewind application in run mode.
 */
public class RestartRunModeAction extends SelectionProviderAction {
	
	public static final String ACTION_ID = "org.eclipse.codewind.ui.restartRunModeAction";

    protected CodewindEclipseApplication app;
    
    public RestartRunModeAction(ISelectionProvider selectionProvider) {
        super(selectionProvider, Messages.RestartInRunMode);
        setImageDescriptor(CodewindUIPlugin.getImageDescriptor(CodewindUIPlugin.LAUNCH_RUN_ICON));
        selectionChanged(getStructuredSelection());
    }

    @Override
    public void selectionChanged(IStructuredSelection sel) {
        if (sel.size() == 1) {
            Object obj = sel.getFirstElement();
            if (obj instanceof CodewindEclipseApplication) {
            	app = (CodewindEclipseApplication)obj;
            	if (app.isAvailable() && app.getProjectCapabilities().canRestart()) {
		            setEnabled(app.getAppStatus() == AppStatus.STARTED || app.getAppStatus() == AppStatus.STARTING);
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
        	Logger.logError("RestartRunModeAction ran but no application was selected"); //$NON-NLS-1$
			return;
		}

		// Clear out any old launch and debug target
		app.clearDebugger();

		Job job = new Job(NLS.bind(Messages.RestartInRunModeTask, app.name)) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					// Restart the project in run mode
					ProjectUtil.restartProject(app.name, app.projectID, StartMode.RUN.startMode, app.connection.getConid(), monitor);
					return Status.OK_STATUS;
				} catch (Exception e) {
					Logger.logError("Error initiating restart for project: " + app.name, e); //$NON-NLS-1$
					return new Status(IStatus.ERROR, CodewindUIPlugin.PLUGIN_ID, NLS.bind(Messages.ErrorOnRestartMsg, app.name), e);
				}
			}
		};
		job.schedule();
    }
    
    public boolean showAction() {
    	// Don't show the action if the app does not support restart
    	return (app != null && app.isAvailable() && app.getProjectCapabilities().canRestart());
    }
}
