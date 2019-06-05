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
import org.eclipse.codewind.ui.CodewindUIPlugin;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;

public class ApplicationOverviewEditorInput implements IEditorInput {
	
	public static final String EDITOR_ID = "org.eclipse.codewind.ui.editors.appOverview";
	
	public final CodewindApplication app;
	
	public ApplicationOverviewEditorInput(CodewindApplication app) {
		this.app = app;
	}

	@Override
	public <T> T getAdapter(Class<T> adapter) {
		return Platform.getAdapterManager().getAdapter(this, adapter);
	}

	@Override
	public boolean exists() {
		return app != null;
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return CodewindUIPlugin.getDefaultIcon();
	}

	@Override
	public String getName() {
		return app.name;
	}

	@Override
	public IPersistableElement getPersistable() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getToolTipText() {
		// TODO Auto-generated method stub
		return null;
	}

}
