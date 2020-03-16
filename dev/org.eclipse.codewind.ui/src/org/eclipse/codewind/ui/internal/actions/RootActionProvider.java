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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.navigator.CommonActionProvider;
import org.eclipse.ui.navigator.ICommonActionExtensionSite;
import org.eclipse.ui.navigator.ICommonMenuConstants;

/**
 * Action provider for the Codewind view.
 */
public class RootActionProvider extends CommonActionProvider {
	
	private OpenAppAction openAppAction;
	private RestartRunModeAction restartRunAction;
	private RestartDebugModeAction restartDebugAction;
	
    @Override
    public void init(ICommonActionExtensionSite aSite) {
        super.init(aSite);
        ISelectionProvider selProvider = aSite.getStructuredViewer();
        openAppAction = new OpenAppAction(selProvider);
        restartRunAction = new RestartRunModeAction(selProvider);
        restartDebugAction = new RestartDebugModeAction(selProvider);
    }

	@Override
	public void fillActionBars(IActionBars actionBars) {
		super.fillActionBars(actionBars);
		actionBars.setGlobalActionHandler(OpenAppAction.ACTION_ID, openAppAction);
		actionBars.setGlobalActionHandler(RestartRunModeAction.ACTION_ID, restartRunAction);
		actionBars.setGlobalActionHandler(RestartDebugModeAction.ACTION_ID, restartDebugAction);
		
		IContributionManager cm = actionBars.getToolBarManager();
		IContributionItem[] items = cm.getItems();
		List<IAction> existingActions = new ArrayList<IAction>();
		for (IContributionItem item : items) {
			if (item instanceof ActionContributionItem) {
				existingActions.add(((ActionContributionItem)item).getAction());
			}
		}
		if (!existingActions.contains(openAppAction)) {
			cm.appendToGroup(ICommonMenuConstants.GROUP_OPEN, openAppAction);
		}
		if (!existingActions.contains(restartRunAction)) {
			cm.appendToGroup(ICommonMenuConstants.GROUP_OPEN, restartRunAction);
		}
		if (!existingActions.contains(restartDebugAction)) {
			cm.appendToGroup(ICommonMenuConstants.GROUP_OPEN, restartDebugAction);
		}
	}

}
