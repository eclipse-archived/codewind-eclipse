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

import org.eclipse.codewind.core.internal.CodewindEclipseApplication;
import org.eclipse.codewind.core.internal.console.ProjectLogInfo;
import org.eclipse.codewind.ui.internal.messages.Messages;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.actions.SelectionProviderAction;
import org.eclipse.ui.navigator.CommonActionProvider;
import org.eclipse.ui.navigator.ICommonActionConstants;
import org.eclipse.ui.navigator.ICommonActionExtensionSite;
import org.eclipse.ui.navigator.ICommonViewerSite;

/**
 * Action provider for Codewind applications in the Codewind view.
 */
public class CodewindApplicationActionProvider extends CommonActionProvider {
	
	private ISelectionProvider selProvider;
//	private ValidateAction validateAction;
	private RestartRunModeAction restartRunAction;
	private RestartDebugModeAction restartDebugAction;
	private AttachDebuggerAction attachDebuggerAction;
	private OpenAppMonitorAction openAppMonitorAction;
	private OpenPerfMonitorAction openPerfMonitorAction;
	private ContainerShellAction containerShellAction;
	private EnableDisableAutoBuildAction enableDisableAutoBuildAction;
	private EnableDisableInjectMetricsAction enableDisableInjectMetricsAction;
	private EnableDisableProjectAction enableDisableProjectAction;
	private UnbindProjectAction unbindProjectAction;
	private OpenAppAction openAppAction;
	private OpenAppOverviewAction openAppOverviewAction;
	private ImportProjectAction importProjectAction;
	private ShowAllLogsAction showAllLogsAction;
	private HideAllLogsAction hideAllLogsAction;
	private StartBuildAction startBuildAction;
	private OpenAppDoubleClickAction openAppDoubleClickAction;
	
    @Override
    public void init(ICommonActionExtensionSite aSite) {
        super.init(aSite);
        selProvider = aSite.getStructuredViewer();
//        validateAction = new ValidateAction(selProvider);
        restartRunAction = new RestartRunModeAction(selProvider);
        restartDebugAction = new RestartDebugModeAction(selProvider);
        attachDebuggerAction = new AttachDebuggerAction(selProvider);
        openAppMonitorAction = new OpenAppMonitorAction(selProvider);
        openPerfMonitorAction = new OpenPerfMonitorAction(selProvider);
        containerShellAction = new ContainerShellAction(selProvider);
        enableDisableAutoBuildAction = new EnableDisableAutoBuildAction(selProvider);
        enableDisableInjectMetricsAction = new EnableDisableInjectMetricsAction(selProvider);
        enableDisableProjectAction = new EnableDisableProjectAction(selProvider);
        unbindProjectAction = new UnbindProjectAction(selProvider);
        openAppAction = new OpenAppAction(selProvider);
        openAppOverviewAction = new OpenAppOverviewAction(selProvider);
        showAllLogsAction = new ShowAllLogsAction();
        hideAllLogsAction = new HideAllLogsAction();
        importProjectAction = new ImportProjectAction(selProvider);
        startBuildAction = new StartBuildAction(selProvider);
        openAppDoubleClickAction = new OpenAppDoubleClickAction(selProvider);
    }
    
	@Override
	public void fillContextMenu(IMenuManager menu) {
		selProvider.setSelection(selProvider.getSelection());

		menu.add(openAppAction);
		menu.add(openAppOverviewAction);
		if (containerShellAction.showAction()) {
			menu.add(containerShellAction);
		}
		menu.add(new Separator());

		int numItems = menu.getItems().length;
		if (openAppMonitorAction.showAction()) {
			menu.add(openAppMonitorAction);
		}
		if (openPerfMonitorAction.showAction()) {
			menu.add(openPerfMonitorAction);
		}
		if (enableDisableInjectMetricsAction.showAction()) {
			menu.add(enableDisableInjectMetricsAction);
		}
		if (menu.getItems().length > numItems) {
			menu.add(new Separator());
		}

		menu.add(importProjectAction);
		addLogFileSubMenu(menu);
		menu.add(new Separator());

		numItems = menu.getItems().length;
		if (restartRunAction.showAction()) {
			menu.add(restartRunAction);
		}
		if (restartDebugAction.showAction()) {
			menu.add(restartDebugAction);
		}
		if (attachDebuggerAction.showAction()) {
			menu.add(attachDebuggerAction);
		}
		if (menu.getItems().length > numItems) {
			menu.add(new Separator());
		}

		menu.add(startBuildAction);
//		if (validateAction.showAction()) {
//			menu.add(validateAction);
//		}
		menu.add(enableDisableAutoBuildAction);
		menu.add(new Separator());

		menu.add(enableDisableProjectAction);
		menu.add(unbindProjectAction);
	}

	private void addLogFileSubMenu(IMenuManager menu) {
		final ICommonViewerSite viewSite = getActionSite().getViewSite();
		ISelection selection = viewSite.getSelectionProvider().getSelection();
		if (!(selection instanceof IStructuredSelection)) {
			return;
		}

		IStructuredSelection sel = (IStructuredSelection) selection;
		if (sel.size() == 1) {
			Object obj = sel.getFirstElement();
			if (obj instanceof CodewindEclipseApplication) {
				final CodewindEclipseApplication app = (CodewindEclipseApplication) obj;
				if (app.isAvailable() && app.getLogInfos() != null && !app.getLogInfos().isEmpty()) {
					MenuManager menuMgr = new MenuManager(Messages.ShowLogFilesMenu, "ShowLogFiles");
					showAllLogsAction.setApp(app);
					menuMgr.add(showAllLogsAction);
					hideAllLogsAction.setApp(app);
					menuMgr.add(hideAllLogsAction);
					menuMgr.add(new Separator());
					for (ProjectLogInfo logInfo : app.getLogInfos()) {
						menuMgr.add(new LogFileAction(app, logInfo, viewSite));
					}
					menu.add(menuMgr);
				}
			}
		}
	}

	@Override
	public void fillActionBars(IActionBars actionBars) {
		super.fillActionBars(actionBars);
		actionBars.setGlobalActionHandler(ICommonActionConstants.OPEN, openAppDoubleClickAction);
	}

	private static class OpenAppDoubleClickAction extends SelectionProviderAction {
		private final OpenAppAction actionDelegate;

		public OpenAppDoubleClickAction(ISelectionProvider selectionProvider) {
			super(selectionProvider, "");
			actionDelegate = new OpenAppAction(selectionProvider);
			selectionChanged(getStructuredSelection());
		}

		@Override
		public void selectionChanged(IStructuredSelection sel) {
			actionDelegate.selectionChanged(sel);
		}

		@Override
		public void run() {
			actionDelegate.run();
		}
	}
}
