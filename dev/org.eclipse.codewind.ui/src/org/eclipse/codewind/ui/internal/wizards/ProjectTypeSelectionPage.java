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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
		this.projectPath = projectPath;
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
		typeViewer.setInput(typeMap.values());
		GridData typeViewerData = new GridData(GridData.FILL, GridData.FILL, true, true);
		typeViewerData.minimumHeight = 200;
		typeViewer.getTable().setLayoutData(typeViewerData);
	   
		subtypeLabel = new Text(composite, SWT.READ_ONLY);
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
					Object element = event.getElement();
					typeViewer.setCheckedElements(new Object[] { element });
					updateSubtypes((ProjectTypeInfo) element);
				}
				else {
					updateSubtypes(null);
				}
				getWizard().getContainer().updateButtons();
			}
		});

		subtypeViewer.addCheckStateListener(new ICheckStateListener() {
			@Override
			public void checkStateChanged(CheckStateChangedEvent event) {
				if (event.getChecked()) {
					subtypeViewer.setCheckedElements(new Object[] { event.getElement() });
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
							updateTables(false);
						}
					}
				} catch (Exception e) {
					MessageDialog.openError(getShell(), Messages.RepoListErrorTitle, NLS.bind(Messages.RepoListErrorMsg, e));
				}
			}
		});
 
		subtypeLabel.setVisible(false);
		subtypeViewer.getTable().setVisible(false);

		setProjectPath(projectPath);

		typeViewer.getTable().setFocus();
		setControl(composite);
	}

	public boolean canFinish() {
		
		if (typeViewer == null)
			return false;
		
		ProjectTypeInfo projectType = getType();
		if (projectType == null)
			return false;
		
		// TODO
		
		return true;
	}
	
	private class ProjectTypeLabelProvider extends LabelProvider {

		@Override
		public Image getImage(Object element) {
			return null;
		}

		@Override
		public String getText(Object element) {
			String label = ((ProjectTypeInfo) element).getId();
			return ProjectType.getDisplayName(label);
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
		updateTables(true);
	}
	
	public CodewindConnection getConnection() {
		return connection;
	}
	
	public ProjectTypeInfo getType() {
		// return what's selected in the types viewer
		Object[] checked = typeViewer.getCheckedElements();		
		return (checked.length == 0) ? null : (ProjectTypeInfo) checked[0];
	}
	
	public ProjectSubtypeInfo getSubtype() {
		
		ProjectTypeInfo projectType = getType();
		
		// no subtype if:
		// 1. no type is selected
		// 2. selected type has no subtypes label; it is a Codewind built-in type which has language, not subtype
		// 3. not currently allowing subtypes selection
		if (projectType == null ||  
			projectType.getSubtypesLabel().length() == 0 || 
			!allowSecondarySelection(projectType)) {
			return null;
		}
		
		// return what's selected in the subtypes viewer
		Object[] checked = subtypeViewer.getCheckedElements();
		return (checked.length == 0) ? null : (ProjectSubtypeInfo) checked[0];
	}
	
	public String getLanguage() {
		
		ProjectTypeInfo projectType = getType();
		
		// no language if no type is selected
		if (projectType == null)
			return null;
		
		// selected type has no subtypes label; it is a Codewind built-in type
		// what's selected in the subtypes viewer is the language
		if (projectType.getSubtypesLabel().length() == 0) {
			Object[] checked = subtypeViewer.getCheckedElements();
			return (checked.length == 0) ? ProjectLanguage.LANGUAGE_UNKNOWN.getId() : ((ProjectSubtypeInfo) checked[0]).id;
		}
		
		// for non-Codewind types, fallback to detected language
		return projectInfo.language.getId();
	}

	private void updateTables(boolean init) {
		
		if (typeViewer == null || typeViewer.getTable().isDisposed()) {
			return;
		}
		
		Collection<ProjectTypeInfo> projectTypes = typeMap.values();
		typeViewer.setInput(projectTypes);
		if (projectTypes.size() == 0) {
			setErrorMessage(Messages.SelectProjectTypeNoProjectTypes);
			updateSubtypes(null);
			return;
		}
		setErrorMessage(null);

		// when first entering the wizard, attempt to select type that matches the detected type
		if (init) { 
			ProjectTypeInfo projectType = typeMap.get(projectInfo.type.getId());
			if (projectType != null)
				typeViewer.setCheckedElements(new Object[] { projectType });
			else
				typeViewer.setAllChecked(false);
			updateSubtypes(projectType);
		}
		// otherwise, see if anything got unchecked
		// e.g. removing a repo that contained previously selected type
		else {
			Object[] checked = typeViewer.getCheckedElements();
			if (checked.length == 0)
				updateSubtypes(null);
		}
	}
	
	// allow subtype/language selection for:
	// 1. project type of docker (select language)
	// 2. project type different than the detected one
	//    e.g. project was detected as docker, then user switch selection to an 
	//    extension project type, they should be allowed to choose the subtype
	private boolean allowSecondarySelection(ProjectTypeInfo projectType) {
		String type = projectType.getId();
		return type.equals(ProjectType.TYPE_DOCKER.getId()) || !type.equals(projectInfo.type.getId());
	}
	
	private void updateSubtypes(ProjectTypeInfo projectType) {
		
		if (subtypeViewer == null || subtypeViewer.getTable().isDisposed()) {
			return;
		}
		
		boolean shouldShow = false;
		
		if (projectType == null)
			subtypeViewer.setInput(new Object[0]);
		else {
			List<ProjectSubtypeInfo> projectSubtypes = projectType.getSubtypes();
			subtypeViewer.setInput(projectSubtypes);
			
			// only 1 possible choice, select it
			if (projectSubtypes.size() == 1) {
				subtypeViewer.setCheckedElements(new Object[] { projectSubtypes.get(0) });
			}
			// otherwise if more than 1 choice and check if subtype/language selection is allowed
			else if (projectSubtypes.size() > 1 && allowSecondarySelection(projectType)) {
					
				shouldShow = true;
				
				String label = projectType.getSubtypesLabel();
				if ("".equals(label))
					label = Messages.SelectProjectTypePageLanguageLabel;
				else
					label = Messages.bind(Messages.SelectProjectTypePageSubtypeLabel, label);
				subtypeLabel.setText(label);
				subtypeLabel.pack();
			}
		}
		
		subtypeLabel.setVisible(shouldShow);
		subtypeViewer.getTable().setVisible(shouldShow);
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
		List<ProjectTypeInfo> projectTypes = null;
		Map<String, ProjectTypeInfo> typeMap = new HashMap<String, ProjectTypeInfo>();
		try {
			projectTypes = connection.requestProjectTypes();
		} catch (Exception e) {
			Logger.logError("An error occurred trying to get the list of project types for connection: " + connection.baseUrl, e); //$NON-NLS-1$
			return null;
		}
		if (projectTypes == null || projectTypes.isEmpty()) {
			Logger.log("The list of project types is empty for connection: " + connection.baseUrl); //$NON-NLS-1$
			return typeMap;
		}
		for (ProjectTypeInfo projectType : projectTypes) {
			ProjectTypeInfo existing = typeMap.get(projectType.getId());
			if (existing == null) {
				typeMap.put(projectType.getId(), projectType);
			}
			else {
				existing.addSubtypes(projectType.getSubtypes());
			}
		}
		return typeMap;
	}

}
