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

import org.eclipse.codewind.core.internal.Logger;
import org.eclipse.codewind.ui.internal.messages.Messages;
import org.eclipse.codewind.ui.internal.views.ViewHelper;
import org.eclipse.jface.action.Action;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
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
		
		// Open the J2EE perspective
		try {
			IWorkbench workbench = PlatformUI.getWorkbench();
			workbench.showPerspective("org.eclipse.jst.j2ee.J2EEPerspective", workbench.getActiveWorkbenchWindow());
		} catch (Exception e) {
			Logger.logError("An error occurred trying to open the J2EE perspective", e);
		}

		// Open the Codewind welcome page
		IEditorPart part = OpenWelcomePageAction.openWelcomePage();

		// Open the Codewind Explorer view
		ViewHelper.openCodewindExplorerViewNoExec();

		// Make the welcome page the focus
		if (part != null) {
			IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
			if (window != null) {
				IWorkbenchPage page = window.getActivePage();
				if (page != null) {
					page.activate(part);
				}
			}
		}
	}

}
