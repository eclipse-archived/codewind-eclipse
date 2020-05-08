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

import java.util.Optional;

import org.eclipse.codewind.core.CodewindCorePlugin;
import org.eclipse.codewind.core.internal.Logger;
import org.eclipse.codewind.core.internal.cli.OtherUtil;
import org.eclipse.codewind.core.internal.connection.CodewindConnection;
import org.eclipse.codewind.ui.CodewindUIPlugin;
import org.eclipse.codewind.ui.internal.IDEUtil;
import org.eclipse.codewind.ui.internal.messages.Messages;
import org.eclipse.codewind.ui.internal.wizards.DiagnosticsDialog;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.SelectionProviderAction;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.console.IConsoleView;

/**
 * Action for collecting Codewind diagnostics
 */
public class DiagnosticsAction extends SelectionProviderAction {
	
	private CodewindConnection conn;
	
	public DiagnosticsAction(ISelectionProvider selectionProvider) {
		super(selectionProvider, Messages.DiagnosticsActionLabel);
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
			Logger.logError("DiagnosticsAction ran but no connection was selected"); //$NON-NLS-1$
			return;
		}
		
		DiagnosticsDialog dialog = new DiagnosticsDialog(Display.getDefault().getActiveShell(), conn);
		if (dialog.open() == IStatus.OK) {
			Job job = new Job(NLS.bind(Messages.DiagnosticsActionJobLabel, conn.getName())) {
    			@Override
    			protected IStatus run(IProgressMonitor monitor) {
    				try {
    					SubMonitor mon = SubMonitor.convert(monitor, NLS.bind(Messages.DiagnosticsActionJobLabel, conn.getName()), 100);
    					ILaunch launch = OtherUtil.startDiagnostics(conn.getName(), conn.getConid(), dialog.includeEclipseWorkspace(), dialog.includeProjectInfo(), mon.split(70));
    					Optional<IConsole> consoleResult = IDEUtil.getConsoleForLaunch(launch);
    					while(!mon.isCanceled() && !launch.isTerminated() && !consoleResult.isPresent()) {
    						try {
								Thread.sleep(200);
							} catch (Exception e) {
								// Ignore
							}
    						mon.worked(10);
    						mon.setWorkRemaining(30);
    						consoleResult = IDEUtil.getConsoleForLaunch(launch);
    					}
    					if (mon.isCanceled()) {
    						return Status.CANCEL_STATUS;
    					}
    					if (consoleResult.isPresent()) {
    						final IConsole console = consoleResult.get();
    						Display.getDefault().asyncExec(() -> {
	    						IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
	    						IWorkbenchPage page = window == null ? null : window.getActivePage();
	    						if (page != null) {
	    							try {
	    								IConsoleView view = (IConsoleView) page.showView(IConsoleConstants.ID_CONSOLE_VIEW);
	    								view.display(console);
	    							} catch (Exception e) {
	    								Logger.logError("An error occurred trying to open the console view", e); //$NON-NLS-1$
	    							}
	    						}
    						});
    					} else {
    						Logger.logError("Could not find the console for launch: " + launch); //$NON-NLS-1$
    					}
    				} catch (Exception e) {
    					Logger.logError("An error occurred trying to generate diagnostics for connection: " + conn.getName(), e); //$NON-NLS-1$
    					return new Status(IStatus.ERROR, CodewindUIPlugin.PLUGIN_ID, NLS.bind(Messages.DiagnosticsErrorMsg, conn.getName()), e);
    				}
    				return Status.OK_STATUS;
    			}
    		};
    		job.schedule();
			
		}
	}

	public boolean showAction() {
		return conn != null && CodewindCorePlugin.getDefault().getPreferenceStore().getBoolean(CodewindCorePlugin.ENABLE_SUPPORT_FEATURES);
	}
}
