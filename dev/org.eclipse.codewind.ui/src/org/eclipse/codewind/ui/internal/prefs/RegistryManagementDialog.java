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

package org.eclipse.codewind.ui.internal.prefs;

import java.util.List;

import org.eclipse.codewind.core.internal.connection.CodewindConnection;
import org.eclipse.codewind.core.internal.connection.RegistryInfo;
import org.eclipse.codewind.ui.CodewindUIPlugin;
import org.eclipse.codewind.ui.internal.messages.Messages;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

public class RegistryManagementDialog extends TitleAreaDialog {
	
	private final CodewindConnection connection;
	private final List<RegistryInfo> regList;
	private RegistryManagementComposite regComposite;
	
	public RegistryManagementDialog(Shell parentShell, CodewindConnection connection, List<RegistryInfo> regList) {
		super(parentShell);
		this.connection = connection;
		this.regList = regList;
	}
	
	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(Messages.RegMgmtDialogTitle);
	}
	
	@Override
	protected boolean isResizable() {
		return true;
	}

	@Override
	protected Control createButtonBar(Composite parent) {
		return super.createButtonBar(parent);
	}

	protected Control createDialogArea(Composite parent) {
		setTitleImage(CodewindUIPlugin.getImage(CodewindUIPlugin.CODEWIND_BANNER));
		setTitle(Messages.RegMgmtDialogTitle);
		if (connection.isLocal()) {
			setMessage(Messages.RegMgmtDialogLocalMessage);
		} else {
			setMessage(Messages.RegMgmtDialogMessage);
		}
		
		Composite content = (Composite) super.createDialogArea(parent);
		content.setLayout(new GridLayout());
		content.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true));
		
		regComposite = new RegistryManagementComposite(content, connection, regList);
		GridData data = new GridData(GridData.FILL, GridData.FILL, true, true);
		data.widthHint = 250;
		regComposite.setLayoutData(data);

		return parent; 
	}

	public boolean hasChanges() {
		return regComposite.hasChanges();
	}
	
	public IStatus updateRegistries(IProgressMonitor monitor) {
		return regComposite.updateRegistries(monitor);
	}
	
	@Override
	protected Point getInitialSize() {
		Point point = super.getInitialSize();
		return new Point(700, point.y);
	}
}
