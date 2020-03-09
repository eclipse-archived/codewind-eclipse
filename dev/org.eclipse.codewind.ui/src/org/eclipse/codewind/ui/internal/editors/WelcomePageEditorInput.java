/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.codewind.ui.internal.editors;

import org.eclipse.codewind.ui.CodewindUIPlugin;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPersistableElement;

public class WelcomePageEditorInput implements IEditorInput, IPersistableElement {
	
	public static final String EDITOR_ID = "org.eclipse.codewind.ui.editors.welcomePage"; //$NON-NLS-1$
	
	public WelcomePageEditorInput() {
	}

	@Override
	public <T> T getAdapter(Class<T> adapter) {
		return Platform.getAdapterManager().getAdapter(this, adapter);
	}

	@Override
	public boolean exists() {
		return true;
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return CodewindUIPlugin.getDefaultIcon();
	}

	@Override
	public String getName() {
		return "Welcome to Codewind";
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
		WelcomePageEditorInputFactory.saveState(memento, this);
	}

	@Override
	public String getFactoryId() {
		return WelcomePageEditorInputFactory.FACTORY_ID;
	}

	@Override
	// Used to decide if the editor is already open
	public boolean equals(Object obj) {
		return obj == this || obj instanceof WelcomePageEditorInput;
	}

}
