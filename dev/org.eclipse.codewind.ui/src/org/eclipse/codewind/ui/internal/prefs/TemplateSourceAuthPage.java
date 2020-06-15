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

package org.eclipse.codewind.ui.internal.prefs;

import java.lang.reflect.InvocationTargetException;
import java.net.URI;

import org.eclipse.codewind.core.internal.IAuthInfo;
import org.eclipse.codewind.ui.internal.messages.Messages;
import org.eclipse.codewind.ui.internal.wizards.CompositeContainer;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

public class TemplateSourceAuthPage extends WizardPage implements CompositeContainer {

	private TemplateSourceAuthComposite composite;
	private boolean isLogonMethod = true;
	private String username;

	protected TemplateSourceAuthPage(String shellTitle, String pageTitle) {
		super(shellTitle);
		setTitle(pageTitle);
		setDescription(Messages.AddRepoAuthPageMessage);
	}
	
	protected TemplateSourceAuthPage(String shellTitle, String pageTitle, boolean isLogonMethod, String username) {
		this(shellTitle, pageTitle);
		this.isLogonMethod = isLogonMethod;
		this.username = username;
	}

	@Override
	public void createControl(Composite parent) {
		Composite outer = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		layout.horizontalSpacing = 5;
		layout.verticalSpacing = 7;
		outer.setLayout(layout);
		outer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		composite = new TemplateSourceAuthComposite(outer, this, isLogonMethod, username);
		GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
		data.widthHint = 250;
		composite.setLayoutData(data);
		setControl(outer);
	}

	void updatePage(URI uri) {
		composite.updatePage(uri);
	}

	@Override
	public boolean canFlipToNextPage() {
		return canFinish();
	}

	boolean isActivePage() {
		return isCurrentPage();
	}

	boolean canFinish() {
		return composite.canFinish();
	}

	boolean isLogonMethod() {
		return composite.isLogonMethod();
	}

	String getUsername() {
		return composite.getUsername();
	}

	String getPassword() {
		return composite.getPassword();
	}

	String getToken() {
		return composite.getToken();
	}
	
	IAuthInfo getAuthInfo() {
		return composite.getAuthInfo();
	}
	
	@Override
	public void update() {
		getWizard().getContainer().updateButtons();
	}
	
	@Override
	public void run(IRunnableWithProgress runnable) throws InvocationTargetException, InterruptedException {
		getWizard().getContainer().run(true, true, runnable);
	}
}
