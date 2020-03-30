/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *	 IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.codewind.ui.internal.wizards;

import java.util.List;

import org.eclipse.codewind.core.internal.connection.CodewindConnection;
import org.eclipse.codewind.core.internal.connection.LocalConnection;
import org.eclipse.codewind.core.internal.connection.RemoteConnection;
import org.eclipse.codewind.ui.CodewindUIPlugin;
import org.eclipse.codewind.ui.internal.messages.Messages;
import org.eclipse.codewind.ui.internal.views.CodewindNavigatorLabelProvider;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;

public class ConnectionSelectionPage extends WizardPage {
	
	private List<CodewindConnection> connections;
	private CodewindConnection connection;
	private TableViewer connViewer;

	protected ConnectionSelectionPage(List<CodewindConnection> connections) {
		super(Messages.SelectConnectionPageName);
		setTitle(Messages.SelectConnectionPageTitle);
		setDescription(Messages.SelectConnectionPageDescription);
		this.connections = connections;
	}

	@Override
	public void createControl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		layout.horizontalSpacing = 5;
		layout.verticalSpacing = 7;
		composite.setLayout(layout);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		composite.setLayoutData(data);
		
		connViewer = new TableViewer(composite, SWT.BORDER | SWT.SINGLE | SWT.V_SCROLL);
		connViewer.setContentProvider(ArrayContentProvider.getInstance());
		connViewer.setLabelProvider(new ConnLabelProvider());
		connViewer.setInput(connections);
		connViewer.getTable().setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true));
		connViewer.addSelectionChangedListener((SelectionChangedEvent event) -> {
			connection = (CodewindConnection)connViewer.getStructuredSelection().getFirstElement();
			validate();
		});
		
		// Add Context Sensitive Help
		PlatformUI.getWorkbench().getHelpSystem().setHelp(composite, CodewindUIPlugin.MAIN_CONTEXTID);

		setControl(composite);
	}
	
	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible) {
			connViewer.getTable().setFocus();
		}
	}

	private void validate() {
		String errorMsg = null;
		if (connection == null) {
			errorMsg = Messages.SelectConnectionPageNoSelectionMsg;
		}
		setErrorMessage(errorMsg);
		getWizard().getContainer().updateButtons();
	}

	@Override
	public boolean canFlipToNextPage() {
		return canFinish();
	}
	
	public boolean isActivePage() {
		return isCurrentPage();
	}

	public boolean canFinish() {
		return connection != null;
	}
	
	public CodewindConnection getConnection() {
		return connection;
	}
	
	private class ConnLabelProvider extends LabelProvider {
		@Override
		public Image getImage(Object element) {
			return CodewindNavigatorLabelProvider.getCodewindImage(element);
		}

		@Override
		public String getText(Object element) {
			if (element instanceof LocalConnection) {
				return ((CodewindConnection)element).getName();
			} else if (element instanceof RemoteConnection) {
				return ((CodewindConnection)element).getName() + " (" + ((CodewindConnection)element).getBaseURI() + ")";
			}
			return null;
		}
	}
	
}
