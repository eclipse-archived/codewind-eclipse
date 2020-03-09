/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.codewind.ui.internal.wizards;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.eclipse.codewind.core.internal.CodewindObjectFactory;
import org.eclipse.codewind.core.internal.Logger;
import org.eclipse.codewind.core.internal.cli.AuthToken;
import org.eclipse.codewind.core.internal.cli.AuthUtil;
import org.eclipse.codewind.core.internal.cli.ConnectionUtil;
import org.eclipse.codewind.core.internal.connection.CodewindConnection;
import org.eclipse.codewind.core.internal.connection.CodewindConnectionManager;
import org.eclipse.codewind.ui.CodewindUIPlugin;
import org.eclipse.codewind.ui.internal.UIConstants;
import org.eclipse.codewind.ui.internal.messages.Messages;
import org.eclipse.codewind.ui.internal.views.ViewHelper;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;

public class CodewindConnectionComposite extends Composite {
	
	private CompositeContainer container;
	private CodewindConnection connection;
	private boolean isUpdate;
	private boolean isChanged = false;

	private Text connNameText;
	private Text connURLText, connUserText, connPassText;
	
	private String name, url, user, pass;
	private boolean isUpdating = false;

	public CodewindConnectionComposite(Composite parent, CompositeContainer container) {
		this(parent, container, null);
	}
	
	public CodewindConnectionComposite(Composite parent, CompositeContainer container, CodewindConnection connection) {
		super(parent, SWT.NONE);
		this.container = container;
		this.connection = connection;
		isUpdate = connection != null;
		createControl();
	}
	
	protected void createControl() {
        GridLayout layout = new GridLayout();
        layout.numColumns = 2;
        layout.marginHeight = 20;
		layout.marginWidth = 8;
		layout.horizontalSpacing = 7;
		layout.verticalSpacing = 7;
        this.setLayout(layout);
        
        createLabel(Messages.CodewindConnectionComposite_ConnNameLabel, this, 1);
        connNameText = createConnText(this, SWT.NONE, 1);
       
        createLabel(Messages.CodewindConnectionComposite_UrlLabel, this, 1);
        connURLText = createConnText(this, SWT.NONE, 1);
        
        createLabel(Messages.CodewindConnectionComposite_UserLabel, this, 1);
        connUserText = createConnText(this, SWT.NONE, 1);
        
        createLabel(Messages.CodewindConnectionComposite_PasswordLabel, this, 1);
        connPassText = createConnText(this, SWT.PASSWORD, 1);
        
        new Label(this, SWT.NONE).setLayoutData(new GridData(GridData.FILL, GridData.FILL, false, false, 2, 1));
		
		Link learnMoreLink = new Link(this, SWT.NONE);
		learnMoreLink.setText("<a>" + Messages.RegMgmtLearnMoreLink + "</a>"); //$NON-NLS-1$ //$NON-NLS-2$
		learnMoreLink.setLayoutData(new GridData(GridData.BEGINNING, GridData.END, false, false, 1, 1));
		
		learnMoreLink.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				try {
					IWorkbenchBrowserSupport browserSupport = PlatformUI.getWorkbench().getBrowserSupport();
					IWebBrowser browser = browserSupport.getExternalBrowser();
					URL url = new URL(UIConstants.REMOTE_SETUP_URL);
					browser.openURL(url);
				} catch (Exception e) {
					Logger.logError("An error occurred trying to open an external browser at: " + UIConstants.TEMPLATES_INFO_URL, e); //$NON-NLS-1$
				}
			}
		});

        initialize();
        connNameText.setFocus();
	}

	private void createLabel(String labelStr, Composite parent, int horizontalSpan) {
		Label label = new Label(parent, SWT.NONE);
		label.setText(labelStr);
		GridData data = new GridData(SWT.FILL, SWT.CENTER, false, false, horizontalSpan, 1);
		label.setLayoutData(data);
	}

	private Text createConnText(Composite parent, int styles, int horizontalSpan) {
		Text text = new Text(parent, SWT.BORDER | styles);
		text.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, horizontalSpan, 1));
		text.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent arg0) {
				if (isUpdating) {
					return;
				}
				isChanged = true;
				validateAndUpdate();
			}
		});
		return text;
	}
	
	private void initialize() {
		if (connection != null) {
			isUpdating = true;
			connNameText.setText(connection.getName());
			connURLText.setText(connection.getBaseURI().toString());
			connUserText.setText(connection.getUsername());
			isUpdating = false;
		}
	}
	
	private void validateAndUpdate() {
		validate();
		container.update();
	}
	
	private void validate() {
		String errorMsg = validateConnectionInfo();
		container.setErrorMessage(errorMsg);
	}
	
	private String validateConnectionInfo() {
		name = connNameText.getText().trim();
		url = connURLText.getText().trim();
		user = connUserText.getText().trim();
		pass = connPassText.getText().trim();
		
		// Check that connection name is set
		if (name == null || name.isEmpty()) {
			return Messages.CodewindConnectionComposite_NoConnNameError;
		} else {
			CodewindConnection existingConnection = CodewindConnectionManager.getActiveConnectionByName(name);
			if (existingConnection != null && (connection == null || !existingConnection.getConid().equals(connection.getConid()))) {
				return NLS.bind(Messages.CodewindConnectionComposite_ConnNameInUseError, name);
			}
		}
		
		// Check that the url is valid and not already used
		if (!url.isEmpty()) {
			try {
				new URI(url);
			} catch (URISyntaxException e) {
				return NLS.bind(Messages.CodewindConnectionComposite_InvalidUrlError, url);
			}
			CodewindConnection existingConnection = CodewindConnectionManager.getActiveConnection(url.endsWith("/") ? url : url + "/"); //$NON-NLS-1$ //$NON-NLS-2$
			if (existingConnection != null && (connection == null || !existingConnection.getConid().equals(connection.getConid()))) {
				return NLS.bind(Messages.CodewindConnectionComposite_UrlInUseError, new String[] {existingConnection.getName(), url});
			}
		}

		// Check that all of the connection fields are filled in
		if (url.isEmpty() || user.isEmpty()) {
			return Messages.CodewindConnectionComposite_MissingConnDetailsError;
		}
		if (pass.isEmpty()) {
			if (isUpdate) {
				return Messages.CodewindConnectionComposite_NoPasswordForUpdateError;
			}
			return Messages.CodewindConnectionComposite_MissingConnDetailsError;
		}
		
		return null;
	}

	public boolean canFinish() {
		return validateConnectionInfo() == null && (!isUpdate || isChanged);
	}

	public String getConnectionName() {
		return name;
	}

	public IStatus createConnection(IProgressMonitor monitor) {
		String conid = null;
		connection = null;
		SubMonitor mon = SubMonitor.convert(monitor, 100);
		try {
			URI uri = new URI(url);
			conid = ConnectionUtil.addConnection(name, uri.toString(), user, mon.split(20));
			if (mon.isCanceled()) {
				removeConnection(conid);
				return Status.CANCEL_STATUS;
			}
			connection = CodewindObjectFactory.createRemoteConnection(name, uri, conid, user, null);
			
			AuthToken token = AuthUtil.genAuthToken(user, pass, conid, mon.split(30));
			if (mon.isCanceled()) {
				removeConnection();
				return Status.CANCEL_STATUS;
			}
			connection.setAuthToken(token);
			
			connection.connect(mon.split(50));
			if (mon.isCanceled()) {
				removeConnection();
				return Status.CANCEL_STATUS;
			}
		} catch (Exception e) {
			String msg;
			if (connection == null) {
				msg = NLS.bind(Messages.CodewindConnectionCreateError, name);
			} else {
				msg = NLS.bind(Messages.CodewindConnectionConnectError, name);
			}
			return new Status(IStatus.ERROR, CodewindUIPlugin.PLUGIN_ID, msg, e);
		} finally {
			if (connection != null) {
				CodewindConnectionManager.add(connection);
				ViewHelper.openCodewindExplorerView();
				CodewindUIPlugin.getUpdateHandler().updateAll();
			}
		}
		return Status.OK_STATUS;
	}
	
	public IStatus updateConnection(IProgressMonitor monitor) {
		SubMonitor mon = SubMonitor.convert(monitor, 100);
		
		if (connection.isConnected()) {
			connection.disconnect();
		}
		try {
			ConnectionUtil.updateConnection(connection.getConid(), name, url, user, mon.split(20));
			if (mon.isCanceled()) {
				return Status.CANCEL_STATUS;
			}
			connection.setName(name);
			connection.setBaseURI(new URI(url));
			connection.setUsername(user);
			
			AuthToken token = AuthUtil.genAuthToken(user, pass, connection.getConid(), mon.split(30));
			if (mon.isCanceled()) {
				return Status.CANCEL_STATUS;
			}
			connection.setAuthToken(token);
			
			connection.connect(mon.split(50));
			if (mon.isCanceled()) {
				return Status.CANCEL_STATUS;
			}
		} catch (Exception e) {
			return new Status(IStatus.ERROR, CodewindUIPlugin.PLUGIN_ID, NLS.bind(Messages.CodewindConnectionUpdateError, name), e);
		} finally {
			ViewHelper.openCodewindExplorerView();
			CodewindUIPlugin.getUpdateHandler().updateConnection(connection);
		}
		
		return Status.OK_STATUS;
	}
	
	private void removeConnection() {
		if (connection == null) {
			return;
		}
		connection.disconnect();
		removeConnection(connection.getConid());
		connection = null;
	}
	
	private void removeConnection(String conid) {
		if (conid == null) {
			return;
		}
		try {
			ConnectionUtil.removeConnection(conid, new NullProgressMonitor());
		} catch (Exception e) {
			Logger.logError("An error occurred trying to de-register connection: " + conid, e); //$NON-NLS-1$
		}
	}
}
