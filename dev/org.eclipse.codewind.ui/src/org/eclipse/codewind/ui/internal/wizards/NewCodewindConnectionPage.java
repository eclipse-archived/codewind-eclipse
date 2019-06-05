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

import org.eclipse.codewind.core.internal.InstallUtil;
import org.eclipse.codewind.core.internal.Logger;
import org.eclipse.codewind.core.internal.CodewindObjectFactory;
import org.eclipse.codewind.core.internal.ProcessHelper.ProcessResult;
import org.eclipse.codewind.core.internal.connection.CodewindConnection;
import org.eclipse.codewind.core.internal.connection.CodewindConnectionManager;
import org.eclipse.codewind.ui.internal.messages.Messages;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/**
 * This simple page allows the user to add new Codewind connections, by entering a hostname and port and
 * validating that Codewind is indeed reachable at the given address.
 */
public class NewCodewindConnectionPage extends WizardPage {

	private Text hostnameText, portText;

	private CodewindConnection connection;

	protected NewCodewindConnectionPage() {
		super(Messages.NewConnectionPage_ShellTitle);
		setTitle(Messages.NewConnectionPage_WizardTitle);
		setDescription(Messages.NewConnectionPage_WizardDescription);
	}

	@Override
	public void createControl(Composite parent) {
		Composite shell = new Composite(parent, SWT.NULL);
		shell.setLayout(new GridLayout());

		createHostnameAndPortFields(shell);

		setControl(shell);
	}

	private void createHostnameAndPortFields(Composite shell) {
		Composite hostPortGroup = new Composite(shell, SWT.NONE);
		//gridData.verticalSpan = 2;
		hostPortGroup.setLayout(new GridLayout(3, false));
		hostPortGroup.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false));

		GridData hostnamePortLabelData = new GridData(GridData.FILL, GridData.FILL, false, false);

		Label hostnameLabel = new Label(hostPortGroup, SWT.NONE);
		hostnameLabel.setText(Messages.NewConnectionPage_HostnameLabel);
		hostnameLabel.setLayoutData(hostnamePortLabelData);

		// Only localhost supported now so make read only and set to localhost
		hostnameText = new Text(hostPortGroup, SWT.BORDER | SWT.READ_ONLY);
		GridData hostnamePortTextData = new GridData(GridData.FILL, GridData.BEGINNING, true, false, 2, 1);
		hostnameText.setLayoutData(hostnamePortTextData);
		Color bg = hostnameText.getBackground();
		Color fg = hostnameText.getForeground();
        final Color gray = new Color(bg.getDevice(), (bg.getRed() + fg.getRed()) / 2, (bg.getGreen() + fg.getGreen()) / 2, (bg.getBlue() + fg.getBlue()) / 2);
        hostnameText.addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(DisposeEvent event) {
                gray.dispose();
            }
        });
        hostnameText.setForeground(gray);
		hostnameText.setText("localhost"); //$NON-NLS-1$
		final String localhostOnly = Messages.NewConnectionPage_OnlyLocalhostSupported;
		hostnameLabel.setToolTipText(localhostOnly);
		hostnameText.setToolTipText(localhostOnly);

		// Invalidate the wizard when the host or port are changed so that the user has to test the connection again.
		ModifyListener modifyListener = new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent arg0) {
				removePreviousConnection();

				setErrorMessage(null);
				setMessage(Messages.NewConnectionPage_TestToProceed);
				getWizard().getContainer().updateButtons();
			}
		};

		hostnameText.addModifyListener(modifyListener);

		Label portLabel = new Label(hostPortGroup, SWT.NONE);
		portLabel.setText(Messages.NewConnectionPage_PortLabel);
		portLabel.setLayoutData(hostnamePortLabelData);

		portText = new Text(hostPortGroup, SWT.BORDER);
		portText.setLayoutData(hostnamePortTextData);
		portText.setText("9090"); //$NON-NLS-1$

		portText.addModifyListener(modifyListener);

		final Button testConnectionBtn = new Button(hostPortGroup, SWT.PUSH);
		testConnectionBtn.setText(Messages.NewConnectionPage_TestConnectionBtn);
		testConnectionBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				// Block the Test Connection button while we test it
				testConnectionBtn.setEnabled(false);
				testConnection();
				testConnectionBtn.setEnabled(true);
				testConnectionBtn.setFocus();
			}
		});
		testConnectionBtn.setLayoutData(new GridData(GridData.BEGINNING, GridData.CENTER, false, false));

		// In the Local case, the user can only create one connection,
		// so if they have one already, block the Add button.
		if (CodewindConnectionManager.activeConnectionsCount() > 0) {
			testConnectionBtn.setEnabled(false);
			String existingConnectionUrl = CodewindConnectionManager.activeConnections().get(0).baseUrl.toString();
			setErrorMessage(
					NLS.bind(Messages.NewConnectionPage_ErrAConnectionAlreadyExists,
					existingConnectionUrl));
		} else {
			testConnectionBtn.setFocus();
		}
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
		String hostname = hostnameText.getText().trim();
		String portStr = portText.getText().trim();

		URI uri = null;
		try {
			int port = Integer.parseInt(portStr);

			uri = CodewindConnection.buildUrl(hostname, port);
		}
		catch(NumberFormatException e) {
			Logger.logError(e);
			setErrorMessage(NLS.bind(Messages.NewConnectionPage_NotValidPortNum, portStr));
		}
		catch(URISyntaxException e) {
			Logger.logError(e);
			setErrorMessage(e.getMessage());
		}

		if (uri == null) {
			return;
		}

		Logger.log("Validating connection: " + uri); //$NON-NLS-1$

		connection = createConnection(uri);

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
			CodewindConnectionManager.add(connection);
		}
	}
	
	private CodewindConnection createConnection(URI uri) {
		try {
			return CodewindObjectFactory.createCodewindConnection(uri);
		} catch (Exception e) {
			// Ignore
		}
		
		IRunnableWithProgress runnable = new IRunnableWithProgress() {
			@Override
			public void run(IProgressMonitor monitor) throws InvocationTargetException {
				try {
					ProcessResult result = InstallUtil.startCodewind(monitor);
					if (result.getExitValue() != 0) {
						throw new InvocationTargetException(null, "There was a problem while trying to start Codewind: " + result.getError()); //$NON-NLS-1$
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
				connection = CodewindObjectFactory.createCodewindConnection(uri);
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
