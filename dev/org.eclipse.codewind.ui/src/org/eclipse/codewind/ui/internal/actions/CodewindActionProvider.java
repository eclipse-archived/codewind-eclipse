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

import org.eclipse.codewind.core.internal.CodewindManager;
import org.eclipse.codewind.core.internal.InstallUtil.InstallStatus;
import org.eclipse.codewind.ui.internal.actions.InstallerAction.ActionType;
import org.eclipse.codewind.ui.internal.views.ViewHelper;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.actions.SelectionProviderAction;
import org.eclipse.ui.navigator.CommonActionProvider;
import org.eclipse.ui.navigator.ICommonActionConstants;
import org.eclipse.ui.navigator.ICommonActionExtensionSite;
import org.eclipse.ui.navigator.ICommonMenuConstants;

/**
 * Action provider for the Codewind view.
 */
public class CodewindActionProvider extends CommonActionProvider {
	
	private InstallerAction installUninstallAction;
	private InstallerAction startStopAction;
	private CodewindDoubleClickAction doubleClickAction;
	
    @Override
    public void init(ICommonActionExtensionSite aSite) {
        super.init(aSite);
        ISelectionProvider selProvider = aSite.getStructuredViewer();
        installUninstallAction = new InstallerAction(ActionType.INSTALL_UNINSTALL, selProvider);
        startStopAction = new InstallerAction(ActionType.START_STOP, selProvider);
        doubleClickAction = new CodewindDoubleClickAction(selProvider);
    }
    
    @Override
    public void fillContextMenu(IMenuManager menu) {
    	if (CodewindManager.getManager().getInstallerStatus() != null) {
    		// If the installer is active then the install actions should not be shown
    		return;
    	}
    	InstallStatus status = CodewindManager.getManager().getInstallStatus(false);
    	menu.appendToGroup(ICommonMenuConstants.GROUP_OPEN, installUninstallAction);
    	if (status == InstallStatus.RUNNING) {
    		String version = CodewindManager.getManager().getVersion();
    		if (version == null || CodewindManager.getManager().isSupportedVersion(version)) {
    			menu.appendToGroup(ICommonMenuConstants.GROUP_OPEN, startStopAction);
    		}
    	} else if (status.isInstalled()) {
	    	menu.appendToGroup(ICommonMenuConstants.GROUP_OPEN, startStopAction);
    	}
    }

	@Override
	public void fillActionBars(IActionBars actionBars) {
		super.fillActionBars(actionBars);
		actionBars.setGlobalActionHandler(ICommonActionConstants.OPEN, doubleClickAction);
	}

	private static class CodewindDoubleClickAction extends SelectionProviderAction {
		
		CodewindManager manager = null;
		
		public CodewindDoubleClickAction(ISelectionProvider selectionProvider) {
			super(selectionProvider, "");
			selectionChanged(getStructuredSelection());
		}

		@Override
		public void selectionChanged(IStructuredSelection sel) {
			if (sel.size() == 1) {
				Object obj = sel.getFirstElement();
				if (obj instanceof CodewindManager) {
					manager = (CodewindManager) obj;
					return;
				}
			}
			manager = null;
		}

		@Override
		public void run() {
			if (manager != null) {
				InstallStatus status = manager.getInstallStatus(false);
				switch(status) {
					case UNINSTALLED:
						CodewindInstall.installCodewind(null);
						break;
					case STOPPED:
						CodewindInstall.startCodewind(null);
						break;
					case RUNNING:
						String version = CodewindManager.getManager().getVersion();
						if (version == null) {
							// An error occurred so do nothing (the error is displayed to the user)
							break;
						}
						if (!CodewindManager.getManager().isSupportedVersion(version)) {
							CodewindInstall.promptAndUpdateCodewind();
							break;
						}
						ViewHelper.toggleExpansion(manager);
						break;
					default:
						// do nothing
						break;
				}
			}
		}
	}

}
