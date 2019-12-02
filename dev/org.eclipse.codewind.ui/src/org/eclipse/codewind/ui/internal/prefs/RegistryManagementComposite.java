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

package org.eclipse.codewind.ui.internal.prefs;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.codewind.core.CodewindCorePlugin;
import org.eclipse.codewind.core.internal.Logger;
import org.eclipse.codewind.core.internal.connection.CodewindConnection;
import org.eclipse.codewind.core.internal.connection.RegistryInfo;
import org.eclipse.codewind.ui.CodewindUIPlugin;
import org.eclipse.codewind.ui.internal.UIConstants;
import org.eclipse.codewind.ui.internal.messages.Messages;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;

public class RegistryManagementComposite extends Composite {
	
	private final CodewindConnection connection;
	private final List<RegistryInfo> regList;
	private List<RegEntry> regEntries;
	private boolean supportsPushReg = false;
	private Table regTable;
	private Button addButton, removeButton;
	private Color gray;
	
	public RegistryManagementComposite(Composite parent, CodewindConnection connection, List<RegistryInfo> regList) {
		super(parent, SWT.NONE);
		this.connection = connection;
		this.regList = regList;
		this.regEntries = getRegEntries(regList);
		this.supportsPushReg = !connection.isLocal();
		createControl();
	}
	
	protected void createControl() {
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginHeight = 8;
		layout.marginWidth = 8;
		layout.horizontalSpacing = 7;
		layout.verticalSpacing = 5;
		setLayout(layout);
		
		Text description = new Text(this, SWT.READ_ONLY | SWT.WRAP | SWT.MULTI);
		description.setText(""); //$NON-NLS-1$
		description.setBackground(this.getBackground());
		description.setForeground(this.getForeground());
		description.setLayoutData(new GridData(GridData.FILL, GridData.END, true, false, 1, 1));
		
		Link learnMoreLink = new Link(this, SWT.NONE);
		learnMoreLink.setText("<a>" + Messages.RegMgmtLearnMoreLink + "</a>"); //$NON-NLS-1$ //$NON-NLS-2$
		learnMoreLink.setLayoutData(new GridData(GridData.END, GridData.END, false, false, 1, 1));
		
		learnMoreLink.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				try {
					IWorkbenchBrowserSupport browserSupport = PlatformUI.getWorkbench().getBrowserSupport();
					IWebBrowser browser = browserSupport.getExternalBrowser();
					URL url = new URL(UIConstants.TEMPLATES_INFO_URL);
					browser.openURL(url);
				} catch (Exception e) {
					Logger.logError("An error occurred trying to open an external browser at: " + UIConstants.TEMPLATES_INFO_URL, e); //$NON-NLS-1$
				}
			}
		});
		
		// Spacer
		new Label(this, SWT.NONE).setLayoutData(new GridData(GridData.FILL, GridData.FILL, false, false, 2, 1));
		
		// Create a composite for the table so can use TableColumnLayout
		Composite tableComp = new Composite(this, SWT.NONE);
		TableColumnLayout tableColumnLayout = new TableColumnLayout();
		tableComp.setLayout(tableColumnLayout);
		tableComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 2));
		
		// Table
		regTable = new Table(tableComp, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL | SWT.MULTI | SWT.FULL_SELECTION);
		GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
		data.heightHint = 100;
		regTable.setLayoutData(data);
		
		// Columns
		TableColumn addressColumn = new TableColumn(regTable, SWT.NONE);
		addressColumn.setText(Messages.RegMgmtAddressColumn);
		addressColumn.setResizable(true);
		
		TableColumn usernameColumn = new TableColumn(regTable, SWT.NONE);
		usernameColumn.setText(Messages.RegMgmtUsernameColumn);
		usernameColumn.setResizable(true);
		
		TableColumn namespaceColumn = null;
		TableColumn pushColumn = null;
		if (supportsPushReg) {
			namespaceColumn = new TableColumn(regTable, SWT.NONE);
			namespaceColumn.setText(Messages.RegMgmtNamespaceColumn);
			namespaceColumn.setResizable(true);
			
			pushColumn = new TableColumn(regTable, SWT.NONE);
			pushColumn.setText(Messages.RegMgmtPushRegColumn);
			pushColumn.setResizable(true);
		}

		regTable.setHeaderVisible(true);
		regTable.setLinesVisible(true);
		regTable.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) {
				updateButtons();
			}
		});
		
		// Table buttons
		addButton = new Button(this, SWT.PUSH);
		addButton.setText(Messages.RegMgmtAddButton);
		addButton.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, false, false));
		
		removeButton = new Button(this, SWT.PUSH);
		removeButton.setText(Messages.RegMgmtRemoveButton);
		removeButton.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, false, false));
		
		addButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				AddDialog dialog = new AddDialog(getShell());
				if (dialog.open() == IStatus.OK) {
					RegEntry repoEntry = dialog.getNewRegEntry();
					if (repoEntry != null) {
						// If the new entry is a push registry then disable push registry for the other entries
						if (repoEntry.isPushReg) {
							regEntries.stream().forEach(regEntry -> { regEntry.isPushReg = false; });
						}
						regEntries.add(repoEntry);
						createItems();
					}
				}
			}
		});
		
		removeButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				Arrays.stream(regTable.getSelection()).forEach(item -> { regEntries.remove(item.getData()); });
				createItems();
			}
		});

		// Set the description text
		String descText = supportsPushReg ? Messages.RegMgmtDescription : Messages.RegMgmtLocalDescription;
		description.setText(descText);
		
		// Resize the columns
		Arrays.stream(regTable.getColumns()).forEach(TableColumn::pack);
		tableColumnLayout.setColumnData(addressColumn, new ColumnWeightData(10, Math.max(250, addressColumn.getWidth()), true));
		tableColumnLayout.setColumnData(usernameColumn, new ColumnWeightData(4, Math.max(75, usernameColumn.getWidth()), true));
		if (supportsPushReg) {
			tableColumnLayout.setColumnData(namespaceColumn, new ColumnWeightData(4, Math.max(75, namespaceColumn.getWidth()), true));
			tableColumnLayout.setColumnData(pushColumn, new ColumnWeightData(1, Math.max(20, pushColumn.getWidth()), true));
		}
		
		createItems();
		updateButtons();
	}

	private void createItems() {
		// Create the items for the table.
		regTable.removeAll();
		Arrays.stream(regTable.getChildren()).filter(Button.class::isInstance).forEach(Control::dispose);
		for (RegEntry regEntry : regEntries) {
			TableItem item = new TableItem(regTable, SWT.NONE);
			item.setData(regEntry);
			
			item.setText(0, regEntry.address);
			item.setText(1, regEntry.username);
			
			if (supportsPushReg) {
				item.setText(2, regEntry.namespace == null ? "" : regEntry.namespace); //$NON-NLS-1$
				
				TableEditor editor = new TableEditor(regTable);
				Button button = new Button(regTable, SWT.RADIO);
				button.setData(regEntry);
				button.pack();
				button.setSelection(regEntry.isPushReg);
				editor.minimumWidth = button.getSize ().x;
				editor.horizontalAlignment = SWT.CENTER;
				editor.verticalAlignment = SWT.CENTER;
				editor.setEditor(button, item, 3);
				button.setSelection(regEntry.isPushReg);
				item.setForeground(2, button.getSelection() ? item.getForeground() : getGray(item));
				
				button.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent e) {
						if (button.getSelection() && (regEntry.namespace == null || regEntry.namespace.isEmpty())) {
							NamespaceDialog dialog = new NamespaceDialog(getShell());
							if (dialog.open() == IStatus.OK) {
								regEntry.namespace = dialog.getNamespace();
								item.setText(2, regEntry.namespace);
							} else {
								button.setSelection(false);
								return;
							}
						}
						// Need to override normal radio button processing to allow
						// the user to unset a push registry
						if (regEntry.isPushReg) {
							regEntry.isPushReg = false;
							button.setSelection(false);
							item.setForeground(2, getGray(item));
						} else {
							regEntry.isPushReg = button.getSelection();
							item.setForeground(2, button.getSelection() ? item.getForeground() : getGray(item));
						}
						
					}
				});	 
			}
		}
	}
	
	private Color getGray(TableItem item) {
		if (gray == null) {
			Color fg = item.getForeground();
			Color bg = item.getBackground();
			gray = new Color(fg.getDevice(), (fg.getRed() + bg.getRed()) / 2, (fg.getGreen() + bg.getGreen()) / 2, (fg.getBlue() + bg.getBlue()) / 2);
		}
		return gray;
	}

	@Override
	public void dispose() {
		if (gray != null) {
			gray.dispose();
		}
	}

	private void updateButtons() {
		removeButton.setEnabled(regTable.getSelection().length > 0);
	}
	
	private List<RegEntry> getRegEntries(List<RegistryInfo> infos) {
		List<RegEntry> entries = new ArrayList<RegEntry>(infos.size());
		for (RegistryInfo info : infos) {
			entries.add(new RegEntry(info));
		}
		return entries;
	}

	// This should only be called once the user has made all of their changes
	// and indicated they want to update (clicked OK or Apply rather than Cancel).
	// Callers should wrap in a job and show progress.
	public IStatus updateRegistries(IProgressMonitor monitor) {
		SubMonitor mon = SubMonitor.convert(monitor, Messages.RegUpdateTask, 100);
		MultiStatus multiStatus = new MultiStatus(CodewindCorePlugin.PLUGIN_ID, IStatus.ERROR, Messages.RegMgmtUpdateError, null);
		
		// Check for the differences between the original repo set and the new set
		for (RegistryInfo info : regList) {
			RegEntry entry = getRegEntry(info.getURL());
			if (entry == null) {
				// Remove the registry
				try {
					connection.requestRemoveRegistry(info.getURL(), info.getUsername());
				} catch (Exception e) {
					Logger.logError("Failed to remove registry: " + info.getURL(), e); //$NON-NLS-1$
					multiStatus.add(new Status(IStatus.ERROR, CodewindCorePlugin.PLUGIN_ID, NLS.bind(Messages.RegMgmtRemoveFailed, info.getURL()), e));
				}
			}
			if (mon.isCanceled()) {
				return Status.CANCEL_STATUS;
			}
			mon.worked(25);
			mon.setWorkRemaining(100);
		}
		for (RegEntry entry : regEntries) {
			RegistryInfo info = entry.info;
			if (info == null) {
				// Add the registry
				try {
					connection.requestAddRegistry(entry.address, entry.username, entry.password);
					if (entry.isPushReg) {
						connection.requestSetPushRegistry(entry.address + "/" + entry.namespace);
					}
				} catch (Exception e) {
					Logger.logError("Failed to add registry: " + entry.address, e); //$NON-NLS-1$
					multiStatus.add(new Status(IStatus.ERROR, CodewindCorePlugin.PLUGIN_ID, NLS.bind(Messages.RegMgmtAddFailed, entry.address), e));
				}
			}
			if (mon.isCanceled()) {
				return Status.CANCEL_STATUS;
			}
			mon.worked(25);
			mon.setWorkRemaining(100);
			if (entry.isPushReg) {
				try {
					connection.requestSetPushRegistry(entry.address + "/" + entry.namespace);
				} catch (Exception e) {
					Logger.logError("Failed to set the push registry: " + info.getURL(), e); //$NON-NLS-1$
					multiStatus.add(new Status(IStatus.ERROR, CodewindCorePlugin.PLUGIN_ID, NLS.bind(Messages.RegMgmtSetPushRegFailed, info.getURL()), e));
				}
			}
			if (mon.isCanceled()) {
				return Status.CANCEL_STATUS;
			}
			mon.worked(25);
			mon.setWorkRemaining(100);
		}
		if (multiStatus.getChildren().length > 0) {
			return multiStatus;
		}
		return Status.OK_STATUS;
	}
	
	public boolean hasChanges() {
		for (RegistryInfo info : regList) {
			RegEntry entry = getRegEntry(info.getURL());
			if (entry == null) {
				return true;
			}
		}
		for (RegEntry entry : regEntries) {
			RegistryInfo info = entry.info;
			if (info == null) {
				return true;
			} else if (entry.isPushReg) {
				return true;
			}
		}
		return false;
	}
	
	private RegEntry getRegEntry(String address) {
		for (RegEntry entry : regEntries) {
			if (address.equals(entry.address)) {
				return entry;
			}
		}
		return null;
	}

	private static class RegEntry {
		public final String address;
		public String namespace;
		public final String username;
		public final String password;
		public boolean isPushReg = false;
		public RegistryInfo info;
		
		public RegEntry(String address, String namespace, String username, String password, boolean isPush) {
			this.address = address;
			this.namespace = namespace;
			this.username = username;
			this.password = password;
			this.isPushReg = isPush;
		}
		
		public RegEntry(RegistryInfo info) {
			this.address = info.getURL();
			this.namespace = info.getNamespace();
			this.username = info.getUsername();
			this.password = null;
			this.isPushReg = info.isPushReg();
			this.info = info;
		}
	}
	
	private class AddDialog extends TitleAreaDialog {
		
		private String address;
		private String namespace;
		private String username;
		private String password;
		private boolean isPushReg = false;
		
		public AddDialog(Shell parentShell) {
			super(parentShell);
		}
		
		@Override
		protected void configureShell(Shell newShell) {
			super.configureShell(newShell);
			newShell.setText(Messages.RegMgmtAddDialogShell);
		}
		
		@Override
		protected boolean isResizable() {
			return true;
		}

		@Override
		protected Control createButtonBar(Composite parent) {
			return super.createButtonBar(parent);
		}

		protected Control createDialogArea(Composite parent) {
			setTitleImage(CodewindUIPlugin.getImage(CodewindUIPlugin.CODEWIND_BANNER));
			setTitle(Messages.RegMgmtAddDialogTitle);
			setMessage(Messages.RegMgmtAddDialogMessage);
			
			final Composite composite = new Composite(parent, SWT.NONE);
			GridLayout layout = new GridLayout();
			layout.marginHeight = 11;
			layout.marginWidth = 9;
			layout.horizontalSpacing = 5;
			layout.verticalSpacing = 7;
			layout.numColumns = 2;
			composite.setLayout(layout);
			GridData data = new GridData(GridData.FILL_BOTH);
			data.minimumWidth = 300;
			composite.setLayoutData(data);
			composite.setFont(parent.getFont());
			
			Label label = new Label(composite, SWT.NONE);
			label.setText(Messages.RegMgmtAddDialogAddressLabel);
			label.setLayoutData(new GridData(GridData.BEGINNING, GridData.CENTER, false, false));
			
			Text addressText = new Text(composite, SWT.BORDER);
			addressText.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false));
			
			label = new Label(composite, SWT.NONE);
			label.setText(Messages.RegMgmtAddDialogUsernameLabel);
			label.setLayoutData(new GridData(GridData.BEGINNING, GridData.CENTER, false, false));
			
			Text usernameText = new Text(composite, SWT.BORDER);
			usernameText.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false));
			
			label = new Label(composite, SWT.NONE);
			label.setText(Messages.RegMgmtAddDialogPasswordLabel);
			label.setLayoutData(new GridData(GridData.BEGINNING, GridData.CENTER, false, false));
			
			Text passwordText = new Text(composite, SWT.BORDER | SWT.PASSWORD);
			passwordText.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false));
			
			addressText.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(ModifyEvent e) {
					address = addressText.getText().trim();
					enableOKButton(validate());
				}
			});
			
			usernameText.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(ModifyEvent e) {
					username = usernameText.getText().trim();
					enableOKButton(validate());
				}
			});
			
			passwordText.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(ModifyEvent e) {
					password = passwordText.getText().trim();
					enableOKButton(validate());
				}
			});
			
			if (supportsPushReg) {
				Label spacer = new Label(composite, SWT.NONE);
				spacer.setLayoutData(new GridData(GridData.BEGINNING, GridData.CENTER, false, false, 2, 1));
				
				Button pushButton = new Button(composite, SWT.CHECK);
				pushButton.setText(Messages.RegMgmtAddDialogPushRegLabel);
				pushButton.setLayoutData(new GridData(GridData.BEGINNING, GridData.CENTER, false, false, 2, 1));
				
				Label namespaceLabel = new Label(composite, SWT.NONE);
				namespaceLabel.setText(Messages.RegMgmtAddDialogNamespaceLabel);
				data = new GridData(GridData.BEGINNING, GridData.CENTER, false, false);
				data.horizontalIndent = 15;
				namespaceLabel.setLayoutData(data);
				
				Text namespaceText = new Text(composite, SWT.BORDER);
				namespaceText.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false));
				
				pushButton.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent event) {
						namespaceLabel.setVisible(pushButton.getSelection());
						namespaceText.setVisible(pushButton.getSelection());
						isPushReg = pushButton.getSelection();
						enableOKButton(validate());
					}
				});
				
				namespaceText.addModifyListener(new ModifyListener() {
					@Override
					public void modifyText(ModifyEvent e) {
						namespace = namespaceText.getText().trim();
						enableOKButton(validate());
					}
				});
				
				pushButton.setSelection(false);
				namespaceLabel.setVisible(false);
				namespaceText.setVisible(false);
			}
			
			return composite; 
		}
		
		@Override
		protected void createButtonsForButtonBar(Composite parent) {
			super.createButtonsForButtonBar(parent);
			enableOKButton(false);
		}

		protected void enableOKButton(boolean value) {
			getButton(IDialogConstants.OK_ID).setEnabled(value);
		}
		
		private boolean validate() {
			if (address == null || address.isEmpty()) {
				setErrorMessage(Messages.RegMgmtAddDialogNoAddress);
				return false;
			} else if (getRegEntry(address) != null) {
				setErrorMessage(Messages.RegMgmtAddDialogAddressInUse);
				return false;
			}
			if (username == null || username.isEmpty()) {
				setErrorMessage(Messages.RegMgmtAddDialogNoUsername);
				return false;
			}
			if (password == null || password.isEmpty()) {
				setErrorMessage(Messages.RegMgmtAddDialogNoPassword);
				return false;
			}
			if (isPushReg && (namespace == null || namespace.isEmpty())) {
				setErrorMessage(Messages.RegMgmtAddDialogNoNamespace);
				return false;
			}
			
			setErrorMessage(null);
			return true;
		}
		
		public RegEntry getNewRegEntry() {
			if (address != null && !address.isEmpty() &&
				username != null && !username.isEmpty() &&
				password != null && !password.isEmpty()) {
				return new RegEntry(address, namespace, username, password, isPushReg);
			}
			return null;
		}
	}
	
	private class NamespaceDialog extends TitleAreaDialog {
		
		private String namespace;
		
		public NamespaceDialog(Shell parentShell) {
			super(parentShell);
		}
		
		@Override
		protected void configureShell(Shell newShell) {
			super.configureShell(newShell);
			newShell.setText(Messages.RegMgmtNamespaceDialogShell);
		}
		
		@Override
		protected boolean isResizable() {
			return true;
		}

		@Override
		protected Control createButtonBar(Composite parent) {
			return super.createButtonBar(parent);
		}

		protected Control createDialogArea(Composite parent) {
			setTitleImage(CodewindUIPlugin.getImage(CodewindUIPlugin.CODEWIND_BANNER));
			setTitle(Messages.RegMgmtNamespaceDialogTitle);
			setMessage(Messages.RegMgmtNamespaceDialogMessage);
			
			final Composite composite = new Composite(parent, SWT.NONE);
			GridLayout layout = new GridLayout();
			layout.marginHeight = 11;
			layout.marginWidth = 9;
			layout.horizontalSpacing = 5;
			layout.verticalSpacing = 7;
			layout.numColumns = 2;
			composite.setLayout(layout);
			GridData data = new GridData(GridData.FILL_BOTH);
			data.minimumWidth = 300;
			composite.setLayoutData(data);
			composite.setFont(parent.getFont());
			
			Label namespaceLabel = new Label(composite, SWT.NONE);
			namespaceLabel.setText(Messages.RegMgmtAddDialogNamespaceLabel);
			data = new GridData(GridData.BEGINNING, GridData.CENTER, false, false);
			data.horizontalIndent = 15;
			namespaceLabel.setLayoutData(data);
			
			Text namespaceText = new Text(composite, SWT.BORDER);
			namespaceText.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false));
			
			namespaceText.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(ModifyEvent e) {
					namespace = namespaceText.getText().trim();
					enableOKButton(validate());
				}
			});
			
			namespaceText.setFocus();
			
			return composite;
		}
		
		@Override
		protected void createButtonsForButtonBar(Composite parent) {
			super.createButtonsForButtonBar(parent);
			enableOKButton(false);
		}

		protected void enableOKButton(boolean value) {
			getButton(IDialogConstants.OK_ID).setEnabled(value);
		}
		
		private boolean validate() {
			if (namespace == null || namespace.isEmpty()) {
				setErrorMessage(Messages.RegMgmtAddDialogNoNamespace);
				return false;
			}
			
			setErrorMessage(null);
			return true;
		}
		
		public String getNamespace() {
			return namespace;
		}
	}
	
}
