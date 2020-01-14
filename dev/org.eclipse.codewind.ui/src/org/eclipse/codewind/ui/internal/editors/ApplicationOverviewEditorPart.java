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

import java.net.URL;
import java.util.Date;

import org.eclipse.codewind.core.internal.CodewindApplication;
import org.eclipse.codewind.core.internal.Logger;
import org.eclipse.codewind.core.internal.connection.CodewindConnection;
import org.eclipse.codewind.core.internal.connection.CodewindConnectionManager;
import org.eclipse.codewind.core.internal.connection.LocalConnection;
import org.eclipse.codewind.ui.CodewindUIPlugin;
import org.eclipse.codewind.ui.internal.IDEUtil;
import org.eclipse.codewind.ui.internal.UIConstants;
import org.eclipse.codewind.ui.internal.actions.OpenAppAction;
import org.eclipse.codewind.ui.internal.messages.Messages;
import org.eclipse.codewind.ui.internal.prefs.CodewindPrefsParentPage;
import org.eclipse.codewind.ui.internal.views.UpdateHandler.AppUpdateListener;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
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
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.forms.ManagedForm;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.events.IHyperlinkListener;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.ide.FileStoreEditorInput;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.EditorPart;

public class ApplicationOverviewEditorPart extends EditorPart {
	
	private static final String SETTINGS_FILE = ".cw-settings";
	private static final String JSON_EDITOR_ID = "org.eclipse.wst.json.ui.JSONEditor";
	private static final String CWSETTINGS_INFO_ID = "org.eclipse.codewind.ui.overview.ProjectSettingsInfo";
	
	private Composite contents;
	private String appName;
	private String connectionId;
	private String connectionName;
	private String projectId;
	
	private ScrolledForm form = null;
	private Composite messageComp = null;
	private Label messageLabel = null;
	private Composite columnComp = null;
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
		ApplicationOverviewEditorInput appInput = null;
		if (input instanceof ApplicationOverviewEditorInput) {
			appInput = (ApplicationOverviewEditorInput)input;
			if ((appInput.connectionId == null && appInput.connectionUri == null) || appInput.projectID == null) {
				Logger.logError("Invalid editor input for application overview. Connection id/uri or project id is null." + input.getClass()); //$NON-NLS-1$
				throw new PartInitException(NLS.bind(Messages.AppOverviewEditorCreateError, input));
			}
		} else {
			Logger.logError("The editor input is not valid for the application overview: " + input.getClass()); //$NON-NLS-1$
        	throw new PartInitException(NLS.bind(Messages.AppOverviewEditorCreateError, input));
		}
		
		setSite(site);
        setInput(input);
        
        // Support old mementos by defaulting to local connection
        appName = appInput.projectName;
        connectionId = appInput.connectionId;
        if (connectionId == null) {
        	connectionId = LocalConnection.DEFAULT_ID;
        }
        connectionName = appInput.connectionName;
        if (connectionName == null) {
        	connectionName = LocalConnection.DEFAULT_NAME;
        }
        projectId = appInput.projectID;
        
        setPartName(NLS.bind(Messages.AppOverviewEditorPartName, new String[] {appName, connectionName}));
        
        CodewindUIPlugin.getUpdateHandler().addAppUpdateListener(connectionId, projectId, new AppUpdateListener() {
			@Override
			public void update() {
				CodewindConnection conn = getConn();
				CodewindApplication app = getApp(conn);
				if (app != null) {
					Display.getDefault().asyncExec(new Runnable() {
						@Override
						public void run() {
							ApplicationOverviewEditorPart.this.update(conn, app);
						}
					});
				}
			}

			@Override
			public void remove() {
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
		// Dispose can be called if the part fails to init in which case these may be null
		if (connectionId != null && projectId != null) {
			CodewindUIPlugin.getUpdateHandler().removeAppUpdateListener(connectionId, projectId);
		}
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
		
		messageComp = toolkit.createComposite(form.getBody());
		GridLayout messageLayout = new GridLayout();
		messageLayout.verticalSpacing = 0;
		messageLayout.horizontalSpacing = 10;
		messageComp.setLayout(messageLayout);
		messageComp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_FILL));
		
		messageLabel = toolkit.createLabel(messageComp, "");
		messageLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_FILL));
		
		columnComp = toolkit.createComposite(form.getBody());
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
		data.widthHint = 250;
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
		
		addSpacer(columnComp, toolkit, 2, 1);
		
		toolkit.createLabel(columnComp, "", SWT.SEPARATOR | SWT.HORIZONTAL).setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false, 2, 1));;
		
		addSpacer(columnComp, toolkit, 2, 1);
		
		Hyperlink preferencesLink = toolkit.createHyperlink(columnComp, "Control opening of overview page on project create and add", SWT.WRAP);
		preferencesLink.setLayoutData(new GridData(GridData.BEGINNING, GridData.CENTER, false, false, 1, 1));
		
		preferencesLink.addHyperlinkListener(new IHyperlinkListener() {
			@Override
			public void linkActivated(HyperlinkEvent arg0) {
				PreferenceDialog dialog = PreferencesUtil.createPreferenceDialogOn(parent.getShell(), CodewindPrefsParentPage.ID, null, null);
				if (dialog != null) {
					dialog.open();
				} else {
					Logger.logError("Could not create the preference dialog for: " + CodewindPrefsParentPage.ID);
				}
			}
			
			@Override
			public void linkEntered(HyperlinkEvent arg0) {
				// Empty
			}
			
			@Override
			public void linkExited(HyperlinkEvent arg0) {
				// Empty
			}
		});

		Button refreshButton = new Button(columnComp, SWT.PUSH);
		refreshButton.setText(Messages.AppOverviewEditorRefreshButton);
		refreshButton.setLayoutData(new GridData(GridData.END, GridData.CENTER, false, false, 1, 1));

		refreshButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				final CodewindConnection conn = getConn();
				final CodewindApplication app = getApp(conn);
				Job job = new Job(NLS.bind(Messages.RefreshProjectJobLabel, app.name)) {
					@Override
					protected IStatus run(IProgressMonitor monitor) {
						app.connection.refreshApps(app.projectID);
						Display.getDefault().asyncExec(() -> ApplicationOverviewEditorPart.this.update(conn, app));
						return Status.OK_STATUS;
					}
				};
				job.schedule();
			}
		});

		CodewindConnection conn = getConn();
		update(conn, getApp(conn));
	}
	
	public void update(CodewindConnection conn, CodewindApplication app) {
		if (conn == null || app == null) {
			messageComp.setVisible(true);
			((GridData)messageComp.getLayoutData()).exclude = false;
			messageLabel.setText(conn == null ? Messages.AppOverviewEditorNoConnection : Messages.AppOverviewEditorNoApplication);
			columnComp.setVisible(false);
			((GridData)columnComp.getLayoutData()).exclude = true;
		} else {
			messageComp.setVisible(false);
			((GridData)messageComp.getLayoutData()).exclude = true;
			columnComp.setVisible(true);
			((GridData)columnComp.getLayoutData()).exclude = false;
			generalSection.update(app);
			projectSettingsSection.update(app);
			buildSection.update(app);
		}
		form.layout(true, true);
		form.reflow(true);
	}
	
	public void enableWidgets(boolean enable) {
		generalSection.enableWidgets(enable);
		projectSettingsSection.enableWidgets(enable);
		buildSection.enableWidgets(enable);
		form.reflow(true);
	}
	
	private CodewindConnection getConn() {
		return CodewindConnectionManager.getConnectionById(connectionId);
	}
	
	private CodewindApplication getApp(CodewindConnection connection) {
		if (connection == null) {
			return null;
		}
		return connection.getAppByID(projectId);
	}
	
	private class GeneralSection {
		
		private final StringEntry typeEntry;
		private final StringEntry languageEntry;
		private final StringEntry locationEntry;
		private final LinkEntry appURLEntry;
		private final StringEntry hostAppPortEntry;
		private final StringEntry hostDebugPortEntry;
		private final StringEntry projectIdEntry;
		private final StringEntry containerIdEntry;
		private final StringEntry statusEntry;
		
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
	        
	        typeEntry = new StringEntry(composite, Messages.AppOverviewEditorTypeEntry);
	        addSpacer(composite);
	        languageEntry = new StringEntry(composite, Messages.AppOverviewEditorLanguageEntry);
	        addSpacer(composite);
	        locationEntry = new StringEntry(composite, Messages.AppOverviewEditorLocationEntry);
	        addSpacer(composite);
	        appURLEntry = new LinkEntry(composite, toolkit, Messages.AppOverviewEditorAppUrlEntry, (url) -> {
	        	CodewindApplication app = getApp(getConn());
	        	if (app == null) {
	        		Logger.logError("Could not get the application for opening in a browser: " + appName); //$NON-NLS-1$
	        		return;
	        	}
	        	OpenAppAction.openAppInBrowser(app);
	        });
	        addSpacer(composite);
	        hostAppPortEntry = new StringEntry(composite, Messages.AppOverviewEditorHostAppPortEntry);
	        addSpacer(composite);
	        hostDebugPortEntry = new StringEntry(composite, Messages.AppOverviewEditorHostDebugPortEntry);
	        addSpacer(composite);
	        projectIdEntry = new StringEntry(composite, Messages.AppOverviewEditorProjectIdEntry);
	        addSpacer(composite);
	        containerIdEntry = new StringEntry(composite, Messages.AppOverviewEditorContainerIdEntry);
	        addSpacer(composite);
	        statusEntry = new StringEntry(composite, Messages.AppOverviewEditorStatusEntry);
		}
		
		public void update(CodewindApplication app) {
			typeEntry.setValue(app.projectType.getDisplayName(), true);
			languageEntry.setValue(app.projectLanguage.getDisplayName(), true);
			locationEntry.setValue(app.fullLocalPath.toOSString(), true);
			appURLEntry.setValue(app.isAvailable() && app.getRootUrl() != null ? app.getRootUrl().toString() : null, true);
			hostAppPortEntry.setValue(app.isAvailable() && app.getHttpPort() > 0 ? Integer.toString(app.getHttpPort()) : null, true);
			String hostDebugPort = null;
			if (app.supportsDebug()) {
				hostDebugPort = app.isAvailable() && app.getDebugPort() > 0 ? Integer.toString(app.getDebugPort()) : null;
			} else {
				hostDebugPort = Messages.AppOverviewEditorDebugNotSupported;
			}
			hostDebugPortEntry.setValue(hostDebugPort, true);
			projectIdEntry.setValue(app.projectID, true);
			containerIdEntry.setValue(app.isAvailable() ? app.getContainerId() : null, true);
			statusEntry.setValue(app.isAvailable() ? Messages.AppOverviewEditorStatusEnabled : Messages.AppOverviewEditorStatusDisabled, true);
		}
		
		public void enableWidgets(boolean enable) {
			// Nothing to do
		}
	}
	
	private class ProjectSettingsSection {
		private final StringEntry contextRootEntry;
		private final StringEntry appPortEntry;
		private final StringEntry debugPortEntry;
		private final Button editButton;
		private final Button infoButton;
		
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
	        
	        contextRootEntry = new StringEntry(composite, Messages.AppOverviewEditorContextRootEntry);
	        addSpacer(composite);
	        appPortEntry = new StringEntry(composite, Messages.AppOverviewEditorAppPortEntry);
	        addSpacer(composite);
	        debugPortEntry = new StringEntry(composite, Messages.AppOverviewEditorDebugPortEntry);
	        addSpacer(composite);
	        
	        Composite buttonComp = toolkit.createComposite(composite);
	        layout = new GridLayout();
	        layout.numColumns = 2;
	        layout.marginHeight = 0;
	        layout.marginWidth = 0;
	        layout.verticalSpacing = 5;
	        layout.horizontalSpacing = 10;
	        buttonComp.setLayout(layout);
	        buttonComp.setLayoutData(new GridData(GridData.END, GridData.END, false, false));
	        
	        editButton = new Button(buttonComp, SWT.PUSH);
	        editButton.setText(Messages.AppOverviewEditorEditProjectSettings);
	        editButton.setLayoutData(new GridData(GridData.END, GridData.CENTER, false, false));
	        editButton.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent event) {
					CodewindApplication app = getApp(getConn());
					if (app == null) {
						// Should not happen
						Logger.logError("Trying to open the settings file from the overview page but the app is not found with name: " + appName + ", and project id: " + projectId); //$NON-NLS-1$  //$NON-NLS-2$
						return;
					}
					IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
					IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(app.name);
					if (project != null && project.isAccessible()) {
						IFile file = project.getFile(SETTINGS_FILE);
						if (file != null && file.exists()) {
							try {
								IDE.openEditor(page, file, JSON_EDITOR_ID);
							} catch (PartInitException e) {
								Logger.logError("Error trying to open project settings file: " + file, e); //$NON-NLS-1$
								MessageDialog.openError(parent.getShell(), Messages.AppOverviewEditorOpenSettingsErrorTitle, NLS.bind(Messages.AppOverviewEditorOpenSettingsErrorMsg, e));
							}
							return;
						}
					}
					// Try using an external file
					IPath path = app.fullLocalPath.append(SETTINGS_FILE);
					if (path.toFile().exists()) {
						IFileStore fileStore = EFS.getLocalFileSystem().getStore(path);
						FileStoreEditorInput input = new FileStoreEditorInput(fileStore);
						try {
							IDE.openEditor(page, input, JSON_EDITOR_ID);
						} catch (PartInitException e) {
							Logger.logError("Error trying to open project settings file: " + path.toOSString(), e); //$NON-NLS-1$
							MessageDialog.openError(parent.getShell(), Messages.AppOverviewEditorOpenSettingsErrorTitle, NLS.bind(Messages.AppOverviewEditorOpenSettingsErrorMsg, e));
						}
						return;
					}
					Logger.logError("Failed to open project settings file for project: " + appName + ", with id: " + projectId); //$NON-NLS-1$ //$NON-NLS-2$
					MessageDialog.openError(parent.getShell(), Messages.AppOverviewEditorOpenSettingsErrorTitle, Messages.AppOverviewEditorOpenSettingsNotFound);
				}
	        });
	        
	        infoButton = new Button(buttonComp, SWT.PUSH);
	        infoButton.setText(Messages.AppOverviewEditorProjectSettingsInfo);
	        infoButton.setLayoutData(new GridData(GridData.END, GridData.CENTER, false, false));
	        infoButton.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent event) {
					try {
						IWorkbenchBrowserSupport browserSupport = PlatformUI.getWorkbench().getBrowserSupport();
						IWebBrowser browser = browserSupport
								.createBrowser(IWorkbenchBrowserSupport.NAVIGATION_BAR | IWorkbenchBrowserSupport.LOCATION_BAR,
										CWSETTINGS_INFO_ID, null, null);
						URL url = new URL(UIConstants.CWSETTINGS_INFO_URL);
						browser.openURL(url);
					} catch (Exception e) {
						Logger.logError("Failed to open the browser for the project settings documentation: " + UIConstants.CWSETTINGS_INFO_URL, e); //$NON-NLS-1$
					}
				}
	        });
		}
		
		public void update(CodewindApplication app) {
			contextRootEntry.setValue(app.getContextRoot() != null ? app.getContextRoot() : "/", true); //$NON-NLS-1$
			appPortEntry.setValue(app.getContainerAppPort(), true);
			String debugPort = null;
			if (app.supportsDebug()) {
				debugPort = app.getContainerDebugPort();
			} else {
				debugPort = Messages.AppOverviewEditorDebugNotSupported;
			}
			debugPortEntry.setValue(debugPort, true);
			boolean hasSettingsFile = hasSettingsFile(app);
			IDEUtil.setControlVisibility(editButton, hasSettingsFile);
			IDEUtil.setControlVisibility(infoButton, hasSettingsFile);
		}
		
		public void enableWidgets(boolean enable) {
			// Nothing to do
		}
		
		private boolean hasSettingsFile(CodewindApplication app) {
			IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(app.name);
			if (project != null && project.isAccessible()) {
				IFile file = project.getFile(SETTINGS_FILE);
				if (file != null && file.exists()) {
					return true;
				}
			}
			IPath path = app.fullLocalPath.append(SETTINGS_FILE);
			if (path.toFile().exists()) {
				return true;
			}
			return false;
		}
	}
	
	private class BuildSection {
		private final StringEntry autoBuildEntry;
		private final StringEntry injectMetricsEntry;
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
	        
	        autoBuildEntry = new StringEntry(composite, Messages.AppOverviewEditorAutoBuildEntry);
	        addSpacer(composite);
	        injectMetricsEntry = new StringEntry(composite, Messages.AppOverviewEditorInjectMetricsEntry);
	        addSpacer(composite);
	        lastBuildEntry = new StringEntry(composite, Messages.AppOverviewEditorLastBuildEntry);
	        addSpacer(composite);
	        lastImageBuildEntry = new StringEntry(composite, Messages.AppOverviewEditorLastImageBuildEntry);
		}
		
		public void update(CodewindApplication app) {
			autoBuildEntry.setValue(app.isAutoBuild() ? Messages.AppOverviewEditorAutoBuildOn : Messages.AppOverviewEditorAutoBuildOff, app.isAvailable());
			injectMetricsEntry.setValue(metricsInjectionState(app.canInjectMetrics(), app.isInjectMetrics()), app.isAvailable());
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
			// Nothing to do
		}
	}
	
	private String formatTimestamp(long timestamp) {
		// Temporary - improve by showing how long ago the build happened
		Date date = new Date(timestamp);
		return date.toLocaleString();
	}
	
	private void addSpacer(Composite composite) {
		new Label(composite, SWT.NONE);
	}
	
	private void addSpacer(Composite composite, FormToolkit toolkit, int horizontalSpan, int verticalSpan) {
		toolkit.createLabel(composite, "").setLayoutData(new GridData(GridData.FILL, GridData.FILL, false, false, horizontalSpan, verticalSpan));
	}

	private class StringEntry {
		private final Text text;
		
		public StringEntry(Composite composite, String name) {
			StyledText label = new StyledText(composite, SWT.READ_ONLY | SWT.SINGLE);
			label.setText(name);
	        IDEUtil.setBold(label);
	        
	        text = new Text(composite, SWT.WRAP | SWT.MULTI | SWT.READ_ONLY);
	        text.setData(FormToolkit.KEY_DRAW_BORDER, Boolean.FALSE);
	        text.setLayoutData(new GridData(GridData.BEGINNING, GridData.CENTER, true, false));
	        IDEUtil.normalizeBackground(text, composite);
		}
		
		public void setValue(String value, boolean enabled) {
			text.setText(value != null && !value.isEmpty() ? value : Messages.AppOverviewEditorNotAvailable);
		}
	}
	
	private class BooleanEntry {
		private final String onText, offText;
		private final Button button;
		
		public BooleanEntry(Composite composite, String name, Image image, String onText, String offText, BooleanAction action) {
			this.onText = onText;
			this.offText = offText;
			
			StyledText label = new StyledText(composite, SWT.READ_ONLY | SWT.SINGLE);
			label.setText(name);
	        IDEUtil.setBold(label);
	        
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
	
	private class LinkEntry {
		private final Text text;
		private final Hyperlink link;
		private String linkUrl;
		
		public LinkEntry(Composite composite, FormToolkit toolkit, String name, LinkAction action) {
			StyledText label = new StyledText(composite, SWT.READ_ONLY | SWT.SINGLE);
			label.setText(name);
			IDEUtil.setBold(label);
	        
			// If not available then use a text field
			text = new Text(composite, SWT.WRAP | SWT.MULTI | SWT.READ_ONLY);
			text.setData(FormToolkit.KEY_DRAW_BORDER, Boolean.FALSE);
			text.setText(Messages.AppOverviewEditorNotAvailable);
			text.setLayoutData(new GridData(GridData.BEGINNING, GridData.CENTER, false, false));
			IDEUtil.normalizeBackground(text, composite);
	        
			link = toolkit.createHyperlink(composite, "", SWT.WRAP);
			link.setVisible(false);
			GridData data = new GridData(GridData.BEGINNING, GridData.CENTER, true, false);
			data.exclude = true;
			link.setLayoutData(data);
			
			link.addHyperlinkListener(new HyperlinkAdapter() {
				@Override
				public void linkActivated(org.eclipse.ui.forms.events.HyperlinkEvent e) {
					action.execute(linkUrl);
				}
			});
		}
		
		public void setValue(String linkUrl, boolean enabled) {
			this.linkUrl = linkUrl;
			if (linkUrl != null && !linkUrl.isEmpty()) {
				link.setText(linkUrl);
				IDEUtil.setControlVisibility(link, true);
				IDEUtil.setControlVisibility(text, false);
				link.setEnabled(enabled);
			} else {
				IDEUtil.setControlVisibility(link, false);
				IDEUtil.setControlVisibility(text, true);
			}
		}
	}
	
	public interface LinkAction {
		public void execute(String url);
	}

	@Override
	public void setFocus() {
		if (contents != null) {
			contents.setFocus();
		}
	}
	
	public String metricsInjectionState(boolean injectMetricsAvailable, boolean injectMetricsEnabled) {
		if (injectMetricsAvailable) {
			return (injectMetricsEnabled) ? Messages.AppOverviewEditorInjectMetricsOn : Messages.AppOverviewEditorInjectMetricsOff;
		}
		return Messages.AppOverviewEditorInjectMetricsUnavailable;
	}

}
