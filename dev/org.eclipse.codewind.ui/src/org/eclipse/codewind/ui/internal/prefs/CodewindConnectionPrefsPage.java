/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.codewind.ui.internal.prefs;

import org.eclipse.codewind.core.CodewindCorePlugin;
import org.eclipse.codewind.core.internal.Logger;
import org.eclipse.codewind.core.internal.connection.CodewindConnection;
import org.eclipse.codewind.core.internal.connection.CodewindConnectionManager;
import org.eclipse.codewind.ui.CodewindUIPlugin;
import org.eclipse.codewind.ui.internal.messages.Messages;
import org.eclipse.codewind.ui.internal.wizards.NewCodewindConnectionWizard;
import org.eclipse.codewind.ui.internal.wizards.WizardLauncher;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

/**
 * This preferences page lists the current Codewind connections, allows adding new ones, and removing existing ones.
 */
public class CodewindConnectionPrefsPage extends PreferencePage implements IWorkbenchPreferencePage {

	public static final String
			MC_CONNECTIONS_PREFSKEY = "org.eclipse.codewind.ui.prefs.connections", //$NON-NLS-1$
			PAGE_ID = "CodewindConnectionsPage";		// must match the value in plugin.xml //$NON-NLS-1$

	private Table connectionsTable;

	public CodewindConnectionPrefsPage() {
		super(Messages.ConnectionPrefsPage_ShellTitle, CodewindUIPlugin.getImageDescriptor(CodewindUIPlugin.CODEWIND_ICON));
		setMessage(Messages.ConnectionPrefsPage_PageTitle);
	}

	@Override
	public void init(IWorkbench arg0) {
		// Note that ConfigurationScope is used. This means that our list of connections is shared
		// between different workspaces.
		setPreferenceStore(new ScopedPreferenceStore(ConfigurationScope.INSTANCE, MC_CONNECTIONS_PREFSKEY));

		// these buttons don't do anything
		noDefaultAndApplyButton();
	}

	@Override
	protected Control createContents(Composite parent) {

		parent.setLayout(new GridLayout(2, false));

		Label existingConnections = new Label(parent, SWT.NONE);
		existingConnections.setText(Messages.ConnectionPrefsPage_TableTitleLabel);
		GridData gridData = new GridData(SWT.FILL, SWT.FILL, false, false, 2, 1);
		existingConnections.setLayoutData(gridData);

		connectionsTable = new Table(parent, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL | SWT.FULL_SELECTION);
		connectionsTable.setLinesVisible(true);
		connectionsTable.setHeaderVisible(true);
		gridData = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 2);
		gridData.widthHint = 450;
		gridData.heightHint = 300;
		connectionsTable.setLayoutData(gridData);

		TableColumn urlsCol = new TableColumn(connectionsTable, SWT.BORDER);
		urlsCol.setText(Messages.ConnectionPrefsPage_URLColumn);
		urlsCol.setWidth(gridData.widthHint / 2);
		
		/*
		TableColumn connectedCol = new TableColumn(connectionsTable, SWT.BORDER);
		connectedCol.setText("Connected");
		connectedCol.setWidth(gridData.widthHint - urlsCol.getWidth() - linkedProjectsCol.getWidth());
		*/

		Button addButton = new Button(parent, SWT.PUSH);
		addButton.setText(Messages.ConnectionPrefsPage_AddBtn);
		gridData = new GridData(SWT.FILL, SWT.BEGINNING, false, false);
		addButton.setLayoutData(gridData);
		addButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent se) {
				Wizard wizard = new NewCodewindConnectionWizard();
				WizardLauncher.launchWizardWithoutSelection(wizard);
			}
		});

		Button removeButton = new Button(parent, SWT.PUSH);
		removeButton.setText(Messages.ConnectionPrefsPage_RemoveBtn);
		gridData = new GridData(SWT.FILL, SWT.BEGINNING, false, false);
		removeButton.setLayoutData(gridData);
		removeButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent se) {

				int[] selected = connectionsTable.getSelectionIndices();
				for(int i : selected) {
					// The URL is in the 0th column of the table
					String url = connectionsTable.getItem(i).getText(0);
					CodewindConnectionManager.removeConnection(url);
				}

				refreshConnectionsList();
				// Popup shell removes the selection, so fix that.
				connectionsTable.setSelection(selected);
			}
		});

		connectionsTable.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				int count = connectionsTable.getSelectionCount();
				if (count > 0) {
					removeButton.setEnabled(true);
				} else {
					removeButton.setEnabled(false);
				}
			}
		});

		refreshConnectionsList();
		addButton.setEnabled(true);
		removeButton.setEnabled(false);

		CodewindCorePlugin.getDefault().getPreferenceStore()
			.addPropertyChangeListener(new IPropertyChangeListener() {
				@Override
				public void propertyChange(PropertyChangeEvent event) {
					if (event.getProperty() == CodewindConnectionManager.CONNECTION_LIST_PREFSKEY) {
						Logger.log("Reloading preferences in MCCPP"); //$NON-NLS-1$
						// calling refreshConnectionsList here results in WidgetDisposed exception if
						// the window is not in focus
						refreshConnectionsList();
					}
				}
			});

		return parent;
	}

	private void refreshConnectionsList() {
		if (!connectionsTable.isDisposed()) {
			connectionsTable.removeAll();
		}

		for(CodewindConnection mcc : CodewindConnectionManager.activeConnections()) {
			addTableRow(mcc.baseUrl.toString(),	false);
		}

		for (String brokenConnectionUrl : CodewindConnectionManager.brokenConnections()) {
			addTableRow(brokenConnectionUrl, true);
		}
	}

	private void addTableRow(String url, boolean isBroken) {

		try {
			TableItem ti = new TableItem(connectionsTable, SWT.NONE);

			ti.setText(new String[] { url.toString() /*, isBroken ? CONNECTION_BAD : CONNECTION_GOOD */ });
		}
		catch(SWTException e) {
			// suppress widget disposed exception - It gets thrown if the window is out of focus,
			// but then the table populates just fine anyway, so I don't know why it is a problem.
			if (!"Widget is disposed".equals(e.getMessage())) { //$NON-NLS-1$
				throw e;
			}
		}
	}
}
