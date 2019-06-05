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

package org.eclipse.codewind.ui.internal.marker;

import org.eclipse.codewind.core.internal.CodewindEclipseApplication;
import org.eclipse.codewind.core.internal.CodewindApplication;
import org.eclipse.codewind.core.internal.connection.CodewindConnection;
import org.eclipse.codewind.core.internal.connection.CodewindConnectionManager;
import org.eclipse.core.resources.IMarker;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolutionGenerator2;

/**
 * Generates the quick fixes for the marker if there are any.
 */
public class CodewindMarkerResolutionGenerator implements IMarkerResolutionGenerator2 {

	@Override
	public IMarkerResolution[] getResolutions(IMarker marker) {
		CodewindApplication app = getApplication(marker);
		if (app == null) {
			return null;
		}
		
		String quickFixId = marker.getAttribute(CodewindEclipseApplication.QUICK_FIX_ID, (String)null);
		String quickFixDescription = marker.getAttribute(CodewindEclipseApplication.QUICK_FIX_DESCRIPTION, (String)null);
		IMarkerResolution resolution = new CodewindMarkerResolution(app, quickFixId, quickFixDescription);
		return new IMarkerResolution[] { resolution };
	}

	@Override
	public boolean hasResolutions(IMarker marker) {
		String quickFixId = marker.getAttribute(CodewindEclipseApplication.QUICK_FIX_ID, (String)null);
		String quickFixDescription = marker.getAttribute(CodewindEclipseApplication.QUICK_FIX_DESCRIPTION, (String)null);
		if (quickFixId == null || quickFixDescription == null) {
			return false;
		}
		
		// Check that the project still exists
		CodewindApplication app = getApplication(marker);
		if (app == null) {
			return false;
		}
		
		return true;
	}
	
	private CodewindApplication getApplication(IMarker marker) {
		String connectionUrl = marker.getAttribute(CodewindEclipseApplication.CONNECTION_URL, (String)null);
		String projectId = marker.getAttribute(CodewindEclipseApplication.PROJECT_ID, (String)null);
		CodewindConnection connection = CodewindConnectionManager.getActiveConnection(connectionUrl);
		if (connection == null) {
			return null;
		}
		CodewindApplication app = connection.getAppByID(projectId);
		return app;
	}

}
