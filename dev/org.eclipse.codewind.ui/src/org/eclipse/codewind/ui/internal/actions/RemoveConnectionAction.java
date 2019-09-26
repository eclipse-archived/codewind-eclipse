/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.codewind.ui.internal.actions;

import org.eclipse.codewind.core.internal.Logger;
import org.eclipse.codewind.core.internal.connection.CodewindConnection;
import org.eclipse.codewind.core.internal.connection.CodewindConnectionManager;
import org.eclipse.codewind.ui.CodewindUIPlugin;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.actions.SelectionProviderAction;

/**
 * Action to remove a Codewind connection.
 */
public class RemoveConnectionAction extends SelectionProviderAction {
	
	protected CodewindConnection connection;

	public RemoveConnectionAction(ISelectionProvider selectionProvider) {
		super(selectionProvider, "Remove");
		setImageDescriptor(CodewindUIPlugin.getDefaultIcon());
		selectionChanged(getStructuredSelection());
	}


	@Override
	public void selectionChanged(IStructuredSelection sel) {
		if (sel.size() == 1) {
			Object obj = sel.getFirstElement();
			if (obj instanceof CodewindConnection) {
				connection = (CodewindConnection) obj;
				setEnabled(connection != null && !connection.isLocal);
				return;
			}
		}
		setEnabled(false);
	}

	@Override
	public void run() {
		if (connection == null) {
			// should not be possible
			Logger.logError("RemoveConnectionAction ran but no connection was selected"); //$NON-NLS-1$
			return;
		}

		try {
			CodewindConnectionManager.removeConnection(connection.baseUrl.toString());
		} catch (Exception e) {
			Logger.logError("Error removing connection: " + connection.baseUrl.toString(), e); //$NON-NLS-1$
		}
	}
	
	public boolean showAction() {
		return connection != null && !connection.isLocal;
	}
}
