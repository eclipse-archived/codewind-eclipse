/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *	 IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.codewind.ui.internal.actions;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.codewind.core.internal.CodewindApplication;
import org.eclipse.codewind.core.internal.Logger;
import org.eclipse.codewind.core.internal.cli.ProjectLinks;
import org.eclipse.codewind.core.internal.cli.ProjectUtil;
import org.eclipse.codewind.core.internal.constants.AppStatus;
import org.eclipse.codewind.ui.internal.messages.Messages;
import org.eclipse.codewind.ui.internal.prefs.LinkManagementDialog;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.SelectionProviderAction;

/**
 * Action to create a new project.
 */
public class ManageLinksAction extends SelectionProviderAction {

	protected CodewindApplication app;
	
	public ManageLinksAction(ISelectionProvider selectionProvider) {
		super(selectionProvider, Messages.LinkMgmtActionLabel);
		selectionChanged(getStructuredSelection());
	}


	@Override
	public void selectionChanged(IStructuredSelection sel) {
		if (sel.size() == 1) {
			Object obj = sel.getFirstElement();
			if (obj instanceof CodewindApplication) {
				app = (CodewindApplication) obj;
				setEnabled(app.isAvailable() && app.getAppStatus() == AppStatus.STARTED);
				return;
			}
		}
		setEnabled(false);
	}

	@Override
	public void run() {
		if (app == null) {
			// should not be possible
			Logger.logError("ManageLinksAction ran but no Codewind application was selected"); //$NON-NLS-1$
			return;
		}
		openManageLinksDialog(app);
	}
	
	public static void openManageLinksDialog(CodewindApplication app) {
		try {
			LinkManagementDialog linkDialog = new LinkManagementDialog(Display.getDefault().getActiveShell(), app);
			if (linkDialog.open() == Window.OK && linkDialog.hasChanges()) {
				Job job = new Job(Messages.LinkUpdateTask) {
					@Override
					protected IStatus run(IProgressMonitor monitor) {
						return linkDialog.updateLinks(monitor);
					}
				};
				job.schedule();
			}
		} catch (Exception e) {
			MessageDialog.openError(Display.getDefault().getActiveShell(), Messages.LinkListErrorTitle, NLS.bind(Messages.LinkListErrorMsg, e));
		}
	}
}
