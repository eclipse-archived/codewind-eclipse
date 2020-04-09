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
import org.eclipse.codewind.core.internal.KubeUtil;
import org.eclipse.codewind.core.internal.Logger;
import org.eclipse.codewind.ui.internal.IDEUtil;
import org.eclipse.codewind.ui.internal.messages.Messages;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.tm.terminal.view.core.TerminalServiceFactory;
import org.eclipse.tm.terminal.view.core.interfaces.ITerminalService;
import org.eclipse.tm.terminal.view.core.interfaces.constants.ITerminalsConnectorConstants;
import org.eclipse.ui.actions.SelectionProviderAction;

/**
 * Action for opening a shell in the application container.
 */
public class ContainerShellAction extends SelectionProviderAction {
	
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
            	setEnabled(app.isAvailable() && checkApp(app));
            	return;
            }
        }
        setEnabled(false);
    }
    
	private boolean checkApp(CodewindApplication app) {
		if (app.connection.isLocal()) {
			return app.getContainerId() != null;
		}
		return app.getPodName() != null && app.getNamespace() != null;
	}
    
    @Override
    public void run() {
        if (app == null) {
        	// should not be possible
        	Logger.logError("ContainerShellAction ran but no application was selected"); //$NON-NLS-1$
			return;
		}
        
        if (!checkApp(app)) {
        	Logger.logError("ContainerShellAction ran but the container id or pod name and namespace for the application is not set: " + app.name); //$NON-NLS-1$
        	return;
        }
        
        // Check that the required bundles are installed and show a dialog if not
        if (!IDEUtil.canOpenTerminal()) {
        	Logger.logError("The container shell cannot be opened because the required terminal service dependencies are not installed."); //$NON-NLS-1$
        	MessageDialog.openError(Display.getDefault().getActiveShell(), Messages.ActionOpenContainerShellMissingDepsTitle, Messages.ActionOpenContainerShellMissingDepsMsg);
        	return;
        }
        
        // exec bash if it's installed, else exec sh
        String command = "sh -c \"if type bash > /dev/null; then bash; else sh; fi\"";

		// Open a shell in the application container
		String processPath = null;
		String processArgs = null;
		if (app.connection.isLocal()) {
			String envPath = CoreUtil.getEnvPath();
			processPath = envPath != null ? envPath + "docker" : "docker"; //$NON-NLS-1$ //$NON-NLS-2$
			processArgs = "exec -it " + app.getContainerId() + " " + command; //$NON-NLS-1$ //$NON-NLS-2$
		} else {
			processPath = KubeUtil.getCommand();
			if (processPath == null) {
				Logger.logError("The container shell cannot be opened because neither of the kubectl or oc commands could be found on the path");
				MessageDialog.openError(Display.getDefault().getActiveShell(),
						Messages.ActionOpenContainerShellErrorTitle, Messages.ErrorNoKubectlMsg);
				return;
			}
			processArgs = "exec -n " + app.getNamespace() + " -it " + app.getPodName() + " -- " + command; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
        String title = NLS.bind(Messages.ContainerShellTitle, app.name, app.connection.getName());
        Map<String, Object> properties = new HashMap<>();
        properties.put(ITerminalsConnectorConstants.PROP_DELEGATE_ID, IDEUtil.LAUNCHER_DELEGATE_ID);
        properties.put(ITerminalsConnectorConstants.PROP_SECONDARY_ID, title);
        properties.put(ITerminalsConnectorConstants.PROP_TITLE, title);
        properties.put(ITerminalsConnectorConstants.PROP_PROCESS_PATH, processPath);
        properties.put(ITerminalsConnectorConstants.PROP_PROCESS_ARGS, processArgs);
        ITerminalService terminal = TerminalServiceFactory.getService();
        if (terminal == null) {
            // This should not happen
            Logger.logError("ContainerShellAction ran but the terminal service is null"); //$NON-NLS-1$
            return;
        }
        terminal.openConsole(properties, null);
    }

    public boolean showAction() {
    	return app != null;
    }
}
