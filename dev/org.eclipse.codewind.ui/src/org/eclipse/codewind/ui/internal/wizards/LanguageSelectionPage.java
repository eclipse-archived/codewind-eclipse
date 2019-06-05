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
	private String language = null;
	private String type = null;

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

        ProjectType projectType = getProjectType();
        
        Text languageLabel = new Text(composite, SWT.READ_ONLY);
        languageLabel.setText(Messages.SelectLanguagePageLanguageLabel);
        languageLabel.setLayoutData(new GridData(GridData.BEGINNING, GridData.CENTER, false, false));
        languageLabel.setBackground(composite.getBackground());
        languageLabel.setForeground(composite.getForeground());
        
        Table languageTable = new Table (composite, SWT.SINGLE | SWT.CHECK | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
    	fillLanguageTable(languageTable);
    	languageTable.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false));
    	
        Text typeLabel = new Text(composite, SWT.READ_ONLY);
        typeLabel.setText(Messages.SelectLanguagePageProjectTypeLabel);
        typeLabel.setLayoutData(new GridData(GridData.BEGINNING, GridData.CENTER, false, false));
        typeLabel.setBackground(composite.getBackground());
        typeLabel.setForeground(composite.getForeground());
        
    	Table typeTable = new Table(composite, SWT.SINGLE | SWT.CHECK | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
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
    			language = (String)item.getData();
    			if (ProjectType.LANGUAGE_JAVA.equals(language)) {
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
    			type = (String)item.getData();
	        } else {
	        	type = null;
	        }
    		getWizard().getContainer().updateButtons();
    	});

    	typeLabel.setVisible(false);
    	typeTable.setVisible(false);

    	if (projectType != null) {
    		language = projectType.language;
    		type = projectType.type;
    		TableItem item = getItem(languageTable, language);
    		if (item != null) {
    			item.setChecked(true);
    		}
    		if (ProjectType.LANGUAGE_JAVA.equals(language)) {
    			item = getItem(typeTable, type);
        		if (item != null) {
        			item.setChecked(true);
    				typeLabel.setVisible(true);
        			typeTable.setVisible(true);
        		}
    		}
    	}

    	languageTable.setFocus();
		setControl(composite);
	}
	
	private TableItem getItem(Table table, String data) {
		for (TableItem item : table.getItems()) {
			if (data.equals(item.getData())) {
				return item;
			}
		}
		return null;
	}
	
	public boolean canFinish() {
		if (language == null || ProjectType.UNKNOWN.equals(language)) {
			return false;
		}
		if (ProjectType.LANGUAGE_JAVA.equals(language)) {
			return type != null;
		}
		return true;
	}
	
	private void fillLanguageTable(Table languageTable) {
		TableItem item = new TableItem(languageTable, SWT.NONE);
		item.setText("Go");
		item.setData(ProjectType.LANGUAGE_GO);
		item.setImage(CodewindUIPlugin.getImage(CodewindUIPlugin.GO_ICON));
		item = new TableItem(languageTable, SWT.NONE);
		item.setText("Java");
		item.setData(ProjectType.LANGUAGE_JAVA);
		item.setImage(CodewindUIPlugin.getImage(CodewindUIPlugin.JAVA_ICON));
		item = new TableItem(languageTable, SWT.NONE);
		item.setText("Node.js");
		item.setData(ProjectType.LANGUAGE_NODEJS);
		item.setImage(CodewindUIPlugin.getImage(CodewindUIPlugin.NODE_ICON));
		item = new TableItem(languageTable, SWT.NONE);
		item.setText("Python");
		item.setData(ProjectType.LANGUAGE_PYTHON);
		item.setImage(CodewindUIPlugin.getImage(CodewindUIPlugin.PYTHON_ICON));
		item = new TableItem(languageTable, SWT.NONE);
		item.setText("Swift");
		item.setData(ProjectType.LANGUAGE_SWIFT);
		item.setImage(CodewindUIPlugin.getImage(CodewindUIPlugin.SWIFT_ICON));
	}
	
	private void fillTypeTable(Table typeTable) {
		TableItem item = new TableItem(typeTable, SWT.NONE);
		item.setText("Liberty");
		item.setData(ProjectType.TYPE_LIBERTY);
		item = new TableItem(typeTable, SWT.NONE);
		item.setText("Spring");
		item.setData(ProjectType.TYPE_SPRING);
		item = new TableItem(typeTable, SWT.NONE);
		item.setText("Other");
		item.setData(ProjectType.TYPE_DOCKER);
	}
	
	public void setProject(IProject project) {
		this.project = project;
	}
	
	public CodewindConnection getConnection() {
		return connection;
	}
	
	public String getLanguage() {
		return language;
	}
	
	public String getType() {
		if (type != null) {
			return type;
		}
		if (ProjectType.LANGUAGE_NODEJS.equals(language)) {
			return "nodejs";
		}
		if (ProjectType.LANGUAGE_SWIFT.equals(language)) {
			return "swift";
		}
        return "docker";
	}
	
	private ProjectType getProjectType() {
		if (connection == null || project == null) {
			return null;
		}
		try {
			return connection.requestProjectValidate(project.getLocation().toFile().getAbsolutePath());
		} catch (Exception e) {
			Logger.logError("Could not get the project type because validate failed for project: " + project.getName()); //$NON-NLS-1$
		}
		return null;
	}
	
}
