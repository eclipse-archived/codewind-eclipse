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

import org.eclipse.codewind.core.internal.CodewindEclipseApplication;
import org.eclipse.codewind.core.internal.Logger;
import org.eclipse.codewind.core.internal.console.CodewindConsoleFactory;
import org.eclipse.codewind.core.internal.console.ProjectLogInfo;
import org.eclipse.codewind.core.internal.console.SocketConsole;
import org.eclipse.codewind.ui.CodewindUIPlugin;
import org.eclipse.codewind.ui.internal.messages.Messages;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.navigator.ICommonViewerSite;

/**
 * Action for showing/hiding a log file for a Codewind application
 */
public class LogFileAction extends Action {
	
	private final ProjectLogInfo logInfo;
	private final CodewindEclipseApplication app;
	
	public LogFileAction(CodewindEclipseApplication app, ProjectLogInfo logInfo, ICommonViewerSite viewSite) {
		super(logInfo.logName, IAction.AS_CHECK_BOX);
    	this.logInfo = logInfo;
    	this.app = app;
    	setChecked(app.getConsole(logInfo) != null);
    }
	
    @Override
    public void run() {
        if (app == null) {
        	// should not be possible
        	Logger.logError("LogFileAction ran but no application was selected"); //$NON-NLS-1$
			return;
		}

        try {
			if (isChecked()) {
				Job job = new Job(NLS.bind(Messages.ShowLogFileJobLabel, new String[] {app.name, logInfo.logName})) {
					@Override
					protected IStatus run(IProgressMonitor monitor) {
						try {
							SocketConsole console = CodewindConsoleFactory.createLogFileConsole(app, logInfo);
							ConsolePlugin.getDefault().getConsoleManager().showConsoleView(console);
							app.addConsole(console);
							return Status.OK_STATUS;
						} catch (Exception e) {
							Logger.logError("An error occurred opening the " + logInfo.logName + " log file for: " + app.name + ", with id: " + app.projectID, e); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
							return new Status(IStatus.ERROR, CodewindUIPlugin.PLUGIN_ID, NLS.bind(Messages.ShowLogFileError, new String[] {logInfo.logName, app.name}), e);
						}
					}
				};
				job.schedule();
			} else {
				Job job = new Job(NLS.bind(Messages.HideLogFileJobLabel, new String[] {app.name, logInfo.logName})) {
					@Override
					protected IStatus run(IProgressMonitor monitor) {
						try {
							SocketConsole console = app.getConsole(logInfo);
							if (console != null) {
								IConsoleManager consoleManager = ConsolePlugin.getDefault().getConsoleManager();
								consoleManager.removeConsoles(new IConsole[] { console });
								app.removeConsole(console);
							}
							return Status.OK_STATUS;
						} catch (Exception e) {
							Logger.logError("An error occurred closing the " + logInfo.logName + " log file for: " + app.name + ", with id: " + app.projectID, e); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
							return new Status(IStatus.ERROR, CodewindUIPlugin.PLUGIN_ID, NLS.bind(Messages.HideLogFileError, new String[] {logInfo.logName, app.name}), e);
						}
					}
				};
				job.schedule();
			}
		} catch (Exception e) {
			// Ignore
		}
    }
}
