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

package org.eclipse.codewind.core.internal.connection;

import java.net.URI;

import org.eclipse.codewind.core.internal.Logger;
import org.eclipse.codewind.core.internal.CoreUtil;
import org.eclipse.codewind.core.internal.CodewindObjectFactory;
import org.eclipse.codewind.core.internal.messages.Messages;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ICoreRunnable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.osgi.util.NLS;

public class CodewindReconnectJob {

	/**
	 * Keep trying to create a CodewindConnection to the given url until the user cancels the job or deletes
	 * the connection from the Preferences page.
	 * To be used when an initial connection cannot be established when loading from prefs on Eclipse start-up.
	 */
	static void createAndStart(final URI url) {
		final String msg = NLS.bind(Messages.ReconnectJob_ReconnectJobName, url);

		Job reconnectJob = Job.create(msg, new ICoreRunnable() {
			@Override
			public void run(IProgressMonitor monitor) throws CoreException {

				monitor.beginTask(msg, 100);

				while (!monitor.isCanceled() &&
						// Note the connection can still be deleted through the Prefs page.
						CodewindConnectionManager.brokenConnections().contains(url.toString())) {

					tryReconnect(monitor);

					if (monitor.isCanceled()) {
						// If they cancel the monitor, we try to delete the connection, but they can still choose
						// to not delete it.
						boolean deleted = CodewindConnectionManager.removeConnection(url.toString());
						// If they decide to not delete, recreate this job so they can continue trying to connect.
						if (!deleted) {
							CodewindReconnectJob.createAndStart(url);
							// Loop, and then this method, will exit after this - so still only one job of this kind
							// should exist at a time.
						}
					}
				}

				Logger.log("Done waiting for Codewind reconnect - monitor is canceled? " + monitor.isCanceled()); //$NON-NLS-1$
				monitor.done();
			}

			private void tryReconnect(IProgressMonitor monitor) {
				// each re-connect attempt takes 2 seconds because that's how long the socket tries to connect for
				// so, we delay for 5 seconds, try to connect for 2 seconds, repeat.
				final int delay = 5000;

				try {
					Thread.sleep(delay);
					//i++;

					Logger.log("Trying to reconnect to Codewind at " + url); //$NON-NLS-1$

					CodewindConnection newConnection = CodewindObjectFactory.createCodewindConnection(url);
					if (newConnection != null) {
						// connection re-established!
						Logger.log("Successfully re-connected to Codewind at " + url); //$NON-NLS-1$
						CodewindConnectionManager.remove(url.toString());
						CodewindConnectionManager.add(newConnection);
						return;
					}
				}
				catch (InterruptedException e) {
					Logger.logError(e);
				}
				catch (CodewindConnectionException e) {
					// nothing, the connection just failed. we'll try again.
				}
				catch (Exception e) {
					// If any other exception occurs,
					// it is most likely that this connection will never succeed.
					Logger.logError(e);
					monitor.setCanceled(true);

					CoreUtil.openDialog(true, Messages.ReconnectJob_ReconnectErrorDialogTitle,
							NLS.bind(Messages.ReconnectJob_ReconnectErrorDialogMsg, url));
				}
			}
		});

		reconnectJob.schedule();
	}
}
