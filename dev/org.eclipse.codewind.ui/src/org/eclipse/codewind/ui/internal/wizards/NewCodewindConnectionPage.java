/*******************************************************************************
 * Copyright (c) 2018, 2019 IBM Corporation and others.
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
import java.util.concurrent.TimeoutException;

import org.eclipse.codewind.core.internal.CodewindManager;
import org.eclipse.codewind.core.internal.CodewindObjectFactory;
import org.eclipse.codewind.core.internal.InstallStatus;
import org.eclipse.codewind.core.internal.InstallUtil;
import org.eclipse.codewind.core.internal.Logger;
import org.eclipse.codewind.core.internal.ProcessHelper.ProcessResult;
import org.eclipse.codewind.core.internal.connection.CodewindConnection;
import org.eclipse.codewind.core.internal.connection.CodewindConnectionManager;
import org.eclipse.codewind.ui.internal.IDEUtil;
import org.eclipse.codewind.ui.internal.messages.Messages;
import org.eclipse.core.runtime.IProgressMonitor;
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
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/**
 * This simple page allows the user to add new Codewind connections, by entering a hostname and port and
 * validating that Codewind is indeed reachable at the given address.
 */
public class NewCodewindConnectionPage extends WizardPage {

	private Text connNameText;
	private Text connURLText, connUserText, connPassText;
	private Button connTestButton;

	private CodewindConnection connection;

	protected NewCodewindConnectionPage() {
		super(Messages.NewConnectionPage_ShellTitle);
		setTitle(Messages.NewConnectionPage_WizardTitle);
		setDescription(Messages.NewConnectionPage_WizardDescription);
	}

	@Override
	public void createControl(Composite parent) {
		GridData data;
		
		Composite composite = new Composite(parent, SWT.NULL);
        GridLayout layout = new GridLayout();
        layout.numColumns = 2;
        layout.horizontalSpacing = 5;
        layout.verticalSpacing = 7;
        composite.setLayout(layout);
        composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL));
        
        createLabel("Connection name:", composite, 1, 0);
        connNameText = new Text(composite, SWT.BORDER);
        connNameText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        connNameText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent arg0) {
				validateAndUpdate();
			}
		});
        
        Group connGroup = new Group(composite, SWT.NONE);
        layout = new GridLayout();
        layout.numColumns = 2;
        layout.marginHeight = 0;
        layout.horizontalSpacing = 5;
        layout.verticalSpacing = 7;
        connGroup.setLayout(layout);
        data = new GridData(SWT.FILL, SWT.BEGINNING, true, false, 2, 1);
        connGroup.setLayoutData(data);
        
        Text connGroupLabel = new Text(connGroup, SWT.READ_ONLY);
        connGroupLabel.setText("Deployment Info:");
        connGroupLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 2, 1));
        IDEUtil.normalizeBackground(connGroupLabel, composite);
       
        createLabel("URL:", connGroup, 1, 15);
        connURLText = createConnText(connGroup, SWT.NONE, 1);
        
        createLabel("User name:", connGroup, 1, 15);
        connUserText = createConnText(connGroup, SWT.NONE, 1);
        
        createLabel("Password:", connGroup, 1, 15);
        connPassText = createConnText(connGroup, SWT.PASSWORD, 1);
        
        connTestButton = new Button(connGroup, SWT.PUSH);
        connTestButton.setText("Test Connection");
        connTestButton.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, false, 2, 1));
        connTestButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent se) {
				testConnection();
				validateAndUpdate();
			}
		});
        
        connTestButton.setEnabled(false);
        connNameText.setFocus();
		setControl(composite);
	}
	
	private void createLabel(String labelStr, Composite parent, int horizontalSpan, int horizontalIndent) {
		Label label = new Label(parent, SWT.NONE);
		label.setText(labelStr);
		GridData data = new GridData(SWT.FILL, SWT.CENTER, false, false, horizontalSpan, 1);
		data.horizontalIndent = horizontalIndent;
		label.setLayoutData(data);
	}

	private Text createConnText(Composite parent, int styles, int horizontalSpan) {
		Text text = new Text(parent, SWT.BORDER | styles);
		text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, horizontalSpan, 1));
		text.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent arg0) {
				removePreviousConnection();
				validateAndUpdate();
			}
		});
		return text;
	}
	
	private void validateAndUpdate() {
		validate();
		getWizard().getContainer().updateButtons();
	}
	
	private void validate() {
		
		// Check that connection name is set
		String name = connNameText.getText().trim();
		if (name.isEmpty()) {
			setErrorMessage("Fill in a name for the connection.");
			return;
		}
		
		// Check that the connection name is not already used
		CodewindConnection existingConnection = CodewindConnectionManager.getActiveConnectionByName(name);
		if (existingConnection != null) {
			setErrorMessage("The name " + name + " is already used for an existing connection");
			return;
		}
		
		// Check that the url is valid and not already used
		String url = connURLText.getText().trim();
		if (!url.isEmpty()) { //$NON-NLS-1$
			try {
				new URI(url);
			} catch (URISyntaxException e) {
				connTestButton.setEnabled(false);
				setErrorMessage("The url is not valid: " + url);
				return;
			}
			existingConnection = CodewindConnectionManager.getActiveConnection(url.endsWith("/") ? url : url + "/");
			if (existingConnection != null) {
				connTestButton.setEnabled(false);
				setErrorMessage("The " + existingConnection.getName() + " connection is already using url: " + url);
				return;
			}
		}

		// Check that all of the connection fields are filled in
		String user = connUserText.getText().trim();
		String pass = connPassText.getText().trim();
		if (url.isEmpty() || user.isEmpty() || pass.isEmpty()) {
			connTestButton.setEnabled(false);
			setErrorMessage("Fill in all of the deployment info fields.");
			return;
		}
		
		connTestButton.setEnabled(true);
		if (connection == null) {
			setErrorMessage("Click `Test Connection` to validate the deployment info.");
			return;
		}
		
		setErrorMessage(null);
	}
	
	@Override
	public boolean canFlipToNextPage() {
		return canFinish();
	}
	
	public boolean canFinish() {
		return connection != null && connection.isConnected();
	}

	void removePreviousConnection() {
		if (connection != null) {
			connection.close();
		}
		connection = null;
	}

	void testConnection() {
		removePreviousConnection();

		// Try to connect to Codewind at the given hostname:port
		String urlStr = connURLText.getText().trim();

		URI uri = null;
		try {
			uri = new URI(urlStr);
		} catch(URISyntaxException e) {
			Logger.logError(e);
			setErrorMessage(e.getMessage());
		}

		if (uri == null) {
			return;
		}

		Logger.log("Validating connection: " + uri); //$NON-NLS-1$

		connection = createConnection(connNameText.getText().trim(), uri);

		if(connection != null) {
			setErrorMessage(null);
			setMessage(NLS.bind(Messages.NewConnectionPage_ConnectSucceeded, connection.baseUrl));
		} else {
			setErrorMessage(NLS.bind(Messages.NewConnectionPage_ErrCouldNotConnectToMC, uri));
		}

		getWizard().getContainer().updateButtons();
	}

	/**
	 * Test canFinish before calling this to make sure it will never return null.
	 */
	CodewindConnection getConnection() {
		return connection;
	}

	void performFinish() {
		if (connection != null) {
			// In case the name was changed after the connection was tested
			connection.setName(connNameText.getText().trim());
			
			CodewindConnectionManager.add(connection);
		}
	}
	
	private CodewindConnection createConnection(String name, URI uri) {
		try {
			return CodewindObjectFactory.createCodewindConnection(name, uri, false);
		} catch (Exception e) {
			// Ignore
		}
		
		IRunnableWithProgress runnable = new IRunnableWithProgress() {
			@Override
			public void run(IProgressMonitor monitor) throws InvocationTargetException {
				try {
					InstallStatus status = CodewindManager.getManager().getInstallStatus();
					ProcessResult result = InstallUtil.startCodewind(status.getVersion(), monitor);
					if (result.getExitValue() != 0) {
						Logger.logError("Installer start failed with return code: " + result.getExitValue() + ", output: " + result.getOutput() + ", error: " + result.getError()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						String errorText = result.getError() != null && !result.getError().isEmpty() ? result.getError() : result.getOutput();
						throw new InvocationTargetException(null, "There was a problem while trying to start Codewind: " + errorText); //$NON-NLS-1$
					}
				} catch (IOException e) {
					throw new InvocationTargetException(e, "An error occurred trying to start Codewind: " + e.getMessage()); //$NON-NLS-1$
				} catch (TimeoutException e) {
					throw new InvocationTargetException(e, "Codewind did not start in the expected time: " + e.getMessage()); //$NON-NLS-1$
				}
			}
		};
		try {
			getWizard().getContainer().run(true, true, runnable);
		} catch (InvocationTargetException e) {
			Logger.logError("An error occurred trying to start Codewind", e); //$NON-NLS-1$
			return null;
		} catch (InterruptedException e) {
			Logger.logError("Codewind start was interrupted", e); //$NON-NLS-1$
			return null;
		}

		// Try again to create a connection
		CodewindConnection connection = null;
		for (int i = 0; i < 10; i++) {
			try {
				connection = CodewindObjectFactory.createCodewindConnection(name, uri, false);
				break;
			} catch (Exception e) {
				try {
					Thread.sleep(500);
				} catch (InterruptedException e1) {
					// Ignore
				}
			}
		}
		if (connection == null) {
			Logger.logError("Failed to connect to Codewind at: " + uri.toString()); //$NON-NLS-1$
		}
		return connection;
	}
}
