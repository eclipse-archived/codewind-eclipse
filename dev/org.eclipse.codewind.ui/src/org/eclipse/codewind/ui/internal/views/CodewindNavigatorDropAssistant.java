/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.codewind.ui.internal.views;

import java.util.concurrent.TimeoutException;

import org.eclipse.codewind.core.CodewindCorePlugin;
import org.eclipse.codewind.core.internal.CodewindApplication;
import org.eclipse.codewind.core.internal.Logger;
import org.eclipse.codewind.core.internal.cli.ProjectUtil;
import org.eclipse.codewind.core.internal.connection.CodewindConnection;
import org.eclipse.codewind.core.internal.constants.ProjectType;
import org.eclipse.codewind.ui.CodewindUIPlugin;
import org.eclipse.codewind.ui.internal.actions.OpenAppOverviewAction;
import org.eclipse.codewind.ui.internal.messages.Messages;
import org.eclipse.codewind.ui.internal.prefs.RegistryManagementDialog;
import org.eclipse.codewind.ui.internal.wizards.BindProjectWizard;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.widgets.Display;
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
		
		String jobName = NLS.bind(Messages.MoveProjectJobLabel, new String[] {targetConn.getName(), sourceApp.name});
		Job job = new Job(jobName) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					SubMonitor mon = SubMonitor.convert(monitor, jobName, 120);
					
					// Check for a push registry if Codewind style project
					if (!targetConn.isLocal() && ProjectType.isCodewindStyle(sourceApp.projectType.getId()) && !targetConn.requestHasPushRegistry()) {
						Display.getDefault().syncExec(new Runnable() {
							@Override
							public void run() {
								if (MessageDialog.openConfirm(getShell(), Messages.NoPushRegistryTitle, Messages.NoPushRegistryMessage)) {
									RegistryManagementDialog.open(getShell(), targetConn, mon.split(40));
								} else {
									mon.setCanceled(true);
								}
							}
						});
						if (mon.isCanceled()) {
							return Status.CANCEL_STATUS;
						}
						if (!targetConn.requestHasPushRegistry()) {
							return new Status(IStatus.ERROR, CodewindUIPlugin.PLUGIN_ID, Messages.NoPushRegistryError, null);
						}
					}
					mon.setWorkRemaining(80);
					
					// Remove the application from the current connection
					ProjectUtil.removeProject(sourceApp.name, sourceApp.projectID, mon.split(10));
					if (monitor.isCanceled()) {
						return Status.CANCEL_STATUS;
					}
					
					// Add the application to the target connection
					ProjectUtil.bindProject(sourceApp.name, sourceApp.fullLocalPath.toOSString(), sourceApp.projectLanguage.getId(), sourceApp.projectType.getId(), targetConn.getConid(), mon.split(60));
					if (mon.isCanceled()) {
						BindProjectWizard.cleanup(sourceApp.name, targetConn);
						return Status.CANCEL_STATUS;
					}
					mon.split(10);
					targetConn.refreshApps(null);
					CodewindApplication newApp = targetConn.getAppByName(sourceApp.name);
					if (newApp != null) {
						if (CodewindCorePlugin.getDefault().getPreferenceStore().getBoolean(CodewindCorePlugin.AUTO_OPEN_OVERVIEW_PAGE)) {
							Display.getDefault().asyncExec(() -> OpenAppOverviewAction.openAppOverview(newApp));
						}
					} else {
						Logger.logError("The " + sourceApp.name + " application could not be found on connection: " + targetConn.getName()); //$NON-NLS-1$ //$NON-NLS-2$
					}
					mon.worked(10);
					mon.done();
					CodewindUIPlugin.getUpdateHandler().updateConnection(targetConn);
				} catch (TimeoutException e) {
					Logger.logError("A timeout occurred trying to move project: " + sourceApp.fullLocalPath.toOSString(), e); //$NON-NLS-1$
					BindProjectWizard.cleanup(sourceApp.name, targetConn);
					return new Status(IStatus.ERROR, CodewindUIPlugin.PLUGIN_ID, NLS.bind(Messages.MoveProjectTimeout, new String[] {sourceApp.name, sourceApp.connection.getName(), targetConn.getName()}), e);
				} catch (Exception e) {
					Logger.logError("An error occured trying to move project: " + sourceApp.fullLocalPath.toOSString(), e); //$NON-NLS-1$
					return new Status(IStatus.ERROR, CodewindUIPlugin.PLUGIN_ID, NLS.bind(Messages.MoveProjectError, new String[] {sourceApp.name, sourceApp.connection.getName(), targetConn.getName()}), e);
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
