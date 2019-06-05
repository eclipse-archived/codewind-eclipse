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

package org.eclipse.codewind.ui.internal.console;

import org.eclipse.codewind.core.internal.console.SocketConsole;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.console.IConsolePageParticipant;
import org.eclipse.ui.part.IPageBookViewPage;

public class SocketConsolePageParticipant implements IConsolePageParticipant {

	@Override
	public <T> T getAdapter(Class<T> arg0) {
		return null;
	}

	@Override
	public void activated() {
		// Empty
	}

	@Override
	public void deactivated() {
		// Empty
	}

	@Override
	public void dispose() {
		// Empty
	}

	@Override
	public void init(IPageBookViewPage page, IConsole console) {
		if (console instanceof SocketConsole) {
			ShowOnContentChangeAction contentChange = new ShowOnContentChangeAction((SocketConsole)console);
			
			// Contribute to the toolbar
	        IActionBars actionBars = page.getSite().getActionBars();
	        IToolBarManager mgr = actionBars.getToolBarManager();
	        mgr.appendToGroup(IConsoleConstants.OUTPUT_GROUP, contentChange);
		}
	}

}
