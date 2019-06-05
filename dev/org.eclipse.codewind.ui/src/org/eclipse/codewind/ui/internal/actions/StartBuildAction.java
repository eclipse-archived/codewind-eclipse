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
import org.eclipse.codewind.core.internal.CoreUtil;
import org.eclipse.codewind.core.internal.CodewindApplication;
import org.eclipse.codewind.core.internal.constants.BuildStatus;
import org.eclipse.codewind.core.internal.constants.CoreConstants;
import org.eclipse.codewind.ui.internal.messages.Messages;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

/**
 * Action to start an application build.
 */
public class StartBuildAction implements IObjectActionDelegate {

	protected CodewindApplication app;

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		if (!(selection instanceof IStructuredSelection)) {
			action.setEnabled(false);
			return;
		}

		IStructuredSelection sel = (IStructuredSelection) selection;
		if (sel.size() == 1) {
			Object obj = sel.getFirstElement();
			if (obj instanceof CodewindApplication) {
				app = (CodewindApplication) obj;
				if (app.isAvailable() && app.getBuildStatus() != BuildStatus.IN_PROGRESS && app.getBuildStatus() != BuildStatus.QUEUED) {
					action.setEnabled(true);
					return;
				}
			}
		}
		action.setEnabled(false);
	}

	@Override
	public void run(IAction action) {
		if (app == null) {
			// should not be possible
			Logger.logError("StartBuildAction ran but no application was selected"); //$NON-NLS-1$
			return;
		}

		try {
			app.connection.requestProjectBuild(app, CoreConstants.VALUE_ACTION_BUILD);
		} catch (Exception e) {
			Logger.logError("Error requesting build for application: " + app.name, e); //$NON-NLS-1$
			CoreUtil.openDialog(true, NLS.bind(Messages.StartBuildError, app.name), e.getMessage());
			return;
		}
	}

	@Override
	public void setActivePart(IAction arg0, IWorkbenchPart arg1) {
		// nothing
	}
}
