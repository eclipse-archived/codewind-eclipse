/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *	 IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.codewind.ui.internal.actions;

import java.util.List;

import org.eclipse.codewind.core.internal.Logger;
import org.eclipse.codewind.core.internal.connection.CodewindConnection;
import org.eclipse.codewind.ui.CodewindUIPlugin;
import org.eclipse.codewind.ui.internal.messages.Messages;
import org.eclipse.codewind.ui.internal.wizards.BindProjectWizard;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.actions.SelectionProviderAction;

/**
 * Action to create a new project.
 */
public class BindAction extends SelectionProviderAction {

	protected CodewindConnection connection;
	
	public BindAction(ISelectionProvider selectionProvider) {
		super(selectionProvider, Messages.BindActionLabel);
		setImageDescriptor(CodewindUIPlugin.getImageDescriptor(CodewindUIPlugin.BIND_PROJECT_ICON));
		selectionChanged(getStructuredSelection());
	}


	@Override
	public void selectionChanged(IStructuredSelection sel) {
		if (sel.size() == 1) {
			Object obj = sel.getFirstElement();
			if (obj instanceof CodewindConnection) {
				connection = (CodewindConnection)obj;
				setEnabled(connection.isConnected());
				return;
			}
		}
		setEnabled(false);
	}

	@Override
	public void run() {
		if (connection == null) {
			// should not be possible
			Logger.logError("BindAction ran but no Codewind connection was selected"); //$NON-NLS-1$
			return;
		}
		openBindProjectWizard(connection);
	}
	
	public static void openBindProjectWizard(CodewindConnection connection) {
		try {
			BindProjectWizard wizard = new BindProjectWizard(connection);
			WizardDialog dialog = new WizardDialog(Display.getDefault().getActiveShell(), wizard);
			dialog.open();
		} catch (Exception e) {
			Logger.logError("An error occurred opening the bind project wizard for connection: " + connection.getBaseURI(), e); //$NON-NLS-1$
		}
	}
	
	public static void openBindProjectWizard(List<CodewindConnection> connections) {
		try {
			BindProjectWizard wizard = new BindProjectWizard(connections);
			WizardDialog dialog = new WizardDialog(Display.getDefault().getActiveShell(), wizard);
			dialog.open();
		} catch (Exception e) {
			Logger.logError("An error occurred opening the bind project wizard", e); //$NON-NLS-1$
		}
	}
}
