/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
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

import org.eclipse.codewind.core.internal.Logger;
import org.eclipse.codewind.core.internal.CoreUtil;
import org.eclipse.codewind.core.internal.CodewindApplication;
import org.eclipse.codewind.core.internal.constants.ProjectType;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.tm.terminal.view.core.interfaces.constants.ITerminalsConnectorConstants;
import org.eclipse.tm.terminal.view.ui.interfaces.ILauncherDelegate;
import org.eclipse.tm.terminal.view.ui.launcher.LauncherDelegateManager;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

/**
 * Action for opening a shell in the application container.
 */
public class ContainerShellAction implements IObjectActionDelegate {
	
	private static final String LAUNCHER_DELEGATE_ID = "org.eclipse.tm.terminal.connector.local.launcher.local"; //$NON-NLS-1$
	
    protected CodewindApplication app;
    protected ILauncherDelegate delegate;
    
    public ContainerShellAction() {
    	delegate = LauncherDelegateManager.getInstance().getLauncherDelegate(LAUNCHER_DELEGATE_ID, false);
    	if (delegate == null) {
    		Logger.logError("Could not get the local terminal launcher delegate."); //$NON-NLS-1$
    	}
    }

    @Override
    public void selectionChanged(IAction action, ISelection selection) {
        if (delegate == null || !(selection instanceof IStructuredSelection)) {
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
        
        if (delegate == null) {
        	// should not be possible
        	Logger.logError("ContainerShellAction ran but the local terminal laucher delegate is null"); //$NON-NLS-1$
			return;
		}
        
        // The command varies depending on the application type
        String command = "bash"; //$NON-NLS-1$
        if (app.projectType.isType(ProjectType.TYPE_DOCKER) && app.projectType.isLanguage(ProjectType.LANGUAGE_PYTHON)) {
        	command = "sh"; //$NON-NLS-1$
        }

        // Open a shell in the application container
        String envPath = CoreUtil.getEnvPath();
        String dockerPath = envPath != null ? envPath + "docker" : "docker"; //$NON-NLS-1$  //$NON-NLS-2$
        Map<String, Object> properties = new HashMap<>();
        properties.put(ITerminalsConnectorConstants.PROP_DELEGATE_ID, delegate.getId());
        properties.put(ITerminalsConnectorConstants.PROP_TITLE, app.name);
        properties.put(ITerminalsConnectorConstants.PROP_PROCESS_PATH, dockerPath);
        properties.put(ITerminalsConnectorConstants.PROP_PROCESS_ARGS, "exec -it " + app.getContainerId() + " " + command); //$NON-NLS-1$ //$NON-NLS-2$
        delegate.execute(properties, null);
    }

	@Override
	public void setActivePart(IAction arg0, IWorkbenchPart arg1) {
		// nothing
	}
}
