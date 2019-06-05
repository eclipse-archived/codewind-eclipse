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

package org.eclipse.codewind.ui.internal.editors;

import org.eclipse.codewind.core.internal.CodewindApplication;
import org.eclipse.codewind.core.internal.Logger;
import org.eclipse.codewind.core.internal.connection.CodewindConnection;
import org.eclipse.codewind.ui.CodewindUIPlugin;
import org.eclipse.codewind.ui.internal.actions.EnableDisableAutoBuildAction;
import org.eclipse.codewind.ui.internal.actions.EnableDisableProjectAction;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.ManagedForm;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.part.EditorPart;

public class ApplicationOverviewEditorPart extends EditorPart {
	
	private Composite contents;
	private String appName;
	private String projectID;
	private CodewindConnection connection;
	
	private GeneralSection generalSection = null;
	private ProjectSettingsSection projectSettingsSection = null;
	private BuildSection buildSection = null;

	@Override
	public void doSave(IProgressMonitor arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void doSaveAs() {
		// TODO Auto-generated method stub

	}

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		if (!(input instanceof ApplicationOverviewEditorInput) || ((ApplicationOverviewEditorInput)input).app == null) {
			Logger.logError("Could not retreive the application from the editor input: " + input.getClass());
        	throw new PartInitException("The application overview editor could not be created for the input: " + input + ". Check the logs for more details.");
		}
		
		setSite(site);
        setInput(input);
        
        CodewindApplication application = ((ApplicationOverviewEditorInput)input).app;
        appName = application.name;
        projectID = application.projectID;
        connection = application.connection;
        
        setPartName("Application Overview: " + appName);
        
        CodewindUIPlugin.getUpdateHandler().addAppUpdateListener(projectID, (app) -> update(app));
	}

	@Override
	public void dispose() {
		CodewindUIPlugin.getUpdateHandler().removeAppUpdateListener(projectID);
		super.dispose();
	}

	@Override
	public boolean isDirty() {
		return false;
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	@Override
	public void createPartControl(Composite parent) {
		ManagedForm managedForm = new ManagedForm(parent);
		ScrolledForm form = managedForm.getForm();
		FormToolkit toolkit = managedForm.getToolkit();
		toolkit.decorateFormHeading(form.getForm());
		form.setText(appName);
		form.setImage(CodewindUIPlugin.getImage(CodewindUIPlugin.CODEWIND_ICON));
		form.getBody().setLayout(new GridLayout());
		
		Composite columnComp = toolkit.createComposite(form.getBody());
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.verticalSpacing = 0;
		layout.horizontalSpacing = 10;
		columnComp.setLayout(layout);
		columnComp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_FILL));
		
		// left column
		Composite leftColumnComp = toolkit.createComposite(columnComp);
		layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		layout.verticalSpacing = 10;
		layout.horizontalSpacing = 0;
		leftColumnComp.setLayout(layout);
		GridData data = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_FILL);
		data.widthHint = 120;
		leftColumnComp.setLayoutData(data);
		
		generalSection = new GeneralSection(leftColumnComp, toolkit);
		
		// right column
		Composite rightColumnComp = toolkit.createComposite(columnComp);
		layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		layout.verticalSpacing = 10;
		layout.horizontalSpacing = 0;
		rightColumnComp.setLayout(layout);
		rightColumnComp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_FILL));
		
		projectSettingsSection = new ProjectSettingsSection(rightColumnComp, toolkit);
		
		buildSection = new BuildSection(rightColumnComp, toolkit);

		form.reflow(true);
	}
	
	public void update(CodewindApplication app) {
		generalSection.update(app);
		projectSettingsSection.update(app);
		buildSection.update(app);
	}
	
	private CodewindApplication getApp() {
		return connection.getAppByID(projectID);
	}
	
	private class GeneralSection {
		
		private final StringEntry languageString;
		private final StringEntry locationString;
		private final StringEntry containerIdString;
		private final BooleanEntry statusBoolean;
		
		public GeneralSection(Composite parent, FormToolkit toolkit) {
			Section section = toolkit.createSection(parent, ExpandableComposite.TWISTIE | ExpandableComposite.TITLE_BAR | Section.DESCRIPTION);
	        section.setText("General");
	        section.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_FILL));
	        section.setExpanded(true);

	        Composite composite = toolkit.createComposite(section);
	        GridLayout layout = new GridLayout();
	        layout.numColumns = 1;
	        layout.marginHeight = 5;
	        layout.marginWidth = 10;
	        layout.verticalSpacing = 5;
	        layout.horizontalSpacing = 10;
	        composite.setLayout(layout);
	        composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_FILL));
	        toolkit.paintBordersFor(composite);
	        section.setClient(composite);
	        
	        languageString = new StringEntry(composite, "Language");
	        new Label(composite, SWT.NONE);
	        locationString = new StringEntry(composite, "Location");
	        new Label(composite, SWT.NONE);
	        containerIdString = new StringEntry(composite, "Application container Id");
	        new Label(composite, SWT.NONE);
	        statusBoolean = new BooleanEntry(composite, "Status", null, "Enabled", "Disabled", (value) -> {
	        	CodewindApplication app = getApp();
	        	if (app == null) {
	        		Logger.logError("Could not get the application for updating project enablement for project id: " + projectID); //$NON-NLS-1$
	        		return;
	        	}
	        	EnableDisableProjectAction.enableDisableProject(app, value);
	        });
		}
		
		public void update(CodewindApplication app) {
			languageString.setValue(app.projectType.language, true);
			locationString.setValue(app.fullLocalPath.toOSString(), true);
			containerIdString.setValue(app.getContainerId(), true);
			statusBoolean.setValue(app.isAvailable(), true);
		}
	}
	
	private class ProjectSettingsSection {
		private final StringEntry appURLEntry;
		private final StringEntry appPortEntry;
		private final StringEntry debugPortEntry;
		private final Button editButton;
		
		public ProjectSettingsSection(Composite parent, FormToolkit toolkit) {
			Section section = toolkit.createSection(parent, ExpandableComposite.TWISTIE | ExpandableComposite.TITLE_BAR | Section.DESCRIPTION);
	        section.setText("Project Settings");
	        section.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_FILL));
	        section.setExpanded(true);
	        
	        Composite composite = toolkit.createComposite(section);
	        GridLayout layout = new GridLayout();
	        layout.numColumns = 1;
	        layout.marginHeight = 5;
	        layout.marginWidth = 10;
	        layout.verticalSpacing = 5;
	        layout.horizontalSpacing = 10;
	        composite.setLayout(layout);
	        composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_FILL));
	        toolkit.paintBordersFor(composite);
	        section.setClient(composite);
	        
	        appURLEntry = new StringEntry(composite, "Application URL");
	        new Label(composite, SWT.NONE);
	        appPortEntry = new StringEntry(composite, "Application port");
	        new Label(composite, SWT.NONE);
	        debugPortEntry = new StringEntry(composite, "Debug port");
	        
	        editButton = new Button(composite, SWT.PUSH);
	        editButton.setText("Edit project settings");
	        editButton.setLayoutData(new GridData(GridData.END, GridData.CENTER, false, false));
		}
		
		public void update(CodewindApplication app) {
			appURLEntry.setValue(app.getBaseUrl() != null ? app.getBaseUrl().toString() : null, true);
			appPortEntry.setValue(app.getHttpPort() > 0 ? Integer.toString(app.getHttpPort()) : null, true);
			debugPortEntry.setValue(app.getDebugPort() > 0 ? Integer.toString(app.getDebugPort()) : null, true);
		}
	}
	
	private class BuildSection {
		private final BooleanEntry autoBuildEntry;
		private final StringEntry lastBuildEntry;
		
		public BuildSection(Composite parent, FormToolkit toolkit) {
			Section section = toolkit.createSection(parent, ExpandableComposite.TWISTIE | ExpandableComposite.TITLE_BAR | Section.DESCRIPTION);
	        section.setText("Build");
	        section.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_FILL));
	        section.setExpanded(true);
	
	        Composite composite = toolkit.createComposite(section);
	        GridLayout layout = new GridLayout();
	        layout.numColumns = 1;
	        layout.marginHeight = 5;
	        layout.marginWidth = 10;
	        layout.verticalSpacing = 5;
	        layout.horizontalSpacing = 10;
	        composite.setLayout(layout);
	        composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_FILL));
	        toolkit.paintBordersFor(composite);
	        section.setClient(composite);
	        
	        autoBuildEntry = new BooleanEntry(composite, "Auto build", null, "On", "Off", (value) -> {
	        	CodewindApplication app = getApp();
	        	if (app == null) {
	        		Logger.logError("Could not get the application for updating auto build setting for project id: " + projectID); //$NON-NLS-1$
	        		return;
	        	}
	        	EnableDisableAutoBuildAction.enableDisableAutoBuild(app, value);
	        });
	        new Label(composite, SWT.NONE);
	        lastBuildEntry = new StringEntry(composite, "Last build");
		}
		
		public void update(CodewindApplication app) {
			autoBuildEntry.setValue(app.isAutoBuild(), app.isAvailable());
			lastBuildEntry.setValue("2 minutes ago", true);
		}
	}

	private class StringEntry {
		private final Text text;
		
		public StringEntry(Composite composite, String name) {
			StyledText label = new StyledText(composite, SWT.NONE);
			label.setText(name);
	        setBold(label);
	        
	        text = new Text(composite, SWT.WRAP | SWT.MULTI | SWT.READ_ONLY);
	        text.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false));
		}
		
		public void setValue(String value, boolean enabled) {
			text.setText(value != null && !value.isEmpty() ? value : "Not available");
		}
	}
	
//	private void addTextEntry(Composite composite, String name, String value, boolean enabled) {
//		StyledText label = new StyledText(composite, SWT.NONE);
//		label.setText(name);
//        setBold(label);
//        
//        Text text = new Text(composite, SWT.BORDER);
//        text.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false));
//        text.setText(value != null ? value : "Not available");
//        text.setEnabled(enabled);
//	}
	
	private class BooleanEntry {
		private final String onText, offText;
		private final BooleanAction action;
		private final Button button;
		
		public BooleanEntry(Composite composite, String name, Image image, String onText, String offText, BooleanAction action) {
			this.onText = onText;
			this.offText = offText;
			this.action = action;
			
			StyledText label = new StyledText(composite, SWT.NONE);
			label.setText(name);
	        setBold(label);
	        
	        button = new Button(composite, SWT.TOGGLE);
	        // Make sure the button is big enough
	        button.setText(onText.length() > offText.length() ? onText : offText);
	        if (image != null) {
	        	button.setImage(image);
	        }
	        button.setLayoutData(new GridData(GridData.BEGINNING, GridData.CENTER, false, false));
	        button.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent event) {
					button.setText(button.getSelection() ? onText : offText);
					action.execute(button.getSelection());
				}
			});
		}
		
		public void setValue(boolean value, boolean enabled) {
			button.setSelection(value);
			button.setText(value ? onText : offText);
	        button.setEnabled(enabled);
		}
	}
	
	public interface BooleanAction {
		public void execute(boolean value);
	}
	
	private void setBold(StyledText text) {
		StyleRange range = new StyleRange();
        range.start = 0;
        range.length = text.getText().length();
        range.fontStyle = SWT.BOLD;
        text.setStyleRange(range);
	}

	@Override
	public void setFocus() {
		if (contents != null) {
			contents.setFocus();
		}
	}

}
