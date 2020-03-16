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

package org.eclipse.codewind.ui.internal.actions;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.ui.navigator.CommonActionProvider;
import org.eclipse.ui.navigator.ICommonActionExtensionSite;

/**
 * Action provider for the Codewind view.
 */
public class CodewindActionProvider extends CommonActionProvider {
	
	private ISelectionProvider selProvider;
	private OpenWelcomePageAction openWelcomePageAction;
	private AddConnectionAction addConnectionAction;
	
    @Override
    public void init(ICommonActionExtensionSite aSite) {
        super.init(aSite);
        selProvider = aSite.getStructuredViewer();
        openWelcomePageAction = new OpenWelcomePageAction(selProvider);
        addConnectionAction = new AddConnectionAction(selProvider);
    }
    
	@Override
	public void fillContextMenu(IMenuManager menu) {
		selProvider.setSelection(selProvider.getSelection());
		menu.add(openWelcomePageAction);
		menu.add(new Separator());
		menu.add(addConnectionAction);
	}
}
