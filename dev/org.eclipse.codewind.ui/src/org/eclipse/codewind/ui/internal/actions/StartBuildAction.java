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

import org.eclipse.codewind.core.internal.CodewindApplication;
import org.eclipse.codewind.core.internal.CoreUtil;
import org.eclipse.codewind.core.internal.Logger;
import org.eclipse.codewind.core.internal.constants.BuildStatus;
import org.eclipse.codewind.core.internal.constants.CoreConstants;
import org.eclipse.codewind.ui.CodewindUIPlugin;
import org.eclipse.codewind.ui.internal.messages.Messages;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.actions.SelectionProviderAction;

/**
 * Action to start an application build.
 */
public class StartBuildAction extends SelectionProviderAction {

	protected CodewindApplication app;
	
	public StartBuildAction(ISelectionProvider selectionProvider) {
		super(selectionProvider, Messages.StartBuildActionLabel);
		setImageDescriptor(CodewindUIPlugin.getImageDescriptor(CodewindUIPlugin.BUILD_ICON));
		selectionChanged(getStructuredSelection());
	}

	@Override
	public void selectionChanged(IStructuredSelection sel) {
		if (sel.size() == 1) {
			Object obj = sel.getFirstElement();
			if (obj instanceof CodewindApplication) {
				app = (CodewindApplication) obj;
				if (app.isAvailable() && app.getBuildStatus() != BuildStatus.IN_PROGRESS && app.getBuildStatus() != BuildStatus.QUEUED) {
					setEnabled(true);
					return;
				}
			}
		}
		setEnabled(false);
	}

	@Override
	public void run() {
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
}
