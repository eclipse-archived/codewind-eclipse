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

import java.lang.reflect.InvocationTargetException;

import org.eclipse.codewind.core.internal.connection.CodewindConnection;
import org.eclipse.codewind.ui.CodewindUIPlugin;
import org.eclipse.codewind.ui.internal.messages.Messages;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.operation.ModalContext;
import org.eclipse.jface.wizard.ProgressMonitorPart;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

public class EditConnectionDialog extends TitleAreaDialog implements CompositeContainer {
	
	private final CodewindConnection connection;
	private CodewindConnectionComposite connectionComp;
	private ProgressMonitorPart progressMon;
	
	public EditConnectionDialog(Shell parentShell, CodewindConnection connection) {
		super(parentShell);
		this.connection = connection;
	}
	
	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(Messages.EditConnectionDialogShell);
	}
	
	@Override
	protected boolean isResizable() {
		return true;
	}
	
	protected Control createDialogArea(Composite parent) {
		setTitleImage(CodewindUIPlugin.getImage(CodewindUIPlugin.CODEWIND_BANNER));
		setTitle(Messages.EditConnectionDialogTitle);
		setMessage(NLS.bind(Messages.EditConnectionDialogMessage, connection.getName()));
		
		Composite content = (Composite) super.createDialogArea(parent);
		content.setLayout(new GridLayout(1, false));
		content.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true));
		
		connectionComp = new CodewindConnectionComposite(content, this, connection);
		connectionComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		layout.marginHeight = 10;
		layout.marginWidth = 20;
		progressMon = new ProgressMonitorPart(parent, layout, true);
		progressMon.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		progressMon.setVisible(false);
		
		return parent;
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		super.createButtonsForButtonBar(parent);
		getButton(IDialogConstants.OK_ID).setEnabled(false);
	}

	@Override
	protected void okPressed() {
		super.okPressed();
		Job job = new Job(NLS.bind(Messages.UpdateConnectionJobLabel, connectionComp.getConnectionName())) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				return connectionComp.updateConnection(monitor);
			}
		};
		job.schedule();
	}

	@Override
	public void validate() {
		connectionComp.validate();
		getButton(IDialogConstants.OK_ID).setEnabled(connectionComp.canFinish());
	}

	@Override
	public void run(IRunnableWithProgress runnable) throws InvocationTargetException, InterruptedException {
		progressMon.setVisible(true);
		try {
			ModalContext.run(runnable, true, progressMon, getShell().getDisplay());
		} finally {
			progressMon.done();
			progressMon.setVisible(false);
		}
	}
	
	@Override
	protected Point getInitialSize() {
		Point point = super.getInitialSize();
		return new Point(650, point.y);
	}
}
