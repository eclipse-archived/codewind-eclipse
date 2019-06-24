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

import org.eclipse.codewind.core.internal.Logger;
import org.eclipse.codewind.core.internal.connection.CodewindConnection;
import org.eclipse.codewind.core.internal.constants.ProjectInfo;
import org.eclipse.codewind.core.internal.constants.ProjectLanguage;
import org.eclipse.codewind.core.internal.constants.ProjectType;
import org.eclipse.codewind.ui.CodewindUIPlugin;
import org.eclipse.codewind.ui.internal.messages.Messages;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

public class ProjectTypeSelectionPage extends WizardPage {

	private CodewindConnection connection = null;
	private IProject project = null;
	private ProjectType type = null;
	private ProjectLanguage language = null;
	private Text languageLabel = null;
	private CheckboxTableViewer languageViewer = null;
	private Text typeLabel = null;
	private CheckboxTableViewer typeViewer = null;

	protected ProjectTypeSelectionPage(CodewindConnection connection, IProject project) {
		super(Messages.SelectProjectTypePageName);
		setTitle(Messages.SelectProjectTypePageTitle);
		setDescription(Messages.SelectProjectTypePageDescription);
		this.connection = connection;
		this.project = project;
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
		
		typeViewer = CheckboxTableViewer.newCheckList(composite, SWT.BORDER);
		typeViewer.setContentProvider(ArrayContentProvider.getInstance());
		typeViewer.setLabelProvider(new ProjectTypeLabelProvider());
		typeViewer.setInput(getProjectTypeArray());
		typeViewer.getTable().setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false));
	   
		languageLabel = new Text(composite, SWT.READ_ONLY);
		languageLabel.setText(Messages.SelectProjectTypePageLanguageLabel);
		languageLabel.setLayoutData(new GridData(GridData.BEGINNING, GridData.CENTER, false, false));
		languageLabel.setBackground(composite.getBackground());
		languageLabel.setForeground(composite.getForeground());
		
		languageViewer = CheckboxTableViewer.newCheckList(composite, SWT.BORDER);
		languageViewer.setContentProvider(ArrayContentProvider.getInstance());
		languageViewer.setLabelProvider(new LanguageLabelProvider());
		languageViewer.setInput(getLanguageArray());
		languageViewer.getTable().setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false));
		
		typeViewer.addCheckStateListener(new ICheckStateListener() {
			@Override
			public void checkStateChanged(CheckStateChangedEvent event) {
				if (event.getChecked()) {
					typeViewer.setCheckedElements(new Object[] {event.getElement()});
					type = (ProjectType) event.getElement();
				} else {
					type = null;
				}
				languageLabel.setVisible(type == ProjectType.TYPE_DOCKER);
				languageViewer.getTable().setVisible(type == ProjectType.TYPE_DOCKER);
				getWizard().getContainer().updateButtons();
			}
		});

		languageViewer.addCheckStateListener(new ICheckStateListener() {
			@Override
			public void checkStateChanged(CheckStateChangedEvent event) {
				if (event.getChecked()) {
					languageViewer.setCheckedElements(new Object[] {event.getElement()});
					language = (ProjectLanguage) event.getElement();
				} else {
					language = null;
				}
				getWizard().getContainer().updateButtons();
			}
		});
 
		languageLabel.setVisible(false);
		languageViewer.getTable().setVisible(false);

		updateTables();

		typeViewer.getTable().setFocus();
		setControl(composite);
	}

	public boolean canFinish() {
		if (type == null || type == ProjectType.TYPE_UNKNOWN) {
			return false;
		}
		return true;
	}
	
	private ProjectType[] getProjectTypeArray() {
		return new ProjectType[] {ProjectType.TYPE_LIBERTY, ProjectType.TYPE_NODEJS, ProjectType.TYPE_SPRING,
				ProjectType.TYPE_SWIFT, ProjectType.TYPE_DOCKER};
	}
	
	private ProjectLanguage[] getLanguageArray() {
		return new ProjectLanguage[] {ProjectLanguage.LANGUAGE_GO, ProjectLanguage.LANGUAGE_JAVA, ProjectLanguage.LANGUAGE_NODEJS,
				ProjectLanguage.LANGUAGE_PYTHON, ProjectLanguage.LANGUAGE_SWIFT};
	}
	
	private class ProjectTypeLabelProvider extends LabelProvider {

		@Override
		public Image getImage(Object element) {
			return null;
		}

		@Override
		public String getText(Object element) {
			return ((ProjectType)element).getDisplayName();
		}
		
	}
	
	private class LanguageLabelProvider extends LabelProvider {

		@Override
		public Image getImage(Object element) {
			switch((ProjectLanguage)element) {
				case LANGUAGE_GO:
					return CodewindUIPlugin.getImage(CodewindUIPlugin.GO_ICON);
				case LANGUAGE_JAVA:
					return CodewindUIPlugin.getImage(CodewindUIPlugin.JAVA_ICON);
				case LANGUAGE_NODEJS:
					return CodewindUIPlugin.getImage(CodewindUIPlugin.NODE_ICON);
				case LANGUAGE_PYTHON:
					return CodewindUIPlugin.getImage(CodewindUIPlugin.PYTHON_ICON);
				case LANGUAGE_SWIFT:
					return CodewindUIPlugin.getImage(CodewindUIPlugin.SWIFT_ICON);
				default:
					return null;
			}
		}

		@Override
		public String getText(Object element) {
			return ((ProjectLanguage)element).getDisplayName();
		}
		
	}
	
	public void setProject(IProject project) {
		this.project = project;
		updateTables();
	}
	
	public CodewindConnection getConnection() {
		return connection;
	}
	
	public ProjectType getType() {
		// Type should not be null since the page cannot finish until a type is selected
		if (type == null) {
			Logger.logError("The project type is null on the project type selection page");
			return ProjectType.TYPE_UNKNOWN;
		}
		return type;
	}
	
	public ProjectLanguage getLanguage() {
		// Type should not be null since the page cannot finish until a type is selected
		if (type == null) {
			Logger.logError("The project type is null on the project type selection page");
			return ProjectLanguage.LANGUAGE_UNKNOWN;
		}
		switch(type) {
			case TYPE_LIBERTY:
			case TYPE_SPRING:
				return ProjectLanguage.LANGUAGE_JAVA;
			case TYPE_NODEJS:
				return ProjectLanguage.LANGUAGE_NODEJS;
			case TYPE_SWIFT:
				return ProjectLanguage.LANGUAGE_SWIFT;
			default:
				if (language == null) {
					return ProjectLanguage.LANGUAGE_UNKNOWN;
				}
				return language;
		}
	}

	private void updateTables() {
		ProjectInfo projectInfo = getProjectInfo();
		if (projectInfo != null) {
			type = projectInfo.type;
			language = projectInfo.language;
			typeViewer.setChecked(type, true);
			if (type == ProjectType.TYPE_DOCKER) {
				languageViewer.setChecked(language, true);
				languageLabel.setVisible(true);
				languageViewer.getTable().setVisible(true);
			}
		}
	}

	ProjectInfo getProjectInfo() {
		if (connection == null || project == null) {
			return null;
		}

		try {
			return connection.requestProjectValidate(project.getLocation().toFile().getAbsolutePath());
		} catch (Exception e) {
			Logger.logError("An error occurred trying to get the project type for project: " + project.getName(), e); //$NON-NLS-1$
		}

		return null;
	}
	
}
