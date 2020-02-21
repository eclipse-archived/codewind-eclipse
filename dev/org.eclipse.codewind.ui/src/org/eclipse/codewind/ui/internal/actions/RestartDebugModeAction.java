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

import org.eclipse.codewind.core.internal.CodewindEclipseApplication;
import org.eclipse.codewind.core.internal.CoreUtil;
import org.eclipse.codewind.core.internal.Logger;
import org.eclipse.codewind.core.internal.constants.AppStatus;
import org.eclipse.codewind.core.internal.constants.StartMode;
import org.eclipse.codewind.ui.CodewindUIPlugin;
import org.eclipse.codewind.ui.internal.IDEUtil;
import org.eclipse.codewind.ui.internal.messages.Messages;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.actions.SelectionProviderAction;

/**
 * Action to restart a Codewind application in debug mode.
 */
public class RestartDebugModeAction extends SelectionProviderAction {
	
	public static final String ACTION_ID = "org.eclipse.codewind.ui.restartDebugModeAction";

    protected CodewindEclipseApplication app;
    
    public RestartDebugModeAction(ISelectionProvider selectionProvider) {
        super(selectionProvider, Messages.RestartInDebugMode);
        setImageDescriptor(CodewindUIPlugin.getImageDescriptor(CodewindUIPlugin.LAUNCH_DEBUG_ICON));
        selectionChanged(getStructuredSelection());
    }

    @Override
    public void selectionChanged(IStructuredSelection sel) {
        if (sel.size() == 1) {
            Object obj = sel.getFirstElement();
            if (obj instanceof CodewindEclipseApplication) {
            	app = (CodewindEclipseApplication)obj;
            	if (app.isAvailable() && app.supportsDebug()) {
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
        	Logger.logError("RestartDebugModeAction ran but no application was selected"); //$NON-NLS-1$
			return;
		}
        
        // Check for a project for Java applications only since currently this is the only
        // language that can be debugged within Eclipse
        if (app.projectLanguage.isJava()) {
	        IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(app.name);
	        // Check if the project has been imported into Eclipse. If not, offer to import it.
	        if (project == null || !project.exists()) {
	        	int result = IDEUtil.openQuestionCancelDialog(NLS.bind(Messages.ProjectNotImportedDialogTitle, app.name), NLS.bind(Messages.ProjectNotImportedDialogMsg, app.name));
	        	if (result == 0) {
	        		// Import the project
	        		ImportProjectAction.importProject(app);
	        	} else if (result == 2) {
	        		// Cancel selected
	        		return;
	        	}
	        // Check if the project is open in Eclipse. If not, offer to open it.
	        } else if (!project.isOpen()) {
	        	int result = IDEUtil.openQuestionCancelDialog(NLS.bind(Messages.ProjectClosedDialogTitle, app.name), NLS.bind(Messages.ProjectClosedDialogMsg, app.name));
	        	if (result == 0) {
	        		// Open the project
	        		Job job = new Job(NLS.bind(Messages.ProjectOpenJob, app.name)) {
	        			@Override
	        			protected IStatus run(IProgressMonitor monitor) {
	        				try {
	        					project.open(monitor);
	        					return Status.OK_STATUS;
	        				} catch (CoreException e) {
	        					return new Status(IStatus.ERROR, CodewindUIPlugin.PLUGIN_ID,
	        							NLS.bind(Messages.ProjectOpenError, app.name), e);
	        				}
	        			}
	        		};
	        		job.setPriority(Job.LONG);
	        		job.schedule();
	        	} else if (result == 2) {
	        		// Cancel selected
	        		return;
	        	}
	        }
        } else if (!app.canInitiateDebugSession()) {
        	// If can't attach or help launch a debugger, inform the user and allow them to cancel the operation
        	MessageDialogWithToggle noDebugSessionQuestion = MessageDialogWithToggle.openOkCancelConfirm(
					Display.getDefault().getActiveShell(), NLS.bind(Messages.NoDebugSetupDialogTitle, app.name),
					Messages.NoDebugSetupDialogMsg,
					Messages.NoDebugSetupDialogToggle, true, null, null);
			if (noDebugSessionQuestion.getReturnCode() == IDialogConstants.CANCEL_ID) {
				return;
			}
			// If the user requested it, notify them when the debug port becomes available
			app.setDebugPortNotify(noDebugSessionQuestion.getToggleState());
        }

        try {
        	// Clear out any old launch and debug target
        	app.clearDebugger();
        	
        	// Restart the project in debug mode. The debugger will be attached when the restart result
        	// event is received from Codewind.
        	// Try debug mode first since it allows debug of initialization.  If not supported use
        	// debugNoInit mode.
        	if (app.getProjectCapabilities().supportsDebugMode()) {
        		app.connection.requestProjectRestart(app, StartMode.DEBUG.startMode);
        	} else if (app.getProjectCapabilities().supportsDebugNoInitMode()) {
        		app.connection.requestProjectRestart(app, StartMode.DEBUG_NO_INIT.startMode);
        	} else {
        		// Should never get here
        		Logger.logError("Project restart in debug mode requested but project does not support any debug modes: " + app.name); //$NON-NLS-1$
        	}
		} catch (Exception e) {
			Logger.logError("Error initiating restart for project: " + app.name, e); //$NON-NLS-1$
			CoreUtil.openDialog(true, Messages.ErrorOnRestartDialogTitle, e.getMessage());
			return;
		}
    }
    
    public boolean showAction() {
    	// Don't show the action if the app does not support debug
    	return (app != null && app.connection.isLocal() && app.isAvailable() && app.supportsDebug());
    }
}
