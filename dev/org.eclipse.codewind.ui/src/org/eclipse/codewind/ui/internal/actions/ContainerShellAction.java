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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.codewind.core.internal.CodewindApplication;
import org.eclipse.codewind.core.internal.CoreUtil;
import org.eclipse.codewind.core.internal.Logger;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.tm.terminal.view.core.TerminalServiceFactory;
import org.eclipse.tm.terminal.view.core.interfaces.ITerminalService;
import org.eclipse.tm.terminal.view.core.interfaces.constants.ITerminalsConnectorConstants;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

/**
 * Action for opening a shell in the application container.
 */
public class ContainerShellAction implements IObjectActionDelegate {
	
	private static final String LAUNCHER_DELEGATE_ID = "org.eclipse.tm.terminal.connector.local.launcher.local"; //$NON-NLS-1$
	
    protected CodewindApplication app;

    @Override
    public void selectionChanged(IAction action, ISelection selection) {
        if (!(selection instanceof IStructuredSelection)) {
            action.setEnabled(false);
            return;
        }

        IStructuredSelection sel = (IStructuredSelection) selection;
        if (sel.size() == 1) {
            Object obj = sel.getFirstElement();
            if (obj instanceof CodewindApplication) {
            	app = (CodewindApplication)obj;
            	action.setEnabled(app.isAvailable() && app.getContainerId() != null);
            	return;
            }
        }
        action.setEnabled(false);
    }

    @Override
    public void run(IAction action) {
        if (app == null) {
        	// should not be possible
        	Logger.logError("ContainerShellAction ran but no application was selected"); //$NON-NLS-1$
			return;
		}
        
        if (app.getContainerId() == null) {
        	Logger.logError("ContainerShellAction ran but the container id for the application is not set: " + app.name); //$NON-NLS-1$
			return;
        }
        
        // exec bash if it's installed, else exec sh
        String command = "sh -c \"if type bash > /dev/null; then bash; else sh; fi\"";

        // Open a shell in the application container
        String envPath = CoreUtil.getEnvPath();
        String dockerPath = envPath != null ? envPath + "docker" : "docker"; //$NON-NLS-1$  //$NON-NLS-2$
        Map<String, Object> properties = new HashMap<>();
        properties.put(ITerminalsConnectorConstants.PROP_DELEGATE_ID, LAUNCHER_DELEGATE_ID);
        properties.put(ITerminalsConnectorConstants.PROP_SECONDARY_ID, app.name);
        properties.put(ITerminalsConnectorConstants.PROP_TITLE, app.name);
        properties.put(ITerminalsConnectorConstants.PROP_PROCESS_PATH, dockerPath);
        properties.put(ITerminalsConnectorConstants.PROP_PROCESS_ARGS, "exec -it " + app.getContainerId() + " " + command); //$NON-NLS-1$ //$NON-NLS-2$
        ITerminalService terminal = TerminalServiceFactory.getService();
        if (terminal == null) {
            // This should not happen
            Logger.logError("ContainerShellAction ran but the terminal service is null"); //$NON-NLS-1$
            return;
        }
        terminal.openConsole(properties, null);
    }

	@Override
	public void setActivePart(IAction arg0, IWorkbenchPart arg1) {
		// nothing
	}
}
