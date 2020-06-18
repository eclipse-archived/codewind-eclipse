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

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;

import org.eclipse.codewind.core.internal.HttpUtil;
import org.eclipse.codewind.core.internal.HttpUtil.HttpResult;
import org.eclipse.codewind.core.internal.IAuthInfo;
import org.eclipse.codewind.ui.CodewindUIPlugin;
import org.eclipse.codewind.ui.internal.IDEUtil;
import org.eclipse.codewind.ui.internal.messages.Messages;
import org.eclipse.codewind.ui.internal.wizards.CompositeContainer;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.osgi.util.NLS;
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

public class TemplateSourceURLPage extends WizardPage implements CompositeContainer {

	private Text urlText;
	private String urlValue;
	private boolean authRequiredValue = false;
	private TemplateSourceAuthComposite authComposite;
	private boolean isLogonMethod = true;
	private String username;
	private Button testButton;

	protected TemplateSourceURLPage(String shellTitle, String pageTitle) {
		super(shellTitle);
		setTitle(pageTitle);
		setDescription(Messages.AddRepoURLPageMessage);
	}
	
	protected TemplateSourceURLPage(String shellTitle, String pageTitle, String url, boolean requiresAuthentication, boolean isLogonMethod, String username) {
		this(shellTitle, pageTitle);
		this.urlValue = url;
		this.authRequiredValue = requiresAuthentication;
		this.isLogonMethod = isLogonMethod;
		this.username = username;
	}

	@Override
	public void createControl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.horizontalSpacing = 5;
		layout.verticalSpacing = 5;
		composite.setLayout(layout);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		composite.setLayoutData(data);

		// URL for the template source
		Label label = new Label(composite, SWT.NONE);
		label.setText(Messages.AddRepoDialogUrlLabel);
		label.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));

		urlText = new Text(composite, SWT.BORDER);
		urlText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		(new Label(composite, SWT.NONE)).setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 2, 1));
		
		Button authRequiredButton = new Button(composite, SWT.CHECK);
		authRequiredButton.setText(Messages.AddRepoDialogAuthRequiredCheckboxLabel);
		authRequiredButton.setToolTipText(Messages.AddRepoDialogAuthRequiredCheckboxTooltip);
		authRequiredButton.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false, 2, 1));
		
		authRequiredButton.setSelection(authRequiredValue);
		
		authComposite = new TemplateSourceAuthComposite(composite, this, isLogonMethod, username);
		authComposite.hideTestButton();
		authComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		
		(new Label(composite, SWT.NONE)).setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 2, 1));
		
		testButton = new Button(composite, SWT.PUSH);
		testButton.setText(Messages.AddRepoDialogTestButtonLabel);
		testButton.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, false, 2, 1));
		
		urlText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent arg0) {
				urlValue = IDEUtil.getTextValue(urlText);
				authComposite.updatePage(urlValue);
				validate();
			}
		});
		
		authRequiredButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				authRequiredValue = authRequiredButton.getSelection();
				authComposite.setCompositeEnabled(authRequiredValue);
				validate();
			}
		});
		
		testButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				IRunnableWithProgress runnable = new IRunnableWithProgress() {
					@Override
					public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
						try {
							SubMonitor.convert(monitor, NLS.bind(Messages.AddRepoDialogTestTaskLabel, urlValue), IProgressMonitor.UNKNOWN);
							URI uri = new URI(urlValue);
							HttpResult result = HttpUtil.get(uri, getAuthInfo());
							if (!result.isGoodResponse) {
								String errorMsg = result.error;
								if (errorMsg == null || errorMsg.trim().isEmpty()) {
									errorMsg = NLS.bind(Messages.AddRepoDialogTestFailedDefaultMsg, result.responseCode);
								}
								throw new InvocationTargetException(new IOException(errorMsg));
							}
						} catch (Exception e) {
							if (e instanceof InvocationTargetException) {
								throw (InvocationTargetException) e;
							}
							throw new InvocationTargetException(e, e.toString());
						}
					}
				};
				try {
					run(runnable);
					setErrorMessage(null);
					setMessage(Messages.AddRepoDialogTestSuccessMsg);
				} catch (Exception e) {
					String msg = e instanceof InvocationTargetException ? ((InvocationTargetException)e).getTargetException().toString() : e.toString();
					IDEUtil.openInfoDialog(Messages.AddRepoDialogTestFailedTitle, NLS.bind(Messages.AddRepoDialogTestFailedError, urlValue, msg));
					setErrorMessage(Messages.AddRepoDialogTestFailedMsg);
				}
			}
		});


		// Add Context Sensitive Help
		PlatformUI.getWorkbench().getHelpSystem().setHelp(composite, CodewindUIPlugin.MAIN_CONTEXTID);

		if (urlValue != null) {
			urlText.setText(urlValue);
		}
		authComposite.setCompositeEnabled(authRequiredValue);
		testButton.setEnabled(urlValue != null);
		setControl(composite);
	}

	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible) {
			urlText.setFocus();
		}
	}

	public void validate() {
		String errorMsg = null;
		if (urlValue == null) {
			errorMsg = Messages.AddRepoDialogNoUrl;
		}
		String authError = authComposite.validate();
		setMessage(Messages.AddRepoURLPageMessage);
		setErrorMessage(errorMsg != null ? errorMsg : authError);
		testButton.setEnabled(urlValue != null);
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
		return urlValue != null && (!authRequiredValue || authComposite.canFinish());
	}

	String getTemplateSourceUrl() {
		return urlValue;
	}
	
	boolean getAuthRequired() {
		return authRequiredValue;
	}

	String getUsername() {
		return authRequiredValue ? authComposite.getUsername() : null;
	}

	String getPassword() {
		return authRequiredValue ? authComposite.getPassword() : null;
	}

	String getToken() {
		return authRequiredValue ? authComposite.getToken() : null;
	}
	
	IAuthInfo getAuthInfo() {
		return authRequiredValue ? authComposite.getAuthInfo() : null;
	}
	
	@Override
	public void run(IRunnableWithProgress runnable) throws InvocationTargetException, InterruptedException {
		getWizard().getContainer().run(true, true, runnable);
	}
}
