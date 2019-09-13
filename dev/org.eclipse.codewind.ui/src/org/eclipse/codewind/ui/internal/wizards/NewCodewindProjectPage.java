/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *	 IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.codewind.ui.internal.wizards;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;

import org.eclipse.codewind.core.internal.CodewindManager;
import org.eclipse.codewind.core.internal.InstallStatus;
import org.eclipse.codewind.core.internal.InstallUtil;
import org.eclipse.codewind.core.internal.Logger;
import org.eclipse.codewind.core.internal.ProcessHelper.ProcessResult;
import org.eclipse.codewind.core.internal.connection.CodewindConnection;
import org.eclipse.codewind.core.internal.connection.ProjectTemplateInfo;
import org.eclipse.codewind.core.internal.connection.RepositoryInfo;
import org.eclipse.codewind.core.internal.constants.ProjectLanguage;
import org.eclipse.codewind.core.internal.constants.ProjectType;
import org.eclipse.codewind.ui.internal.messages.Messages;
import org.eclipse.codewind.ui.internal.prefs.RepositoryManagementDialog;
import org.eclipse.codewind.ui.internal.views.ViewHelper;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.SearchPattern;

public class NewCodewindProjectPage extends WizardPage {
	
	private static final Pattern projectNamePattern = Pattern.compile("^[a-zA-Z0-9_.-]+$"); //$NON-NLS-1$
	
	private CodewindConnection connection;
	private List<ProjectTemplateInfo> templateList;
	private SearchPattern pattern = new SearchPattern(SearchPattern.RULE_PATTERN_MATCH | SearchPattern.RULE_PREFIX_MATCH | SearchPattern.RULE_BLANK_MATCH);
	private Text filterText;
	private Table selectionTable;
	private Text descriptionLabel;
	private Text projectNameText;

	protected NewCodewindProjectPage(CodewindConnection connection, List<ProjectTemplateInfo> templateList) {
		super(Messages.NewProjectPage_ShellTitle);
		setTitle(Messages.NewProjectPage_WizardTitle);
		setDescription(Messages.NewProjectPage_WizardDescription);
		this.connection = connection;
		this.templateList = templateList;
		setPageComplete(false);
	}

	@Override
	public void createControl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NULL);
		composite.setLayout(new GridLayout());
		
		if (connection == null) {
			setupConnection();
			if (connection == null) {
				setErrorMessage(Messages.NewProjectPage_CodewindConnectError);
				setControl(composite);
				return;
			}
		}
		
		if (templateList == null || templateList.isEmpty()) {
			getTemplates();
			if (templateList == null || templateList.isEmpty()) {
				setErrorMessage(Messages.NewProjectPage_TemplateListError);
				setControl(composite);
				return;
			}
		}
		
		templateList.sort(new Comparator<ProjectTemplateInfo>() {
			@Override
			public int compare(ProjectTemplateInfo info1, ProjectTemplateInfo info2) {
				return info1.getLabel().compareTo(info2.getLabel());
			}
		});

		createContents(composite);

		setControl(composite);
	}

	private void createContents(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(2, false));
		composite.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true));
		
		Label label = new Label(composite, SWT.NONE);
		label.setText(Messages.NewProjectPage_ProjectNameLabel);
		
		projectNameText = new Text(composite, SWT.BORDER);
		projectNameText.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false));

		Label spacer = new Label(composite, SWT.NONE);
		spacer.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false, 2, 1));

		// Project template composite
		Text templateText = new Text(composite, SWT.READ_ONLY);
		templateText.setText(Messages.NewProjectPage_TemplateGroupLabel);
		templateText.setLayoutData(new GridData(GridData.BEGINNING, GridData.CENTER, false, false, 2, 1));
		templateText.setBackground(composite.getBackground());
		templateText.setForeground(composite.getForeground());
		
		Group templateGroup = new Group(composite, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginHeight = 8;
		layout.marginWidth = 8;
		layout.horizontalSpacing = 7;
		layout.verticalSpacing = 7;
		templateGroup.setLayout(layout);
		templateGroup.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true, 2, 1));
		
		// Filter text
		filterText = new Text(templateGroup, SWT.BORDER);
		filterText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		filterText.setMessage(Messages.NewProjectPage_FilterMessage);

		// Table
		selectionTable = new Table(templateGroup, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL | SWT.FULL_SELECTION);
		GridData data = new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1);
		data.heightHint = 100;
		selectionTable.setLayoutData(data);
		
		// Columns
		final TableColumn templateColumn = new TableColumn(selectionTable, SWT.NONE);
		templateColumn.setText(Messages.NewProjectPage_TemplateColumn);
		templateColumn.setResizable(true);
		templateColumn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				sortTable(selectionTable, templateColumn);
			}
		});
		final TableColumn typeColumn = new TableColumn(selectionTable, SWT.NONE);
		typeColumn.setText(Messages.NewProjectPage_TypeColumn);
		typeColumn.setResizable(true);
		typeColumn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				sortTable(selectionTable, typeColumn);
			}
		});
		final TableColumn languageColumn = new TableColumn(selectionTable, SWT.NONE);
		languageColumn.setText(Messages.NewProjectPage_LanguageColumn);
		languageColumn.setResizable(true);
		languageColumn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				sortTable(selectionTable, languageColumn);
			}
		});

		selectionTable.setHeaderVisible(true);
		selectionTable.setLinesVisible(false);
		selectionTable.setSortDirection(SWT.DOWN);
		selectionTable.setSortColumn(templateColumn);
		
		createItems(selectionTable, "");

		resizeColumns(selectionTable);
		
		// Description text
		ScrolledComposite descriptionScroll = new ScrolledComposite(templateGroup, SWT.V_SCROLL);
		descriptionLabel = new Text(descriptionScroll, SWT.WRAP | SWT.READ_ONLY);
		descriptionLabel.setText(NLS.bind(Messages.NewProjectPage_DescriptionLabel, ""));
		descriptionLabel.setBackground(templateGroup.getBackground());
		descriptionLabel.setForeground(templateGroup.getForeground());
		descriptionScroll.setContent(descriptionLabel);
		
		data = new GridData(GridData.FILL, GridData.FILL, true, false);
		int lineHeight = filterText.computeSize(SWT.DEFAULT, SWT.DEFAULT).y;
		data.heightHint = lineHeight * 2;
		data.horizontalSpan = 2;
		descriptionScroll.setLayoutData(data);
		descriptionScroll.getVerticalBar().setPageIncrement(lineHeight);
		descriptionScroll.getVerticalBar().setIncrement(lineHeight);
		descriptionScroll.setBackground(templateGroup.getBackground());
		descriptionScroll.setForeground(templateGroup.getForeground());
		
		
		// Manage repositories link
		Composite manageReposComp = new Composite(composite, SWT.NONE);
		manageReposComp.setLayout(new GridLayout(2, false));
		manageReposComp.setLayoutData(new GridData(GridData.END, GridData.FILL, false, false, 2, 1));
		
		Label manageRepoLabel = new Label(manageReposComp, SWT.NONE);
		manageRepoLabel.setText(Messages.NewProjectPage_ManageRepoLabel);
		manageRepoLabel.setLayoutData(new GridData(GridData.END, GridData.CENTER, false, false));
		
		Link manageRepoLink = new Link(manageReposComp, SWT.NONE);
		manageRepoLink.setText("<a>" + Messages.NewProjectPage_ManageRepoLink + "</a>");
		manageRepoLink.setToolTipText(Messages.NewProjectPage_ManageRepoTooltip);
		manageRepoLink.setLayoutData(new GridData(GridData.END, GridData.CENTER, false, false));

		manageRepoLink.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				List<RepositoryInfo> repoList;
				try {
					repoList = connection.requestRepositories();
					RepositoryManagementDialog repoDialog = new RepositoryManagementDialog(getShell(), connection, repoList);
					if (repoDialog.open() == Window.OK) {
						if (repoDialog.hasChanges()) {
							IRunnableWithProgress runnable = new IRunnableWithProgress() {
								@Override
								public void run(IProgressMonitor monitor) throws InvocationTargetException {
									SubMonitor mon = SubMonitor.convert(monitor, Messages.RepoUpdateTask, 100);
									IStatus status = repoDialog.updateRepos(mon.split(75));
									if (!status.isOK()) {
										throw new InvocationTargetException(status.getException(), status.getMessage());
									}
									if (mon.isCanceled()) {
										return;
									}
									try {
										mon = mon.split(25);
										mon.setTaskName(Messages.NewProjectPage_RefreshTemplatesTask);
										templateList = connection.requestProjectTemplates(true);
										mon.worked(25);
									} catch (Exception e) {
										throw new InvocationTargetException(e, Messages.NewProjectPage_RefreshTemplatesError);
									}
								}
							};
							try {
								getWizard().getContainer().run(true, true, runnable);
							} catch (InvocationTargetException e) {
								MessageDialog.openError(getShell(), Messages.RepoUpdateErrorTitle, e.getMessage());
								return;
							} catch (InterruptedException e) {
								// The user cancelled the operation
								return;
							}
							updateSelectionTable();
						}
					}
				} catch (Exception e) {
					MessageDialog.openError(getShell(), Messages.RepoListErrorTitle, NLS.bind(Messages.RepoListErrorMsg, e));
				}
			}
		});

		// Listeners
		filterText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent event) {
				updateSelectionTable();
			}
		});

		filterText.addListener(SWT.KeyDown, new Listener() {
			@Override
			public void handleEvent(Event event) {
				if (event.keyCode == SWT.ARROW_DOWN) {
					if (selectionTable.getItemCount() > 0) {
						selectionTable.setSelection(0);
						updateDescription();
						selectionTable.setFocus();
					}
					event.doit = false;
					setPageComplete(validate());
				}
			}
		});

		selectionTable.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) {
				updateDescription();
				setPageComplete(validate());
			}
		});
		
		projectNameText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent arg0) {
				setPageComplete(validate());
			}
		});
		
		if (selectionTable.getItemCount() > 0) {
			selectionTable.setSelection(0);
		}
		updateDescription();
		// resize description since the UI isn't visible yet
		descriptionLabel.setSize(descriptionLabel.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		projectNameText.setFocus();
	}

	private boolean validate() {
		String projectName = projectNameText.getText();
		
		if (projectName == null || projectName.isEmpty()) {
			setErrorMessage(Messages.NewProjectPage_EmptyProjectName);
			return false;
		}
		if (!projectNamePattern.matcher(projectName).matches()) {
			setErrorMessage(Messages.NewProjectPage_InvalidProjectName);
			return false;
		}
		if (connection.getAppByName(projectName) != null) {
			setErrorMessage(NLS.bind(Messages.NewProjectPage_ProjectExistsError, projectName));
			return false;
		}
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
		if (project != null && project.exists()) {
			setErrorMessage(NLS.bind(Messages.NewProjectPage_EclipseProjectExistsError, projectName));
			return false;
		}
		setErrorMessage(null);
		return selectionTable.getSelectionCount() == 1 && projectName != null && !projectName.isEmpty();
	}

	public ProjectTemplateInfo getProjectTemplateInfo() {
		if (selectionTable != null) {
			int index = selectionTable.getSelectionIndex();
			if (index >= 0) {
				TableItem item = selectionTable.getItem(index);
				return (ProjectTemplateInfo)item.getData();
			}
		}
		return null;
	}
	
	public CodewindConnection getConnection() {
		return connection;
	}
	
	public String getProjectName() {
		if (projectNameText != null) {
			return projectNameText.getText();
		}
		return null;
	}

	private void createItems(Table table, String filter) {
		// Create the items for the table.
		table.removeAll();
		pattern.setPattern("*" + filter + "*");
		for (ProjectTemplateInfo templateInfo : templateList) {
			String template = templateInfo.getLabel();
			String type = ProjectType.getDisplayName(templateInfo.getProjectType());
			String language = ProjectLanguage.getDisplayName(templateInfo.getLanguage());
			if (pattern.matches(template) || (type != null && pattern.matches(type)) || (language != null && pattern.matches(language))) {
				TableItem item = new TableItem(table, SWT.NONE);
				item.setForeground(table.getForeground());
				item.setBackground(table.getBackground());
				item.setText(0, template);
				if (type != null) {
					item.setText(1, type);
				}
				if (language != null) {
					item.setText(2, language);
				}
				item.setData(templateInfo);
			}
		}
	}
	
	public static void sortTable(Table table, TableColumn column) {
		TableItem[] items = table.getItems();
		int rows = items.length;
		int dir = table.getSortDirection() == SWT.DOWN ? 1 : -1;
		TableColumn currentColumn = table.getSortColumn();
		int columnNum = 0;
		for (int j = 0; j < table.getColumnCount(); j++) {
			if (table.getColumn(j).equals(column)) {
				columnNum = j;
				break;
			}
		}
		if (column.equals(currentColumn))
			dir = -dir;
		else
			dir = 1;

		// sort an index map, then move the actual rows
		int[] map = new int[rows];
		for (int i = 0; i < rows; i++)
			map[i] = i;

		for (int i = 0; i < rows - 1; i++) {
			for (int j = i + 1; j < rows; j++) {
				TableItem a = items[map[i]];
				TableItem b = items[map[j]];
				if ((a.getText(columnNum).toLowerCase().compareTo(b.getText(columnNum).toLowerCase()) * dir > 0)) {
					int t = map[i];
					map[i] = map[j];
					map[j] = t;
				}
			}
		}

		// can't move existing items or delete first, so append new items to the end and then delete existing rows
		for (int i = 0; i < rows; i++) {
			int n = map[i];
			TableItem item = new TableItem(table, SWT.NONE);
			for (int j = 0; j < table.getColumnCount(); j++)
				item.setText(j, items[n].getText(j));

			item.setImage(items[n].getImage());
			item.setForeground(items[n].getForeground());
			item.setBackground(items[n].getBackground());
			item.setGrayed(items[n].getGrayed());
			item.setChecked(items[n].getChecked());
			item.setData(items[n].getData());
			items[n].dispose();
		}

		table.setSortDirection(dir == 1 ? SWT.DOWN : SWT.UP);
		table.setSortColumn(column);
	}
	
	private void updateSelectionTable() {
		String text = filterText.getText();
		if (text == null) {
			text = "";
		}
		createItems(selectionTable, text);
		if (selectionTable.getItemCount() > 0)
			selectionTable.select(0);
		updateDescription();
		setPageComplete(validate());
	}
	
	public void updateDescription() {
		// Update the description
		TableItem[] items = selectionTable.getSelection();
		String description = "";
		boolean enabled = false;
		if (items.length == 1) {
			enabled = true;
			description = ((ProjectTemplateInfo)items[0].getData()).getDescription();
			if (description == null || description.isEmpty()) {
				description = Messages.NewProjectPage_DescriptionNone;
			}
		}
		String text = NLS.bind(Messages.NewProjectPage_DescriptionLabel, description);
		
		descriptionLabel.setText(text);
		
		// resize label to make scroll bars appear
		int width = descriptionLabel.getParent().getClientArea().width;
		descriptionLabel.setSize(width, descriptionLabel.computeSize(width, SWT.DEFAULT).y);
		
		// resize again if scroll bar added or removed
		int newWidth = descriptionLabel.getParent().getClientArea().width;
		if (newWidth != width) {
			descriptionLabel.setSize(newWidth, descriptionLabel.computeSize(newWidth, SWT.DEFAULT).y);
		}
		
		descriptionLabel.setEnabled(enabled);
	}
	
	public void resizeColumns(Table table) {
		TableLayout tableLayout = new TableLayout();

		int numColumns = table.getColumnCount();
		for (int i = 0; i < numColumns; i++)
			table.getColumn(i).pack();

		for (int i = 0; i < numColumns; i++) {
			int w = Math.max(75, table.getColumn(i).getWidth());
			tableLayout.addColumnData(new ColumnWeightData(w, w, true));
		}

		table.setLayout(tableLayout);
	}
	
	private void setupConnection() {
		final CodewindManager manager = CodewindManager.getManager();
		connection = manager.getLocalConnection();
		if (connection != null && connection.isConnected()) {
			return;
		}
		InstallStatus status = manager.getInstallStatus();
		if (status.isStarted()) {
			connection = manager.createLocalConnection();
			return;
		}
		if (!status.isInstalled()) {
			Logger.logError("In NewCodewindProjectPage setupConnection method and Codewind is not installed or has unknown status.");
			connection = null;
			return;
		}
		
		connection = null;
		IRunnableWithProgress runnable = new IRunnableWithProgress() {
			@Override
			public void run(IProgressMonitor monitor) throws InvocationTargetException {
				try {
					ProcessResult result = InstallUtil.startCodewind(status.getVersion(), monitor);
					if (result.getExitValue() != 0) {
						Logger.logError("Installer start failed with return code: " + result.getExitValue() + ", output: " + result.getOutput() + ", error: " + result.getError()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						String errorText = result.getError() != null && !result.getError().isEmpty() ? result.getError() : result.getOutput();
						throw new InvocationTargetException(null, "There was a problem while trying to start Codewind: " + errorText); //$NON-NLS-1$
					}
					connection = manager.createLocalConnection();
					ViewHelper.refreshCodewindExplorerView(null);
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
			return;
		} catch (InterruptedException e) {
			Logger.logError("Codewind start was interrupted", e); //$NON-NLS-1$
			return;
		}
		
		if (connection == null) {
			Logger.logError("Failed to connect to Codewind at: " + manager.getLocalURI()); //$NON-NLS-1$
		}
	}
	
	private void getTemplates() {
		try {
			templateList = connection.requestProjectTemplates(true);
		} catch (Exception e) {
			Logger.logError("An error occurred trying to get the list of templates", e); //$NON-NLS-1$
		}
	}
}
