/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.codewind.ui.internal.actions;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.TimeoutException;

import org.eclipse.codewind.core.internal.CodewindManager;
import org.eclipse.codewind.core.internal.CodewindManager.InstallerStatus;
import org.eclipse.codewind.core.internal.CoreUtil;
import org.eclipse.codewind.core.internal.Logger;
import org.eclipse.codewind.core.internal.ProcessHelper.ProcessResult;
import org.eclipse.codewind.core.internal.cli.InstallStatus;
import org.eclipse.codewind.core.internal.cli.InstallUtil;
import org.eclipse.codewind.core.internal.connection.CodewindConnection;
import org.eclipse.codewind.core.internal.connection.CodewindConnectionManager;
import org.eclipse.codewind.ui.internal.messages.Messages;
import org.eclipse.codewind.ui.internal.views.ViewHelper;
import org.eclipse.codewind.ui.internal.wizards.BindProjectWizard;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
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
		CodewindConnection connection = null;

		if (project == null) {
			// Should not happen
			Logger.logError("BindProjectAction ran but no project was selected"); //$NON-NLS-1$
			return;
		}

		// If the installer is currently running, show a dialog to the user
		final InstallerStatus installerStatus = CodewindManager.getManager().getInstallerStatus();
		if (installerStatus != null) {
			Display.getDefault().asyncExec(new Runnable() {
				@Override
				public void run() {
					CodewindInstall.installerActiveDialog(installerStatus);
				}
			});
			return;
		}
		
		if (CodewindManager.getManager().getInstallStatus().isInstalled()) {
			connection = setupConnection();
		} else {
			CodewindInstall.codewindInstallerDialog(project);
			return;
		}

		if (connection == null || !connection.isConnected()) {
			CoreUtil.openDialog(true, Messages.BindProjectErrorTitle, Messages.BindProjectConnectionError);
			return;
		}
		
		String projectError = getProjectError(connection, project);
		if (projectError != null) {
			CoreUtil.openDialog(true, Messages.BindProjectErrorTitle, projectError);
			// If connection is new (not already registered), then close it
			if (CodewindConnectionManager.getActiveConnection(connection.getBaseURI().toString()) == null) {
				connection.disconnect();
			}
			return;
		}
		
		BindProjectWizard wizard = new BindProjectWizard(connection, project);
		WizardDialog dialog = new WizardDialog(Display.getDefault().getActiveShell(), wizard);
		if (dialog.open() == Window.CANCEL) {
			// If connection is new (not already registered), then close it
			if (CodewindConnectionManager.getActiveConnection(connection.getBaseURI().toString()) == null) {
				connection.disconnect();
			}
		} else {
			// Add the connection if not already registered
			if (CodewindConnectionManager.getActiveConnection(connection.getBaseURI().toString()) == null) {
				CodewindConnectionManager.add(connection);
			}
			ViewHelper.openCodewindExplorerView();
			ViewHelper.refreshCodewindExplorerView(null);
			ViewHelper.expandConnection(connection);
		}
	}
	
	private String getProjectError(CodewindConnection connection, IProject project) {
		if (connection.getAppByLocation(project.getLocation()) != null) {
			return NLS.bind(Messages.BindProjectAlreadyExistsError,  project.getName());
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
		final CodewindManager manager = CodewindManager.getManager();
		CodewindConnection connection = CodewindConnectionManager.getLocalConnection();
		if (connection != null && connection.isConnected()) {
			return connection;
		}
		InstallStatus status = manager.getInstallStatus();
		if (status.isStarted()) {
			if (!connection.isConnected()) {
				// This should not happen since the connection should be established as part of the start action
				Logger.logError("Local Codewind is started but the connection has not been established or is down");
				return null;
			}
			return connection;
		}
		if (!status.isInstalled()) {
			Logger.logError("In BindProjectAction run method and Codewind is not installed or has unknown status."); //$NON-NLS-1$
			return null;
		}
		
		IRunnableWithProgress runnable = new IRunnableWithProgress() {
			@Override
			public void run(IProgressMonitor monitor) throws InvocationTargetException {
				try {
					ProcessResult result = InstallUtil.startCodewind(status.getVersion(), monitor);
					if (result.getExitValue() != 0) {
						Logger.logError("Installer start failed with return code: " + result.getExitValue() + ", output: " + result.getOutput() + ", error: " + result.getError()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						String errorText = result.getError() != null && !result.getError().isEmpty() ? result.getError() : result.getOutput();
						throw new InvocationTargetException(null, "There was a problem trying to start Codewind: " + errorText); //$NON-NLS-1$
					}
					CodewindConnection connection = CodewindConnectionManager.getLocalConnection();
					ViewHelper.refreshCodewindExplorerView(connection);
				} catch (TimeoutException e) {
					throw new InvocationTargetException(e, "Codewind did not start in the expected time: " + e.getMessage()); //$NON-NLS-1$
				} catch (Exception e) {
					throw new InvocationTargetException(e, "An error occurred trying to start Codewind: " + e.getMessage()); //$NON-NLS-1$
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

		return CodewindConnectionManager.getLocalConnection();
	}
}
