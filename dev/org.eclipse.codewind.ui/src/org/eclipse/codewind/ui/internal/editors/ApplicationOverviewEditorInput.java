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

package org.eclipse.codewind.ui.internal.editors;

import org.eclipse.codewind.core.internal.CodewindApplication;
import org.eclipse.codewind.core.internal.connection.CodewindConnection;
import org.eclipse.codewind.core.internal.connection.CodewindConnectionManager;
import org.eclipse.codewind.ui.CodewindUIPlugin;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPersistableElement;

public class ApplicationOverviewEditorInput implements IEditorInput, IPersistableElement {
	
	public static final String EDITOR_ID = "org.eclipse.codewind.ui.editors.appOverview"; //$NON-NLS-1$
	
	public final String connectionUri;
	public final String projectID;
	public final String projectName;
	
	public ApplicationOverviewEditorInput(CodewindApplication app) {
		this.connectionUri = app.connection.getBaseURI().toString();
		this.projectID = app.projectID;
		this.projectName = app.name;
	}
	
	public ApplicationOverviewEditorInput(String connectionUri, String projectID, String projectName) {
		this.connectionUri = connectionUri;
		this.projectID = projectID;
		this.projectName = projectName;
	}

	@Override
	public <T> T getAdapter(Class<T> adapter) {
		return Platform.getAdapterManager().getAdapter(this, adapter);
	}

	@Override
	public boolean exists() {
		CodewindConnection conn = CodewindConnectionManager.getActiveConnection(connectionUri);
		if (conn == null) {
			return false;
		}
		return conn.getAppByID(projectID) != null;
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return CodewindUIPlugin.getDefaultIcon();
	}

	@Override
	public String getName() {
		return projectName;
	}

	@Override
	public IPersistableElement getPersistable() {
		return this;
	}

	@Override
	public String getToolTipText() {
		return null;
	}

	@Override
	public void saveState(IMemento memento) {
		ApplicationOverviewEditorInputFactory.saveState(memento, this);
	}

	@Override
	public String getFactoryId() {
		return ApplicationOverviewEditorInputFactory.FACTORY_ID;
	}

	@Override
	public boolean equals(Object obj) {
		// Used to decide if the editor is already open
		if (obj == this) {
			return true;
		}
		
		if (!(obj instanceof ApplicationOverviewEditorInput)) {
			return false;
		}
		
		ApplicationOverviewEditorInput input = (ApplicationOverviewEditorInput)obj;
		return connectionUri.equals(input.connectionUri) && projectID.equals(input.projectID);
	}

}
