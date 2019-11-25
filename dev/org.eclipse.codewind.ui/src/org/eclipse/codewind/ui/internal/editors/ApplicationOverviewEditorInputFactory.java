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

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.ui.IElementFactory;
import org.eclipse.ui.IMemento;

public class ApplicationOverviewEditorInputFactory implements IElementFactory {

	public static final String FACTORY_ID = "org.eclipse.codewind.ui.editors.appOverviewEditorInputFactory";
	
	private static final String CONNECTION_ID = "connectionId";
	private static final String CONNECTION_URI = "connectionUri";
	private static final String CONNECTION_NAME = "connectionName";
	private static final String PROJECT_ID = "projectID";
	private static final String PROJECT_NAME = "projectName";
	
	@Override
	public IAdaptable createElement(IMemento memento) {
		String connectionId = memento.getString(CONNECTION_ID);
		String connectionUri = memento.getString(CONNECTION_URI);
		String connectionName = memento.getString(CONNECTION_NAME);
		String projectId = memento.getString(PROJECT_ID);
		String projectName = memento.getString(PROJECT_NAME);
		return new ApplicationOverviewEditorInput(connectionId, connectionUri, connectionName, projectId, projectName);
	}
	
	public static void saveState(IMemento memento, ApplicationOverviewEditorInput input) {
		if (input == null) {
			return;
		}
		
		if (input.connectionId != null) {
			memento.putString(CONNECTION_ID, input.connectionId);
		}
		memento.putString(CONNECTION_URI, input.connectionUri);
		if (input.connectionName != null) {
			memento.putString(CONNECTION_NAME, input.connectionName);
		}
		memento.putString(PROJECT_ID, input.projectID);
		memento.putString(PROJECT_NAME, input.projectName);
	}

}
