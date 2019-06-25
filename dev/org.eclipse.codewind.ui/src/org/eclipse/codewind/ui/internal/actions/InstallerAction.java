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
import org.eclipse.codewind.core.internal.InstallUtil.InstallStatus;
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
					switch (status) {
						case NOT_INSTALLED:
							setText(actionType.enableLabel);
							setEnabled(true);
							break;
						case INSTALLED:
						case RUNNING:
							setText(actionType.disableLabel);
							setEnabled(true);
							break;
						default:
							setText(actionType.enableLabel);
							setEnabled(false);
					}
					return;
				} else {
					switch (status) {
						case NOT_INSTALLED:
							this.setText(actionType.enableLabel);
							this.setEnabled(false);
							break;
						case INSTALLED:
							this.setText(actionType.enableLabel);
							this.setEnabled(true);
							break;
						case RUNNING:
							this.setText(actionType.disableLabel);
							this.setEnabled(true);
							break;
						default:
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
		if (actionType == ActionType.INSTALL_UNINSTALL) {
			if (actionType.enableLabel.equals(getText())) {
				CodewindInstall.installCodewind(null);
			} else {
				CodewindInstall.removeCodewind();
			}
		} else {
			if (actionType.enableLabel.equals(getText())) {
				CodewindInstall.startCodewind(null);
			} else {
				CodewindInstall.stopCodewind();
			}
		}
	}
}
