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

package org.eclipse.codewind.ui.internal.views;

import java.util.HashSet;

import org.eclipse.codewind.core.internal.CodewindApplication;
import org.eclipse.codewind.core.internal.IUpdateHandler;
import org.eclipse.codewind.core.internal.connection.CodewindConnection;

/**
 * Update handler registered on the Codewind core plug-in in order to keep
 * the Codewind view up to date.  Listeners can also register to be kept up
 * to date.
 */
public class UpdateHandler implements IUpdateHandler {
	
	// Add is handled by a modify event on the parent
	public enum UpdateType {
		MODIFY,
		REMOVE;
	}
	
	private HashSet<UpdateListener> updateListeners = new HashSet<UpdateListener>();
	
	@Override
	public void updateAll() {
		ViewHelper.refreshCodewindExplorerView(null);
		updateListeners(UpdateType.MODIFY, null);
	}

	@Override
	public void updateConnection(CodewindConnection connection) {
		ViewHelper.refreshCodewindExplorerView(connection);
		ViewHelper.expandConnection(connection);
		updateListeners(UpdateType.MODIFY, connection);
	}
	
	@Override
	public void updateApplication(CodewindApplication app) {
		ViewHelper.refreshCodewindExplorerView(app);
		ViewHelper.expandConnection(app.connection);
		updateListeners(UpdateType.MODIFY, app);
	}
	
	@Override
	public void removeConnection(CodewindConnection conn) {
		ViewHelper.refreshCodewindExplorerView(null);
		updateListeners(UpdateType.REMOVE, conn);
	}

	@Override
	public void removeApplication(CodewindApplication app) {
		ViewHelper.refreshCodewindExplorerView(app.connection);
		ViewHelper.expandConnection(app.connection);
		updateListeners(UpdateType.REMOVE, app);
	}
	
	private void updateListeners(UpdateType type, Object element) {
		synchronized(updateListeners) {
			updateListeners.stream().forEach(listener -> listener.update(type, element));
		}
	}
	
	public void addUpdateListener(UpdateListener listener) {
		synchronized(updateListeners) {
			updateListeners.add(listener);
		}
	}
	
	public void removeUpdateListener(UpdateListener listener) {
		synchronized(updateListeners) {
			updateListeners.remove(listener);
		}
	}
	
	public interface UpdateListener {
		public void update(UpdateType type, Object element);
	}
}
