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
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;

/**
 * Abstract base action for toggling the display of Codewind logs.
 */
public abstract class ToggleConsoleAction extends Action {

    protected CodewindEclipseApplication app;
    
    public ToggleConsoleAction(String label) {
    	super(label, IAction.AS_CHECK_BOX);
    }

    public void setApp(CodewindEclipseApplication app) {
        this.app = app;

    	if (app.isAvailable() && consoleSupported()) {
        	setChecked(hasConsole());
        	setEnabled(true);
    	} else {
	        setChecked(false);
	        setEnabled(false);
    	}
    }

    @Override
    public void run() {
        if (app == null) {
        	// should not be possible
        	Logger.logError("ToggleConsolesAction ran but no application was selected"); //$NON-NLS-1$
			return;
		}

        if (isChecked()) {
        	IConsole console = createConsole();
        	ConsolePlugin.getDefault().getConsoleManager().showConsoleView(console);
        	setConsole(console);
        } else {
        	IConsole console = getConsole();
        	if (console != null) {
	        	IConsoleManager consoleManager = ConsolePlugin.getDefault().getConsoleManager();
	        	consoleManager.removeConsoles(new IConsole[] { console });
	        	setConsole(null);
        	}
        }
    }

	public abstract boolean consoleSupported();
	protected abstract IConsole createConsole();
	protected abstract void setConsole(IConsole console);
	protected abstract boolean hasConsole();
	protected abstract IConsole getConsole();
}
