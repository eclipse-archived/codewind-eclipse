/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *	 IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.codewind.ui.internal.actions;

import org.eclipse.codewind.core.internal.connection.RemoteConnection;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.navigator.CommonActionProvider;
import org.eclipse.ui.navigator.ICommonActionExtensionSite;
import org.eclipse.ui.navigator.ICommonMenuConstants;
import org.eclipse.ui.navigator.ICommonViewerSite;

/**
 * Action provider for a Codewind connection.
 */
public class RemoteConnectionActionProvider extends CommonActionProvider {
	
	private NewProjectAction newProjectAction;
	private BindAction bindAction;
	private ManageReposAction manageReposAction;
	private RemoveConnectionAction removeConnectionAction;
	
	@Override
	public void init(ICommonActionExtensionSite aSite) {
		super.init(aSite);
		ISelectionProvider selProvider = aSite.getStructuredViewer();
		newProjectAction = new NewProjectAction(selProvider);
		bindAction = new BindAction(selProvider);
		manageReposAction = new ManageReposAction(selProvider);
		removeConnectionAction = new RemoveConnectionAction(selProvider);
	}
	
	@Override
	public void fillContextMenu(IMenuManager menu) {
		final ICommonViewerSite viewSite = getActionSite().getViewSite();
		ISelection selection = viewSite.getSelectionProvider().getSelection();
		if (!(selection instanceof IStructuredSelection)) {
			return;
		}

		IStructuredSelection sel = (IStructuredSelection) selection;
		if (sel.size() == 1) {
			Object obj = sel.getFirstElement();
			if (obj instanceof RemoteConnection) {
				menu.appendToGroup(ICommonMenuConstants.GROUP_NEW, newProjectAction);
				menu.appendToGroup(ICommonMenuConstants.GROUP_NEW, bindAction);
				menu.appendToGroup(ICommonMenuConstants.GROUP_GENERATE, manageReposAction);
				menu.appendToGroup(ICommonMenuConstants.GROUP_ADDITIONS, removeConnectionAction);
			}
		}
	}

}
