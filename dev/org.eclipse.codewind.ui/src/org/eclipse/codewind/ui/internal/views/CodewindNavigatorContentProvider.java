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

package org.eclipse.codewind.ui.internal.views;

import java.util.List;

import org.eclipse.codewind.core.internal.CodewindApplication;
import org.eclipse.codewind.core.internal.connection.CodewindConnection;
import org.eclipse.codewind.core.internal.connection.CodewindConnectionManager;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.viewers.ITreeContentProvider;

/**
 * Content provider for the Codewind view.
 */
public class CodewindNavigatorContentProvider implements ITreeContentProvider {

	@Override
	public Object[] getChildren(Object obj) {
		if (obj instanceof CodewindConnection) {
			CodewindConnection connection = (CodewindConnection)obj;
			List<CodewindApplication> apps = connection.getApps();
			return apps.toArray(new CodewindApplication[apps.size()]);
		}
		return null;
	}

	@Override
	public Object[] getElements(Object obj) {
		List<CodewindConnection> connections = CodewindConnectionManager.activeConnections();
		return connections.toArray(new CodewindConnection[connections.size()]);
	}

	@Override
	public Object getParent(Object obj) {
		if (obj instanceof CodewindConnection) {
			return ResourcesPlugin.getWorkspace().getRoot();
		} else if (obj instanceof CodewindApplication) {
			CodewindApplication app = (CodewindApplication)obj;
			return app.connection;
		}
		return null;
	}

	@Override
	public boolean hasChildren(Object obj) {
		if (obj instanceof CodewindConnection) {
			CodewindConnection connection = (CodewindConnection)obj;
			return !connection.getApps().isEmpty();
		}
		return false;
	}

}
