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

import org.eclipse.codewind.ui.CodewindUIPlugin;
import org.eclipse.codewind.ui.internal.IDEUtil;
import org.eclipse.codewind.ui.internal.messages.Messages;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;

public class TemplateSourceURLPage extends WizardPage {

	private Text urlText;
	private String urlValue;
	private boolean authRequiredValue = false;

	protected TemplateSourceURLPage(String shellTitle, String pageTitle) {
		super(shellTitle);
		setTitle(pageTitle);
		setDescription(Messages.AddRepoURLPageMessage);
	}
	
	protected TemplateSourceURLPage(String shellTitle, String pageTitle, String url, boolean requiresAuthentication) {
		this(shellTitle, pageTitle);
		this.urlValue = url;
		this.authRequiredValue = requiresAuthentication;
	}

	@Override
	public void createControl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.horizontalSpacing = 5;
		layout.verticalSpacing = 15;
		composite.setLayout(layout);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		composite.setLayoutData(data);

		// URL for the template source
		Label label = new Label(composite, SWT.NONE);
		label.setText(Messages.AddRepoDialogUrlLabel);
		label.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));

		urlText = new Text(composite, SWT.BORDER);
		urlText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		urlText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent arg0) {
				validate();
			}
		});
		
		Button authRequiredButton = new Button(composite, SWT.CHECK);
		authRequiredButton.setText(Messages.AddRepoDialogAuthRequiredCheckboxLabel);
		authRequiredButton.setToolTipText(Messages.AddRepoDialogAuthRequiredCheckboxTooltip);
		authRequiredButton.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false, 2, 1));
		
		authRequiredButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				authRequiredValue = authRequiredButton.getSelection();
			}
		});
		
		authRequiredButton.setSelection(authRequiredValue);

		// Add Context Sensitive Help
		PlatformUI.getWorkbench().getHelpSystem().setHelp(composite, CodewindUIPlugin.MAIN_CONTEXTID);

		if (urlValue != null) {
			urlText.setText(urlValue);
		}
		setControl(composite);
	}

	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible) {
			urlText.setFocus();
		}
	}

	private void validate() {
		String errorMsg = null;
		urlValue = IDEUtil.getTextValue(urlText);
		if (urlValue == null) {
			errorMsg = Messages.AddRepoDialogNoUrl;
		}
		setErrorMessage(errorMsg);
		getContainer().updateButtons();
	}

	@Override
	public boolean canFlipToNextPage() {
		return canFinish();
	}

	boolean isActivePage() {
		return isCurrentPage();
	}

	boolean canFinish() {
		return urlValue != null;
	}

	String getTemplateSourceUrl() {
		return urlValue;
	}
	
	boolean getAuthRequired() {
		return authRequiredValue;
	}
}
