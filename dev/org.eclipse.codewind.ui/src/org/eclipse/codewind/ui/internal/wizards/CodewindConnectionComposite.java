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

import org.eclipse.codewind.core.internal.CodewindObjectFactory;
import org.eclipse.codewind.core.internal.Logger;
import org.eclipse.codewind.core.internal.cli.AuthToken;
import org.eclipse.codewind.core.internal.cli.AuthUtil;
import org.eclipse.codewind.core.internal.cli.ConnectionUtil;
import org.eclipse.codewind.core.internal.connection.CodewindConnection;
import org.eclipse.codewind.core.internal.connection.CodewindConnectionManager;
import org.eclipse.codewind.ui.internal.IDEUtil;
import org.eclipse.codewind.ui.internal.messages.Messages;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
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

public class CodewindConnectionComposite extends Composite {
	
	private Container container;
	private CodewindConnection connection;

	private Text connNameText;
	private Text connURLText, connUserText, connPassText;
	private Button connTestButton;
	
	private Group regGroup;
	private Text regURLText, regUserText, regPassText;
	private Button regTestButton;
	private boolean regTested = false;

	public CodewindConnectionComposite(Composite parent, Container container) {
		super(parent, SWT.NONE);
		this.container = container;
		createControl();
	}
	
	protected void createControl() {
        GridLayout layout = new GridLayout();
        layout.horizontalSpacing = 8;
        layout.verticalSpacing = 20;
        this.setLayout(layout);
        
        Group connGroup = new Group(this, SWT.NONE);
        layout = new GridLayout();
        layout.numColumns = 2;
        layout.marginHeight = 8;
		layout.marginWidth = 8;
		layout.horizontalSpacing = 7;
		layout.verticalSpacing = 7;
        connGroup.setLayout(layout);
        GridData data = new GridData(SWT.FILL, SWT.BEGINNING, true, false);
        connGroup.setLayoutData(data);
        connGroup.setText(Messages.CodewindConnectionComposite_ConnDetailsGroup);
        
        Text connGroupLabel = new Text(connGroup, SWT.READ_ONLY | SWT.WRAP | SWT.MULTI);
        connGroupLabel.setText(Messages.CodewindConnectionComposite_ConnDetailsInstructions);
        data = new GridData(SWT.FILL, SWT.CENTER, false, false, 2, 1);
        data.widthHint = 250;
        connGroupLabel.setLayoutData(data);
        IDEUtil.normalizeBackground(connGroupLabel, this);
        
        createLabel(Messages.CodewindConnectionComposite_ConnNameLabel, connGroup, 1, 15);
        connNameText = createConnText(connGroup, SWT.NONE, 1);
       
        createLabel(Messages.CodewindConnectionComposite_UrlLabel, connGroup, 1, 15);
        connURLText = createConnText(connGroup, SWT.NONE, 1);
        
        createLabel(Messages.CodewindConnectionComposite_UserLabel, connGroup, 1, 15);
        connUserText = createConnText(connGroup, SWT.NONE, 1);
        
        createLabel(Messages.CodewindConnectionComposite_PasswordLabel, connGroup, 1, 15);
        connPassText = createConnText(connGroup, SWT.PASSWORD, 1);
        
        connTestButton = new Button(connGroup, SWT.PUSH);
        connTestButton.setText(Messages.CodewindConnectionComposite_TestConnButton);
        connTestButton.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, false, 2, 1));
        connTestButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent se) {
				testConnection();
				validateAndUpdate();
			}
		});
        
        regGroup = new Group(this, SWT.NONE);
        layout = new GridLayout();
        layout.numColumns = 2;
        layout.marginHeight = 8;
		layout.marginWidth = 8;
		layout.horizontalSpacing = 7;
		layout.verticalSpacing = 7;
		regGroup.setLayout(layout);
        data = new GridData(SWT.FILL, SWT.BEGINNING, true, false);
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
        
        connTestButton.setEnabled(false);
        regTestButton.setEnabled(false);
        connNameText.setFocus();
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
		String errorMsg = null;
		errorMsg = validateConnectionInfo();
		if (errorMsg == null) {
			errorMsg = validateRegistryInfo();
		} else {
			// Disable the registry test button if the connection is not set up
			regTestButton.setEnabled(false);
		}
		container.setErrorMessage(errorMsg);
	}
	
	private String validateConnectionInfo() {
		// Check that connection name is set
		String name = connNameText.getText().trim();
		if (name == null || name.isEmpty()) {
			connTestButton.setEnabled(false);
			return Messages.CodewindConnectionComposite_NoConnNameError;
		} else {
			CodewindConnection existingConnection = CodewindConnectionManager.getActiveConnectionByName(name);
			if (existingConnection != null) {
				connTestButton.setEnabled(false);
				return NLS.bind(Messages.CodewindConnectionComposite_ConnNameInUseError, name);
			}
		}
		
		// Check that the url is valid and not already used
		String url = connURLText.getText().trim();
		if (!url.isEmpty()) {
			try {
				new URI(url);
			} catch (URISyntaxException e) {
				connTestButton.setEnabled(false);
				return NLS.bind(Messages.CodewindConnectionComposite_InvalidUrlError, url);
			}
			CodewindConnection existingConnection = CodewindConnectionManager.getActiveConnection(url.endsWith("/") ? url : url + "/"); //$NON-NLS-1$ //$NON-NLS-2$
			if (existingConnection != null) {
				connTestButton.setEnabled(false);
				return NLS.bind(Messages.CodewindConnectionComposite_UrlInUseError, new String[] {existingConnection.getName(), url});
			}
		}

		// Check that all of the connection fields are filled in
		String user = connUserText.getText().trim();
		String pass = connPassText.getText().trim();
		if (url.isEmpty() || user.isEmpty() || pass.isEmpty()) {
			connTestButton.setEnabled(false);
			return Messages.CodewindConnectionComposite_MissingConnDetailsError;
		}
		
		connTestButton.setEnabled(true);
		if (connection == null) {
			return Messages.CodewindConnectionComposite_TestConnMsg;
		}
		
		return null;
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
//		return connection != null && connection.isConnected() && regTested;
		return connection != null && connection.isConnected();
	}

	void removePreviousConnection() {
		if (connection != null) {
			try {
				ConnectionUtil.removeConnection(connection.getName(), connection.getConid(), new NullProgressMonitor());
			} catch (Exception e) {
				Logger.logError("An error occurred while trying to remove the previous connection: " + connection.getName() + ", " + connection.getConid()); //$NON-NLS-1$ //$NON-NLS-2$
			}
			connection.close();
			connection = null;
		}
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
			container.setErrorMessage(e.getMessage());
		}

		if (uri == null) {
			return;
		}

		Logger.log("Validating connection: " + uri); //$NON-NLS-1$

		connection = createConnection(connNameText.getText().trim(), connUserText.getText().trim(), connPassText.getText().trim(), uri);

		if(connection != null && connection.isConnected()) {
			container.setErrorMessage(null);
			container.setMessage(NLS.bind(Messages.CodewindConnectionComposite_ConnectSucceeded, connection.getBaseURI()));
		} else {
			container.setErrorMessage(NLS.bind(Messages.CodewindConnectionComposite_ErrCouldNotConnect, uri));
		}

		container.update();
	}

	/**
	 * Test canFinish before calling this to make sure it will never return null.
	 */
	CodewindConnection getConnection() {
		return connection;
	}

	private CodewindConnection createConnection(String name, String username, String password, URI uri) {
		Throwable exception = null;
		final Boolean[] isCanceled = new Boolean[] {false};
		final CodewindConnection[] connResult = new CodewindConnection[1];
		IRunnableWithProgress runnable = new IRunnableWithProgress() {
			@Override
			public void run(IProgressMonitor monitor) throws InvocationTargetException {
				String conid = null;
				CodewindConnection conn = null;
				SubMonitor mon = SubMonitor.convert(monitor, 100);
				mon.setTaskName(NLS.bind(Messages.CodewindConnectionComposite_TestConnectionJobLabel, uri));
				try {
					// Remove any previous connection first
					removePreviousConnection();
					conid = ConnectionUtil.addConnection(name, uri.toString(), username, mon.split(10));
					AuthToken token = AuthUtil.getAuthToken(username, password, conid, mon.split(30));
					conn = CodewindObjectFactory.createRemoteConnection(name, uri, conid, token);
					conn.connect(mon.split(50));
					connResult[0] = conn;
				} catch (Exception e) {
					if (conn != null) {
						conn.close();
					} else if (conid != null) {
						try {
							ConnectionUtil.removeConnection(name, conid, mon.split(10));
						} catch (Exception e2) {
							Logger.logError("An error occurred trying to de-register connection: " + name, e2); //$NON-NLS-1$
						}
					}
					throw new InvocationTargetException(e, NLS.bind(Messages.CodewindConnectionComposite_ConnErrorMsg, uri));
				}
				isCanceled[0] = monitor.isCanceled();
			}
		};
		try {
			container.run(runnable);
		} catch (InvocationTargetException e) {
			Logger.logError("An error occurred trying to connect to Codewind at: " + uri, e); //$NON-NLS-1$
			exception = e.getCause();
		} catch (InterruptedException e) {
			Logger.logError("Codewind connect was interrupted", e); //$NON-NLS-1$
			exception = e;
		}

		if (isCanceled[0]) {
			return null;
		}
		if (connResult[0] == null || !connResult[0].isConnected()) {
			if (exception != null) {
				Logger.logError("Failed to connect to Codewind at: " + uri.toString(), exception); //$NON-NLS-1$
				IStatus errorStatus = IDEUtil.getMultiStatus(NLS.bind(Messages.CodewindConnectionComposite_ConnErrorMsg, uri), exception);
				ErrorDialog.openError(getShell(), Messages.CodewindConnectionComposite_ConnErrorTitle, NLS.bind(Messages.CodewindConnectionComposite_ConnErrorMsg, uri), errorStatus);
			} else {
				// This should not happen as there should be an exception
				Logger.logError("Failed to connect to Codewind at: " + uri.toString()); //$NON-NLS-1$
				MessageDialog.openError(getShell(), Messages.CodewindConnectionComposite_ConnErrorTitle, Messages.CodewindConnectionComposite_ConnFailed);
			}
		}
		return connResult[0];
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

	public interface Container {
		public void setErrorMessage(String msg);
		
		public void setMessage(String msg);
		
		public void update();
		
		public void run(IRunnableWithProgress runnable) throws InvocationTargetException, InterruptedException;
	}
}
