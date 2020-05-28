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

package org.eclipse.codewind.ui.internal.editors;

import java.net.URL;
import java.util.Date;
import java.util.Optional;

import org.eclipse.codewind.core.internal.CodewindApplication;
import org.eclipse.codewind.core.internal.CodewindEclipseApplication;
import org.eclipse.codewind.core.internal.Logger;
import org.eclipse.codewind.core.internal.connection.CodewindConnection;
import org.eclipse.codewind.core.internal.connection.CodewindConnectionManager;
import org.eclipse.codewind.core.internal.connection.LocalConnection;
import org.eclipse.codewind.core.internal.console.CodewindConsoleFactory;
import org.eclipse.codewind.core.internal.console.ProjectLogInfo;
import org.eclipse.codewind.core.internal.console.SocketConsole;
import org.eclipse.codewind.core.internal.constants.DetailedAppStatus;
import org.eclipse.codewind.ui.CodewindUIPlugin;
import org.eclipse.codewind.ui.internal.IDEUtil;
import org.eclipse.codewind.ui.internal.UIConstants;
import org.eclipse.codewind.ui.internal.actions.OpenAppAction;
import org.eclipse.codewind.ui.internal.messages.Messages;
import org.eclipse.codewind.ui.internal.prefs.CodewindPrefsParentPage;
import org.eclipse.codewind.ui.internal.views.UpdateHandler.UpdateListener;
import org.eclipse.codewind.ui.internal.views.UpdateHandler.UpdateType;
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
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;
import org.eclipse.ui.console.ConsolePlugin;
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

public class ApplicationOverviewEditorPart extends EditorPart implements UpdateListener {
	
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
	private Composite sectionComp = null;
	private ProjectInfoSection projectInfoSection = null;
	private ProjectStatusSection projectStatusSection = null;
	private AppInfoSection appInfoSection = null;
	
	private Font boldFont;

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
        
        CodewindUIPlugin.getUpdateHandler().addUpdateListener(this);
	}

	@Override
	public void dispose() {
		if (boldFont != null) {
			boldFont.dispose();
		}
		CodewindUIPlugin.getUpdateHandler().removeUpdateListener(this);
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
		form.setText(" " + appName + " (" + connectionName + ")");
		form.setImage(CodewindUIPlugin.getImage(CodewindUIPlugin.CODEWIND_BANNER));
		form.getBody().setLayout(new GridLayout());
		
		messageComp = toolkit.createComposite(form.getBody());
		GridLayout messageLayout = new GridLayout();
		messageLayout.verticalSpacing = 0;
		messageLayout.horizontalSpacing = 10;
		messageComp.setLayout(messageLayout);
		messageComp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_FILL));
		
		messageLabel = toolkit.createLabel(messageComp, "");
		messageLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_FILL));
		
		boldFont = IDEUtil.newFont(parent.getShell(), parent.getFont(), SWT.BOLD);
		
		sectionComp = toolkit.createComposite(form.getBody());
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.verticalSpacing = 0;
		layout.horizontalSpacing = 10;
		sectionComp.setLayout(layout);
		sectionComp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_FILL));
		
		projectInfoSection = new ProjectInfoSection(sectionComp, toolkit, 2, 1);
		addSpacer(sectionComp, toolkit, 2, 1);
		projectStatusSection = new ProjectStatusSection(sectionComp, toolkit, 2, 1);
		addSpacer(sectionComp, toolkit, 2, 1);
		appInfoSection = new AppInfoSection(sectionComp, toolkit, 2, 1);
		addSpacer(sectionComp, toolkit, 2, 1);
		
		toolkit.createLabel(sectionComp, "", SWT.SEPARATOR | SWT.HORIZONTAL).setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false, 2, 1));
		
		addSpacer(sectionComp, toolkit, 2, 1);
		
		Hyperlink preferencesLink = toolkit.createHyperlink(sectionComp, "Control opening of overview page on project create and add", SWT.WRAP);
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

		Button refreshButton = new Button(sectionComp, SWT.PUSH);
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
		update(conn, getApp(conn), true);
	}
	
	@Override
	public void update(UpdateType type, Object element) {
		if (element instanceof CodewindApplication && ((CodewindApplication)element).projectID.equals(projectId)) {
			Display.getDefault().asyncExec(() -> {
				switch(type) {
				case MODIFY:
					CodewindApplication app = (CodewindApplication)element;
					ApplicationOverviewEditorPart.this.update(app.connection, app);
					break;
				case REMOVE:
					ApplicationOverviewEditorPart.this.getEditorSite().getPage().closeEditor(ApplicationOverviewEditorPart.this, false);
					break;
				}
			});
		} else if (element instanceof CodewindConnection && ((CodewindConnection)element).getConid().equals(connectionId)) {
			Display.getDefault().asyncExec(() -> {
				switch(type) {
				case MODIFY:
					CodewindConnection conn = (CodewindConnection)element;
					CodewindApplication app = conn.getAppByID(projectId);
					ApplicationOverviewEditorPart.this.update(conn, app);
					break;
				case REMOVE:
					ApplicationOverviewEditorPart.this.getEditorSite().getPage().closeEditor(ApplicationOverviewEditorPart.this, false);
					break;
				}
			});
		} else if (element == null) {
			// A null element means update everything
			CodewindApplication app = getApp(getConn());
			if (app != null) {
				Display.getDefault().asyncExec(() -> {
					switch(type) {
					case MODIFY:
						ApplicationOverviewEditorPart.this.update(app.connection, app);
						break;
					case REMOVE:
						// Do nothing. There should never be a top level remove event.
						break;
					}
				});
			}
		}
	}

	public void update(CodewindConnection conn, CodewindApplication app) {
		update(conn, app, false);
	}
	
	public void update(CodewindConnection conn, CodewindApplication app, boolean init) {
		if (form.isDisposed()) {
			return;
		}
		boolean changed = false;
		if (conn == null || !conn.isConnected() || app == null) {
			changed = !messageComp.getVisible();
			messageComp.setVisible(true);
			((GridData)messageComp.getLayoutData()).exclude = false;
			messageLabel.setText(conn == null || !conn.isConnected() ? Messages.AppOverviewEditorNoConnection : Messages.AppOverviewEditorNoApplication);
			sectionComp.setVisible(false);
			((GridData)sectionComp.getLayoutData()).exclude = true;
		} else {
			changed = messageComp.getVisible();
			messageComp.setVisible(false);
			((GridData)messageComp.getLayoutData()).exclude = true;
			sectionComp.setVisible(true);
			((GridData)sectionComp.getLayoutData()).exclude = false;
			projectInfoSection.update(app);
			projectStatusSection.update(app);
			appInfoSection.update(app);
		}
		if (init || changed) {
			form.layout(true, true);
			form.reflow(true);
		}
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
	
	private class ProjectInfoSection {
		
		private final StringEntry typeEntry;
		private final StringEntry languageEntry;
		private final StringEntry projectIdEntry;
		private final StringEntry locationEntry;
		
		public ProjectInfoSection(Composite parent, FormToolkit toolkit, int hSpan, int vSpan) {
			Section section = toolkit.createSection(parent, ExpandableComposite.TWISTIE | ExpandableComposite.TITLE_BAR);
	        section.setText(Messages.AppOverviewEditorProjectInfoSection);
	        section.setLayoutData(new GridData(SWT.FILL,SWT.FILL, true, false, hSpan, vSpan));
	        section.setExpanded(true);

	        Composite composite = toolkit.createComposite(section);
	        GridLayout layout = new GridLayout();
	        layout.numColumns = 2;
	        layout.marginHeight = 5;
	        layout.marginWidth = 10;
	        layout.verticalSpacing = 5;
	        layout.horizontalSpacing = 10;
	        composite.setLayout(layout);
	        composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_FILL));
	        toolkit.paintBordersFor(composite);
	        section.setClient(composite);
	        
	        typeEntry = new StringEntry(composite, Messages.AppOverviewEditorTypeEntry);
	        languageEntry = new StringEntry(composite, Messages.AppOverviewEditorLanguageEntry);
	        projectIdEntry = new StringEntry(composite, Messages.AppOverviewEditorProjectIdEntry);
	        locationEntry = new StringEntry(composite, Messages.AppOverviewEditorLocationEntry);
		}
		
		public void update(CodewindApplication app) {
			typeEntry.setValue(app.projectType.getDisplayName(), true);
			languageEntry.setValue(app.projectLanguage.getDisplayName(), true);
			projectIdEntry.setValue(app.projectID, true);
			locationEntry.setValue(app.fullLocalPath.toOSString(), true);
		}
	}
	
	private class ProjectStatusSection {
		private final StringEntry autoBuildEntry;
		private final StringEntry injectMetricsEntry;
		private final StringEntry appStatusEntry;
		private final StringEntry buildStatusEntry;
		private final StringEntry lastBuildEntry;
		private final StringEntry lastImageBuildEntry;
		private final Link projectLogs;
		private final Text noProjectLogs;
		
		public ProjectStatusSection(Composite parent, FormToolkit toolkit, int hSpan, int vSpan) {
			Section section = toolkit.createSection(parent, ExpandableComposite.TWISTIE | ExpandableComposite.TITLE_BAR);
	        section.setText(Messages.AppOverviewEditorProjectStatusSection);
	        section.setLayoutData(new GridData(SWT.FILL,SWT.FILL, true, false, hSpan, vSpan));
	        section.setExpanded(true);
	        
	        Composite composite = toolkit.createComposite(section);
	        GridLayout layout = new GridLayout();
	        layout.numColumns = 2;
	        layout.marginHeight = 5;
	        layout.marginWidth = 10;
	        layout.verticalSpacing = 5;
	        layout.horizontalSpacing = 10;
	        composite.setLayout(layout);
	        composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_FILL));
	        toolkit.paintBordersFor(composite);
	        section.setClient(composite);
	        
	        autoBuildEntry = new StringEntry(composite, Messages.AppOverviewEditorAutoBuildEntry);
	        injectMetricsEntry = new StringEntry(composite, Messages.AppOverviewEditorInjectMetricsEntry);
	        appStatusEntry = new StringEntry(composite, Messages.AppOverviewEditorAppStatusEntry);
	        buildStatusEntry = new StringEntry(composite, Messages.AppOverviewEditorBuildStatusEntry);
	        lastImageBuildEntry = new StringEntry(composite, Messages.AppOverviewEditorLastImageBuildEntry);
	        lastBuildEntry = new StringEntry(composite, Messages.AppOverviewEditorLastBuildEntry);
	        
			Label label = new Label(composite, SWT.NONE);
			label.setFont(boldFont);
			label.setText(Messages.AppOverviewEditorProjectLogs);
			label.setLayoutData(new GridData(GridData.BEGINNING, GridData.CENTER, false, false));
			projectLogs = new Link(composite, SWT.NONE);
			projectLogs.setVisible(false);
			GridData data = new GridData(GridData.BEGINNING, GridData.CENTER, true, false);
			data.horizontalIndent = 2;
			data.exclude = true;
			projectLogs.setLayoutData(data);
			IDEUtil.paintBackgroundToMatch(projectLogs, composite);
			projectLogs.addListener(SWT.Selection, event -> {
				CodewindEclipseApplication app = (CodewindEclipseApplication) getApp(getConn());
				if (app == null) {
					Logger.logError("A log link was selected but could not find the application for the " + connectionId //$NON-NLS-1$
							+ " connection with name: " + projectId); //$NON-NLS-1$
					return;
				}
				Optional<ProjectLogInfo> logInfo = app.getLogInfos().stream().filter(info -> info.logName.equals(event.text)).findFirst();
				if (logInfo.isPresent()) {
					try {
						SocketConsole console = app.getConsole(logInfo.get());
						if (console == null) {
							console = CodewindConsoleFactory.createLogFileConsole(app, logInfo.get());
							app.addConsole(console);
						}
						ConsolePlugin.getDefault().getConsoleManager().showConsoleView(console);
					} catch (Exception e) {
						Logger.logError("An error occurred trying to open the " + logInfo.get().logName //$NON-NLS-1$
								+ "log file for application: " + projectId, e); //$NON-NLS-1$
						MessageDialog.openError(parent.getShell(), Messages.AppOverviewEditorOpenLogErrorTitle,
								NLS.bind(Messages.AppOverviewEditorOpenLogErrorMsg, new String[] {logInfo.get().logName, app.name, e.getMessage()}));
					}
				} else {
					Logger.logError("The " + event.text + " was selected but the associated log info could not be found for the " + projectId + " project."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				}
			});
			noProjectLogs = new Text(composite, SWT.READ_ONLY);
			noProjectLogs.setText(Messages.AppOverviewEditorNoProjectLogs);
			noProjectLogs.setData(FormToolkit.KEY_DRAW_BORDER, Boolean.FALSE);
			noProjectLogs.setLayoutData(new GridData(GridData.BEGINNING, GridData.CENTER, true, false));
			IDEUtil.paintBackgroundToMatch(noProjectLogs, composite);
		}
		
		public void update(CodewindApplication app) {
			autoBuildEntry.setValue(app.isAutoBuild() ? Messages.AppOverviewEditorAutoBuildOn : Messages.AppOverviewEditorAutoBuildOff, true);
			injectMetricsEntry.setValue(metricsInjectionState(app.canInjectMetrics(), app.isMetricsInjected()), true);
			appStatusEntry.setValue(getAppStatusString(app), true);
			buildStatusEntry.setValue(getBuildStatusString(app), true);
			long lastImageBuild = app.getLastImageBuild();
			String lastImageBuildStr = Messages.AppOverviewEditorImageNeverBuilt;
			if (lastImageBuild > 0) {
				lastImageBuildStr = formatTimestamp(lastImageBuild);
			}
			lastImageBuildEntry.setValue(lastImageBuildStr, true);
			long lastBuild = app.getLastBuild();
			String lastBuildStr = Messages.AppOverviewEditorProjectNeverBuilt;
			if (lastBuild > 0) {
				lastBuildStr = formatTimestamp(lastBuild);
			}
			lastBuildEntry.setValue(lastBuildStr, true);
			
			if (app.isAvailable() && !app.getLogInfos().isEmpty()) {
				StringBuilder builder = new StringBuilder();
				app.getLogInfos().stream().forEach(info -> {
					if (builder.length() > 0) {
						builder.append(", ");
					}
					builder.append("<a>" + info.logName + "</a>"); //$NON-NLS-1$ //$NON-NLS-2$
				});
				boolean changed = !projectLogs.getVisible() || !builder.toString().equals(projectLogs.getText());
				projectLogs.setText(builder.toString());
				projectLogs.setToolTipText(Messages.AppOverviewEditorProjectLogsTooltip);
				IDEUtil.setControlVisibility(projectLogs, true);
				IDEUtil.setControlVisibility(noProjectLogs, false);
				if (changed) {
					projectLogs.requestLayout();
				}
			} else {
				boolean changed = !noProjectLogs.getVisible();
				IDEUtil.setControlVisibility(projectLogs, false);
				IDEUtil.setControlVisibility(noProjectLogs, true);
				if (changed) {
					noProjectLogs.requestLayout();
				}
			}
		}
		
		private String getAppStatusString(CodewindApplication app) {
			if (app.isAvailable()) {
				StringBuilder builder = new StringBuilder();
				builder.append(app.getAppStatus().getDisplayString(app.getStartMode()));
				DetailedAppStatus details = app.getAppStatusDetails();
				if (details != null && details.getMessage() != null) {
					builder.append(": ");
					if (details.getSeverity() != null) {
						builder.append("(" + details.getSeverity().displayString + ") ");
					}
					builder.append(details.getMessage());
				}
				return builder.toString();
			}
			return Messages.AppOverviewEditorStatusDisabled;
		}
		
		private String getBuildStatusString(CodewindApplication app) {
			if (app.isAvailable()) {
				String buildStatusStr = app.getBuildStatus().getDisplayString();
				if (app.getBuildDetails() != null) {
					buildStatusStr += " (" + app.getBuildDetails() + ")";
				}
				return buildStatusStr;
			}
			return null;
		}
	}
	
	private class AppInfoSection {
		private final StringEntry containerIdEntry;
		private final StringEntry podNameEntry;
		private final StringEntry namespaceEntry;
		private final LinkEntry appURLEntry;
		private final StringEntry hostAppPortEntry;
		private final StringEntry appPortEntry;
		private final StringEntry hostDebugPortEntry;
		private final StringEntry debugPortEntry;
		
		private final Button editButton;
		private final Button infoButton;
		
		public AppInfoSection(Composite parent, FormToolkit toolkit, int hSpan, int vSpan) {
			Section section = toolkit.createSection(parent, ExpandableComposite.TWISTIE | ExpandableComposite.TITLE_BAR);
	        section.setText(Messages.AppOverviewEditorAppInfoSection);
	        section.setLayoutData(new GridData(SWT.FILL,SWT.FILL, true, false, hSpan, vSpan));
	        section.setExpanded(true);
	
	        Composite composite = toolkit.createComposite(section);
	        GridLayout layout = new GridLayout();
	        layout.numColumns = 2;
	        layout.marginHeight = 5;
	        layout.marginWidth = 10;
	        layout.verticalSpacing = 5;
	        layout.horizontalSpacing = 10;
	        composite.setLayout(layout);
	        composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_FILL));
	        toolkit.paintBordersFor(composite);
	        section.setClient(composite);
	        
	        containerIdEntry = new StringEntry(composite, Messages.AppOverviewEditorContainerIdEntry);
	        podNameEntry = new StringEntry(composite, Messages.AppOverviewEditorPodNameEntry);
	        namespaceEntry = new StringEntry(composite, Messages.AppOverviewEditorNamespaceEntry);
	        appURLEntry = new LinkEntry(composite, toolkit, Messages.AppOverviewEditorAppUrlEntry, (url) -> {
	        	CodewindApplication app = getApp(getConn());
	        	if (app == null) {
	        		Logger.logError("Could not get the application for opening in a browser: " + appName); //$NON-NLS-1$
	        		return;
	        	}
	        	OpenAppAction.openAppInBrowser(app);
	        });
	        hostAppPortEntry = new StringEntry(composite, Messages.AppOverviewEditorHostAppPortEntry);
	        appPortEntry = new StringEntry(composite, Messages.AppOverviewEditorAppPortEntry);
	        CodewindConnection conn = getConn();
	        if (conn != null && !conn.isLocal()) {
	        	hostDebugPortEntry = new StringEntry(composite, Messages.AppOverviewEditorLocalDebugPortEntry);
	        } else {
	        	hostDebugPortEntry = new StringEntry(composite, Messages.AppOverviewEditorHostDebugPortEntry);
	        }
	        debugPortEntry = new StringEntry(composite, Messages.AppOverviewEditorDebugPortEntry);
	        
	        Composite buttonComp = toolkit.createComposite(composite);
	        layout = new GridLayout();
	        layout.numColumns = 2;
	        layout.marginTop = 20;
	        layout.marginHeight = 0;
	        layout.marginWidth = 0;
	        layout.verticalSpacing = 5;
	        layout.horizontalSpacing = 10;
	        buttonComp.setLayout(layout);
	        buttonComp.setLayoutData(new GridData(GridData.END, GridData.END, false, false, 2, 1));
	        
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
			if (app.connection.isLocal()) {
				containerIdEntry.setValue(app.isAvailable() ? app.getContainerId() : null, true);
				podNameEntry.setValue(null, false);
				namespaceEntry.setValue(null, false);
			} else {
				containerIdEntry.setValue(null, false);
				podNameEntry.setValue(app.isAvailable() ? app.getPodName() : null, true);
				namespaceEntry.setValue(app.isAvailable() ? app.getNamespace() : null, true);
			}
			appURLEntry.setValue(app.isAvailable() && app.getRootUrl() != null ? app.getRootUrl().toString() : null, true);
			hostAppPortEntry.setValue(app.isAvailable() && app.getHttpPort() > 0 ? Integer.toString(app.getHttpPort()) : null, true);
			appPortEntry.setValue(app.isAvailable() && app.getContainerAppPort() > 0 ? Integer.toString(app.getContainerAppPort()) : null, true);
			String hostDebugPort = null;
			String debugPort = null;
			if (app.supportsDebug()) {
				if (app.getStartMode().isDebugMode()) {
					hostDebugPort = app.isAvailable() && app.getDebugConnectPort() > 0 ? Integer.toString(app.getDebugConnectPort()) : null;
					debugPort = app.isAvailable() && app.getContainerDebugPort() > 0 ? Integer.toString(app.getContainerDebugPort()) : null;
				} else {
					hostDebugPort = debugPort = Messages.AppOverviewEditorNotDebugging;
				}
			} else {
				hostDebugPort = debugPort = app.getCapabilitiesReady() ? Messages.AppOverviewEditorDebugNotSupported : null;
			}
			hostDebugPortEntry.setValue(hostDebugPort, true);
			debugPortEntry.setValue(debugPort, true);
			boolean hasSettingsFile = hasSettingsFile(app);
			IDEUtil.setControlVisibility(editButton, hasSettingsFile);
			IDEUtil.setControlVisibility(infoButton, hasSettingsFile);
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
	
	private String formatTimestamp(long timestamp) {
		// Temporary - improve by showing how long ago the build happened
		Date date = new Date(timestamp);
		return date.toLocaleString();
	}
	
	private void addSpacer(Composite composite, FormToolkit toolkit, int horizontalSpan, int verticalSpan) {
		toolkit.createLabel(composite, "").setLayoutData(new GridData(GridData.FILL, GridData.FILL, false, false, horizontalSpan, verticalSpan));
	}

	private class StringEntry {
		private final Label label;
		private final Text text;
		
		public StringEntry(Composite composite, String name) {
			label = new Label(composite, SWT.NONE);
			label.setFont(boldFont);
			label.setText(name);
			label.setLayoutData(new GridData(GridData.BEGINNING, GridData.CENTER, false, false));
	        
	        text = new Text(composite, SWT.WRAP | SWT.MULTI | SWT.READ_ONLY);
	        text.setData(FormToolkit.KEY_DRAW_BORDER, Boolean.FALSE);
	        text.setLayoutData(new GridData(GridData.BEGINNING, GridData.CENTER, true, false));
	        IDEUtil.paintBackgroundToMatch(text, composite);
		}
		
		public void setValue(String value, boolean visible) {
			boolean changed = visible != label.getVisible();
			if (changed) {
				IDEUtil.setControlVisibility(label, visible);
				IDEUtil.setControlVisibility(text, visible);
			}
			if (visible) {
				String valueText = value != null && !value.isEmpty() ? value : Messages.AppOverviewEditorNotAvailable;
				if (!valueText.equals(text.getText())) {
					text.setText(valueText);
					changed = true;
				}
			}
			if (changed) {
				text.requestLayout();
			}
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
			Label label = new Label(composite, SWT.NONE);
			label.setFont(boldFont);
			label.setText(name);
	        
			// If not available then use a text field
			text = new Text(composite, SWT.WRAP | SWT.MULTI | SWT.READ_ONLY);
			text.setData(FormToolkit.KEY_DRAW_BORDER, Boolean.FALSE);
			text.setText(Messages.AppOverviewEditorNotAvailable);
			text.setLayoutData(new GridData(GridData.BEGINNING, GridData.CENTER, false, false));
			IDEUtil.paintBackgroundToMatch(text, composite);
	        
			link = toolkit.createHyperlink(composite, "", SWT.WRAP);
			link.setVisible(false);
			GridData data = new GridData(GridData.BEGINNING, GridData.CENTER, true, false);
			data.horizontalIndent = 2;
			data.exclude = true;
			link.setLayoutData(data);
			
			link.addHyperlinkListener(new HyperlinkAdapter() {
				@Override
				public void linkActivated(org.eclipse.ui.forms.events.HyperlinkEvent e) {
					action.execute(linkUrl);
				}
			});
		}
		
		public void setValue(String linkUrl, boolean visible) {
			this.linkUrl = linkUrl;
			if (linkUrl != null && !linkUrl.isEmpty()) {
				boolean changed = !link.getVisible() || !linkUrl.equals(link.getText());
				link.setText(linkUrl);
				IDEUtil.setControlVisibility(link, true);
				IDEUtil.setControlVisibility(text, false);
				if (changed) {
					link.requestLayout();
				}
			} else {
				boolean changed = !text.getVisible();
				IDEUtil.setControlVisibility(link, false);
				IDEUtil.setControlVisibility(text, true);
				if (changed) {
					text.requestLayout();
				}
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
