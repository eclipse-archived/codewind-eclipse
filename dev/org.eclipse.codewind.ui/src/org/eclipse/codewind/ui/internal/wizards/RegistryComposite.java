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

package org.eclipse.codewind.ui.internal.wizards;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;

import org.eclipse.codewind.core.internal.Logger;
import org.eclipse.codewind.core.internal.connection.CodewindConnection;
import org.eclipse.codewind.ui.internal.IDEUtil;
import org.eclipse.codewind.ui.internal.messages.Messages;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
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
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.json.JSONException;

public class RegistryComposite extends Composite {
	
	private CompositeContainer container;
	private CodewindConnection connection;

	private Group regGroup;
	private Text regURLText, regUserText, regPassText;
	private Button regTestButton;
	private boolean regTested = false;

	public RegistryComposite(Composite parent, CompositeContainer container) {
		super(parent, SWT.NONE);
		this.container = container;
		createControl();
	}
	
	protected void createControl() {
        GridLayout layout = new GridLayout();
        layout.horizontalSpacing = 8;
        layout.verticalSpacing = 20;
        this.setLayout(layout);
        
        regGroup = new Group(this, SWT.NONE);
        layout = new GridLayout();
        layout.numColumns = 2;
        layout.marginHeight = 8;
		layout.marginWidth = 8;
		layout.horizontalSpacing = 7;
		layout.verticalSpacing = 7;
		regGroup.setLayout(layout);
        GridData data = new GridData(SWT.FILL, SWT.BEGINNING, true, false);
        regGroup.setLayoutData(data);
        regGroup.setText(Messages.CodewindConnectionComposite_RegDetailsGroup);
        
        Text regGroupLabel = new Text(regGroup, SWT.READ_ONLY | SWT.WRAP | SWT.MULTI);
        regGroupLabel.setText(Messages.CodewindConnectionComposite_RegDetailsInstructions);
        data = new GridData(SWT.FILL, SWT.CENTER, false, false, 2, 1);
        data.widthHint = 250;
        regGroupLabel.setLayoutData(data);
        IDEUtil.normalizeBackground(regGroupLabel, this);
        
        createLabel(Messages.CodewindConnectionComposite_UrlLabel, regGroup, 1, 15);
        regURLText = createRegText(regGroup, SWT.NONE, 1);
        
        createLabel(Messages.CodewindConnectionComposite_UserLabel, regGroup, 1, 15);
        regUserText = createRegText(regGroup, SWT.NONE, 1);
        
        createLabel(Messages.CodewindConnectionComposite_PasswordLabel, regGroup, 1, 15);
        regPassText = createRegText(regGroup, SWT.PASSWORD, 1);
        
        regTestButton = new Button(regGroup, SWT.PUSH);
        regTestButton.setText(Messages.CodewindConnectionComposite_TestRegButton);
        regTestButton.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, false, 2, 1));
        regTestButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent se) {
				testRegistry();
				validateAndUpdate();
			}
		});
        
        regTestButton.setEnabled(false);
        regURLText.setFocus();
	}

	private void createLabel(String labelStr, Composite parent, int horizontalSpan, int horizontalIndent) {
		Label label = new Label(parent, SWT.NONE);
		label.setText(labelStr);
		GridData data = new GridData(SWT.FILL, SWT.CENTER, false, false, horizontalSpan, 1);
		data.horizontalIndent = horizontalIndent;
		label.setLayoutData(data);
	}
	
	private Text createRegText(Composite parent, int styles, int horizontalSpan) {
		Text text = new Text(parent, SWT.BORDER | styles);
		text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, horizontalSpan, 1));
		text.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent arg0) {
				regTested = false;
				validateAndUpdate();
			}
		});
		return text;
	}
	
	private void validateAndUpdate() {
		validate();
		container.update();
	}
	
	private void validate() {
		String errorMsg = validateRegistryInfo();
		container.setErrorMessage(errorMsg);
	}

	public String validateRegistryInfo() {
		// Check that the url is valid
		String url = regURLText.getText().trim();
		if (!url.isEmpty()) {
			try {
				new URI(url);
			} catch (URISyntaxException e) {
				regTestButton.setEnabled(false);
				return NLS.bind(Messages.CodewindConnectionComposite_InvalidUrlError, url);
			}
		}

		// Check that all of the registry fields are filled in
		String user = regUserText.getText().trim();
		String pass = regPassText.getText().trim();
		if (url.isEmpty() || user.isEmpty() || pass.isEmpty()) {
			regTestButton.setEnabled(false);
			return Messages.CodewindConnectionComposite_MissingRegDetailsError;
		}
		
		regTestButton.setEnabled(true);
		return null;
	}

	public boolean canFinish() {
		return regTested;
	}

	private boolean testRegistry() {
		if (connection == null || !connection.isConnected()) {
			// This should not happen since the button should not be enabled in this case
			Logger.logError("Registry test requested but connection is null or not connected");;
			return false;
		}
		
		String urlStr = regURLText.getText().trim();
		String msg[] = new String[1];
		IRunnableWithProgress runnable = new IRunnableWithProgress() {
			@Override
			public void run(IProgressMonitor monitor) throws InvocationTargetException {
				try {
					msg[0] = connection.requestRegistryTest(urlStr);
				} catch (Exception e) {
					throw new InvocationTargetException(e, NLS.bind(Messages.CodewindConnectionComposite_RegErrorMsg, urlStr));
				}
			}
		};
		
		Throwable exception = null;
		try {
			container.run(runnable);
		} catch (InvocationTargetException e) {
			Logger.logError("An error occurred trying to test the registry: " + urlStr, e); //$NON-NLS-1$
			exception = e.getCause();
		} catch (InterruptedException e) {
			Logger.logError("Registry test was interrupted for: " + urlStr, e); //$NON-NLS-1$
			exception = e;
		}
		
		if (exception != null) {
			Logger.logError("Registry test failed for: " + urlStr, exception); //$NON-NLS-1$
			IStatus errorStatus = IDEUtil.getMultiStatus(NLS.bind(Messages.CodewindConnectionComposite_RegErrorMsg, urlStr), exception);
			ErrorDialog.openError(getShell(), Messages.CodewindConnectionComposite_ConnErrorTitle, NLS.bind(Messages.CodewindConnectionComposite_RegErrorMsg, urlStr), errorStatus);
			return false;
		} else if (msg[0] != null) {
			Logger.logError("Registry test failed for: " + urlStr + ", with message: " + msg[0]); //$NON-NLS-1$ //$NON-NLS-2$
			MessageDialog.openError(getShell(), Messages.CodewindConnectionComposite_RegErrorTitle, NLS.bind(Messages.CodewindConnectionComposite_RegFailed, msg[0]));
			return false;
		}
		
		return true;
	}
	
	public void setRegistry() throws IOException, JSONException {
		String urlStr = regURLText.getText().trim();
		connection.requestRegistrySet(urlStr);
	}
}
