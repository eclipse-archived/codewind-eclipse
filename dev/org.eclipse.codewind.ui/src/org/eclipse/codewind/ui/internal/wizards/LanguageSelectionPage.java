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

public class LanguageSelectionPage extends WizardPage {

	private CodewindConnection connection = null;
	private IProject project = null;
	private ProjectType type = null;
	private ProjectLanguage language = null;
	private Text languageLabel = null;
	private Table languageTable = null;
	private Text typeLabel = null;
	private Table typeTable = null;

	protected LanguageSelectionPage(CodewindConnection connection, IProject project) {
		super(Messages.SelectLanguagePageName);
		setTitle(Messages.SelectLanguagePageTitle);
		setDescription(Messages.SelectLanguagePageDescription);
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

        languageLabel = new Text(composite, SWT.READ_ONLY);
        languageLabel.setText(Messages.SelectLanguagePageLanguageLabel);
        languageLabel.setLayoutData(new GridData(GridData.BEGINNING, GridData.CENTER, false, false));
        languageLabel.setBackground(composite.getBackground());
        languageLabel.setForeground(composite.getForeground());
        
        languageTable = new Table (composite, SWT.SINGLE | SWT.CHECK | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
    	fillLanguageTable(languageTable);
    	languageTable.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false));
    	
        typeLabel = new Text(composite, SWT.READ_ONLY);
        typeLabel.setText(Messages.SelectLanguagePageProjectTypeLabel);
        typeLabel.setLayoutData(new GridData(GridData.BEGINNING, GridData.CENTER, false, false));
        typeLabel.setBackground(composite.getBackground());
        typeLabel.setForeground(composite.getForeground());
        
        typeTable = new Table(composite, SWT.SINGLE | SWT.CHECK | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
        fillTypeTable(typeTable);
        typeTable.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false));
    	
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
    			if (language == ProjectLanguage.LANGUAGE_JAVA) {
		        	typeLabel.setVisible(true);
		        	typeTable.setVisible(true);
    			} else {
    				typeLabel.setVisible(false);
    				typeTable.setVisible(false);
    			}
	        } else {
	        	language = null;
	        	typeLabel.setVisible(false);
				typeTable.setVisible(false);
	        }
    		getWizard().getContainer().updateButtons();
    	});
    	
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
	        } else {
	        	type = null;
	        }
    		getWizard().getContainer().updateButtons();
    	});

    	typeLabel.setVisible(false);
    	typeTable.setVisible(false);

    	updateTables();

    	languageTable.setFocus();
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
		if (language == null || language == ProjectLanguage.LANGUAGE_UNKNOWN) {
			return false;
		}
		if (language == ProjectLanguage.LANGUAGE_JAVA) {
			return type != null;
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
		item.setText("Other");
		item.setData(ProjectType.TYPE_DOCKER);
	}
	
	public void setProject(IProject project) {
		this.project = project;
		updateTables();
	}
	
	public CodewindConnection getConnection() {
		return connection;
	}
	
	public ProjectLanguage getLanguage() {
		return language;
	}
	
	public ProjectType getType() {
		if (type != null) {
			return type;
		}
		if (language == ProjectLanguage.LANGUAGE_NODEJS) {
			return ProjectType.TYPE_NODEJS;
		}
		if (language == ProjectLanguage.LANGUAGE_SWIFT) {
			return ProjectType.TYPE_SWIFT;
		}
        return ProjectType.TYPE_DOCKER;
	}
	
	private void updateTables() {
		ProjectInfo projectInfo = getProjectInfo();
		if (projectInfo != null) {
			language = projectInfo.language;
			type = projectInfo.type;
			TableItem item = getItem(languageTable, language);
			if (item != null) {
				item.setChecked(true);
			}
			if (language == ProjectLanguage.LANGUAGE_JAVA) {
				item = getItem(typeTable, type);
				if (item != null) {
					item.setChecked(true);
					typeLabel.setVisible(true);
					typeTable.setVisible(true);
				}
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
