/*******************************************************************************
 * Copyright (c) 2018, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.codewind.ui.internal.marker;

import org.eclipse.codewind.core.internal.Logger;
import org.eclipse.codewind.core.internal.CodewindApplication;
import org.eclipse.codewind.ui.CodewindUIPlugin;
import org.eclipse.codewind.ui.internal.messages.Messages;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.IMarkerResolution;

public class CodewindMarkerResolution implements IMarkerResolution {
	
	private final CodewindApplication app;
	private final String quickFixId;
	private final String quickFixDescription;
	
	public CodewindMarkerResolution(CodewindApplication app, String quickFixId, String quickFixDescription) {
		this.app = app;
		this.quickFixId = quickFixId;
		this.quickFixDescription = quickFixDescription;
	}

	@Override
	public String getLabel() {
		return quickFixDescription;
	}

	@Override
	public void run(IMarker marker) {
		// Some day there should be an API that takes the quick fix id and executes
		// it.  For now, just make a regenerate request.
		try {
			app.connection.requestValidateGenerate(app);
			IResource resource = marker.getResource();
			if (resource != null) {
				Job job = new Job(NLS.bind(Messages.refreshResourceJobLabel, resource.getName())) {
					@Override
					protected IStatus run(IProgressMonitor monitor) {
						try {
							resource.refreshLocal(IResource.DEPTH_INFINITE, monitor);
				            return Status.OK_STATUS;
						} catch (Exception e) {
							Logger.logError("An error occurred while refreshing the resource: " + resource.getLocation()); //$NON-NLS-1$
							return new Status(IStatus.ERROR, CodewindUIPlugin.PLUGIN_ID,
									NLS.bind(Messages.RefreshResourceError, resource.getLocation()), e);
						}
					}
				};
				job.setPriority(Job.LONG);
				job.schedule();
			}
		} catch (Exception e) {
			Logger.logError("The generate request failed for application: " + app.name, e); //$NON-NLS-1$
		}
	}
}
