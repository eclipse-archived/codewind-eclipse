/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *	 IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.codewind.ui.internal.actions;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.codewind.core.CodewindCorePlugin;
import org.eclipse.codewind.core.internal.Logger;
import org.eclipse.codewind.core.internal.cli.LogLevels;
import org.eclipse.codewind.core.internal.cli.OtherUtil;
import org.eclipse.codewind.core.internal.connection.CodewindConnection;
import org.eclipse.codewind.ui.CodewindUIPlugin;
import org.eclipse.codewind.ui.internal.messages.Messages;
import org.eclipse.codewind.ui.internal.wizards.LogLevelSelectionDialog;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.SelectionProviderAction;

/**
 * Action for setting the Codewind server log level
 */
public class LogLevelAction extends SelectionProviderAction {
	
	private CodewindConnection conn;
	private LogLevels logLevels;
	
	public LogLevelAction(ISelectionProvider selectionProvider) {
		super(selectionProvider, Messages.LogLevelAction);
		selectionChanged(getStructuredSelection());
	}

	@Override
	public void selectionChanged(IStructuredSelection sel) {
		if (sel.size() == 1) {
			Object obj = sel.getFirstElement();
			if (obj instanceof CodewindConnection) {
				conn = (CodewindConnection)obj;
				setEnabled(conn.isConnected());
				return;
			}
		}
		setEnabled(false);
	}

	@Override
	public void run() {
		if (conn == null) {
			// should not be possible
			Logger.logError("LogLevelAction ran but no connection was selected"); //$NON-NLS-1$
			return;
		}
		
		IRunnableWithProgress runnable = new IRunnableWithProgress() {
			@Override
			public void run(IProgressMonitor monitor) throws InvocationTargetException {
				try {
					SubMonitor mon = SubMonitor.convert(monitor, NLS.bind(Messages.LogLevelsFetchTaskLabel, conn.getName()), 100);
					logLevels = OtherUtil.getLoglevels(conn.getName(), conn.getConid(), mon.split(100));
				} catch (Exception e) {
					throw new InvocationTargetException(e, "An error occurred trying to fetch the server log levels for connection: " + conn.getName()); //$NON-NLS-1$
				}
			}
		};
		
		try {
			PlatformUI.getWorkbench().getProgressService().busyCursorWhile(runnable);
		} catch (InvocationTargetException e) {
			Throwable cause = ((InvocationTargetException)e).getCause();
			Logger.logError("An error occurred getting the log levels for connection: " + conn.getName(), e); //$NON-NLS-1$
			MessageDialog.openError(Display.getDefault().getActiveShell(), Messages.LogLevelsFetchErrorTitle, NLS.bind(Messages.LogLevelsFetchError, new String[] {conn.getName(), cause.getLocalizedMessage()}));
			return;
		} catch (InterruptedException e) {
			// The user cancelled the operation
			return;
		}
		
		LogLevelSelectionDialog dialog = new LogLevelSelectionDialog(Display.getDefault().getActiveShell(), logLevels);
		if (dialog.open() == IStatus.OK) {
			String level = dialog.getSelectedLevel();
			if (!logLevels.getCurrentLevel().equals(level)) {
				Job job = new Job(NLS.bind(Messages.LogLevelSetJobLabel, new String[] {conn.getName(), level})) {
					@Override
					protected IStatus run(IProgressMonitor monitor) {
						try {
							OtherUtil.setLoglevels(conn.getName(), level, conn.getConid(), monitor);
						} catch (Exception e) {
							Logger.logError("An error occurred updating the log level for the " + conn.getName() + " connection to: " + level, e); //$NON-NLS-1$ //$NON-NLS-2$
							return new Status(IStatus.ERROR, CodewindUIPlugin.PLUGIN_ID, NLS.bind(Messages.LogLevelSetError, new String[] {level, conn.getName()}), e);
						}
						return Status.OK_STATUS;
					}
				};
				job.schedule();
			}
		}
	}

	public boolean showAction() {
		return CodewindCorePlugin.getDefault().getPreferenceStore().getBoolean(CodewindCorePlugin.ENABLE_SUPPORT_FEATURES);
	}
}
