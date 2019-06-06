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

import java.util.Date;

import org.eclipse.codewind.core.internal.CodewindApplication;
import org.eclipse.codewind.core.internal.Logger;
import org.eclipse.codewind.core.internal.connection.CodewindConnection;
import org.eclipse.codewind.core.internal.connection.CodewindConnectionManager;
import org.eclipse.codewind.ui.CodewindUIPlugin;
import org.eclipse.codewind.ui.internal.actions.EnableDisableAutoBuildAction;
import org.eclipse.codewind.ui.internal.actions.EnableDisableProjectAction;
import org.eclipse.codewind.ui.internal.messages.Messages;
import org.eclipse.codewind.ui.internal.views.UpdateHandler.AppUpdateListener;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.osgi.util.NLS;
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
import org.eclipse.swt.widgets.Display;
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
	
	private ScrolledForm form = null;
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
		CodewindApplication application = null;
		if (input instanceof ApplicationOverviewEditorInput) {
			ApplicationOverviewEditorInput appInput = (ApplicationOverviewEditorInput)input;
			if (appInput.connectionUri != null && appInput.projectID != null) {
				connection = CodewindConnectionManager.getActiveConnection(appInput.connectionUri);
				if (connection != null) {
					application = connection.getAppByID(appInput.projectID);
				}
			}
		}
		if (application == null) {
			Logger.logError("Could not retreive the application from the editor input: " + input.getClass()); //$NON-NLS-1$
        	throw new PartInitException(NLS.bind(Messages.AppOverviewEditorCreateError, input));
		}
		
		setSite(site);
        setInput(input);
        
        appName = application.name;
        projectID = application.projectID;
        
        setPartName(NLS.bind(Messages.AppOverviewEditorPartName, appName));
        
        CodewindUIPlugin.getUpdateHandler().addAppUpdateListener(projectID, new AppUpdateListener() {
			@Override
			public void update(CodewindApplication app) {
				Display.getDefault().asyncExec(new Runnable() {
					@Override
					public void run() {
						ApplicationOverviewEditorPart.this.update(app);
					}
				});
			}

			@Override
			public void remove(CodewindApplication app) {
				Display.getDefault().asyncExec(new Runnable() {
					@Override
					public void run() {
						ApplicationOverviewEditorPart.this.getEditorSite().getPage().closeEditor(ApplicationOverviewEditorPart.this, false);
					}
				});
			}
        });
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
		form = managedForm.getForm();
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
		
		update(getApp());
	}
	
	public void update(CodewindApplication app) {
		generalSection.update(app);
		projectSettingsSection.update(app);
		buildSection.update(app);
		form.reflow(true);
	}
	
	public void enableWidgets(boolean enable) {
		generalSection.enableWidgets(enable);
		projectSettingsSection.enableWidgets(enable);
		buildSection.enableWidgets(enable);
		form.reflow(true);
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
	        section.setText(Messages.AppOverviewEditorGeneralSection);
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
	        
	        languageString = new StringEntry(composite, Messages.AppOverviewEditorLanguageEntry);
	        new Label(composite, SWT.NONE);
	        locationString = new StringEntry(composite, Messages.AppOverviewEditorLocationEntry);
	        new Label(composite, SWT.NONE);
	        containerIdString = new StringEntry(composite, Messages.AppOverviewEditorContainerIdEntry);
	        new Label(composite, SWT.NONE);
	        statusBoolean = new BooleanEntry(composite, Messages.AppOverviewEditorStatusEntry, null,
	        		Messages.AppOverviewEditorStatusEnabled, Messages.AppOverviewEditorStatusDisabled, (value) -> {
	        	CodewindApplication app = getApp();
	        	if (app == null) {
	        		Logger.logError("Could not get the application for updating project enablement for project id: " + projectID); //$NON-NLS-1$
	        		return;
	        	}
	        	EnableDisableProjectAction.enableDisableProject(app, value);
	        	ApplicationOverviewEditorPart.this.enableWidgets(value);
	        });
		}
		
		public void update(CodewindApplication app) {
			languageString.setValue(app.projectLanguage.getDisplayName(), true);
			locationString.setValue(app.fullLocalPath.toOSString(), true);
			containerIdString.setValue(app.isAvailable() ? app.getContainerId() : null, true);
			statusBoolean.setValue(app.isAvailable(), true);
		}
		
		public void enableWidgets(boolean enable) {
			// Nothing to do
		}
	}
	
	private class ProjectSettingsSection {
		private final StringEntry appURLEntry;
		private final StringEntry appPortEntry;
		private final StringEntry debugPortEntry;
		private final Button editButton;
		
		public ProjectSettingsSection(Composite parent, FormToolkit toolkit) {
			Section section = toolkit.createSection(parent, ExpandableComposite.TWISTIE | ExpandableComposite.TITLE_BAR | Section.DESCRIPTION);
	        section.setText(Messages.AppOverviewEditorProjectSettingsSection);
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
	        
	        appURLEntry = new StringEntry(composite, Messages.AppOverviewEditorAppUrlEntry);
	        new Label(composite, SWT.NONE);
	        appPortEntry = new StringEntry(composite, Messages.AppOverviewEditorAppPortEntry);
	        new Label(composite, SWT.NONE);
	        debugPortEntry = new StringEntry(composite, Messages.AppOverviewEditorDebugPortEntry);
	        
	        editButton = new Button(composite, SWT.PUSH);
	        editButton.setText(Messages.AppOverviewEditorEditProjectSettings);
	        editButton.setLayoutData(new GridData(GridData.END, GridData.CENTER, false, false));
		}
		
		public void update(CodewindApplication app) {
			appURLEntry.setValue(app.isAvailable() && app.getBaseUrl() != null ? app.getBaseUrl().toString() : null, true);
			appPortEntry.setValue(app.isAvailable() && app.getHttpPort() > 0 ? Integer.toString(app.getHttpPort()) : null, true);
			debugPortEntry.setValue(app.isAvailable() && app.getDebugPort() > 0 ? Integer.toString(app.getDebugPort()) : null, true);
		}
		
		public void enableWidgets(boolean enable) {
			// Nothing to do
		}
	}
	
	private class BuildSection {
		private final BooleanEntry autoBuildEntry;
		private final StringEntry lastBuildEntry;
		private final StringEntry lastImageBuildEntry;
		
		public BuildSection(Composite parent, FormToolkit toolkit) {
			Section section = toolkit.createSection(parent, ExpandableComposite.TWISTIE | ExpandableComposite.TITLE_BAR | Section.DESCRIPTION);
	        section.setText(Messages.AppOverviewEditorBuildSection);
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
	        
	        autoBuildEntry = new BooleanEntry(composite, Messages.AppOverviewEditorAutoBuildEntry, null,
	        		Messages.AppOverviewEditorAutoBuildOn, Messages.AppOverviewEditorAutoBuildOff, (value) -> {
	        	CodewindApplication app = getApp();
	        	if (app == null) {
	        		Logger.logError("Could not get the application for updating auto build setting for project id: " + projectID); //$NON-NLS-1$
	        		return;
	        	}
	        	EnableDisableAutoBuildAction.enableDisableAutoBuild(app, value);
	        });
	        new Label(composite, SWT.NONE);
	        lastBuildEntry = new StringEntry(composite, Messages.AppOverviewEditorLastBuildEntry);
	        new Label(composite, SWT.NONE);
	        lastImageBuildEntry = new StringEntry(composite, Messages.AppOverviewEditorLastImageBuildEntry);
		}
		
		public void update(CodewindApplication app) {
			autoBuildEntry.setValue(app.isAutoBuild(), app.isAvailable());
			long lastBuild = app.getLastBuild();
			String lastBuildStr = Messages.AppOverviewEditorProjectNeverBuilt;
			if (lastBuild > 0) {
				lastBuildStr = formatTimestamp(lastBuild);
			}
			lastBuildEntry.setValue(lastBuildStr, true);
			long lastImageBuild = app.getLastImageBuild();
			String lastImageBuildStr = Messages.AppOverviewEditorImageNeverBuilt;
			if (lastImageBuild > 0) {
				lastImageBuildStr = formatTimestamp(lastBuild);
			}
			lastImageBuildEntry.setValue(lastImageBuildStr, true);
		}
		
		public void enableWidgets(boolean enable) {
			autoBuildEntry.enableEntry(enable);
		}
	}
	
	private String formatTimestamp(long timestamp) {
		// Temporary - improve by showing how long ago the build happened
		Date date = new Date(timestamp);
		return date.toLocaleString();
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
			text.setText(value != null && !value.isEmpty() ? value : Messages.AppOverviewEditorNotAvailable);
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
		private final Button button;
		
		public BooleanEntry(Composite composite, String name, Image image, String onText, String offText, BooleanAction action) {
			this.onText = onText;
			this.offText = offText;
			
			StyledText label = new StyledText(composite, SWT.NONE);
			label.setText(name);
	        setBold(label);
	        
	        button = new Button(composite, SWT.TOGGLE);
	        button.setText(onText.length() > offText.length() ? onText : offText);
	        if (image != null) {
	        	button.setImage(image);
	        }
	        
	        // Make sure the button is big enough
	        GridData data = new GridData(GridData.BEGINNING, GridData.CENTER, false, false);
	        data.widthHint = button.computeSize(SWT.DEFAULT, SWT.DEFAULT, true).x;
	        button.setLayoutData(data);
	        
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
		
		public void enableEntry(boolean enable) {
			button.setEnabled(enable);
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
