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
import org.eclipse.codewind.ui.CodewindUIPlugin;
import org.eclipse.codewind.ui.internal.messages.Messages;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.actions.SelectionProviderAction;
import org.eclipse.ui.internal.wizards.datatransfer.SmartImportJob;

/**
 * Action for importing a Codewind project into Eclipse.  This makes
 * the source available for editing and debugging.
 */
@SuppressWarnings("restriction")
public class ImportProjectAction extends SelectionProviderAction {

	protected CodewindApplication app;
	
	public ImportProjectAction(ISelectionProvider selectionProvider) {
		super(selectionProvider, Messages.ImportProjectActionLabel);
		setImageDescriptor(CodewindUIPlugin.getImageDescriptor(CodewindUIPlugin.IMPORT_ICON));
		selectionChanged(getStructuredSelection());
	}

	@Override
	public void selectionChanged(IStructuredSelection sel) {
		if (sel.size() == 1) {
			Object obj = sel.getFirstElement();
			if (obj instanceof CodewindApplication) {
				app = (CodewindApplication) obj;
				if (app.isAvailable()) {
					IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(app.name);
					setEnabled(project == null || !project.exists());
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
			Logger.logError("ImportProjectAction ran but no application was selected"); //$NON-NLS-1$
			return;
		}

		importProject(app);
	}
	
	/**
	 * Import a Codewind project into Eclipse using Smart Import.
	 */
	public static void importProject(CodewindApplication app) {
		try {
			IPath path = app.fullLocalPath;
			SmartImportJob importJob = new SmartImportJob(path.toFile(), null, true, false);
			importJob.schedule();
		} catch (Exception e) {
			Logger.logError("Error importing project: " + app.name, e); //$NON-NLS-1$
			CoreUtil.openDialog(true, NLS.bind(Messages.ImportProjectError, app.name), e.getMessage());
			return;
		}
	}
}
