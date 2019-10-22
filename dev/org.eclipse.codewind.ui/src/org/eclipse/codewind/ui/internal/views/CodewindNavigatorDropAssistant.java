/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.codewind.ui.internal.views;

import org.eclipse.codewind.core.internal.CodewindApplication;
import org.eclipse.codewind.core.internal.Logger;
import org.eclipse.codewind.core.internal.connection.CodewindConnection;
import org.eclipse.codewind.ui.CodewindUIPlugin;
import org.eclipse.codewind.ui.internal.messages.Messages;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.ui.navigator.CommonDropAdapter;
import org.eclipse.ui.navigator.CommonDropAdapterAssistant;

public class CodewindNavigatorDropAssistant extends CommonDropAdapterAssistant {
	
	private CodewindConnection targetConn;
	private CodewindApplication sourceApp;
	
	@Override
	public IStatus validateDrop(Object target, int operation, TransferData transferData) {
		targetConn = null;
		sourceApp = null;
		if (!(target instanceof CodewindConnection) || !((CodewindConnection)target).isConnected()) {
			return Status.CANCEL_STATUS;
		}
		targetConn = (CodewindConnection)target;
		
		// Check that the source is an application that is not already added to this connection
		if (LocalSelectionTransfer.getTransfer().isSupportedType(transferData)) {
			ISelection s = LocalSelectionTransfer.getTransfer().getSelection();
			if (!s.isEmpty() && s instanceof IStructuredSelection) {
				IStructuredSelection sel = (IStructuredSelection) s;
				Object obj = sel.getFirstElement();
				if (obj instanceof CodewindApplication) {
					CodewindApplication app = targetConn.getAppByName(((CodewindApplication)obj).name);
					if (app != null) {
						return Status.CANCEL_STATUS;
					}
					sourceApp = (CodewindApplication)obj;
					return Status.OK_STATUS;
				}
			}
		}
		return Status.CANCEL_STATUS;
	}

	@Override
	public IStatus handleDrop(CommonDropAdapter dropAdapter, DropTargetEvent dropTargetEvent, Object target) {
		if (targetConn == null || sourceApp == null) {
			// This should not happen
			Logger.logError("Drop handler called but either the target or the source is null");
			return Status.CANCEL_STATUS;
		}
		
		// Remove the application from the current connection
		Job job = new Job(NLS.bind(Messages.UnbindActionJobTitle, sourceApp.name)) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					sourceApp.connection.requestProjectUnbind(sourceApp.projectID);
				} catch (Exception e) {
					Logger.logError("Error requesting application remove: " + sourceApp.name, e); //$NON-NLS-1$
					return new Status(IStatus.ERROR, CodewindUIPlugin.PLUGIN_ID, NLS.bind(Messages.UnbindActionError, sourceApp.name), e);
				}
				if (monitor.isCanceled()) {
					return Status.CANCEL_STATUS;
				}
				return Status.OK_STATUS;
			}
		};
		job.schedule();
		
		// Add the application to the target connection
		job = new Job(NLS.bind(Messages.BindProjectWizardJobLabel, sourceApp.name)) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					targetConn.requestProjectBind(sourceApp.name, sourceApp.fullLocalPath.toOSString(), sourceApp.projectLanguage.getId(), sourceApp.projectType.getId());
					targetConn.refreshApps(null);
				} catch (Exception e) {
					Logger.logError("An error occured trying to add the project to Codewind: " + sourceApp.fullLocalPath.toOSString(), e); //$NON-NLS-1$
					return new Status(IStatus.ERROR, CodewindUIPlugin.PLUGIN_ID, NLS.bind(Messages.BindProjectWizardError, sourceApp.fullLocalPath.toOSString()), e);
				}
				if (monitor.isCanceled()) {
					return Status.CANCEL_STATUS;
				}
				return Status.OK_STATUS;
			}
		};
		job.schedule();
		
		return Status.OK_STATUS;
	}
}
