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

import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;

import org.eclipse.codewind.core.internal.CodewindObjectFactory;
import org.eclipse.codewind.core.internal.Logger;
import org.eclipse.codewind.core.internal.connection.CodewindConnection;
import org.eclipse.codewind.core.internal.connection.CodewindConnectionManager;
import org.eclipse.codewind.ui.internal.IDEUtil;
import org.eclipse.codewind.ui.internal.messages.Messages;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
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
        layout.horizontalSpacing = 8;
        layout.verticalSpacing = 20;
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
        layout.marginHeight = 8;
		layout.marginWidth = 8;
		layout.horizontalSpacing = 7;
		layout.verticalSpacing = 7;
        connGroup.setLayout(layout);
        data = new GridData(SWT.FILL, SWT.BEGINNING, true, false, 2, 1);
        connGroup.setLayoutData(data);
        connGroup.setText("Deployment");
        
        Text connGroupLabel = new Text(connGroup, SWT.READ_ONLY);
        connGroupLabel.setText("Fill in the deployment information:");
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
				getWizard().getContainer().updateButtons();
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

		if(connection != null && connection.isConnected()) {
			setErrorMessage(null);
			setMessage(NLS.bind(Messages.NewConnectionPage_ConnectSucceeded, connection.getBaseURI()));
		} else {
			setErrorMessage(NLS.bind(Messages.NewConnectionPage_ErrCouldNotConnect, uri));
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
		Exception exception = null;
		final Boolean[] isCanceled = new Boolean[] {false};
		CodewindConnection conn = CodewindObjectFactory.createCodewindConnection(name, uri, false);
		
		IRunnableWithProgress runnable = new IRunnableWithProgress() {
			@Override
			public void run(IProgressMonitor monitor) throws InvocationTargetException {
				try {
					SubMonitor mon = SubMonitor.convert(monitor, 100);
					mon.setTaskName(NLS.bind(Messages.NewConnectionPage_TestConnectionJobLabel, uri));
					conn.connect(mon.split(100));
				} catch (Exception e) {
					throw new InvocationTargetException(e, "An error occurred trying to connect to Codewind: " + e.getMessage()); //$NON-NLS-1$
				}
				isCanceled[0] = monitor.isCanceled();
			}
		};
		try {
			getWizard().getContainer().run(true, true, runnable);
		} catch (InvocationTargetException e) {
			Logger.logError("An error occurred trying to connect to Codewind at: " + uri, e); //$NON-NLS-1$
			exception = e;
		} catch (InterruptedException e) {
			Logger.logError("Codewind connect was interrupted", e); //$NON-NLS-1$
			exception = e;
		}

		if (isCanceled[0]) {
			return null;
		}
		if (!conn.isConnected()) {
			if (exception != null) {
				Logger.logError("Failed to connect to Codewind at: " + uri.toString(), exception); //$NON-NLS-1$
				IStatus errorStatus = IDEUtil.getMultiStatus("An error occurred trying to connect to Codewind at: " + uri, exception);
				ErrorDialog.openError(getShell(), "Codewind Connect Error", "An error occurred trying to connect to Codewind at: " + uri, errorStatus);
			} else {
				// This should not happen as there should be an exception
				Logger.logError("Failed to connect to Codewind at: " + uri.toString());
				MessageDialog.openError(getShell(), "Codewind Connect Error", "Connecting to Codewind was not successful. Check workspace logs for details.");
			}
		}
		return conn;
	}
}
