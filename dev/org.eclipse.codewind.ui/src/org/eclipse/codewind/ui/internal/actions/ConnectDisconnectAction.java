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

package org.eclipse.codewind.ui.internal.actions;

import org.eclipse.codewind.core.internal.Logger;
import org.eclipse.codewind.core.internal.connection.RemoteConnection;
import org.eclipse.codewind.ui.CodewindUIPlugin;
import org.eclipse.codewind.ui.internal.messages.Messages;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.actions.SelectionProviderAction;

/**
 * Action for enabling/disabling a Codewind project.  Not currently used.
 */
public class ConnectDisconnectAction extends SelectionProviderAction {

	protected RemoteConnection conn;
	
	public ConnectDisconnectAction(ISelectionProvider selectionProvider) {
		super(selectionProvider, Messages.DisconnectActionLabel);
		setImageDescriptor(CodewindUIPlugin.getImageDescriptor(CodewindUIPlugin.REMOTE_CONNECT_ICON));
		selectionChanged(getStructuredSelection());
	}

	@Override
	public void selectionChanged(IStructuredSelection sel) {
		if (sel.size() == 1) {
			Object obj = sel.getFirstElement();
			if (obj instanceof RemoteConnection) {
				conn = (RemoteConnection)obj;
				if (conn.isConnected()) {
					setText(Messages.DisconnectActionLabel);
				} else {
					setText(Messages.ConnectActionLabel);
				}
				setEnabled(true);
				return;
			}
		}
		
		setEnabled(false);
	}
	
	@Override
	public void run() {
		if (conn == null) {
			// should not be possible
			Logger.logError("ConnectDisconnectAction ran but a remote connection was not selected"); //$NON-NLS-1$
			return;
		}
		
		if (conn.isConnected()) {
			Job job = new Job(NLS.bind(Messages.DisconnectJobLabel, conn.getBaseURI())) {
				@Override
				protected IStatus run(IProgressMonitor monitor) {
					try {
						conn.disconnect();
						CodewindUIPlugin.getUpdateHandler().updateConnection(conn);
						return Status.OK_STATUS;
					} catch (Exception e) {
						Logger.logError("An error occurred disconnecting from: " + conn.getBaseURI(), e); //$NON-NLS-1$
						return new Status(IStatus.ERROR, CodewindUIPlugin.PLUGIN_ID, NLS.bind(Messages.DisconnectJobError, conn.getBaseURI()), e);
					}
				}
			};
			job.schedule();
		} else {
			connectRemoteCodewind(conn);
		}
	}
	
	public static void connectRemoteCodewind(RemoteConnection conn) {
		Job job = new Job(NLS.bind(Messages.ConnectJobLabel, conn.getBaseURI())) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					conn.connect(monitor);
					CodewindUIPlugin.getUpdateHandler().updateConnection(conn);
					return Status.OK_STATUS;
				} catch (Exception e) {
					Logger.logError("An error occurred connecting to: " + conn.getBaseURI(), e); //$NON-NLS-1$
					return new Status(IStatus.ERROR, CodewindUIPlugin.PLUGIN_ID, NLS.bind(Messages.ConnectJobError, conn.getBaseURI()), e);
				}
			}
		};
		job.schedule();
	}
}
