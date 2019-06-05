/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
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
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.concurrent.TimeoutException;

import org.eclipse.codewind.core.internal.InstallUtil;
import org.eclipse.codewind.core.internal.Logger;
import org.eclipse.codewind.core.internal.CoreUtil;
import org.eclipse.codewind.core.internal.PlatformUtil;
import org.eclipse.codewind.core.internal.ProcessHelper.ProcessResult;
import org.eclipse.codewind.core.internal.connection.CodewindConnection;
import org.eclipse.codewind.core.internal.connection.CodewindConnectionManager;
import org.eclipse.codewind.ui.internal.messages.Messages;
import org.eclipse.codewind.ui.internal.views.ViewHelper;
import org.eclipse.codewind.ui.internal.wizards.BindProjectWizard;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

public class BindProjectAction implements IObjectActionDelegate {
	
	private IWorkbenchPart part;
	private IProject project;

	@Override
	public void run(IAction action) {
		if (project == null) {
			// Should not happen
			Logger.logError("BindProjectAction ran but no project was selected"); //$NON-NLS-1$
			return;
		}
		
		CodewindConnection connection = setupConnection();
		if (connection == null || !connection.isConnected()) {
			CoreUtil.openDialog(true, Messages.BindProjectErrorTitle, Messages.BindProjectConnectionError);
			return;
		}
		
		String projectError = getProjectError(connection, project);
		if (projectError != null) {
			CoreUtil.openDialog(true, Messages.BindProjectErrorTitle, projectError);
			// If connection is new (not already registered), then close it
			if (CodewindConnectionManager.getActiveConnection(connection.baseUrl.toString()) == null) {
				connection.close();
			}
			return;
		}
		
		BindProjectWizard wizard = new BindProjectWizard(connection, project);
		WizardDialog dialog = new WizardDialog(Display.getDefault().getActiveShell(), wizard);
		if (dialog.open() == Window.CANCEL) {
			// If connection is new (not already registered), then close it
			if (CodewindConnectionManager.getActiveConnection(connection.baseUrl.toString()) == null) {
				connection.close();
			}
		} else {
			// Add the connection if not already registered
			if (CodewindConnectionManager.getActiveConnection(connection.baseUrl.toString()) == null) {
				CodewindConnectionManager.add(connection);
			}
			ViewHelper.openCodewindExplorerView();
			ViewHelper.refreshCodewindExplorerView(null);
			ViewHelper.expandConnection(connection);
		}
	}
	
	private String getProjectError(CodewindConnection connection, IProject project) {
		if (connection.getAppByName(project.getName()) != null) {
			return NLS.bind(Messages.BindProjectAlreadyExistsError,  project.getName());
		}
		IPath workspacePath = connection.getWorkspacePath();
		IPath projectPath = project.getLocation();
		if (PlatformUtil.getOS() == PlatformUtil.OperatingSystem.WINDOWS) {
			workspacePath = new Path(workspacePath.toPortableString().toLowerCase());
			projectPath = new Path(projectPath.toPortableString().toLowerCase());
		}
		if (!workspacePath.isPrefixOf(projectPath)) {
			return NLS.bind(Messages.BindProjectBadLocationError, project.getName(), connection.getWorkspacePath().toOSString());
		}
		return null;
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
        if (!(selection instanceof IStructuredSelection)) {
            action.setEnabled(false);
            return;
        }

        IStructuredSelection sel = (IStructuredSelection) selection;
        if (sel.size() == 1) {
            Object obj = sel.getFirstElement();
            if (obj instanceof IProject) {
            	project = (IProject)obj;
            	action.setEnabled(project.isAccessible());
            	return;
            }
        }
        
        action.setEnabled(false);
	}

	@Override
	public void setActivePart(IAction action, IWorkbenchPart part) {
		this.part = part;
	}
	
	
	private CodewindConnection setupConnection() {
		CodewindConnection connection = null;
		List<CodewindConnection> connections = CodewindConnectionManager.activeConnections();
		if (connections != null && !connections.isEmpty()) {
			connection = connections.get(0);
		} else {
			try {
				// Will throw an Exception if fails
				connection = CodewindConnectionManager.createConnection(CodewindConnectionManager.DEFAULT_CONNECTION_URL);
			} catch(Exception e) {
				Logger.log("Attempting to connect to Codewind failed: " + e.getMessage()); //$NON-NLS-1$
			}
		}
		if (connection != null && connection.isConnected()) {
			return connection;
		}
		
		IRunnableWithProgress runnable = new IRunnableWithProgress() {
			@Override
			public void run(IProgressMonitor monitor) throws InvocationTargetException {
				try {
					ProcessResult result = InstallUtil.startCodewind(monitor);
					if (result.getExitValue() != 0) {
						throw new InvocationTargetException(null, "There was a problem trying to start Codewind: " + result.getError()); //$NON-NLS-1$
					}
				} catch (IOException e) {
					throw new InvocationTargetException(e, "An error occurred trying to start Codewind: " + e.getMessage()); //$NON-NLS-1$
				} catch (TimeoutException e) {
					throw new InvocationTargetException(e, "Codewind did not start in the expected time: " + e.getMessage()); //$NON-NLS-1$
				}
			}
		};
		try {
			ProgressMonitorDialog dialog = new ProgressMonitorDialog(part.getSite().getShell());
			dialog.run(true, true, runnable);
		} catch (InvocationTargetException e) {
			Logger.logError("An error occurred trying to start Codewind", e); //$NON-NLS-1$
			return null;
		} catch (InterruptedException e) {
			Logger.logError("Codewind start was interrupted", e); //$NON-NLS-1$
			return null;
		}
		
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
			}
			if (!connection.isConnected()) {
				Logger.logError("The connection at " + connection.baseUrl + " is not active."); //$NON-NLS-1$ //$NON-NLS-2$
				return null;
			}
			return connection;
		}
		
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
		}
		if (connection == null) {
			Logger.logError("Failed to connect to Codewind at: " + CodewindConnectionManager.DEFAULT_CONNECTION_URL); //$NON-NLS-1$
		}
		return connection;
	}
}
