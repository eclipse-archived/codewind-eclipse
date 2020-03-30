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
import java.util.Collections;
import java.util.List;

import org.eclipse.codewind.core.internal.cli.LogLevels;
import org.eclipse.codewind.ui.CodewindUIPlugin;
import org.eclipse.codewind.ui.internal.messages.Messages;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

public class LogLevelSelectionDialog extends TitleAreaDialog {

	private final LogLevels logLevels;
	private String selectedLevel = null;
	private List<String> levelList;
	private List<String> levelLabels;
	private int currentLevel = -1;
	
	public LogLevelSelectionDialog(Shell parentShell, LogLevels logLevels) {
		super(parentShell);
		this.logLevels = logLevels;
		this.selectedLevel = logLevels.getCurrentLevel();
	}
	
	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(Messages.LogLevelDialogShell);
	}
	
	@Override
	protected boolean isResizable() {
		return true;
	}
	
	protected Control createDialogArea(Composite parent) {
		setTitleImage(CodewindUIPlugin.getImage(CodewindUIPlugin.CODEWIND_BANNER));
		setTitle(Messages.LogLevelDialogTitle);
		setMessage(Messages.LogLevelDialogMessage);
		
		final Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginHeight = 11;
		layout.marginWidth = 9;
		layout.horizontalSpacing = 5;
		layout.verticalSpacing = 7;
		layout.numColumns = 2;
		composite.setLayout(layout);
		GridData data = new GridData(GridData.FILL_BOTH);
		data.widthHint = 200;
		composite.setLayoutData(data);
		composite.setFont(parent.getFont());
		
		Label label = new Label(composite, SWT.NONE);
		label.setText(Messages.LogLevelDialogLogLabel);
		
		initLevels();
		
		Combo logLevelCombo = new Combo(composite, SWT.READ_ONLY);
		logLevelCombo.setItems(levelLabels.toArray(new String[levelLabels.size()]));
		logLevelCombo.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		logLevelCombo.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent event) {
				int index = logLevelCombo.getSelectionIndex();
				if (index >= 0)
					selectedLevel = levelList.get(index);
			}
		});

		// Add Context Sensitive Help
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, CodewindUIPlugin.MAIN_CONTEXTID);
		
		if (currentLevel >= 0) {
			logLevelCombo.select(currentLevel);
		}
		
		logLevelCombo.setFocus();
		
		return composite;
	}
	
	private void initLevels() {
		levelList = logLevels.getAllLevels();
		Collections.sort(levelList);
		levelLabels = new ArrayList<String>(levelList.size());
		for (int i = 0; i < levelList.size(); i++) {
			String level = levelList.get(i);
			String label = level;
			if (level.equals(logLevels.getDefaultLevel())) {
				label = label + " " + Messages.LogLevelDialogLogDefault;
			}
			if (level.equals(logLevels.getCurrentLevel())) {
				currentLevel = i;
			}
			levelLabels.add(label);
		}
	}
	
	public String getSelectedLevel() {
		return selectedLevel;
	}

	@Override
	protected Point getInitialSize() {
		Point point = super.getInitialSize();
		return new Point(750, point.y);
	}
}
