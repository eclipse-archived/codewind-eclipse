/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
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
import org.eclipse.codewind.core.internal.cli.InstallUtil;
import org.eclipse.codewind.core.internal.connection.LocalConnection;
import org.eclipse.codewind.ui.internal.IDEUtil;
import org.eclipse.codewind.ui.internal.messages.Messages;
import org.eclipse.codewind.ui.internal.views.ViewHelper;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.actions.SelectionProviderAction;

public class LocalDoubleClickAction extends SelectionProviderAction {
	
	LocalConnection connection = null;
	
	public LocalDoubleClickAction(ISelectionProvider selectionProvider) {
		super(selectionProvider, "");
		selectionChanged(getStructuredSelection());
	}

	@Override
	public void selectionChanged(IStructuredSelection sel) {
		if (sel.size() == 1) {
			Object obj = sel.getFirstElement();
			if (obj instanceof LocalConnection) {
				connection = (LocalConnection) obj;
				return;
			}
		}
		connection = null;
	}

	@Override
	public void run() {
		performInstall(connection, true);
	}
	
	public static void performInstall(LocalConnection connection, boolean runFromView) {
		if (connection != null) {
			if (!runFromView) {
				ViewHelper.openCodewindExplorerView();
			}
			CodewindManager manager = CodewindManager.getManager();
			InstallStatus status = manager.getInstallStatus();
			if (status.isStarted()) {
				if (!runFromView) {
					IDEUtil.openInfoDialog(Messages.CodewindInstalledDialogTitle, Messages.CodewindInstalledDialogMsg);
				} else {
					ViewHelper.expandConnection(connection);
				}
			} else if (status.isInstalled()) {
				CodewindInstall.startCodewind(status.getVersion(), null);
			} else if (status.hasInstalledVersions()) {
				boolean result = IDEUtil.openConfirmDialog(Messages.UpdateCodewindDialogTitle, Messages.UpdateCodewindDialogMsg);
				if (result) {
					CodewindInstall.updateCodewind(InstallUtil.getVersion(), true, null);
				}
			} else if (status.isUnknown()) {
				// An error occurred so do nothing (the error is displayed to the user)
			} else {
				CodewindInstall.installCodewind(InstallUtil.getVersion(), null);
			}
		}
	}
}
