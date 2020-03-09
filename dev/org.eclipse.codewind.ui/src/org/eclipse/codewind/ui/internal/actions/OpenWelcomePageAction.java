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

import org.eclipse.codewind.core.internal.CodewindManager;
import org.eclipse.codewind.core.internal.Logger;
import org.eclipse.codewind.ui.CodewindUIPlugin;
import org.eclipse.codewind.ui.internal.editors.WelcomePageEditorInput;
import org.eclipse.codewind.ui.internal.editors.WelcomePageEditorPart;
import org.eclipse.codewind.ui.internal.messages.Messages;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.actions.SelectionProviderAction;

/**
 * Action to open the welcome page.
 */
public class OpenWelcomePageAction extends SelectionProviderAction {

	public OpenWelcomePageAction(ISelectionProvider selectionProvider) {
		super(selectionProvider, Messages.WelcomePageAction);
		setImageDescriptor(CodewindUIPlugin.getDefaultIcon());
		selectionChanged(getStructuredSelection());
	}

	@Override
	public void selectionChanged(IStructuredSelection sel) {
		if (sel.size() == 1) {
			Object obj = sel.getFirstElement();
			if (obj instanceof CodewindManager) {
				setEnabled(true);
				return;
			}
		}
		setEnabled(false);
	}

	@Override
	public void run() {
		openWelcomePage();
	}

	public static void openWelcomePage() {
		IWorkbenchWindow workbenchWindow = CodewindUIPlugin.getDefault().getWorkbench().getActiveWorkbenchWindow();
		IWorkbenchPage page = workbenchWindow.getActivePage();

		try {
			WelcomePageEditorInput input = new WelcomePageEditorInput();
			IEditorPart editorPart = page.openEditor(input, WelcomePageEditorInput.EDITOR_ID);
			if (!(editorPart instanceof WelcomePageEditorPart)) {
				// This should not happen
				Logger.logError("Welcome page editor part is the wrong type: " + editorPart.getClass()); //$NON-NLS-1$
			}
		} catch (Exception e) {
			Logger.logError("An error occurred opening the welcome page editor", e); //$NON-NLS-1$
		}
	}
}
