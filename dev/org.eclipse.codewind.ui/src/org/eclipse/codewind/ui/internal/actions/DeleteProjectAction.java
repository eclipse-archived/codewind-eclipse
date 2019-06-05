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

import org.eclipse.codewind.core.internal.CodewindEclipseApplication;
import org.eclipse.codewind.core.internal.Logger;
import org.eclipse.codewind.core.internal.CoreUtil;
import org.eclipse.codewind.ui.CodewindUIPlugin;
import org.eclipse.codewind.ui.internal.messages.Messages;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.SelectionProviderAction;

/**
 * Action for deleting a Codewind project.  If a project exists in the
 * workspace with the same name and location as the Codewind project,
 * it will be deleted as well.
 */
public class DeleteProjectAction extends SelectionProviderAction {
	
	CodewindEclipseApplication app;
	
	public DeleteProjectAction(ISelectionProvider selectionProvider) {
		super(selectionProvider, Messages.DeleteProjectLabel);
		selectionChanged(getStructuredSelection());
		setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_ETOOL_DELETE));
	}

	@Override
	public void selectionChanged(IStructuredSelection sel) {
		if (sel.size() == 1) {
			Object obj = sel.getFirstElement();
			if (obj instanceof CodewindEclipseApplication) {
				app = (CodewindEclipseApplication) obj;
				setEnabled(true);
				return;
			}
		}
		setEnabled(false);
	}

	@Override
	public void run() {
		if (app == null) {
			// should not be possible
			Logger.logError("DeleteProjectAction ran but no application was selected"); //$NON-NLS-1$
			return;
		}

		if (CoreUtil.openConfirmDialog(Messages.DeleteProjectTitle, NLS.bind(Messages.DeleteProjectMessage, app.name))) {
			Job job = new Job(NLS.bind(Messages.DeleteProjectJobTitle, app.name)) {
    			@Override
    			protected IStatus run(IProgressMonitor monitor) {
    				try {
    					app.connection.requestProjectDelete(app.projectID);
    					IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(app.name);
    					if (project != null && project.exists() && project.getLocation().toFile().equals(app.fullLocalPath.toFile())) {
    						project.delete(false, true, monitor);
    					}
    					return Status.OK_STATUS;
    				} catch (Exception e) {
    					Logger.logError("An error occurred deleting the project: " + app.name + ", with id: " + app.projectID, e); //$NON-NLS-1$ //$NON-NLS-2$
    					return new Status(IStatus.ERROR, CodewindUIPlugin.PLUGIN_ID, NLS.bind(Messages.DeleteProjectErrorMsg, app.name), e);
    				}
    			}
    		};
    		job.schedule();
		}
	}
}
