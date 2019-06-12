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
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

public class ProjectTypeSelectionPage extends WizardPage {

	private CodewindConnection connection = null;
	private IProject project = null;
	private ProjectType type = null;
	private ProjectLanguage language = null;
	private Text languageLabel = null;
	private Table languageTable = null;
	private Text typeLabel = null;
	private Table typeTable = null;

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
        
        typeTable = new Table(composite, SWT.SINGLE | SWT.CHECK | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
        fillTypeTable(typeTable);
        typeTable.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false));

        languageLabel = new Text(composite, SWT.READ_ONLY);
        languageLabel.setText(Messages.SelectProjectTypePageLanguageLabel);
        languageLabel.setLayoutData(new GridData(GridData.BEGINNING, GridData.CENTER, false, false));
        languageLabel.setBackground(composite.getBackground());
        languageLabel.setForeground(composite.getForeground());
        
        languageTable = new Table (composite, SWT.SINGLE | SWT.CHECK | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
    	fillLanguageTable(languageTable);
    	languageTable.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false));
    	
    	typeTable.addListener(SWT.Selection, event -> {
    		TableItem item = null;
    		if (event.detail == SWT.CHECK) {
				item = (TableItem)event.item;
				if (item.getChecked()) {
					for (TableItem it : typeTable.getItems()) {
						if (it != item) {
							it.setChecked(false);
						}
					}
				}
			} else {
				item = (TableItem)event.item;
				item.setChecked(!item.getChecked());
			}
    		if (item != null && item.getChecked()) {
    			type = (ProjectType)item.getData();
    			if (type == ProjectType.TYPE_DOCKER) {
    				languageLabel.setVisible(true);
    				languageTable.setVisible(true);
    			} else {
    				languageLabel.setVisible(false);
    				languageTable.setVisible(false);
    			}
	        } else {
	        	type = null;
	        	languageLabel.setVisible(false);
				languageTable.setVisible(false);
	        }
    		getWizard().getContainer().updateButtons();
    	});

    	languageTable.addListener(SWT.Selection, event -> {
    		TableItem item = null;
    		if (event.detail == SWT.CHECK) {
				item = (TableItem)event.item;
				if (item.getChecked()) {
					for (TableItem it : languageTable.getItems()) {
						if (it != item) {
							it.setChecked(false);
						}
					}
				}
			} else {
				item = (TableItem)event.item;
				item.setChecked(!item.getChecked());
			}
    		if (item != null && item.getChecked()) {
    			language = (ProjectLanguage)item.getData();
	        } else {
	        	language = null;
	        }
    		getWizard().getContainer().updateButtons();
    	});
 
    	languageLabel.setVisible(false);
    	languageTable.setVisible(false);

    	updateTables();

    	typeTable.setFocus();
		setControl(composite);
	}
	
	private TableItem getItem(Table table, Object data) {
		for (TableItem item : table.getItems()) {
			if (data.equals(item.getData())) {
				return item;
			}
		}
		return null;
	}
	
	public boolean canFinish() {
		if (type == null || type == ProjectType.TYPE_UNKNOWN) {
			return false;
		}
		return true;
	}
	
	private void fillLanguageTable(Table languageTable) {
		TableItem item = new TableItem(languageTable, SWT.NONE);
		item.setText(ProjectLanguage.LANGUAGE_GO.getDisplayName());
		item.setData(ProjectLanguage.LANGUAGE_GO);
		item.setImage(CodewindUIPlugin.getImage(CodewindUIPlugin.GO_ICON));
		item = new TableItem(languageTable, SWT.NONE);
		item.setText(ProjectLanguage.LANGUAGE_JAVA.getDisplayName());
		item.setData(ProjectLanguage.LANGUAGE_JAVA);
		item.setImage(CodewindUIPlugin.getImage(CodewindUIPlugin.JAVA_ICON));
		item = new TableItem(languageTable, SWT.NONE);
		item.setText(ProjectLanguage.LANGUAGE_NODEJS.getDisplayName());
		item.setData(ProjectLanguage.LANGUAGE_NODEJS);
		item.setImage(CodewindUIPlugin.getImage(CodewindUIPlugin.NODE_ICON));
		item = new TableItem(languageTable, SWT.NONE);
		item.setText(ProjectLanguage.LANGUAGE_PYTHON.getDisplayName());
		item.setData(ProjectLanguage.LANGUAGE_PYTHON);
		item.setImage(CodewindUIPlugin.getImage(CodewindUIPlugin.PYTHON_ICON));
		item = new TableItem(languageTable, SWT.NONE);
		item.setText(ProjectLanguage.LANGUAGE_SWIFT.getDisplayName());
		item.setData(ProjectLanguage.LANGUAGE_SWIFT);
		item.setImage(CodewindUIPlugin.getImage(CodewindUIPlugin.SWIFT_ICON));
	}
	
	private void fillTypeTable(Table typeTable) {
		TableItem item = new TableItem(typeTable, SWT.NONE);
		item.setText(ProjectType.TYPE_LIBERTY.getDisplayName());
		item.setData(ProjectType.TYPE_LIBERTY);
		item = new TableItem(typeTable, SWT.NONE);
		item.setText(ProjectType.TYPE_SPRING.getDisplayName());
		item.setData(ProjectType.TYPE_SPRING);
		item = new TableItem(typeTable, SWT.NONE);
		item.setText(ProjectType.TYPE_NODEJS.getDisplayName());
		item.setData(ProjectType.TYPE_NODEJS);
		item = new TableItem(typeTable, SWT.NONE);
		item.setText(ProjectType.TYPE_SWIFT.getDisplayName());
		item.setData(ProjectType.TYPE_SWIFT);
		item = new TableItem(typeTable, SWT.NONE);
		item.setText(ProjectType.TYPE_DOCKER.getDisplayName());
		item.setData(ProjectType.TYPE_DOCKER);
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
			TableItem item = getItem(typeTable, type);
			if (item != null) {
				item.setChecked(true);
			}
			if (type == ProjectType.TYPE_DOCKER) {
				item = getItem(languageTable, language);
				if (item != null) {
					item.setChecked(true);
				}
				languageLabel.setVisible(true);
				languageTable.setVisible(true);
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
