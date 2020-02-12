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

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.ui.IElementFactory;
import org.eclipse.ui.IMemento;

public class WelcomePageEditorInputFactory implements IElementFactory {

	public static final String FACTORY_ID = "org.eclipse.codewind.ui.editors.welcomePageEditorInputFactory";
	
	@Override
	public IAdaptable createElement(IMemento memento) {
		return new WelcomePageEditorInput();
	}
	
	public static void saveState(IMemento memento, WelcomePageEditorInput input) {
		// Do nothing
	}

}
