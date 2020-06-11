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

package org.eclipse.codewind.ui.internal.prefs;

import org.eclipse.codewind.core.internal.CodewindApplication;
import org.eclipse.codewind.ui.CodewindUIPlugin;
import org.eclipse.codewind.ui.internal.messages.Messages;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

public class LinkManagementDialog extends TitleAreaDialog {
	
	private final CodewindApplication srcApp;
	private LinkManagementComposite linkComposite;
	
	public LinkManagementDialog(Shell parentShell, CodewindApplication srcApp) {
		super(parentShell);
		this.srcApp = srcApp;
	}
	
	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(Messages.LinkMgmtDialogTitle);
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
		setTitle(Messages.LinkMgmtDialogTitle);
		setMessage(NLS.bind(Messages.LinkMgmtDialogMessage, srcApp.name));
		
		Composite content = (Composite) super.createDialogArea(parent);
		content.setLayout(new GridLayout());
		content.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true));
		
		linkComposite = new LinkManagementComposite(content, srcApp);
		GridData data = new GridData(GridData.FILL, GridData.FILL, true, true);
		data.widthHint = 250;
		linkComposite.setLayoutData(data);

		return parent; 
	}

	public boolean hasChanges() {
		return linkComposite.hasChanges();
	}
	
	public IStatus updateLinks(IProgressMonitor monitor) {
		return linkComposite.updateLinks(monitor);
	}
	
	@Override
	protected Point getInitialSize() {
		Point point = super.getInitialSize();
		return new Point(700, point.y);
	}
}
