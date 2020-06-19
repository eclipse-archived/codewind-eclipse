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
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.Arrays;
import java.util.Base64;

import org.eclipse.codewind.core.internal.HttpUtil;
import org.eclipse.codewind.core.internal.HttpUtil.HttpResult;
import org.eclipse.codewind.core.internal.IAuthInfo;
import org.eclipse.codewind.core.internal.Logger;
import org.eclipse.codewind.ui.CodewindUIPlugin;
import org.eclipse.codewind.ui.internal.IDEUtil;
import org.eclipse.codewind.ui.internal.messages.Messages;
import org.eclipse.codewind.ui.internal.wizards.CompositeContainer;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;

public class TemplateSourceAuthComposite extends Composite {

	private CompositeContainer container;
	private Button logonButton, tokenButton, testButton;
	private boolean isLogonMethod;
	private Composite logonComposite, tokenComposite;
	private Text usernameText, passwordText, tokenText;
	private String usernameValue, passwordValue, tokenValue;
	private String url;

	protected TemplateSourceAuthComposite(Composite parent, CompositeContainer container, boolean isLogonMethod, String username) {
		super(parent, SWT.NONE);
		this.container = container;
		this.isLogonMethod = isLogonMethod;
		this.usernameValue = username;
		createControl();
	}

	protected void createControl() {
		GridLayout layout = new GridLayout();
		layout.horizontalSpacing = 5;
		layout.verticalSpacing = 15;
		setLayout(layout);
		
		Group authGroup = new Group(this, SWT.NONE);
		authGroup.setText(Messages.AddRepoDialogAuthGroup);
		layout = new GridLayout();
		layout.horizontalSpacing = 5;
		layout.verticalSpacing = 15;
		layout.marginHeight = 15;
		authGroup.setLayout(layout);
		authGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		// Logon button and composite
		logonButton = new Button(authGroup, SWT.RADIO);
		logonButton.setText(Messages.AddRepoDialogLogonAuthButton);
		logonButton.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
		logonButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				setEnablement(logonButton.getSelection());
				container.validate();
			}
		});

		logonComposite = createSubComposite(authGroup, 2);

		Label label = new Label(logonComposite, SWT.NONE);
		label.setText(Messages.AddRepoDialogUsernameLabel);
		label.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));

		usernameText = new Text(logonComposite, SWT.BORDER);
		usernameText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		usernameText.addModifyListener((event) -> {
			usernameValue = IDEUtil.getTextValue(usernameText);
			container.validate();
		});

		label = new Label(logonComposite, SWT.NONE);
		label.setText(Messages.AddRepoDialogPasswordLabel);
		label.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));

		passwordText = new Text(logonComposite, SWT.BORDER | SWT.PASSWORD);
		passwordText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		passwordText.addModifyListener((event) -> {
			passwordValue = IDEUtil.getTextValue(passwordText);
			container.validate();
		});

		// Token button and composite
		tokenButton = new Button(authGroup, SWT.RADIO);
		tokenButton.setText(Messages.AddRepoDialogAccessTokenAuthButton);
		tokenButton.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
		tokenButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				setEnablement(!tokenButton.getSelection());
				container.validate();
			}
		});

		tokenComposite = createSubComposite(authGroup, 2);

		label = new Label(tokenComposite, SWT.NONE);
		label.setText(Messages.AddRepoDialogAccessTokenLabel);
		label.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));

		tokenText = new Text(tokenComposite, SWT.BORDER | SWT.PASSWORD);
		tokenText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		tokenText.addModifyListener((event) -> {
			tokenValue = IDEUtil.getTextValue(tokenText);
			container.validate();
		});
		
		testButton = new Button(this, SWT.PUSH);
		testButton.setText(Messages.AddRepoDialogAuthTestButtonLabel);
		testButton.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, false));
		
		testButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				IRunnableWithProgress runnable = new IRunnableWithProgress() {
					@Override
					public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
						try {
							SubMonitor.convert(monitor, NLS.bind(Messages.AddRepoDialogAuthTestTaskLabel, url), IProgressMonitor.UNKNOWN);
							URI uri = new URI(url);
							HttpResult result = HttpUtil.get(uri, getAuthInfo());
							if (!result.isGoodResponse) {
								String errorMsg = result.error;
								if (errorMsg == null || errorMsg.trim().isEmpty()) {
									errorMsg = NLS.bind(Messages.AddRepoDialogAuthTestFailedDefaultMsg, result.responseCode);
								}
								throw new InvocationTargetException(new IOException(errorMsg));
							}
						} catch (InvocationTargetException e) {
							throw e;
						} catch (Exception e) {
							throw new InvocationTargetException(e, e.toString());
						}
					}
				};
				try {
					container.run(runnable);
					container.setErrorMessage(null);
					container.setMessage(Messages.AddRepoDialogAuthTestSuccessMsg);
				} catch (Exception e) {
					String msg = e instanceof InvocationTargetException ? ((InvocationTargetException)e).getCause().toString() : e.toString();
					IDEUtil.openInfoDialog(Messages.AddRepoDialogAuthTestFailedTitle, NLS.bind(Messages.AddRepoDialogAuthTestFailedError, url, msg));
					container.setErrorMessage(Messages.AddRepoDialogAuthTestFailedMsg);
				}
			}
		});

		testButton.setEnabled(false);
		logonButton.setSelection(isLogonMethod);
		tokenButton.setSelection(!isLogonMethod);
		if (isLogonMethod && usernameValue != null) {
			usernameText.setText(usernameValue);
		}
		setEnablement(isLogonMethod);

		// Add Context Sensitive Help
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, CodewindUIPlugin.MAIN_CONTEXTID);
	}

	protected Composite createSubComposite(Composite parent, int numColumn) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = numColumn;
		layout.marginHeight = 2;
		layout.marginWidth = 10;
		layout.verticalSpacing = 5;
		layout.horizontalSpacing = 15;
		composite.setLayout(layout);
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		return composite;
	}

	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible) {
			if (logonButton.getSelection()) {
				usernameText.setFocus();
			} else {
				tokenText.setFocus();
			}
		}
	}
	
	void updatePage(String url) {
		this.url = url;
	}
	
	void hideTestButton() {
		testButton.setVisible(false);
		((GridData) testButton.getLayoutData()).exclude = true;
	}

	private void setEnablement(boolean isLogonMethod) {
		Arrays.stream(logonComposite.getChildren()).forEach(c -> c.setEnabled(isLogonMethod));
		Arrays.stream(tokenComposite.getChildren()).forEach(c -> c.setEnabled(!isLogonMethod));
	}

	String validate() {
		String errorMsg = null;
		if (isCompositeEnabled()) {
			isLogonMethod = logonButton.getSelection();
			if (isLogonMethod) {
				if (usernameValue == null) {
					errorMsg = Messages.AddRepoDialogNoUsername;
				} else if (passwordValue == null) {
					errorMsg = Messages.AddRepoDialogNoPassword;
				}
			} else {
				if (tokenValue == null) {
					errorMsg = Messages.AddRepoDialogNoAccessToken;
				}
			}
			testButton.setEnabled(url != null && errorMsg == null);
		}
		
		return errorMsg;
	}

	boolean canFinish() {
		if (isLogonMethod) {
			return usernameValue != null && passwordValue != null;
		}
		return tokenValue != null;
	}

	String getUsername() {
		return isLogonMethod ? usernameValue : null;
	}

	String getPassword() {
		return isLogonMethod ? passwordValue : null;
	}

	String getToken() {
		return !isLogonMethod ? tokenValue : null;
	}
	
	IAuthInfo getAuthInfo() {
		return isLogonMethod ? new LogonAuth(usernameValue, passwordValue) : new TokenAuth(tokenValue);
	}
	
	private boolean isCompositeEnabled() {
		return logonButton.isEnabled();
	}
	
	void setCompositeEnabled(boolean enabled) {
		if (enabled) {
			logonButton.setEnabled(true);
			tokenButton.setEnabled(true);
			setEnablement(isLogonMethod);
		} else {
			disableAll(this);
		}
	}
	
	private void disableAll(Control control) {
		if (control instanceof Composite) {
			Arrays.stream(((Composite) control).getChildren()).forEach(c -> disableAll(c));
		} else {
			control.setEnabled(false);
		}
	}
	
	public static class LogonAuth implements IAuthInfo {
		
		private final String username;
		private final String password;
		
		public LogonAuth(String username, String password) {
			this.username = username;
			this.password = password;
		}

		@Override
		public boolean isValid() {
			return username != null && password != null;
		}

		@Override
		public String getHttpAuthorization() {
			try {
				String auth = username + ":" + password;
				return "Basic " + Base64.getEncoder().encodeToString(auth.getBytes("UTF-8"));
			} catch (UnsupportedEncodingException e) {
				Logger.logError("An unsupported encoding exception occurred trying to encode the logon authentication."); //$NON-NLS-1$
			}
			return null;
		}
	}
	
	public static class TokenAuth implements IAuthInfo {
		
		private final String token;
		
		public TokenAuth(String token) {
			this.token = token;
		}
		
		@Override
		public boolean isValid() {
			return token != null;
		}

		@Override
		public String getHttpAuthorization() {
			return "bearer " + token;
		}

	}
}
