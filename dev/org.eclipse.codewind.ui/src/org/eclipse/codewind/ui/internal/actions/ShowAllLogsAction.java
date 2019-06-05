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
import org.eclipse.codewind.ui.internal.messages.Messages;
import org.eclipse.jface.action.Action;
import org.eclipse.ui.console.ConsolePlugin;

/**
 * Show all log files action
 */
public class ShowAllLogsAction extends Action {

    protected CodewindEclipseApplication app;
    
    public ShowAllLogsAction() {
    	super(Messages.ShowAllLogFilesAction);
    }

    public void setApp(CodewindEclipseApplication app) {
    	this.app = app;

    	// Only enable if there is a log file that does not currently have a console
    	boolean enabled = false;
    	if (app.getLogInfos() != null && !app.getLogInfos().isEmpty()) {
	    	for (ProjectLogInfo logInfo : app.getLogInfos()) {
	    		if (app.getConsole(logInfo) == null) {
	    			enabled = true;
	    			break;
	    		}
	    	}
    	}
    	setEnabled(enabled);
    }

    @Override
    public void run() {
        if (app == null) {
        	// should not be possible
        	Logger.logError("ShowAllLogsAction ran but no application was selected"); //$NON-NLS-1$
			return;
		}
        
        if (app.getLogInfos() == null || app.getLogInfos().isEmpty()) {
        	Logger.logError("ShowAllLogsAction ran but there are no logs for the selected application: " + app.name); //$NON-NLS-1$
        	return;
        }
        
        // Create consoles for any log files that don't have one yet
        for (ProjectLogInfo logInfo : app.getLogInfos()) {
    		if (app.getConsole(logInfo) == null) {
    			SocketConsole console = CodewindConsoleFactory.createLogFileConsole(app, logInfo);
    			ConsolePlugin.getDefault().getConsoleManager().showConsoleView(console);
    			app.addConsole(console);
    		}
    	}
    }
}
