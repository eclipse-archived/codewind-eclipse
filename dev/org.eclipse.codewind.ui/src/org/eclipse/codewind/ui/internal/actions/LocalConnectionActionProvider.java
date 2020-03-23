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

import org.eclipse.codewind.core.internal.CodewindManager;
import org.eclipse.codewind.core.internal.cli.InstallStatus;
import org.eclipse.codewind.ui.internal.actions.InstallerAction.ActionType;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.navigator.CommonActionProvider;
import org.eclipse.ui.navigator.ICommonActionConstants;
import org.eclipse.ui.navigator.ICommonActionExtensionSite;

/**
 * Action provider for the Codewind view.
 */
public class LocalConnectionActionProvider extends CommonActionProvider {
	
	private ISelectionProvider selProvider;
	private InstallerAction installUninstallAction;
	private InstallerAction startStopAction;
	private NewProjectAction newProjectAction;
	private BindAction bindAction;
	private ManageRegistriesAction manageRegistriesAction;
	private ManageReposAction manageReposAction;
	private LocalDoubleClickAction doubleClickAction;
	private LogLevelAction logLevelAction;
	
    @Override
    public void init(ICommonActionExtensionSite aSite) {
        super.init(aSite);
        selProvider = aSite.getStructuredViewer();
        newProjectAction = new NewProjectAction(selProvider);
        bindAction = new BindAction(selProvider);
        manageRegistriesAction = new ManageRegistriesAction(selProvider);
        manageReposAction = new ManageReposAction(selProvider);
        installUninstallAction = new InstallerAction(ActionType.INSTALL_UNINSTALL, selProvider);
        startStopAction = new InstallerAction(ActionType.START_STOP, selProvider);
        doubleClickAction = new LocalDoubleClickAction(selProvider);
        logLevelAction = new LogLevelAction(selProvider);
    }
    
	@Override
	public void fillContextMenu(IMenuManager menu) {
		if (CodewindManager.getManager().getInstallerStatus() != null) {
			// If the installer is active then the install actions should not be shown
			return;
		}
		selProvider.setSelection(selProvider.getSelection());
		menu.add(newProjectAction);
		menu.add(bindAction);
		menu.add(new Separator());
		menu.add(manageRegistriesAction);
		menu.add(manageReposAction);
		menu.add(new Separator());
		InstallStatus status = CodewindManager.getManager().getInstallStatus();
		menu.add(installUninstallAction);
		if (status.isInstalled()) {
			menu.add(startStopAction);
		}
		if (logLevelAction.showAction()) {
			menu.add(new Separator());
			menu.add(logLevelAction);
		}
	}

	@Override
	public void fillActionBars(IActionBars actionBars) {
		super.fillActionBars(actionBars);
		actionBars.setGlobalActionHandler(ICommonActionConstants.OPEN, doubleClickAction);
	}
}
