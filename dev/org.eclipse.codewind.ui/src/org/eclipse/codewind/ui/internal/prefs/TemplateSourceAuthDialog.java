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

import java.lang.reflect.InvocationTargetException;
import java.net.URI;

import org.eclipse.codewind.core.internal.connection.RepositoryInfo;
import org.eclipse.codewind.ui.CodewindUIPlugin;
import org.eclipse.codewind.ui.internal.IDEUtil;
import org.eclipse.codewind.ui.internal.messages.Messages;
import org.eclipse.codewind.ui.internal.wizards.CompositeContainer;
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
import org.eclipse.swt.widgets.Text;

public class TemplateSourceAuthDialog extends TitleAreaDialog implements CompositeContainer {
	
	private URI uri;
	private RepositoryInfo repo;
	private TemplateSourceAuthComposite composite;
	private ProgressMonitorPart progressMon;
	
	public TemplateSourceAuthDialog(Shell parentShell, URI uri, RepositoryInfo repo) {
		super(parentShell);
		this.uri = uri;
		this.repo = repo;
	}
	
	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(Messages.RepoAuthDialogShell);
	}
	
	@Override
	protected boolean isResizable() {
		return true;
	}
	
	protected Control createDialogArea(Composite parent) {
		setTitleImage(CodewindUIPlugin.getImage(CodewindUIPlugin.CODEWIND_BANNER));
		setTitle(Messages.RepoAuthDialogTitle);
		setMessage(Messages.RepoAuthDialogMsg);
		
		Composite content = (Composite) super.createDialogArea(parent);
		content.setLayout(new GridLayout(1, false));
		content.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true));
		
		Text descriptionText = new Text(content, SWT.READ_ONLY | SWT.MULTI | SWT.WRAP);
		descriptionText.setText(NLS.bind(Messages.RepoAuthDialogDescription, uri.toString()));
		descriptionText.setLayoutData(new GridData(SWT.BEGINNING, SWT.FILL, true, false));
		IDEUtil.normalizeBackground(descriptionText, content);
		
		String username = repo.getUsername();
		composite = new TemplateSourceAuthComposite(content, this, !repo.hasAuthentication() || username != null, username);
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		layout.marginHeight = 10;
		layout.marginWidth = 20;
		progressMon = new ProgressMonitorPart(parent, layout, true);
		progressMon.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		progressMon.setVisible(false);
		
		composite.updatePage(uri.toString());
		
		return parent;
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		super.createButtonsForButtonBar(parent);
		getButton(IDialogConstants.OK_ID).setEnabled(false);
	}

	@Override
	public void validate() {
		setMessage(Messages.RepoAuthDialogMsg);
		setErrorMessage(composite.validate());
		getButton(IDialogConstants.OK_ID).setEnabled(composite.canFinish());
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
		return new Point(650, point.y + 100);
	}
	
	public String getUsername() {
		return composite.getUsername();
	}

	public String getPassword() {
		return composite.getPassword();
	}

	public String getToken() {
		return composite.getToken();
	}
}
