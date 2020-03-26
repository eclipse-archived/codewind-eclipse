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

package org.eclipse.codewind.ui.internal.actions;

import org.eclipse.codewind.ui.internal.messages.Messages;
import org.eclipse.codewind.ui.internal.views.ViewHelper;
import org.eclipse.jface.action.Action;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.intro.IIntroManager;
import org.eclipse.ui.intro.IIntroPart;

public class GetStartedAction extends Action {
	
	public GetStartedAction() {
		setText(Messages.GetStartedIntroMessage);
	}

	@Override
	public void run() {
		// Close the Eclipse welcome page
		IIntroManager manager = PlatformUI.getWorkbench().getIntroManager();
		IIntroPart introPart = manager.getIntro();
		if (introPart != null) {
			manager.closeIntro(introPart);
		}
		
		// Open the Codewind Explorer view
		ViewHelper.openCodewindExplorerViewNoExec();
		
		// Open the Codewind welcome page
		// Make sure this done last so that it has focus
		OpenWelcomePageAction.openWelcomePage();
	}

}
