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

import static org.eclipse.codewind.core.internal.constants.ProjectType.TYPE_DOCKER;

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
import org.eclipse.codewind.core.internal.constants.ProjectInfo;
import org.eclipse.codewind.core.internal.constants.ProjectLanguage;
import org.eclipse.codewind.core.internal.constants.ProjectType;
import org.eclipse.codewind.ui.internal.messages.Messages;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;

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
					updateSubtypes(false, (ProjectTypeInfo) element);
				}
				else {
					updateSubtypes(false, null);
				}
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

		subtypeLabel.setVisible(false);
		subtypeViewer.getTable().setVisible(false);

		setProjectPath(projectPath, false);

		typeViewer.getTable().setFocus();
		setControl(composite);
	}

	public boolean canFinish() {
		
		if (typeViewer == null)
			return false;
		
		ProjectTypeInfo projectType = getType();
		if (projectType == null)
			return false;
		
		// for docker type language selection is optional
		// for non-docker when selected type is different than the detected type,
		// user need to choose a subtype to proceed
		if (!projectType.eq(TYPE_DOCKER) && !projectType.eq(projectInfo.type))
			return subtypeViewer.getCheckedElements().length != 0;
		
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

	public void setProjectPath(IPath projectPath, boolean fromPrevPage) {
		this.projectPath = projectPath;
		this.projectInfo = null;
		if (projectPath == null) {
			return;
		}
		
		IRunnableWithProgress runnable = new IRunnableWithProgress() {
			@Override
			public void run(IProgressMonitor monitor) throws InvocationTargetException {
				SubMonitor mon = SubMonitor.convert(monitor, NLS.bind(Messages.SelectProjectTypeValidateTask, projectPath.lastSegment()), 100);
				if (typeMap == null) {
					projectInfo = getProjectInfo(mon.split(75));
					typeMap = getProjectTypeMap(mon.split(25));
				}
				else
					projectInfo = getProjectInfo(mon.split(100));
			}
		};
			
		try {
			if (fromPrevPage && getWizard() != null && getWizard().getContainer() != null)
				getContainer().run(true, true, runnable);
			else
				PlatformUI.getWorkbench().getProgressService().busyCursorWhile(runnable);
			updateTables(true);
			
		} catch (InvocationTargetException e) {
			Logger.logError("An error occurred getting the project info for: " + projectPath.lastSegment(), e);
			return;
		} catch (InterruptedException e) {
			// The user cancelled the operation
			return;
		}
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
		// 3. selected type is same as detected type; user does not need to choose subtype
		if (projectType == null ||  
			projectType.getSubtypesLabel().length() == 0 || 
			projectType.eq(projectInfo.type)) {
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
			updateSubtypes(init, null);
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
			updateSubtypes(init, projectType);
		}
		// otherwise, see if anything got unchecked
		// e.g. removing a repo that contained previously selected type
		else {
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
		
		if (projectType == null)
			subtypeViewer.setInput(new Object[0]);
		else {
			List<ProjectSubtypeInfo> projectSubtypes = projectType.getSubtypes();
			subtypeViewer.setInput(projectSubtypes);
			
			// only 1 possible choice, select it
			if (projectSubtypes.size() == 1) {
				subtypeViewer.setCheckedElements(new Object[] { projectSubtypes.get(0) });
			}
			// otherwise if more than 1 choice, allow subtype/language selection if:
			// 1. selected type is docker (can select language)
			// 2. selected type is different than the detected type
			//    e.g. project was detected as docker, then user switch selection to an 
			//    extension project type, they should be allowed to choose the subtype
			else if (projectSubtypes.size() > 1) {
				
				boolean isDocker = projectType.eq(TYPE_DOCKER);
			
				if (isDocker || !projectType.eq(projectInfo.type)) {
					
					// if possible, select language that was detected
					if (init && isDocker) {
						subtypeViewer.setCheckedElements(
								new Object[] { projectType.new ProjectSubtypeInfo(projectInfo.language.getId()) });
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

	private Map<String, ProjectTypeInfo> getProjectTypeMap(IProgressMonitor monitor) {
		SubMonitor mon = SubMonitor.convert(monitor, 100);
		List<ProjectTypeInfo> projectTypes = null;
		Map<String, ProjectTypeInfo> typeMap = new HashMap<String, ProjectTypeInfo>();
		try {
			projectTypes = connection.requestProjectTypes();
			mon.worked(100);
		} catch (Exception e) {
			Logger.logError("An error occurred trying to get the list of project types for connection: " + connection.baseUrl, e); //$NON-NLS-1$
			return typeMap;
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
