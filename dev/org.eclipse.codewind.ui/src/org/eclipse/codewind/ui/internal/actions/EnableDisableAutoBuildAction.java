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
import org.eclipse.codewind.core.internal.constants.CoreConstants;
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
 * Action for enabling/disabling auto build on a Codewind project.
 */
public class EnableDisableAutoBuildAction extends SelectionProviderAction {

    protected CodewindEclipseApplication app;
    
	public EnableDisableAutoBuildAction(ISelectionProvider selectionProvider) {
		super(selectionProvider, Messages.DisableAutoBuildLabel);
		selectionChanged(getStructuredSelection());
	}

    @Override
    public void selectionChanged(IStructuredSelection sel) {
        if (sel.size() == 1) {
            Object obj = sel.getFirstElement();
            if (obj instanceof CodewindEclipseApplication) {
            	app = (CodewindEclipseApplication)obj;
            	if (app.isAvailable()) {
	            	if (app.isAutoBuild()) {
	                	setText(Messages.DisableAutoBuildLabel);
	                } else {
	                	setText(Messages.EnableAutoBuildLabel);
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
        	Logger.logError("EnableDisableAutoBuildAction ran but no application was selected"); //$NON-NLS-1$
			return;
		}
        
        enableDisableAutoBuild(app, !app.isAutoBuild());
    }
    
	public static void enableDisableAutoBuild(CodewindApplication app, boolean enable) {
		Job job = new Job(NLS.bind(Messages.EnableDisableAutoBuildJob, app.name)) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					String actionKey = enable ? CoreConstants.VALUE_ACTION_ENABLEAUTOBUILD : CoreConstants.VALUE_ACTION_DISABLEAUTOBUILD;
					app.connection.requestProjectBuild(app, actionKey);
					app.setAutoBuild(enable);
					return Status.OK_STATUS;
				} catch (Exception e) {
					Logger.logError("An error occurred changing auto build setting for: " + app.name + ", with id: " + app.projectID, e); //$NON-NLS-1$ //$NON-NLS-2$
					return new Status(IStatus.ERROR, CodewindUIPlugin.PLUGIN_ID, NLS.bind(Messages.ErrorOnEnableDisableAutoBuild, app.name), e);
				}
			}
		};
		job.schedule();
	}
}
