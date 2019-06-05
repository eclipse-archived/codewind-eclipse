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

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeoutException;

import org.eclipse.codewind.core.internal.InstallUtil;
import org.eclipse.codewind.core.internal.Logger;
import org.eclipse.codewind.core.internal.CoreUtil;
import org.eclipse.codewind.core.internal.ProcessHelper.ProcessResult;
import org.eclipse.codewind.core.internal.connection.CodewindConnection;
import org.eclipse.codewind.core.internal.connection.CodewindConnectionManager;
import org.eclipse.codewind.ui.CodewindUIPlugin;
import org.eclipse.codewind.ui.internal.messages.Messages;
import org.eclipse.codewind.ui.internal.views.ViewHelper;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IActionDelegate2;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;

/**
 * Action to create a new Codewind connection. This action is used in several places
 * including:
 *    The File > New menu
 *    Popup menu in the Codewind view
 *    Toolbar action in the Codewind view
 *
 */
public class CreateConnectionAction extends Action implements IViewActionDelegate, IActionDelegate2 {
	
	public CreateConnectionAction(Shell shell) {
        super(Messages.ActionNewConnection);
        setImageDescriptor(CodewindUIPlugin.getImageDescriptor(CodewindUIPlugin.CODEWIND_ICON));
    }

    public CreateConnectionAction() {
        // Intentionally empty
    }

	
	@Override
	public void run(IAction arg0) {
//		run();
		setupConnection();
	}

	@Override
	public void run() {
//		Wizard wizard = new NewCodewindConnectionWizard();
//		WizardLauncher.launchWizardWithoutSelection(wizard);
		List<CodewindConnection> connections = CodewindConnectionManager.activeConnections();
		if (connections != null && !connections.isEmpty() && connections.get(0).isConnected()) {
			CoreUtil.openDialog(false, Messages.ConnectionErrorTitle, Messages.ConnectionAlreadyExistsError);
			return;
		}
		setupConnection();
	}

	@Override
	public void selectionChanged(IAction arg0, ISelection arg1) {
		// Intentionally empty
	}

	@Override
	public void dispose() {
		// TODO Auto-generated method stub

	}

	@Override
	public void init(IAction arg0) {
		// Intentionally empty
	}

	@Override
	public void runWithEvent(IAction arg0, Event arg1) {
		run();
	}

	@Override
	public void init(IViewPart arg0) {
		// Intentionally empty
	}
	
    public static void setupConnection() {
		List<CodewindConnection> connections = CodewindConnectionManager.activeConnections();
		CodewindConnection conn = null;
		if (connections != null && !connections.isEmpty()) {
			conn = connections.get(0);
			if (conn.isConnected()) {
				return;
			}
		}
		final CodewindConnection existingConnection = conn;
		
    	Job job = new Job(Messages.ConnectingJobLabel) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				SubMonitor mon = SubMonitor.convert(monitor, 100);
				mon.setTaskName(Messages.DetectingConnectionTask);
				CodewindConnection connection = existingConnection;
				if (connection == null) {
					// Try to create a connection
					try {
						connection = CodewindConnectionManager.createConnection(CodewindConnectionManager.DEFAULT_CONNECTION_URL);
					} catch(Exception e) {
						Logger.log("Attempting to connect to Codewind failed: " + e.getMessage()); //$NON-NLS-1$
					}
				}
				mon.worked(5);
				
				if (connection == null || !connection.isConnected()) {
					// Try to start Codewind
					try {
						ProcessResult result = InstallUtil.startCodewind(mon.split(75));
						if (result.getExitValue() != 0) {
							return new Status(IStatus.ERROR, CodewindUIPlugin.PLUGIN_ID, NLS.bind(Messages.StartCodewindErrorWithMsg, result.getError()));
						}
					} catch (IOException e) {
						return new Status(IStatus.ERROR, CodewindUIPlugin.PLUGIN_ID, Messages.StartCodewindError, e);
					} catch (TimeoutException e) {
						return new Status(IStatus.ERROR, CodewindUIPlugin.PLUGIN_ID, Messages.StartCodewindTimeout, e);
					}
				}
				if (mon.isCanceled()) {
					return Status.CANCEL_STATUS;
				}
				
				mon.setTaskName(Messages.ConnectingTask);
				// If there was a connection, check to see if it is connected to Codewind now
				if (connection != null) {
					for (int i = 0; i < 10; i++) {
						if (connection.isConnected()) {
							break;
						}
						try {
							Thread.sleep(500);
						} catch (InterruptedException e1) {
							// Ignore
						}
						if (mon.isCanceled()) {
							return Status.CANCEL_STATUS;
						}
						mon.worked(1);
					}
					if (!connection.isConnected()) {
						if (connection != existingConnection) {
							connection.close();
						}
						Logger.logError("The connection at " + connection.baseUrl + " is not active."); //$NON-NLS-1$ //$NON-NLS-2$
						return new Status(IStatus.ERROR, CodewindUIPlugin.PLUGIN_ID, Messages.StartCodewindNotActive);
					}
				} else {
					// If there was no connection, try to create one
					for (int i = 0; i < 10; i++) {
						try {
							connection = CodewindConnectionManager.createConnection(CodewindConnectionManager.DEFAULT_CONNECTION_URL);
							break;
						} catch (Exception e) {
							try {
								Thread.sleep(500);
							} catch (InterruptedException e1) {
								// Ignore
							}
						}
						if (mon.isCanceled()) {
							return Status.CANCEL_STATUS;
						}
						mon.worked(1);
					}
					if (connection == null) {
						return new Status(IStatus.ERROR, CodewindUIPlugin.PLUGIN_ID, Messages.StartCodewindConnectionError);
					}
				}
				
				if (CodewindConnectionManager.getActiveConnection(connection.baseUrl.toString()) == null) {
					CodewindConnectionManager.add(connection);
				}
				ViewHelper.refreshCodewindExplorerView(null);
				ViewHelper.expandConnection(connection);
				
				return Status.OK_STATUS;
		    }
    	};
    	job.schedule();
    }

}
