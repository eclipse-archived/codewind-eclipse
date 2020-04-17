/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *	 IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.codewind.ui.internal.wizards;

import org.eclipse.codewind.core.internal.CoreUtil;
import org.eclipse.codewind.core.internal.connection.CodewindConnection;
import org.eclipse.codewind.ui.CodewindUIPlugin;
import org.eclipse.codewind.ui.internal.IDEUtil;
import org.eclipse.codewind.ui.internal.messages.Messages;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
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
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.SearchPattern;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;

public class ProjectSelectionPage extends WizardPage {
	
	private SearchPattern pattern = new SearchPattern(SearchPattern.RULE_PATTERN_MATCH | SearchPattern.RULE_PREFIX_MATCH | SearchPattern.RULE_BLANK_MATCH);
	private final CodewindConnection connection;
	private Button workspaceProject;
	private Text filterText;
	private CheckboxTableViewer projectList;
	private Text noProjectsText;
	private Button fileSystemProject;
	private Text pathText;
	private IProject project = null;
	private String projectPath = null;
	private boolean hasValidProjects = false;

	protected ProjectSelectionPage(CodewindConnection connection) {
		super(Messages.SelectProjectPageName);
		setTitle(Messages.SelectProjectPageTitle);
		setDescription(Messages.SelectProjectPageDescription);
		pattern.setPattern("*");
		this.connection = connection;
		hasValidProjects = hasValidProjects();
	}

	@Override
	public void createControl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 3;
		layout.horizontalSpacing = 5;
		layout.verticalSpacing = 7;
		composite.setLayout(layout);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		composite.setLayoutData(data);

		// Workspace project radio button
		workspaceProject = new Button(composite, SWT.RADIO);
		workspaceProject.setText(Messages.SelectProjectPageWorkspaceProject);
		workspaceProject.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, false, false, 3, 1));
		
		if (hasValidProjects) {
			// Filter text
			filterText = new Text(composite, SWT.BORDER);
			data = new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1);
			data.horizontalIndent = 20;
			filterText.setLayoutData(data);
			filterText.setMessage(Messages.SelectProjectPageFilterText);
			
			// Workspace project list
			projectList = CheckboxTableViewer.newCheckList(composite, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
			projectList.setContentProvider(new WorkbenchContentProvider());
			projectList.setLabelProvider(new WorkbenchLabelProvider());
			projectList.setInput(ResourcesPlugin.getWorkspace().getRoot());
			data = new GridData(SWT.FILL, SWT.FILL, true, true, 3, 1);
			data.heightHint = 160;
			data.horizontalIndent = 20;
			projectList.getTable().setLayoutData(data);
			projectList.addFilter(new ViewerFilter() {
				@Override
				public boolean select(Viewer viewer, Object parentElem, Object elem) {
					if (!(elem instanceof IProject)) {
						return false;
					}
					return isValidProject((IProject)elem);
				}
			});
			
			workspaceProject.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					filterText.setEnabled(workspaceProject.getSelection());
					projectList.getTable().setEnabled(workspaceProject.getSelection());
					validate();
				}
			});
			
			filterText.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(ModifyEvent event) {
					String filter = filterText.getText();
					if (filter == null || filter.isEmpty()) {
						filterText.setMessage(Messages.SelectProjectPageFilterText);
						pattern.setPattern("*");
					} else {
						pattern.setPattern("*" + filter + "*");
					}
					projectList.refresh();
				}
			});
			
			projectList.addCheckStateListener(new ICheckStateListener() {
				@Override
				public void checkStateChanged(CheckStateChangedEvent event) {
					if (event.getChecked()) {
						projectList.setCheckedElements(new Object[] {event.getElement()});
					}
					validate();
				}
			});
		} else {
			noProjectsText = new Text(composite, SWT.READ_ONLY | SWT.WRAP);
			noProjectsText.setText(Messages.SelectProjectPageNoWorkspaceProjects);
			data = new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1);
			data.horizontalIndent = 20;
			data.widthHint = 150;
			noProjectsText.setLayoutData(data);
			IDEUtil.normalizeBackground(noProjectsText, composite);
		}
		
		new Label(composite, SWT.NONE).setLayoutData(new GridData(GridData.FILL, GridData.FILL, false, false, 3, 1));
		
		// File system project radio button
		fileSystemProject = new Button(composite, SWT.RADIO);
		fileSystemProject.setText(Messages.SelectProjectPageFilesystemProject);
		fileSystemProject.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, false, false, 3, 1));
		
		Label pathLabel = new Label(composite, SWT.NONE);
		pathLabel.setText(Messages.SelectProjectPagePathLabel);
		data = new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1);
		data.horizontalIndent = 20;
		pathLabel.setLayoutData(data);
		
		// Project path
		pathText = new Text(composite, SWT.BORDER);
		data = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
		data.horizontalIndent = 20;
		pathText.setLayoutData(data);
		
		// Browse button
		Button browseButton = new Button(composite, SWT.PUSH);
		browseButton.setText(Messages.SelectProjectPageBrowseButton);
		data = new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1);
		data.horizontalIndent = 20;
		browseButton.setData(data);
		
		fileSystemProject.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				pathLabel.setEnabled(fileSystemProject.getSelection());
				pathText.setEnabled(fileSystemProject.getSelection());
				browseButton.setEnabled(fileSystemProject.getSelection());
				validate();
			}
		});
		
		pathText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent arg0) {
				validate();
			}
		});
		
		browseButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent se) {
				DirectoryDialog dialog = new DirectoryDialog(parent.getShell());
				dialog.setMessage(Messages.SelectProjectPageFilesystemDialogTitle);
				String selectedDirectory = dialog.open();
				if (selectedDirectory != null && !selectedDirectory.trim().isEmpty()) {
					pathText.setText(selectedDirectory.trim());
				}
				validate();
			}
		});

		// Add Context Sensitive Help
		PlatformUI.getWorkbench().getHelpSystem().setHelp(composite, CodewindUIPlugin.MAIN_CONTEXTID);

		if (hasValidProjects) {
			workspaceProject.setSelection(true);
			fileSystemProject.setSelection(false);
			filterText.setFocus();
		} else {
			fileSystemProject.setSelection(true);
			workspaceProject.setSelection(false);
			workspaceProject.setEnabled(false);
			noProjectsText.setEnabled(false);
			pathText.setFocus();
		}
		
		if (hasValidProjects) {
			filterText.setEnabled(workspaceProject.getSelection());
			projectList.getTable().setEnabled(workspaceProject.getSelection());
		}
		pathLabel.setEnabled(fileSystemProject.getSelection());
		pathText.setEnabled(fileSystemProject.getSelection());
		browseButton.setEnabled(fileSystemProject.getSelection());
		
		if (hasValidProjects && workspaceProject.getSelection() && projectList.getTable().getItemCount() == 1) {
			projectList.setChecked(projectList.getElementAt(0), true);
		}
		
		validate();
		setControl(composite);
	}
	
	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible) {
			if (workspaceProject.getSelection()) {
				filterText.setFocus();
			} else {
				pathText.setFocus();
			}
		}
	}

	private boolean hasValidProjects() {
		IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		for (IProject project : projects) {
			if (isValidProject(project)) {
				return true;
			}
		}
		return false;
	}
	
	private boolean isValidProject(IProject project) {
		if (!project.isAccessible()) {
			return false;
		}
		if (!pattern.matches(project.getName())) {
			return false;
		}
		if (connection.getAppByLocation(project.getLocation()) != null) {
			return false;
		}
		return true;
	}
	
	private void validate() {
		String errorMsg = null;
		if (workspaceProject.getSelection()) {
			if (hasValidProjects) {
				Object[] checked = projectList.getCheckedElements();
				project = checked.length == 1 ? (IProject) checked[0] : null;
				// It is an error if the project is located in the codewind-data folder
				IPath dataPath = CoreUtil.getCodewindDataPath();
				if (project != null && dataPath != null && dataPath.isPrefixOf(project.getFullPath())) {
					errorMsg = NLS.bind(Messages.ProjectLocationInCodewindDataDirError, dataPath.toOSString());
					project = null;
				}
				projectPath = null;
			}
		} else {
			String text = pathText.getText();
			projectPath = text != null && !text.trim().isEmpty() ? text.trim() : null;
			project = null;
			if (projectPath != null) {
				IPath path = new Path(projectPath);
				if (path.segmentCount() == 0) {
					errorMsg = Messages.SelectProjectPageIncompletePath;
					projectPath = null;
				} else if (!path.toFile().exists() || !path.toFile().isDirectory()) {
					errorMsg = Messages.SelectProjectPageNoExistError;
					projectPath = null;
				} else {
					if (connection.getAppByLocation(path) != null) {
						// It is an error if a Codewind project already exists with the same location
						errorMsg = NLS.bind(Messages.SelectProjectPageCWProjectExistsError, path);
						projectPath = null;
					} else {
						// It is an error if a project exists with the same name but has a different location
						IProject existingProject = ResourcesPlugin.getWorkspace().getRoot().getProject(path.lastSegment());
						if (existingProject.exists() && !path.toFile().equals(existingProject.getLocation().toFile())) {
							errorMsg = NLS.bind(Messages.SelectProjectPageProjectExistsError, path.lastSegment());
							projectPath = null;
						} else {
							// It is an error if the project is located in the codewind-data folder
							IPath dataPath = CoreUtil.getCodewindDataPath();
							if (dataPath != null && dataPath.isPrefixOf(path)) {
								errorMsg = NLS.bind(Messages.ProjectLocationInCodewindDataDirError, dataPath.toOSString());
								projectPath = null;
							}
						}
					}
				}
			}
		}
		setErrorMessage(errorMsg);
		getWizard().getContainer().updateButtons();
	}

	@Override
	public boolean canFlipToNextPage() {
		return canFinish();
	}

	public boolean isActivePage() {
		return isCurrentPage();
	}

	public boolean canFinish() {
		return project != null || projectPath != null;
	}
	
	public IProject getProject() {
		return project;
	}
	
	public IPath getProjectPath() {
		if (project != null) {
			return project.getLocation();
		} else if (projectPath != null) {
			return new Path(projectPath);
		}
		return null;
	}
}
