/*******************************************************************************
 * Copyright (c) 2018, 2020 IBM Corporation and others.
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
import org.eclipse.codewind.ui.internal.messages.Messages;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.tm.terminal.view.core.TerminalServiceFactory;
import org.eclipse.tm.terminal.view.core.interfaces.ITerminalService;
import org.eclipse.tm.terminal.view.core.interfaces.constants.ITerminalsConnectorConstants;
import org.eclipse.ui.actions.SelectionProviderAction;

/**
 * Action for opening a shell in the application container.
 */
public class ContainerShellAction extends SelectionProviderAction {
	
	private static final String TERMINAL_SERVICE_BUNDLE_ID = "org.eclipse.tm.terminal.view.core"; //$NON-NLS-1$
	private static final String LAUNCHER_BUNDLE_ID = "org.eclipse.tm.terminal.connector.local"; //$NON-NLS-1$
	private static final String LAUNCHER_DELEGATE_ID = "org.eclipse.tm.terminal.connector.local.launcher.local"; //$NON-NLS-1$
	
    protected CodewindApplication app;
    
    public ContainerShellAction(ISelectionProvider selectionProvider) {
        super(selectionProvider, Messages.ActionOpenContainerShell);
        selectionChanged(getStructuredSelection());
    }

    @Override
    public void selectionChanged(IStructuredSelection sel) {
        if (sel.size() == 1) {
            Object obj = sel.getFirstElement();
            if (obj instanceof CodewindApplication) {
            	app = (CodewindApplication)obj;
            	setEnabled(app.isAvailable() && app.getContainerId() != null);
            	return;
            }
        }
        setEnabled(false);
    }

    @Override
    public void run() {
        if (app == null) {
        	// should not be possible
        	Logger.logError("ContainerShellAction ran but no application was selected"); //$NON-NLS-1$
			return;
		}
        
        if (app.getContainerId() == null) {
        	Logger.logError("ContainerShellAction ran but the container id for the application is not set: " + app.name); //$NON-NLS-1$
			return;
        }
        
        // Check that the required bundles are installed and show a dialog if not
        if (Platform.getBundle(TERMINAL_SERVICE_BUNDLE_ID) == null || Platform.getBundle(LAUNCHER_BUNDLE_ID) == null) {
        	Logger.logError("The container shell cannot be opened because the required terminal service dependencies are not installed."); //$NON-NLS-1$
        	MessageDialog.openError(Display.getDefault().getActiveShell(), Messages.ActionOpenContainerShellMissingDepsTitle, Messages.ActionOpenContainerShellMissingDepsMsg);
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

    public boolean showAction() {
    	return app != null && app.connection.isLocal();
    }
}
