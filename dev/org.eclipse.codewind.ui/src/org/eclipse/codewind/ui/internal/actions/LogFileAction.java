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

import org.eclipse.codewind.core.internal.CodewindEclipseApplication;
import org.eclipse.codewind.core.internal.Logger;
import org.eclipse.codewind.core.internal.console.CodewindConsoleFactory;
import org.eclipse.codewind.core.internal.console.ProjectLogInfo;
import org.eclipse.codewind.core.internal.console.SocketConsole;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.navigator.ICommonViewerSite;

/**
 * Action for showing a log file for a Codewind application
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

        if (isChecked()) {
    		SocketConsole console = CodewindConsoleFactory.createLogFileConsole(app, logInfo);
			ConsolePlugin.getDefault().getConsoleManager().showConsoleView(console);
			app.addConsole(console);
        } else {
        	SocketConsole console = app.getConsole(logInfo);
        	if (console != null) {
				IConsoleManager consoleManager = ConsolePlugin.getDefault().getConsoleManager();
				consoleManager.removeConsoles(new IConsole[] { console });
				app.removeConsole(console);
        	}
        }
    }
}
