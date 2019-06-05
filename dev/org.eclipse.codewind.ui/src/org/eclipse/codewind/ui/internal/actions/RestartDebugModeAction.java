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
import org.eclipse.codewind.core.internal.Logger;
import org.eclipse.codewind.core.internal.CoreUtil;
import org.eclipse.codewind.core.internal.constants.AppState;
import org.eclipse.codewind.core.internal.constants.ProjectType;
import org.eclipse.codewind.core.internal.constants.StartMode;
import org.eclipse.codewind.ui.CodewindUIPlugin;
import org.eclipse.codewind.ui.internal.messages.Messages;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IActionDelegate2;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPart;

/**
 * Action to restart a Codewind application in debug mode.
 */
public class RestartDebugModeAction implements IObjectActionDelegate, IViewActionDelegate, IActionDelegate2 {

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
            	if (app.isAvailable() && app.supportsDebug()) {
		            action.setEnabled(app.getAppState() == AppState.STARTED || app.getAppState() == AppState.STARTING);
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
        	Logger.logError("RestartDebugModeAction ran but no application was selected"); //$NON-NLS-1$
			return;
		}
        
        // Check for a project for Java applications only since currently this is the only
        // language that can be debugged within Eclipse
        if (app.projectType.isLanguage(ProjectType.LANGUAGE_JAVA)) {
	        IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(app.name);
	        // Check if the project has been imported into Eclipse. If not, offer to import it.
	        if (project == null || !project.exists()) {
	        	int result = openDialog(NLS.bind(Messages.ProjectNotImportedDialogTitle, app.name), NLS.bind(Messages.ProjectNotImportedDialogMsg, app.name));
	        	if (result == 0) {
	        		// Import the project
	        		ImportProjectAction.importProject(app);
	        	} else if (result == 2) {
	        		// Cancel selected
	        		return;
	        	}
	        // Check if the project is open in Eclipse. If not, offer to open it.
	        } else if (!project.isOpen()) {
	        	int result = openDialog(NLS.bind(Messages.ProjectClosedDialogTitle, app.name), NLS.bind(Messages.ProjectClosedDialogMsg, app.name));
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
    
    /*
     * Dialog which asks the user a question and they can select Yes, No
     * or Cancel.
     * Returns:
     *  0 - user selected Yes
     *  1 - user selected No
     *  2 - user selected Cancel
     */
    private static int openDialog(String title, String msg) {
    	final int[] result = new int[1];
		Display.getDefault().syncExec(new Runnable() {
			@Override
			public void run() {
				Shell shell = Display.getDefault().getActiveShell();
				String[] buttonLabels = new String[] {Messages.DialogYesButton, Messages.DialogNoButton, Messages.DialogCancelButton};
				MessageDialog dialog = new MessageDialog(shell, title, CodewindUIPlugin.getImage(CodewindUIPlugin.CODEWIND_ICON),
						msg, MessageDialog.QUESTION, buttonLabels, 0);
				result[0] = dialog.open();
			}
		});
		
		return result[0];
	}
    
	@Override
	public void runWithEvent(IAction action, Event event) {
		run(action);
	}

	@Override
	public void setActivePart(IAction arg0, IWorkbenchPart arg1) {
		// nothing
	}

	@Override
	public void dispose() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void init(IAction arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void init(IViewPart arg0) {
		// TODO Auto-generated method stub
		
	}
}
