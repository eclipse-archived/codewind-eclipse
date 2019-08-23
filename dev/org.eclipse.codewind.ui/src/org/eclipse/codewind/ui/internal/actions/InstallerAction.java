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

import org.eclipse.codewind.core.internal.CodewindManager;
import org.eclipse.codewind.core.internal.InstallStatus;
import org.eclipse.codewind.core.internal.InstallUtil;
import org.eclipse.codewind.ui.internal.IDEUtil;
import org.eclipse.codewind.ui.internal.messages.Messages;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.actions.SelectionProviderAction;

/**
 * Action to create a new project.
 */
public class InstallerAction extends SelectionProviderAction {
	
	public final ActionType actionType;

	public enum ActionType {
		INSTALL_UNINSTALL(Messages.InstallerActionInstallLabel, Messages.InstallerActionUninstallLabel),
		START_STOP(Messages.InstallerActionStartLabel, Messages.InstallerActionStopLabel);
		
		public final String enableLabel;
		public final String disableLabel;
		
		private ActionType(String onLabel, String offLabel) {
			this.enableLabel = onLabel;
			this.disableLabel = offLabel;
		}
	}
	
	public InstallerAction(ActionType actionType, ISelectionProvider selectionProvider) {
		super(selectionProvider, actionType.disableLabel);
		this.actionType = actionType;
		selectionChanged(getStructuredSelection());
	}

	@Override
	public void selectionChanged(IStructuredSelection sel) {
		if (sel.size() == 1) {
			Object obj = sel.getFirstElement();
			if (obj instanceof CodewindManager) {
				InstallStatus status = CodewindManager.getManager().getInstallStatus(false);
				if (actionType == ActionType.INSTALL_UNINSTALL) {
					if (status.isInstalled()) {
						setText(actionType.disableLabel);
						setEnabled(true);
					} else if (status.hasInstalledVersions()) {
						setText(Messages.InstallerActionUpdateLabel);
						setEnabled(true);
					} else if (status.isUnknown()) {
						setText(actionType.enableLabel);
						setEnabled(false);
					} else {
						setText(actionType.enableLabel);
						setEnabled(true);
					}
					return;
				} else {
					if (status.isStarted()) {
						this.setText(actionType.disableLabel);
						this.setEnabled(true);
					} else if (status.isInstalled()) {
						this.setText(actionType.enableLabel);
						this.setEnabled(true);
					} else if (status.isUnknown()) {
						this.setText(actionType.enableLabel);
						this.setEnabled(false);
					} else {
						this.setText(actionType.enableLabel);
						this.setEnabled(false);
					}
					return;
				}
			}
		}
		setEnabled(false);
	}

	@Override
	public void run() {
		InstallStatus status = CodewindManager.getManager().getInstallStatus(false);
		if (actionType == ActionType.INSTALL_UNINSTALL) {
			if (Messages.InstallerActionUpdateLabel.equals(getText())) {
				boolean result = IDEUtil.openConfirmDialog(Messages.UpdateCodewindDialogTitle, Messages.UpdateCodewindDialogMsg);
				if (result) {
					CodewindInstall.updateCodewind(InstallUtil.getVersion(), true, null);
				}
			} else if (actionType.enableLabel.equals(getText())) {
				CodewindInstall.installCodewind(InstallUtil.getVersion(), null);
			} else {
				CodewindInstall.removeCodewind(status.getVersion());
			}
		} else {
			if (actionType.enableLabel.equals(getText())) {
				CodewindInstall.startCodewind(status.getVersion(), null);
			} else {
				CodewindInstall.stopCodewind();
			}
		}
	}
}
