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
        openAppDoubleClickAction = new OpenAppDoubleClickAction(selProvider);
    }
    
    @Override
    public void fillContextMenu(IMenuManager menu) {
    	selProvider.setSelection(selProvider.getSelection());
//    	if (validateAction.showAction()) {
//    		menu.appendToGroup(ICommonMenuConstants.GROUP_BUILD, validateAction);
//    	}
    	if (restartRunAction.showAction()) {
    		menu.appendToGroup(ICommonMenuConstants.GROUP_GENERATE, restartRunAction);
    	}
    	if (restartDebugAction.showAction()) {
    		menu.appendToGroup(ICommonMenuConstants.GROUP_GENERATE, restartDebugAction);
    	}
    	if (attachDebuggerAction.showAction()) {
    		menu.appendToGroup(ICommonMenuConstants.GROUP_GENERATE, attachDebuggerAction);
    	}
    	if (openAppMonitorAction.showAction()) {
    		menu.appendToGroup(ICommonMenuConstants.GROUP_OPEN, openAppMonitorAction);
    	}
    	if (openPerfMonitorAction.showAction()) {
    		menu.appendToGroup(ICommonMenuConstants.GROUP_OPEN, openPerfMonitorAction);
    	}
    	if (containerShellAction.showAction()) {
    		menu.appendToGroup(ICommonMenuConstants.GROUP_OPEN, containerShellAction);
    	}
    	menu.appendToGroup(ICommonMenuConstants.GROUP_BUILD, enableDisableAutoBuildAction);
    	if (enableDisableInjectMetricsAction.showAction()) {
    		menu.appendToGroup(ICommonMenuConstants.GROUP_BUILD, enableDisableInjectMetricsAction);
    	}
    	menu.appendToGroup(ICommonMenuConstants.GROUP_ADDITIONS, enableDisableProjectAction);
    	menu.appendToGroup(ICommonMenuConstants.GROUP_ADDITIONS, unbindProjectAction);
    	
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
			actionDelegate = new OpenAppAction();
			selectionChanged(getStructuredSelection());
		}

		@Override
		public void selectionChanged(IStructuredSelection sel) {
			actionDelegate.selectionChanged(this, sel);
		}

		@Override
		public void run() {
			actionDelegate.run(this);
		}
	}
}
