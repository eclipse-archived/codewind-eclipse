/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.codewind.ui.internal.wizards;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.codewind.core.internal.CodewindApplication;
import org.eclipse.codewind.core.internal.CoreUtil;
import org.eclipse.codewind.core.internal.connection.CodewindConnection;
import org.eclipse.codewind.ui.CodewindUIPlugin;
import org.eclipse.codewind.ui.internal.IDEUtil;
import org.eclipse.codewind.ui.internal.messages.Messages;
import org.eclipse.core.runtime.IPath;
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

public class ProjectDeployedDialog extends TitleAreaDialog {
	
	public enum Behaviour {
		REMOVE,
		DISABLE,
		MAINTAIN
	};
	
	private final IPath projectPath;
	private final CodewindConnection newConn;
	private final List<CodewindApplication> existingDeployments;
	private Behaviour selectedBehaviour = Behaviour.REMOVE;
	
	public ProjectDeployedDialog(Shell parentShell, IPath projectPath, CodewindConnection newConn, List<CodewindApplication> existingDeployments) {
		super(parentShell);
		this.projectPath = projectPath;
		this.newConn = newConn;
		this.existingDeployments = existingDeployments;
	}
	
	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(Messages.ProjectDeployedDialogShell);
	}
	
	@Override
	protected boolean isResizable() {
		return true;
	}
	
	protected Control createDialogArea(Composite parent) {
		setTitleImage(CodewindUIPlugin.getImage(CodewindUIPlugin.CODEWIND_BANNER));
		setTitle(Messages.ProjectDeployedDialogTitle);
		setMessage(NLS.bind(Messages.ProjectDeployedDialogMessage, projectPath.toOSString()));
		
		final Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginHeight = 11;
		layout.marginWidth = 9;
		layout.horizontalSpacing = 5;
		layout.verticalSpacing = 15;
		composite.setLayout(layout);
		GridData data = new GridData(GridData.FILL_BOTH);
		data.widthHint = 200;
		composite.setLayoutData(data);
		composite.setFont(parent.getFont());
		
		Text instructionText = new Text(composite, SWT.READ_ONLY | SWT.MULTI | SWT.WRAP);
		instructionText.setText(NLS.bind(Messages.ProjectDeployedDialogInstructions, getExistingConnections()));
		IDEUtil.normalizeBackground(instructionText, composite);
		instructionText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Group radioGroup = new Group(composite, SWT.NONE);
		layout = new GridLayout();
		layout.marginHeight = 11;
		layout.marginWidth = 9;
		layout.horizontalSpacing = 5;
		layout.verticalSpacing = 7;
		radioGroup.setLayout(layout);
		radioGroup.setText(Messages.ProjectDeployedDialogGroupLabel);
		radioGroup.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		Button removeButton = addBehaviourButton(radioGroup, Messages.ProjectDeployedDialogRemoveLabel, NLS.bind(Messages.ProjectDeployedDialogRemoveTooltip, newConn.getName()), Behaviour.REMOVE);
		addBehaviourButton(radioGroup, Messages.ProjectDeployedDialogDisableLabel, NLS.bind(Messages.ProjectDeployedDialogDisableTooltip, newConn.getName()), Behaviour.DISABLE);
		addBehaviourButton(radioGroup, Messages.ProjectDeployedDialogMaintainLabel, NLS.bind(Messages.ProjectDeployedDialogMaintainTooltip, newConn.getName()), Behaviour.MAINTAIN);

		// Add Context Sensitive Help
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, CodewindUIPlugin.MAIN_CONTEXTID);
		
		selectedBehaviour = Behaviour.REMOVE;
		removeButton.setSelection(true);
		removeButton.setFocus();
		
		return composite;
	}
	
	private Button addBehaviourButton(Composite parent, String label, String tooltip, Behaviour behaviour) {
		Button button = new Button(parent, SWT.RADIO);
		button.setText(label);
		button.setToolTipText(tooltip);
		button.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
		button.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (button.getSelection()) {
					selectedBehaviour = behaviour;
				}
			}
		});
		return button;
	}
	
	public Behaviour getSelectedBehaviour() {
		return selectedBehaviour;
	}

	@Override
	protected Point getInitialSize() {
		Point point = super.getInitialSize();
		return new Point(750, point.y);
	}
	
	private String getExistingConnections() {
		List<String> connectionNames = new ArrayList<String>();
		existingDeployments.forEach(app -> {
			if (!connectionNames.contains(app.connection.getName())) {
				connectionNames.add(app.connection.getName());
			}
		});
		connectionNames.sort((String str1, String str2) -> str1.compareTo(str2));
		return CoreUtil.formatString(connectionNames.toArray(new String[connectionNames.size()]), ", ");
	}
}
