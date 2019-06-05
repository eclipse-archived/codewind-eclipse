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
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

/**
 * Action to remove a Codewind connection.
 */
public class RemoveConnectionAction implements IObjectActionDelegate {

	protected CodewindConnection connection;

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		if (!(selection instanceof IStructuredSelection)) {
			action.setEnabled(false);
			return;
		}

		IStructuredSelection sel = (IStructuredSelection) selection;
		if (sel.size() == 1) {
			Object obj = sel.getFirstElement();
			if (obj instanceof CodewindConnection) {
				connection = (CodewindConnection) obj;
				action.setEnabled(true);
				return;
			}
		}
		action.setEnabled(false);
	}

	@Override
	public void run(IAction action) {
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

	@Override
	public void setActivePart(IAction arg0, IWorkbenchPart arg1) {
		// nothing
	}
}
