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

import static org.eclipse.codewind.core.internal.constants.ProjectType.TYPE_DOCKER;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.codewind.core.internal.Logger;
import org.eclipse.codewind.core.internal.cli.TemplateUtil;
import org.eclipse.codewind.core.internal.connection.CodewindConnection;
import org.eclipse.codewind.core.internal.connection.ProjectTypeInfo;
import org.eclipse.codewind.core.internal.connection.ProjectTypeInfo.ProjectSubtypeInfo;
import org.eclipse.codewind.core.internal.connection.RepositoryInfo;
import org.eclipse.codewind.core.internal.constants.ProjectInfo;
import org.eclipse.codewind.core.internal.constants.ProjectLanguage;
import org.eclipse.codewind.core.internal.constants.ProjectType;
import org.eclipse.codewind.ui.internal.messages.Messages;
import org.eclipse.codewind.ui.internal.prefs.RepositoryManagementDialog;
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
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Text;

public class ProjectTypeSelectionPage extends WizardPage {

	private CodewindConnection connection = null;
	private Map<String, ProjectTypeInfo> typeMap;
	private Text subtypeLabel = null;
	private CheckboxTableViewer subtypeViewer = null;
	private Text typeLabel = null;
	private CheckboxTableViewer typeViewer = null;
	private ProjectInfo detectedProjectInfo = null;
	private ProjectTypeInfo projectTypeInfo = null;
	private ProjectSubtypeInfo projectSubtypeInfo = null;

	protected ProjectTypeSelectionPage(CodewindConnection connection) {
		super(Messages.SelectProjectTypePageName);
		setTitle(Messages.SelectProjectTypePageTitle);
		setDescription(Messages.SelectProjectTypePageDescription);
		this.connection = connection;
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

		// Project type table
		typeLabel = new Text(composite, SWT.READ_ONLY);
		typeLabel.setText(Messages.SelectProjectTypePageProjectTypeLabel);
		typeLabel.setLayoutData(new GridData(GridData.BEGINNING, GridData.CENTER, false, false));
		typeLabel.setBackground(composite.getBackground());
		typeLabel.setForeground(composite.getForeground());
		
		typeViewer = CheckboxTableViewer.newCheckList(composite, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		typeViewer.setContentProvider(ArrayContentProvider.getInstance());
		typeViewer.setLabelProvider(new ProjectTypeLabelProvider());
		typeViewer.setComparator(new ViewerComparator());
		GridData typeViewerData = new GridData(GridData.FILL, GridData.FILL, true, true);
		typeViewerData.minimumHeight = 200;
		typeViewer.getTable().setLayoutData(typeViewerData);
	   
		// Project subtype table. This will show the language for docker project types. This table
		// is only shown in certain circumstances.
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
					projectTypeInfo = (ProjectTypeInfo) event.getElement();
					typeViewer.setCheckedElements(new Object[] { projectTypeInfo });
				} else {
					projectTypeInfo = null;
				}
				updateSubtypes(false, projectTypeInfo);
			}
		});

		subtypeViewer.addCheckStateListener(new ICheckStateListener() {
			@Override
			public void checkStateChanged(CheckStateChangedEvent event) {
				if (event.getChecked()) {
					projectSubtypeInfo = (ProjectSubtypeInfo) event.getElement();
					subtypeViewer.setCheckedElements(new Object[] { projectSubtypeInfo });
				} else {
					projectSubtypeInfo = null;
				}
				getWizard().getContainer().updateButtons();
			}
		});
		
		// Manage repositories link
		Composite manageReposComp = new Composite(composite, SWT.NONE);
		manageReposComp.setLayout(new GridLayout(1, false));
		manageReposComp.setLayoutData(new GridData(GridData.END, GridData.FILL, false, false, 1, 1));
		
		Link manageRepoLink = new Link(manageReposComp, SWT.NONE);
		manageRepoLink.setText(Messages.SelectProjectTypeManageRepoLabel + " <a>" + Messages.SelectProjectTypeManageRepoLink + "</a>");
		manageRepoLink.setToolTipText(Messages.SelectProjectTypeManageRepoTooltip);
		manageRepoLink.setLayoutData(new GridData(GridData.END, GridData.CENTER, false, false));

		manageRepoLink.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				List<RepositoryInfo> repoList;
				try {
					repoList = TemplateUtil.listTemplateSources(connection.getConid(), new NullProgressMonitor());
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
										typeMap = getProjectTypeMap(mon);
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
 
		// Don't show the subtype table by default, when updateTables is called it will decide
		// whether to show it nor not
		subtypeLabel.setVisible(false);
		subtypeViewer.getTable().setVisible(false);

		typeViewer.getTable().setFocus();
		setControl(composite);
	}
	
	public boolean isActivePage() {
		return isCurrentPage();
	}

	public boolean canFinish() {
		
		if (typeViewer == null)
			return false;
		
		ProjectTypeInfo projectType = getProjectTypeInfo();
		if (projectType == null)
			return false;
		
		// For docker type projects the language selection is optional
		// For non-docker type projects if the selected type is different than the detected type,
		// the user must choose a subtype to proceed
		if (!projectType.eq(TYPE_DOCKER) && (detectedProjectInfo == null || !projectType.eq(detectedProjectInfo.type))) {
			return subtypeViewer.getCheckedElements().length != 0;
		}
		
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
			// If the label is a language then this will return a display name
			// for the language, otherwise it will just return the label passed in
			return ProjectLanguage.getDisplayName(label);
		}
	}
	
	public void initPage(CodewindConnection connection, ProjectInfo detectedProjectInfo) {
		this.connection = connection;
		initTypeMap();
		this.detectedProjectInfo = detectedProjectInfo;
		// If the project type could not be detected then detectedProjectInfo will be null
		if (detectedProjectInfo == null) {
			projectTypeInfo = null;
			projectSubtypeInfo = null;
		} else {
			// The type may not be in the typeMap if the user has disabled some template sources
			projectTypeInfo = typeMap.get(detectedProjectInfo.type.getId());
			projectSubtypeInfo = projectTypeInfo != null ? projectTypeInfo.new ProjectSubtypeInfo(detectedProjectInfo.language.getId()) : null;
		}
		updateTables(true);
	}
	
	public void initTypeMap() {
		IRunnableWithProgress runnable = new IRunnableWithProgress() {
			@Override
			public void run(IProgressMonitor monitor) throws InvocationTargetException {
				SubMonitor mon = SubMonitor.convert(monitor, Messages.SelectProjectTypeGatherTypesTask, 100);
				typeMap = getProjectTypeMap(mon.split(100));
			}
		};
			
		try {
			getContainer().run(true, true, runnable);
		} catch (InvocationTargetException e) {
			Logger.logError("An error occurred gathering the project types for connection: " + connection.getBaseURI(), e);
			return;
		} catch (InterruptedException e) {
			// The user cancelled the operation
			return;
		}
	}

	public ProjectTypeInfo getProjectTypeInfo() {
		return projectTypeInfo;
	}
	
	public ProjectSubtypeInfo getProjectSubtypeInfo() {
		
		ProjectTypeInfo projectType = getProjectTypeInfo();
		
		// There is no subtype when:
		// 1. No type is selected
		// 2. The selected type has no subtypes label; it is a Codewind built-in type which has language, not subtype
		// 3. The selected type is the same as the detected type (the subtype is only needed if a different type was selected
		//    in order to run validate again with the new type and subtype; if the detected type is used then
		//    validate does not need to be run again)
		if (projectType == null ||  
			projectType.getSubtypesLabel().length() == 0 || 
			(detectedProjectInfo != null && projectType.eq(detectedProjectInfo.type))) {
			return null;
		}
		
		return projectSubtypeInfo;
	}
	
	public String getLanguage() {
		
		ProjectTypeInfo projectType = getProjectTypeInfo();
		
		// no language if no type is selected
		if (projectType == null)
			return null;
		
		// selected type has no subtypes label; it is a Codewind built-in type
		// what's selected in the subtypes viewer is the language
		if (projectType.getSubtypesLabel().length() == 0) {
			return projectSubtypeInfo == null ? ProjectLanguage.LANGUAGE_UNKNOWN.getId() : projectSubtypeInfo.id;
		}
		
		// for non-Codewind types, fallback to detected language
		return detectedProjectInfo == null ? ProjectLanguage.LANGUAGE_UNKNOWN.getId() : detectedProjectInfo.language.getId();
	}

	private void updateTables(boolean init) {
		
		if (typeViewer == null || typeViewer.getTable().isDisposed()) {
			return;
		}
		
		Collection<ProjectTypeInfo> projectTypes = typeMap.values();
		typeViewer.setInput(projectTypes);
		if (projectTypes.size() == 0) {
			setErrorMessage(Messages.SelectProjectTypeNoProjectTypes);
			updateSubtypes(init, null);
			return;
		}
		setErrorMessage(null);

		if (projectTypes.size() == 1) {
			// If there is only one project type then select it
			ProjectTypeInfo[] typeArray = projectTypes.toArray(new ProjectTypeInfo[1]);
			typeViewer.setCheckedElements(typeArray);
			updateSubtypes(init, typeArray[0]);
		} else if (init && detectedProjectInfo != null) { 
			// If first entering the wizard, attempt to select type that matches the detected type
			projectTypeInfo = typeMap.get(detectedProjectInfo.type.getId());
			if (projectTypeInfo != null) {
				typeViewer.setCheckedElements(new Object[] { projectTypeInfo });
			} else {
				typeViewer.setAllChecked(false);
			}
			updateSubtypes(init, projectTypeInfo);
		} else {
			// otherwise, see if anything got unchecked
			// e.g. removing a repo that contained previously selected type
			Object[] checked = typeViewer.getCheckedElements();
			if (checked.length == 0)
				updateSubtypes(init, null);
		}
	}
	
	private void updateSubtypes(boolean init, ProjectTypeInfo projectType) {
		
		if (subtypeViewer == null || subtypeViewer.getTable().isDisposed()) {
			return;
		}
		
		boolean shouldShow = false;
		projectSubtypeInfo = null;
		
		if (projectType == null) {
			subtypeViewer.setInput(new Object[0]);
		} else {
			List<ProjectSubtypeInfo> projectSubtypes = projectType.getSubtypes();
			subtypeViewer.setInput(projectSubtypes);
			
			// If only 1 possible choice then select it
			if (projectSubtypes.size() == 1) {
				projectSubtypeInfo = projectSubtypes.get(0);
				subtypeViewer.setCheckedElements(new Object[] { projectSubtypeInfo });
			}
			// If there is more than 1 choice, allow subtype/language selection if:
			// 1. The selected type is docker (can select language)
			// 2. The selected type is different than the detected type
			//    e.g. project was detected as docker, but the user switched the selection to an 
			//    extension project type, they should be allowed to choose the subtype
			else if (projectSubtypes.size() > 1) {
				
				boolean isDocker = projectType.eq(TYPE_DOCKER);
			
				if (isDocker || detectedProjectInfo == null || !projectType.eq(detectedProjectInfo.type)) {
					
					// If possible, select language that was detected
					if (init && isDocker && detectedProjectInfo != null) {
						projectSubtypeInfo = projectType.new ProjectSubtypeInfo(detectedProjectInfo.language.getId());
						subtypeViewer.setCheckedElements(new Object[] { projectSubtypeInfo });
					}
					
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
		}
		
		subtypeLabel.setVisible(shouldShow);
		subtypeViewer.getTable().setVisible(shouldShow);
		getWizard().getContainer().updateButtons();
	}
	
	private Map<String, ProjectTypeInfo> getProjectTypeMap(IProgressMonitor monitor) {
		SubMonitor mon = SubMonitor.convert(monitor, 100);
		List<ProjectTypeInfo> projectTypes = null;
		Map<String, ProjectTypeInfo> typeMap = new HashMap<String, ProjectTypeInfo>();
		try {
			projectTypes = connection.requestProjectTypes();
			mon.worked(100);
		} catch (Exception e) {
			Logger.logError("An error occurred trying to get the list of project types for connection: " + connection.getBaseURI(), e); //$NON-NLS-1$
			return typeMap;
		}
		if (projectTypes == null || projectTypes.isEmpty()) {
			Logger.log("The list of project types is empty for connection: " + connection.getBaseURI()); //$NON-NLS-1$
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
