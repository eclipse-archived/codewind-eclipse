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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.codewind.core.internal.connection.CodewindConnection;
import org.eclipse.codewind.ui.CodewindUIPlugin;
import org.eclipse.codewind.ui.internal.IDEUtil;
import org.eclipse.codewind.ui.internal.messages.Messages;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;

public class DiagnosticsDialog extends TitleAreaDialog {

	private final CodewindConnection conn;
	private boolean includeEclipseWorkspace, includeProjectInfo;
	private Button eclipseWorkspaceButton, projectInfoButton;
	private List<Button> includeButtons = new ArrayList<Button>();
	private Button selectAllButton, clearAllButton;
	
	public DiagnosticsDialog(Shell parentShell, CodewindConnection conn) {
		super(parentShell);
		this.conn = conn;
	}
	
	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(Messages.DiagnosticsDialogShell);
	}
	
	@Override
	protected boolean isResizable() {
		return true;
	}
	
	protected Control createDialogArea(Composite parent) {
		setTitleImage(CodewindUIPlugin.getImage(CodewindUIPlugin.CODEWIND_BANNER));
		setTitle(NLS.bind(Messages.DiagnosticsDialogTitle, conn.getName()));
		setMessage(Messages.DiagnosticsDialogMsg);
		
		final Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginHeight = 11;
		layout.marginWidth = 9;
		layout.horizontalSpacing = 5;
		layout.verticalSpacing = 15;
		layout.numColumns = 2;
		composite.setLayout(layout);
		GridData data = new GridData(GridData.FILL_BOTH);
		data.widthHint = 200;
		composite.setLayoutData(data);
		composite.setFont(parent.getFont());
		
		Text text = new Text(composite, SWT.WRAP | SWT.READ_ONLY);
		text.setText(Messages.DiagnosticsDialogAdditionalInfoText);
		text.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		IDEUtil.normalizeBackground(text, composite);
		
		Group group = new Group(composite, SWT.NONE);
		group.setText(Messages.DiagnosticsDialogAdditionalInfoGroup);
		layout = new GridLayout();
		layout.marginHeight = 11;
		layout.marginWidth = 9;
		layout.horizontalSpacing = 5;
		layout.verticalSpacing = 7;
		group.setLayout(layout);
		group.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		eclipseWorkspaceButton = new Button(group, SWT.CHECK);
		eclipseWorkspaceButton.setText(Messages.DiagnosticsDialogEclipseWorkspaceButton);
		includeButtons.add(eclipseWorkspaceButton);
		
		projectInfoButton = new Button(group, SWT.CHECK);
		projectInfoButton.setText(Messages.DiagnosticsDialogProjectInfoButton);
		includeButtons.add(projectInfoButton);
		
		Composite buttonComposite = new Composite(composite, SWT.NONE);
		layout = new GridLayout();
		layout.marginHeight = 11;
		layout.marginWidth = 9;
		layout.horizontalSpacing = 5;
		layout.verticalSpacing = 7;
		buttonComposite.setLayout(layout);
		buttonComposite.setLayoutData(new GridData(SWT.FILL, SWT.END, false, false));
		
		selectAllButton = new Button(buttonComposite, SWT.PUSH);
		selectAllButton.setText(Messages.DiagnosticsDialogSelectAllButton);
		selectAllButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		
		clearAllButton = new Button(buttonComposite, SWT.PUSH);
		clearAllButton.setText(Messages.DiagnosticsDialogClearAllButton);
		clearAllButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		
		eclipseWorkspaceButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				updateSelectButtons();
			}
		});
		
		projectInfoButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				updateSelectButtons();
			}
		});
		
		selectAllButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				includeButtons.stream().forEach(button -> button.setSelection(true));
				updateSelectButtons();
			}
		});
		
		clearAllButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				includeButtons.stream().forEach(button -> button.setSelection(false));
				updateSelectButtons();
			}
		});
		
		// Add Context Sensitive Help
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, CodewindUIPlugin.MAIN_CONTEXTID);
		
		eclipseWorkspaceButton.setSelection(true);
		projectInfoButton.setSelection(false);
		updateSelectButtons();
		
		return composite;
	}
	
	private void updateSelectButtons() {
		selectAllButton.setEnabled(includeButtons.stream().filter(button -> !button.getSelection()).findFirst().isPresent());
		clearAllButton.setEnabled(includeButtons.stream().filter(button -> button.getSelection()).findFirst().isPresent());
	}

	@Override
	protected void okPressed() {
		includeEclipseWorkspace = eclipseWorkspaceButton.getSelection();
		includeProjectInfo = projectInfoButton.getSelection();
		super.okPressed();
	}

	@Override
	protected Point getInitialSize() {
		Point point = super.getInitialSize();
		return new Point(750, point.y);
	}
	
	public boolean includeEclipseWorkspace() {
		return includeEclipseWorkspace;
	}
	
	public boolean includeProjectInfo() {
		return includeProjectInfo;
	}
}
