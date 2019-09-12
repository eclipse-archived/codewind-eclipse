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

package org.eclipse.codewind.ui.internal.views;

import java.util.List;

import org.eclipse.codewind.core.internal.CodewindApplication;
import org.eclipse.codewind.core.internal.CodewindManager;
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
		if (obj instanceof CodewindManager) {
			List<CodewindConnection> connections = CodewindConnectionManager.activeConnections();
			return connections.toArray(new CodewindConnection[connections.size()]);
		} else if (obj instanceof CodewindConnection) {
			CodewindConnection connection = (CodewindConnection)obj;
			List<CodewindApplication> apps = connection.getApps();
			return apps.toArray(new CodewindApplication[apps.size()]);
		}
		return null;
	}

	@Override
	public Object[] getElements(Object obj) {
		return new Object[] { CodewindManager.getManager() };
	}

	@Override
	public Object getParent(Object obj) {
		if (obj instanceof CodewindManager) {
			return ResourcesPlugin.getWorkspace().getRoot();
		} else if (obj instanceof CodewindConnection) {
			return CodewindManager.getManager();
		} else if (obj instanceof CodewindApplication) {
			CodewindApplication app = (CodewindApplication)obj;
			return app.connection;
		}
		return null;
	}

	@Override
	public boolean hasChildren(Object obj) {
		if (obj instanceof CodewindManager) {
			CodewindManager manager = (CodewindManager) obj;
			if (manager.getInstallStatus().isStarted()) {
				// Make sure the local connection is there if Codewind is running
				if (manager.getLocalConnection() == null) {
					manager.createLocalConnection();
				}
				return !CodewindConnectionManager.activeConnections().isEmpty();
			}
		} else if (obj instanceof CodewindConnection) {
			CodewindConnection connection = (CodewindConnection)obj;
			return !connection.getApps().isEmpty();
		}
		return false;
	}

}
