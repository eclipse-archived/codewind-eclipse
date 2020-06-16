/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
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
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.eclipse.codewind.core.CodewindCorePlugin;
import org.eclipse.codewind.core.internal.CodewindApplication;
import org.eclipse.codewind.core.internal.Logger;
import org.eclipse.codewind.core.internal.cli.ProjectLinks;
import org.eclipse.codewind.core.internal.cli.ProjectLinks.LinkInfo;
import org.eclipse.codewind.core.internal.cli.ProjectUtil;
import org.eclipse.codewind.core.internal.constants.AppStatus;
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
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;
import org.eclipse.ui.dialogs.SearchPattern;

public class LinkManagementComposite extends Composite {
	
	private static final String envVarRegex = "[a-zA-Z_][a-zA-Z0-9_]*";
	private static final Pattern envVarPattern = Pattern.compile(envVarRegex);
	
	private final CodewindApplication srcApp;
	private final ProjectLinks projectLinks;
	private List<LinkEntry> linkEntries;
	private Table linkTable;
	private Button addButton, renameButton, removeButton;
	private SearchPattern pattern = new SearchPattern(SearchPattern.RULE_PATTERN_MATCH | SearchPattern.RULE_PREFIX_MATCH | SearchPattern.RULE_BLANK_MATCH);
	
	public LinkManagementComposite(Composite parent, CodewindApplication app) {
		super(parent, SWT.NONE);
		this.srcApp = app;
		this.projectLinks = app.getProjectLinks();
		this.linkEntries = getLinkEntries(projectLinks);
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
		learnMoreLink.setText("<a>" + Messages.LinkMgmtLearnMoreLink + "</a>"); //$NON-NLS-1$ //$NON-NLS-2$
		learnMoreLink.setLayoutData(new GridData(GridData.END, GridData.END, false, false, 1, 1));
		
		learnMoreLink.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				try {
					IWorkbenchBrowserSupport browserSupport = PlatformUI.getWorkbench().getBrowserSupport();
					IWebBrowser browser = browserSupport.getExternalBrowser();
					URL url = new URL(UIConstants.REGISTRY_INFO_URL);
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
		tableComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 3));
		
		// Table
		linkTable = new Table(tableComp, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL | SWT.MULTI | SWT.FULL_SELECTION);
		GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
		data.heightHint = 100;
		linkTable.setLayoutData(data);
		
		// Columns
		TableColumn projectColumn = new TableColumn(linkTable, SWT.NONE);
		projectColumn.setText(Messages.LinkMgmtProjectColumn);
		projectColumn.setResizable(true);
		
		TableColumn envVarColumn = new TableColumn(linkTable, SWT.NONE);
		envVarColumn.setText(Messages.LinkMgmtEnvVarColumn);
		envVarColumn.setResizable(true);
		
		linkTable.setHeaderVisible(true);
		linkTable.setLinesVisible(true);
		linkTable.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) {
				updateButtons();
			}
		});
		
		// Table buttons
		addButton = new Button(this, SWT.PUSH);
		addButton.setText(Messages.LinkMgmtAddButton);
		addButton.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, false, false));
		
		renameButton = new Button(this, SWT.PUSH);
		renameButton.setText(Messages.LinkMgmtRenameButton);
		renameButton.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, false, false));

		removeButton = new Button(this, SWT.PUSH);
		removeButton.setText(Messages.LinkMgmtRemoveButton);
		removeButton.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, false, false));
		
		addButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				AddDialog dialog = new AddDialog(getShell());
				if (dialog.open() == IStatus.OK) {
					LinkEntry linkEntry = dialog.getNewLinkEntry();
					if (linkEntry != null) {
						linkEntries.add(linkEntry);
						createItems();
					}
				}
			}
		});
		
		renameButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				LinkEntry linkEntry = (LinkEntry) linkTable.getSelection()[0].getData();
				RenameDialog dialog = new RenameDialog(getShell(), linkEntry.envVar);
				if (dialog.open() == IStatus.OK) {
					String newName = dialog.getEnvVar();
					linkEntry.envVar = newName;
					createItems();
				}
			}
		});

		removeButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				Arrays.stream(linkTable.getSelection()).forEach(item -> { linkEntries.remove(item.getData()); });
				createItems();
			}
		});
		
		// Add Context Sensitive Help
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, CodewindUIPlugin.MAIN_CONTEXTID);
		
		// Set the description text
		description.setText(NLS.bind(Messages.LinkMgmtDescription, srcApp.name, srcApp.connection.getName()));
		
		// Resize the columns
		Arrays.stream(linkTable.getColumns()).forEach(TableColumn::pack);
		tableColumnLayout.setColumnData(projectColumn, new ColumnWeightData(10, Math.max(150, projectColumn.getWidth()), true));
		tableColumnLayout.setColumnData(envVarColumn, new ColumnWeightData(10, Math.max(150, envVarColumn.getWidth()), true));
		
		createItems();
		updateButtons();
		linkTable.setFocus();
	}

	@Override
	public boolean setFocus() {
		return linkTable.setFocus();
	}

	private void createItems() {
		// Create the items for the table.
		linkTable.removeAll();
		for (LinkEntry linkEntry : linkEntries) {
			TableItem item = new TableItem(linkTable, SWT.NONE);
			item.setData(linkEntry);
			if (linkEntry.targetApp != null) {
				item.setText(0, linkEntry.targetApp.name);
			} else {
				String text = Messages.LinkMgmtErrorTargetMissing;
				String projectName = linkEntry.info == null ? null : linkEntry.info.getProjectName();
				if (projectName != null && !projectName.isEmpty()) {
					text += " (" + projectName + ")";
				}
				item.setText(0, text);
				item.setImage(0, PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJS_ERROR_TSK));
			}
			item.setText(1, linkEntry.envVar);
		}
	}
	
	private void updateButtons() {
		renameButton.setEnabled(linkTable.getSelectionCount() == 1);
		removeButton.setEnabled(linkTable.getSelectionCount() > 0);
	}
	
	private List<LinkEntry> getLinkEntries(ProjectLinks links) {
		return links.getLinks().stream().map(link -> new LinkEntry(link)).collect(Collectors.toList());
	}

	// This should only be called once the user has made all of their changes
	// and indicated they want to update (clicked OK or Apply rather than Cancel).
	// Callers should wrap in a job and show progress.
	public IStatus updateLinks(IProgressMonitor monitor) {
		SubMonitor mon = SubMonitor.convert(monitor, Messages.LinkUpdateTask, 100);
		MultiStatus multiStatus = new MultiStatus(CodewindCorePlugin.PLUGIN_ID, IStatus.ERROR, Messages.LinkMgmtUpdateError, null);
		
		// Keep track of any target project ids so they can be updated as well
		List<String> targetProjects = new ArrayList<String>();
		
		// Check for the differences between the original link set and the new set
		for (LinkInfo info : projectLinks.getLinks()) {
			Optional<LinkEntry> entry = getLinkEntry(info);
			if (!entry.isPresent()) {
				// Remove the link
				try {
					targetProjects.add(info.getProjectId());
					ProjectUtil.removeLink(srcApp.name, srcApp.projectID, info.getProjectName(), info.getEnvVar(), mon.split(25));
				} catch (Exception e) {
					Logger.logError("Failed to remove link for " + srcApp.name + " with variable: " + info.getEnvVar(), e); //$NON-NLS-1$ //$NON-NLS-2$
					multiStatus.add(new Status(IStatus.ERROR, CodewindCorePlugin.PLUGIN_ID, NLS.bind(Messages.LinkMgmtRemoveFailed, srcApp.name, info.getEnvVar()), e));
				}
			} else if (!entry.get().envVar.equals(info.getEnvVar())) {
				// Rename the link
				try {
					targetProjects.add(info.getProjectId());
					ProjectUtil.renameLink(srcApp.name, srcApp.projectID, info.getEnvVar(), entry.get().envVar, mon.split(25));
				} catch (Exception e) {
					Logger.logError("Failed to remove link for " + srcApp.name + " with variable: " + info.getEnvVar(), e); //$NON-NLS-1$ //$NON-NLS-2$
					multiStatus.add(new Status(IStatus.ERROR, CodewindCorePlugin.PLUGIN_ID, NLS.bind(Messages.LinkMgmtRemoveFailed, srcApp.name, info.getEnvVar()), e));
				}
			}
			if (mon.isCanceled()) {
				return Status.CANCEL_STATUS;
			}
			mon.setWorkRemaining(100);
		}
		for (LinkEntry entry : linkEntries) {
			if (entry.info == null) {
				// Create the link
				try {
					targetProjects.add(entry.targetApp.projectID);
					ProjectUtil.createLink(srcApp.name, srcApp.projectID, entry.targetApp.name, entry.targetApp.projectID, entry.envVar, mon.split(25));
				} catch (Exception e) {
					Logger.logError("Failed to create a link from " + srcApp.name + " to " + entry.targetApp.name + " with variable: " + entry.envVar, e); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					multiStatus.add(new Status(IStatus.ERROR, CodewindCorePlugin.PLUGIN_ID, NLS.bind(Messages.LinkMgmtAddFailed, new String[] {srcApp.name, entry.targetApp.name, entry.envVar}), e));
				}
			}
			if (mon.isCanceled()) {
				return Status.CANCEL_STATUS;
			}
			mon.setWorkRemaining(100);
		}
		
		// Update the application
		srcApp.connection.refreshApps(srcApp.projectID);
		CodewindUIPlugin.getUpdateHandler().updateApplication(srcApp);
		
		// Update target projects
		targetProjects.stream().forEach(projectID -> {
			CodewindApplication targetApp = srcApp.connection.getAppByID(projectID);
			if (targetApp != null) {
				CodewindUIPlugin.getUpdateHandler().updateApplication(targetApp);
			}
		});
		
		if (multiStatus.getChildren().length > 0) {
			return multiStatus;
		}
		return Status.OK_STATUS;
	}
	
	public boolean hasChanges() {
		for (LinkInfo info : projectLinks.getLinks()) {
			Optional<LinkEntry> entry = getLinkEntry(info);
			if (!entry.isPresent() || !entry.get().envVar.equals(info.getEnvVar())) {
				return true;
			}
		}
		for (LinkEntry entry : linkEntries) {
			if (entry.info == null) {
				return true;
			}
		}
		return false;
	}
	
	private Optional<LinkEntry> getLinkEntry(LinkInfo info) {
		return linkEntries.stream().filter(entry -> entry.info == info).findFirst();
	}
	
	private Optional<LinkEntry> getLinkEntry(String projectName, String envVar) {
		return linkEntries.stream().filter(entry -> entry.targetApp != null && entry.targetApp.name.equals(projectName) && entry.envVar.equals(envVar)).findFirst();
	}

	private class LinkEntry {
		private final CodewindApplication targetApp;
		private String envVar;
		private final LinkInfo info;
		
		public LinkEntry(CodewindApplication targetApp, String envVar) {
			this.targetApp = targetApp;
			this.envVar = envVar;
			this.info = null;
		}
		
		public LinkEntry(LinkInfo info) {
			this.targetApp = srcApp.connection.getAppByID(info.getProjectId());
			this.envVar = info.getEnvVar();
			this.info = info;
		}
	}
	
	private class AddDialog extends TitleAreaDialog {
		
		private String projectName;
		private String envVar;
		private org.eclipse.swt.widgets.List projectList;
		private Text envVarText;
		
		public AddDialog(Shell parentShell) {
			super(parentShell);
		}
		
		@Override
		protected void configureShell(Shell newShell) {
			super.configureShell(newShell);
			newShell.setText(Messages.LinkMgmtAddDialogShell);
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
			setTitle(Messages.LinkMgmtAddDialogTitle);
			setMessage(Messages.LinkMgmtAddDialogMessage);
			
			final Composite composite = new Composite(parent, SWT.NONE);
			GridLayout layout = new GridLayout();
			layout.marginHeight = 11;
			layout.marginWidth = 9;
			layout.horizontalSpacing = 5;
			layout.verticalSpacing = 15;
			layout.numColumns = 2;
			composite.setLayout(layout);
			GridData data = new GridData(GridData.FILL_BOTH);
			data.minimumWidth = 300;
			composite.setLayoutData(data);
			composite.setFont(parent.getFont());
			
			Group projectGroup = new Group(composite, SWT.NONE);
			projectGroup.setText("Select a target project");
			layout = new GridLayout();
			layout.marginHeight = 11;
			layout.marginWidth = 9;
			layout.horizontalSpacing = 5;
			layout.verticalSpacing = 7;
			projectGroup.setLayout(layout);
			projectGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
			
			Text filterText = new Text(projectGroup, SWT.BORDER | SWT.SEARCH);
			filterText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
			filterText.setMessage(Messages.LinkMgmtAddDialogProjectFilterText);
			filterText.setText(getFilterMessage());
			
			projectList = new org.eclipse.swt.widgets.List(projectGroup, SWT.BORDER | SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL);
			data = new GridData(SWT.FILL, SWT.FILL, true, true);
			data.heightHint = 200;
			projectList.setLayoutData(data);
			
			Label label = new Label(composite, SWT.NONE);
			label.setText(Messages.LinkMgmtAddDialogEnvVarLabel);
			label.setLayoutData(new GridData(GridData.BEGINNING, GridData.CENTER, false, false));
			
			envVarText = new Text(composite, SWT.NONE);
			envVarText.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false));
			
			filterText.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(ModifyEvent event) {
					String filter = getFilterMessage().equals(filterText.getText().trim()) ? "" : filterText.getText();
					createItems(projectList, filter);
					if (projectList.getItemCount() > 0) {
						projectList.setSelection(0);
					}
					enableOKButton(validate());
				}
			});
			
			filterText.addFocusListener(new FocusAdapter() {
				@Override
				public void focusGained(FocusEvent e) {
					Display display = filterText.getDisplay();
					display.asyncExec(() -> {
						if (!filterText.isDisposed()) {
							if (getFilterMessage().equals(filterText.getText().trim())) {
								filterText.selectAll();
							}
						}
					});
				}

				@Override
				public void focusLost(FocusEvent e) {
					if (getFilterMessage().equals(filterText.getText().trim())) {
						filterText.setText("");
					}
				}
			});
			
			filterText.addListener(SWT.KeyDown, new Listener() {
				@Override
				public void handleEvent(Event event) {
					if (event.keyCode == SWT.ARROW_DOWN) {
						if (projectList.getItemCount() > 0) {
							projectList.setSelection(0);
							projectList.setFocus();
						}
						event.doit = false;
						enableOKButton(validate());
					}
				}
			});
			
			projectList.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					enableOKButton(validate());
				}
			});
			
			envVarText.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(ModifyEvent e) {
					enableOKButton(validate());
				}
			});
			
			createItems(projectList, "");
			if (projectList.getItemCount() > 0) {
				projectList.setSelection(0);
			}
			filterText.setFocus();
			filterText.selectAll();
			if (projectList.getItemCount() == 0) {
				setErrorMessage(Messages.LinkMgmtAddDialogNoProjects);
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
		
		@Override
		protected void okPressed() {
			projectName = projectList.getSelection()[0];
			envVar = envVarText.getText();
			super.okPressed();
		}
		
		private boolean validate() {
			if (projectList.getSelectionCount() == 0) {
				setErrorMessage(Messages.LinkMgmtAddDialogNoProject);
				return false;
			}
			if (envVarText.getText() == null || envVarText.getText().trim().isEmpty()) {
				setErrorMessage(Messages.LinkMgmtAddDialogNoEnvVar);
				return false;
			}
			if (!envVarPattern.matcher(envVarText.getText().trim()).matches()) {
				setErrorMessage(NLS.bind(Messages.LinkMgmtAddDialogEnvVarInvalid, envVarRegex));
				return false;
			}
			Optional<LinkEntry> entry = getLinkEntry((String) projectList.getSelection()[0], envVarText.getText().trim());
			if (entry.isPresent()) {
				setErrorMessage(NLS.bind(Messages.LinkMgmtAddDialogLinkExist, projectList.getSelection()[0], envVarText.getText().trim()));
				return false;
			}
			
			setErrorMessage(null);
			return true;
		}
		
		public LinkEntry getNewLinkEntry() {
			CodewindApplication app = srcApp.connection.getAppByName(projectName);
			return new LinkEntry(app, envVar);
		}
		
		private void createItems(org.eclipse.swt.widgets.List list, String filter) {
			// Create the items for the table.
			list.removeAll();
			pattern.setPattern(filter == null || filter.isEmpty() ? "*" : "*" + filter + "*");
			List<String> names = new ArrayList<String>();
			for (CodewindApplication app : srcApp.connection.getApps()) {
				if (app == srcApp || app.getAppStatus() != AppStatus.STARTED) {
					continue;
				}
				if (app.isAvailable() && pattern.matches(app.name)) {
					names.add(app.name);
				}
			}
			Collections.sort(names);
			names.stream().forEach(name -> list.add(name));
		}
		
		@Override
		protected Point getInitialSize() {
			Point point = super.getInitialSize();
			return new Point(550, point.y);
		}
		
		private String getFilterMessage() {
			return Messages.LinkMgmtAddDialogProjectFilterText;
		}
	}
	
	private class RenameDialog extends TitleAreaDialog {
		
		private String oldEnvVar;
		private String envVar;
		private Text envVarText;
		
		public RenameDialog(Shell parentShell, String oldEnvVar) {
			super(parentShell);
			this.oldEnvVar = oldEnvVar;
		}
		
		@Override
		protected void configureShell(Shell newShell) {
			super.configureShell(newShell);
			newShell.setText(Messages.LinkMgmtRenameDialogShell);
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
			setTitle(Messages.LinkMgmtRenameDialogTitle);
			setMessage(Messages.LinkMgmtRenameDialogMessage);
			
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
			
			Label variableLabel = new Label(composite, SWT.NONE);
			variableLabel.setText(Messages.LinkMgmtAddDialogEnvVarLabel);
			data = new GridData(GridData.BEGINNING, GridData.CENTER, false, false);
			data.horizontalIndent = 15;
			variableLabel.setLayoutData(data);
			
			envVarText = new Text(composite, SWT.BORDER);
			envVarText.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false));
			envVarText.setText(oldEnvVar);
			
			envVarText.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(ModifyEvent e) {
					enableOKButton(validate());
				}
			});
			
			envVarText.selectAll();
			envVarText.setFocus();
			
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
		
		@Override
		protected void okPressed() {
			envVar = envVarText.getText();
			super.okPressed();
		}
		
		private boolean validate() {
			if (envVarText.getText() == null || envVarText.getText().trim().isEmpty()) {
				setErrorMessage(Messages.LinkMgmtAddDialogNoEnvVar);
			}
			setErrorMessage(null);
			return true;
		}
		
		public String getEnvVar() {
			return envVar;
		}
		
		@Override
		protected Point getInitialSize() {
			Point point = super.getInitialSize();
			return new Point(650, point.y);
		}
	}
}
