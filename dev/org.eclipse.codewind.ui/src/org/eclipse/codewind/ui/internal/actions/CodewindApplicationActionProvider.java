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
	
//	private ValidateAction validateAction;
	private AttachDebuggerAction attachDebuggerAction;
	private OpenAppMonitorAction openAppMonitorAction;
	private OpenPerfMonitorAction openPerfMonitorAction;
	private UnbindProjectAction unbindProjectAction;
	private OpenAppDoubleClickAction openAppDoubleClickAction;
	
    @Override
    public void init(ICommonActionExtensionSite aSite) {
        super.init(aSite);
        ISelectionProvider selProvider = aSite.getStructuredViewer();
//        validateAction = new ValidateAction(selProvider);
        attachDebuggerAction = new AttachDebuggerAction(selProvider);
        openAppMonitorAction = new OpenAppMonitorAction(selProvider);
        openPerfMonitorAction = new OpenPerfMonitorAction(selProvider);
        unbindProjectAction = new UnbindProjectAction(selProvider);
        openAppDoubleClickAction = new OpenAppDoubleClickAction(selProvider);
    }
    
    @Override
    public void fillContextMenu(IMenuManager menu) {
//    	if (validateAction.showAction()) {
//    		menu.appendToGroup(ICommonMenuConstants.GROUP_BUILD, validateAction);
//    	}
    	if (attachDebuggerAction.showAction()) {
    		menu.appendToGroup(ICommonMenuConstants.GROUP_GENERATE, attachDebuggerAction);
    	}
    	if (openAppMonitorAction.showAction()) {
    		menu.appendToGroup(ICommonMenuConstants.GROUP_OPEN, openAppMonitorAction);
    	}
    	if (openPerfMonitorAction.showAction()) {
    		menu.appendToGroup(ICommonMenuConstants.GROUP_OPEN, openPerfMonitorAction);
    	}
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
