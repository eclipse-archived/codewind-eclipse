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

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.codewind.core.internal.InstallUtil;
import org.eclipse.codewind.core.internal.Logger;
import org.eclipse.codewind.core.internal.connection.CodewindConnection;
import org.eclipse.codewind.core.internal.connection.ProjectTypeInfo;
import org.eclipse.codewind.core.internal.connection.ProjectTypeInfo.ProjectSubtypeInfo;
import org.eclipse.codewind.core.internal.connection.RepositoryInfo;
import org.eclipse.codewind.core.internal.constants.ProjectInfo;
import org.eclipse.codewind.core.internal.constants.ProjectLanguage;
import org.eclipse.codewind.core.internal.constants.ProjectType;
import org.eclipse.codewind.ui.internal.messages.Messages;
import org.eclipse.codewind.ui.internal.prefs.RepositoryManagementDialog;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Text;

public class ProjectTypeSelectionPage extends WizardPage {

	private CodewindConnection connection = null;
	private IPath projectPath = null;
	private Map<String, ProjectTypeInfo> typeMap;
	private String type = null;
	private ProjectSubtypeInfo subtype = null;
//	private String language = null;
	private Text subtypeLabel = null;
	private CheckboxTableViewer subtypeViewer = null;
	private Text typeLabel = null;
	private CheckboxTableViewer typeViewer = null;
	private ProjectInfo projectInfo = null;

	protected ProjectTypeSelectionPage(CodewindConnection connection, IPath projectPath) {
		super(Messages.SelectProjectTypePageName);
		setTitle(Messages.SelectProjectTypePageTitle);
		setDescription(Messages.SelectProjectTypePageDescription);
		this.connection = connection;
		setProjectPath(projectPath);
		this.typeMap = getProjectTypeMap();
	}

	@Override
	public void createControl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		layout.horizontalSpacing = 5;
		layout.verticalSpacing = 7;
		composite.setLayout(layout);
		composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		if (typeMap == null) {
			Text errorLabel = new Text(composite, SWT.READ_ONLY | SWT.WRAP);
			errorLabel.setText(Messages.SelectProjectTypeErrorLabel);
			setControl(composite);
			return;
		}
		if (typeMap.isEmpty()) {
			setErrorMessage(Messages.SelectProjectTypeNoProjectTypes);
		}
		
		typeLabel = new Text(composite, SWT.READ_ONLY);
		typeLabel.setText(Messages.SelectProjectTypePageProjectTypeLabel);
		typeLabel.setLayoutData(new GridData(GridData.BEGINNING, GridData.CENTER, false, false));
		typeLabel.setBackground(composite.getBackground());
		typeLabel.setForeground(composite.getForeground());
		
		typeViewer = CheckboxTableViewer.newCheckList(composite, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		typeViewer.setContentProvider(ArrayContentProvider.getInstance());
		typeViewer.setLabelProvider(new ProjectTypeLabelProvider());
		typeViewer.setComparator(new ViewerComparator());
		typeViewer.setInput(typeMap.keySet());
		GridData typeViewerData = new GridData(GridData.FILL, GridData.FILL, true, true);
		typeViewerData.minimumHeight = 200;
		typeViewer.getTable().setLayoutData(typeViewerData);
	   
		subtypeLabel = new Text(composite, SWT.READ_ONLY);
		subtypeLabel.setText(Messages.SelectProjectTypePageLanguageLabel);
		subtypeLabel.setLayoutData(new GridData(GridData.BEGINNING, GridData.CENTER, false, false));
		subtypeLabel.setBackground(composite.getBackground());
		subtypeLabel.setForeground(composite.getForeground());
		
		subtypeViewer = CheckboxTableViewer.newCheckList(composite, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		subtypeViewer.setContentProvider(ArrayContentProvider.getInstance());
		subtypeViewer.setLabelProvider(new ProjectSubtypeLabelProvider());
		subtypeViewer.setComparator(new ViewerComparator());
		subtypeViewer.getTable().setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true));
		
		typeViewer.addCheckStateListener(new ICheckStateListener() {
			@Override
			public void checkStateChanged(CheckStateChangedEvent event) {
				if (event.getChecked()) {
					typeViewer.setCheckedElements(new Object[] {event.getElement()});
					type = (String) event.getElement();
				} else {
					type = null;
				}
				
				List<ProjectSubtypeInfo> projectSubtypes = getProjectSubtypes(type);
				if (projectSubtypes.size() > 1) {
					if (subtype != null) {
						boolean found = false;
						for (ProjectSubtypeInfo projectSubtype : projectSubtypes) {
							if (subtype.equals(projectSubtype)) {
								subtypeViewer.setCheckedElements(new Object[] {subtype});
								found = true;
								break;
							}
						}
						if (!found) {
							subtype = null;
						}
					}
					subtypeLabel.setVisible(true);
					subtypeViewer.setInput(projectSubtypes);
					subtypeViewer.getTable().setVisible(true);
				} else {
					if (projectSubtypes.size() == 1) {
						subtype = projectSubtypes.get(0);
					} else {
						subtype = null;
					}
					subtypeLabel.setVisible(false);
					subtypeViewer.getTable().setVisible(false);
				}
				getWizard().getContainer().updateButtons();
			}
		});

		subtypeViewer.addCheckStateListener(new ICheckStateListener() {
			@Override
			public void checkStateChanged(CheckStateChangedEvent event) {
				if (event.getChecked()) {
					subtypeViewer.setCheckedElements(new Object[] {event.getElement()});
					subtype = (ProjectSubtypeInfo) event.getElement();
				} else {
					subtype = null;
				}
				getWizard().getContainer().updateButtons();
			}
		});
		
		// Manage repositories link
		Composite manageReposComp = new Composite(composite, SWT.NONE);
		manageReposComp.setLayout(new GridLayout(2, false));
		manageReposComp.setLayoutData(new GridData(GridData.END, GridData.FILL, false, false, 1, 1));
		
		Label manageRepoLabel = new Label(manageReposComp, SWT.NONE);
		manageRepoLabel.setText(Messages.SelectProjectTypeManageRepoLabel);
		manageRepoLabel.setLayoutData(new GridData(GridData.END, GridData.CENTER, false, false));
		
		Link manageRepoLink = new Link(manageReposComp, SWT.NONE);
		manageRepoLink.setText("<a>" + Messages.SelectProjectTypeManageRepoLink + "</a>");
		manageRepoLink.setToolTipText(Messages.SelectProjectTypeManageRepoTooltip);
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
										mon.setTaskName(Messages.SelectProjectTypeRefreshTypesTask);
										typeMap = getProjectTypeMap();
										mon.worked(25);
									} catch (Exception e) {
										throw new InvocationTargetException(e, Messages.SelectProjectTypeRefreshTypesError);
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
							updateTables();
						}
					}
				} catch (Exception e) {
					MessageDialog.openError(getShell(), Messages.RepoListErrorTitle, NLS.bind(Messages.RepoListErrorMsg, e));
				}
			}
		});
 
		subtypeLabel.setVisible(false);
		subtypeViewer.getTable().setVisible(false);

		updateTables();

		typeViewer.getTable().setFocus();
		setControl(composite);
	}

	public boolean canFinish() {
		if (type == null || subtype == null) {
			return false;
		}
		return true;
	}
	
	private List<ProjectSubtypeInfo> getProjectSubtypes(String type) {
		ProjectTypeInfo projectType = typeMap.get(type);
		return projectType == null ? Collections.emptyList() : projectType.getProjectSubtypes();
	}
	
	private class ProjectTypeLabelProvider extends LabelProvider {

		@Override
		public Image getImage(Object element) {
			return null;
		}

		@Override
		public String getText(Object element) {
			return ProjectType.getDisplayName((String)element);
		}
		
	}

	private class ProjectSubtypeLabelProvider extends LabelProvider {

		@Override
		public Image getImage(Object element) {
			return null;
		}

		@Override
		public String getText(Object element) {
			String label = ((ProjectSubtypeInfo) element).label;
			return ProjectLanguage.getDisplayName(label);
		}
	}

	public void setProjectPath(IPath projectPath) {
		this.projectPath = projectPath;
		this.projectInfo = null;
		if (projectPath == null) {
			return;
		}
		if (getWizard() != null && getWizard().getContainer() != null) {
			IRunnableWithProgress runnable = new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException {
					SubMonitor mon = SubMonitor.convert(monitor, NLS.bind(Messages.SelectProjectTypeValidateTask, projectPath.lastSegment()), 100);
					projectInfo = getProjectInfo(mon.split(100));
				}
			};
			try {
				getContainer().run(true, true, runnable);
			} catch (InvocationTargetException e) {
				Logger.logError("An error occurred getting the project info for: " + projectPath.lastSegment(), e);
				return;
			} catch (InterruptedException e) {
				// The user cancelled the operation
				return;
			}
		} else {
			projectInfo = getProjectInfo(new NullProgressMonitor());
		}
		updateTables();
	}
	
	public CodewindConnection getConnection() {
		return connection;
	}
	
	public String getType() {
		// Type should not be null since the page cannot finish until a type is selected
		if (type == null) {
			Logger.logError("The project type is null on the project type selection page");
			return ProjectType.TYPE_UNKNOWN.getId();
		}
		return type;
	}
	
	public String getLanguage() {
//		if (language == null) {
			// The language is optional so this is not an error
			return ProjectLanguage.LANGUAGE_UNKNOWN.getId();
//		}
//		return language;
	}

	private void updateTables() {
		if (typeViewer == null || typeViewer.getTable().isDisposed()) {
			return;
		}
		Set<String> projectTypes = typeMap.keySet();
		typeViewer.setInput(projectTypes);
		if (projectTypes.size() == 0) {
			setErrorMessage(Messages.SelectProjectTypeNoProjectTypes);
			updateSubtypes(null, null);
			return;
		}
		setErrorMessage(null);
		if (type != null && typeMap.containsKey(type)) {
			// Maintain the current selection
			typeViewer.setCheckedElements(new Object[] {type});
			List<ProjectSubtypeInfo> projectSubtypes = getProjectSubtypes(type);
			updateSubtypes(projectSubtypes, subtype);
		} else {
			// If no selection, use the project info
			if (projectInfo != null) {
				type = projectInfo.type.getId();
//				language = projectInfo.language.getId();
				if (typeMap.containsKey(type)) {
					typeViewer.setCheckedElements(new Object[] {type});
					List<ProjectSubtypeInfo> projectSubtypes = getProjectSubtypes(type);
					updateSubtypes(projectSubtypes, subtype);
				}
			}
		}
	}
	
	private void updateSubtypes(List<ProjectSubtypeInfo> projectSubtypes, ProjectSubtypeInfo subtype) {
		if (subtypeViewer == null || subtypeViewer.getTable().isDisposed()) {
			return;
		}
		if (projectSubtypes != null && projectSubtypes.size() > 1) {
			subtypeLabel.setVisible(true);
			subtypeViewer.setInput(projectSubtypes);
			subtypeViewer.getTable().setVisible(true);
			if (subtype != null) {
				for (ProjectSubtypeInfo projectSubtype : projectSubtypes) {
					if (subtype.equals(projectSubtype)) {
						subtypeViewer.setCheckedElements(new Object[] {subtype});
						break;
					}
				}
			}
			subtypeViewer.getTable().setVisible(true);
		} else {
			subtypeLabel.setVisible(false);
			subtypeViewer.getTable().setVisible(false);
		}
	}

	private ProjectInfo getProjectInfo(IProgressMonitor monitor) {
		if (connection == null || projectPath == null) {
			return null;
		}

		try {
			return InstallUtil.validateProject(projectPath.lastSegment(), projectPath.toFile().getAbsolutePath(), monitor);
		} catch (Exception e) {
			Logger.logError("An error occurred trying to get the project type for project: " + projectPath.lastSegment(), e); //$NON-NLS-1$
		}

		return null;
	}

	private Map<String, ProjectTypeInfo> getProjectTypeMap() {
		List<ProjectTypeInfo> types = null;
		Map<String, ProjectTypeInfo> typeMap = new HashMap<String, ProjectTypeInfo>();
		try {
			types = connection.requestProjectTypes();
		} catch (Exception e) {
			Logger.logError("An error occurred trying to get the list of project types for connection: " + connection.baseUrl, e); //$NON-NLS-1$
			return null;
		}
		if (types == null || types.isEmpty()) {
			Logger.log("The list of project types is empty for connection: " + connection.baseUrl); //$NON-NLS-1$
			return typeMap;
		}
		for (ProjectTypeInfo type : types) {
			ProjectTypeInfo existingType = typeMap.get(type.getProjectType());
			if (existingType == null) {
				typeMap.put(type.getProjectType(), type);
			}
			else {
				existingType.addProjectSubtypes(type.getProjectSubtypes());
			}
		}
		return typeMap;
	}

}
