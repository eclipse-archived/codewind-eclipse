/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.codewind.ui.internal.editors;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.codewind.core.internal.CodewindManager;
import org.eclipse.codewind.core.internal.Logger;
import org.eclipse.codewind.core.internal.connection.CodewindConnection;
import org.eclipse.codewind.core.internal.connection.CodewindConnectionManager;
import org.eclipse.codewind.core.internal.connection.LocalConnection;
import org.eclipse.codewind.ui.CodewindUIPlugin;
import org.eclipse.codewind.ui.internal.IDEUtil;
import org.eclipse.codewind.ui.internal.UIConstants;
import org.eclipse.codewind.ui.internal.actions.AddConnectionAction;
import org.eclipse.codewind.ui.internal.actions.BindAction;
import org.eclipse.codewind.ui.internal.actions.LocalDoubleClickAction;
import org.eclipse.codewind.ui.internal.actions.NewProjectAction;
import org.eclipse.codewind.ui.internal.messages.Messages;
import org.eclipse.codewind.ui.internal.views.UpdateHandler.UpdateListener;
import org.eclipse.codewind.ui.internal.views.UpdateHandler.UpdateType;
import org.eclipse.codewind.ui.internal.views.ViewHelper;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.ManagedForm;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.forms.widgets.ImageHyperlink;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.part.EditorPart;

public class WelcomePageEditorPart extends EditorPart implements UpdateListener {
	
	private ScrolledForm form = null;
	private Font boldFont, medFont, medBoldFont, largeFont;
	private Control focusControl;
	private Composite radioSelectionComp, localComposite, remoteComposite;
	private Button imagesButton;
	private List<Button> localProjectButtons = new ArrayList<Button>();
	private List<Button> remoteProjectButtons = new ArrayList<Button>();
	
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
		if (!(input instanceof WelcomePageEditorInput)) {
			Logger.logError("The editor input is not valid for the welcome page: " + input.getClass()); //$NON-NLS-1$
        	throw new PartInitException(Messages.WelcomePageEditorCreateError);
		}
		
		setSite(site);
        setInput(input);
        
        setPartName(Messages.WelcomePageEditorPartName);
        
        CodewindUIPlugin.getUpdateHandler().addUpdateListener(this);
	}

	@Override
	public void dispose() {
		// Dispose can be called if the part fails to init in which case these may be null
		if (boldFont != null) {
			boldFont.dispose();
		}
		if (medFont != null) {
			medFont.dispose();
		}
		if (medBoldFont != null) {
			medBoldFont.dispose();
		}
		if (largeFont != null) {
			largeFont.dispose();
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
		form.setImage(CodewindUIPlugin.getImage(CodewindUIPlugin.CODEWIND_BANNER));
		form.setText(Messages.WelcomePageEditorFormTitle);
		form.setMessage(Messages.WelcomePageEditorFormMessage, IMessageProvider.NONE);
		form.getBody().setLayout(new GridLayout());
		
		boldFont = IDEUtil.newFont(parent.getShell(), parent.getFont(), SWT.BOLD);
		medFont = IDEUtil.newFont(parent.getShell(), parent.getFont(), 10, SWT.NONE);
		medBoldFont = IDEUtil.newFont(parent.getShell(), parent.getFont(), 10, SWT.BOLD);
		largeFont = IDEUtil.newFont(parent.getShell(), parent.getFont(), 14, SWT.NONE);
		
		int borderStyle = toolkit.getBorderStyle();
		toolkit.setBorderStyle(SWT.NONE);
		
		// Welcome section
		Composite welcomeComp = toolkit.createComposite(form.getBody());
		welcomeComp.setLayout(getCompLayout(10, 20, 2));
		welcomeComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Text welcomeLabel = toolkit.createText(welcomeComp, Messages.WelcomePageWelcomeHeader, SWT.READ_ONLY);
		welcomeLabel.setFont(largeFont);
		welcomeLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		IDEUtil.paintBackgroundToMatch(welcomeLabel, welcomeComp);
		focusControl = welcomeLabel;
		
		Label imageLabel = toolkit.createLabel(welcomeComp, "");
		imageLabel.setImage(CodewindUIPlugin.getImage(CodewindUIPlugin.WELCOME_IMAGE));
		imageLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 4));
		
		Text welcomeText = toolkit.createText(welcomeComp, Messages.WelcomePageWelcomeText, SWT.WRAP | SWT.READ_ONLY);
		IDEUtil.paintBackgroundToMatch(welcomeText, welcomeComp);
		welcomeText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Hyperlink viewLink = toolkit.createHyperlink(welcomeComp, Messages.WelcomePageCodewindExplorerLink, SWT.WRAP);
		viewLink.setLayoutData(new GridData(GridData.CENTER, GridData.FILL, false, false));
		setHyperlinkAction(viewLink, () -> ViewHelper.openCodewindExplorerView());
		
		// Horizontal separator
		toolkit.createSeparator(form.getBody(), SWT.SEPARATOR | SWT.HORIZONTAL).setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		// Quick start section
		Composite quickStartComp = toolkit.createComposite(form.getBody());
		quickStartComp.setLayout(getCompLayout(10, 20, 2));
		quickStartComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Text quickStartLabel = toolkit.createText(quickStartComp, Messages.WelcomePageQuickStartHeader, SWT.READ_ONLY);
		quickStartLabel.setFont(largeFont);
		IDEUtil.paintBackgroundToMatch(quickStartLabel, quickStartComp);
		quickStartLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 2, 1));
		
		Text quickStartText = toolkit.createText(quickStartComp, Messages.WelcomePageQuickStartText, SWT.WRAP | SWT.READ_ONLY);
		IDEUtil.paintBackgroundToMatch(quickStartText, quickStartComp);
		quickStartText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		
		// Radio button composite
		Composite radioComp = toolkit.createComposite(quickStartComp);
		radioComp.setLayout(getCompLayout(10, 0, 2));
		radioComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 2, 1));
		
		Button localButton = toolkit.createButton(radioComp, Messages.WelcomePageQuickStartLocalButton, SWT.RADIO);
		localButton.setImage(CodewindUIPlugin.getImage(CodewindUIPlugin.LOCAL_ACTIVE_ICON));
		localButton.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, false, false));
		
		Button remoteButton = toolkit.createButton(radioComp, Messages.WelcomePageQuickStartRemoteButton, SWT.RADIO);
		remoteButton.setImage(CodewindUIPlugin.getImage(CodewindUIPlugin.REMOTE_CONNECTED_ICON));
		remoteButton.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, false, false));
		
		// Composite for radio selection
		radioSelectionComp = toolkit.createComposite(radioComp);
		radioSelectionComp.setLayout(getCompLayout(10, 0, 1));
		radioSelectionComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 2, 1));
		
		localComposite = createQuickStartComposite(toolkit, radioSelectionComp, true);
		remoteComposite = createQuickStartComposite(toolkit, radioSelectionComp, false);
		
		localButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				if (localButton.getSelection()) {
					setQuickStartComp(true);
				}
			}
		});
		
		remoteButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				if (remoteButton.getSelection()) {
					setQuickStartComp(false);
				}
			}
		});
		
		// Horizontal separator
		toolkit.createSeparator(form.getBody(), SWT.SEPARATOR | SWT.HORIZONTAL).setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		// Learn section
		Composite learnComp = toolkit.createComposite(form.getBody());
		learnComp.setLayout(getCompLayout(10, 20, 1));
		learnComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Text learnLabel = toolkit.createText(learnComp, Messages.WelcomePageLearnHeader, SWT.READ_ONLY);
		learnLabel.setFont(largeFont);
		IDEUtil.paintBackgroundToMatch(learnLabel, learnComp);
		learnLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 2, 1));
		
		Composite learnInnerComp = toolkit.createComposite(learnComp);
		learnInnerComp.setLayout(getCompLayout(10, 0, 1));
		learnInnerComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		// Commands
		ImageHyperlink commandsLink = toolkit.createImageHyperlink(learnInnerComp, SWT.RIGHT);
		commandsLink.setText(Messages.WelcomePageLearnCommandsLabel);
		commandsLink.setToolTipText(Messages.WelcomePageLearnCommandsTooltip);
		commandsLink.setFont(medBoldFont);
		commandsLink.setImage(CodewindUIPlugin.getImage(CodewindUIPlugin.OPEN_APP_ICON));
		setHyperlinkAction(commandsLink, () -> ViewHelper.openCodewindExplorerView());
		
		Text commandsText = toolkit.createText(learnInnerComp, Messages.WelcomePageLearnCommandsText, SWT.WRAP | SWT.READ_ONLY);
		IDEUtil.paintBackgroundToMatch(commandsText, learnInnerComp);
		commandsText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Hyperlink commandsDocLink = toolkit.createHyperlink(learnInnerComp, Messages.WelcomePageLearnCommandsLink, SWT.WRAP);
		commandsDocLink.setToolTipText(Messages.WelcomePageLearnCommandsLinkTooltip);
		GridData data = new GridData(GridData.FILL, GridData.FILL, true, false);
		data.horizontalIndent = 40;
		commandsDocLink.setLayoutData(data);
		setHyperlinkAction(commandsDocLink, () -> IDEUtil.openExternalBrowser(UIConstants.COMMANDS_OVERVIEW_URL));
		
		// Spacer
		toolkit.createLabel(learnInnerComp, "", SWT.NONE);
		
		// Docs
		ImageHyperlink docsLink = toolkit.createImageHyperlink(learnInnerComp, SWT.RIGHT);
		docsLink.setText(Messages.WelcomePageLearnDocsLabel);
		docsLink.setToolTipText(Messages.WelcomePageLearnDocsTooltip);
		docsLink.setFont(medBoldFont);
		docsLink.setImage(CodewindUIPlugin.getImage(CodewindUIPlugin.OPEN_APP_ICON));
		setHyperlinkAction(docsLink, () -> IDEUtil.openExternalBrowser(UIConstants.DOC_BASE_URL));
		
		Text docsText = toolkit.createText(learnInnerComp, Messages.WelcomePageLearnDocsText, SWT.WRAP | SWT.READ_ONLY);
		IDEUtil.paintBackgroundToMatch(docsText, learnInnerComp);
		docsText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Hyperlink templatesDocLink = toolkit.createHyperlink(learnInnerComp, Messages.WelcomePageLearnDocsTemplatesLink, SWT.WRAP);
		templatesDocLink.setToolTipText(Messages.WelcomePageLearnDocsTemplatesTooltip);
		data = new GridData(GridData.FILL, GridData.FILL, true, false);
		data.horizontalIndent = 40;
		templatesDocLink.setLayoutData(data);
		setHyperlinkAction(templatesDocLink, () -> IDEUtil.openExternalBrowser(UIConstants.TEMPLATES_INFO_URL));
		
		Hyperlink remoteDocLink = toolkit.createHyperlink(learnInnerComp, Messages.WelcomePageLearnDocsRemoteLink, SWT.WRAP);
		remoteDocLink.setToolTipText(Messages.WelcomePageLearnDocsRemoteTooltip);
		data = new GridData(GridData.FILL, GridData.FILL, true, false);
		data.horizontalIndent = 40;
		remoteDocLink.setLayoutData(data);
		setHyperlinkAction(remoteDocLink, () -> IDEUtil.openExternalBrowser(UIConstants.REMOTE_DEPLOY_URL));
		
		// Spacer
		toolkit.createLabel(learnInnerComp, "", SWT.NONE);
		
		// Extensions
		Label extensionsLabel = toolkit.createLabel(learnInnerComp, Messages.WelcomePageLearnExtensionsLabel);
		extensionsLabel.setForeground(docsLink.getForeground());
		extensionsLabel.setFont(medBoldFont);
		
		Text extensionsText = toolkit.createText(learnInnerComp, Messages.WelcomePageLearnExtensionsText, SWT.WRAP | SWT.READ_ONLY);
		IDEUtil.paintBackgroundToMatch(extensionsText, learnInnerComp);
		extensionsText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Hyperlink openAPILink = toolkit.createHyperlink(learnInnerComp, Messages.WelcomePageLearnExtensionsOpenAPILink, SWT.WRAP);
		openAPILink.setToolTipText(Messages.WelcomePageLearnExtensionsOpenAPITooltip);
		data = new GridData(GridData.FILL, GridData.FILL, true, false);
		data.horizontalIndent = 40;
		openAPILink.setLayoutData(data);
		setHyperlinkAction(openAPILink, () -> IDEUtil.openExternalBrowser(UIConstants.CODEWIND_OPENAPI_URL));
		
		Hyperlink dockerToolsLink = toolkit.createHyperlink(learnInnerComp, Messages.WelcomePageLearnExtensionsDockerLink, SWT.WRAP);
		dockerToolsLink.setToolTipText(Messages.WelcomePageLearnExtensionsDockerTooltip);
		data = new GridData(GridData.FILL, GridData.FILL, true, false);
		data.horizontalIndent = 40;
		dockerToolsLink.setLayoutData(data);
		setHyperlinkAction(dockerToolsLink, () -> IDEUtil.openExternalBrowser(UIConstants.DOCKER_TOOLS_URL));
		
		toolkit.setBorderStyle(borderStyle);
		
		// Initialize
		localButton.setSelection(true);
		setQuickStartComp(true);
		updateButtons();
	}
	
	private GridLayout getCompLayout(int hSpacing, int vSpacing, int numColumns) {
		GridLayout layout = new GridLayout();
		layout.horizontalSpacing = hSpacing;
		layout.verticalSpacing = vSpacing;
		layout.numColumns = numColumns;
		return layout;
	}
	
	private void setHyperlinkAction(Hyperlink link, LinkAction action) {
		link.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent arg0) {
				action.execute();
			}
		});
	}
	
	private void setQuickStartComp(boolean isLocal) {
		localComposite.setVisible(isLocal);
		((GridData)localComposite.getLayoutData()).exclude = !isLocal;
		remoteComposite.setVisible(!isLocal);
		((GridData)remoteComposite.getLayoutData()).exclude = isLocal;
		radioSelectionComp.requestLayout();
	}
	
	private Composite createQuickStartComposite(FormToolkit toolkit, Composite parent, boolean isLocal) {
		Composite comp = toolkit.createComposite(parent);
		GridLayout layout = new GridLayout();
		layout.verticalSpacing = 8;
		layout.horizontalSpacing = 70;
		layout.numColumns = 3;
		comp.setLayout(layout);
		comp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		
		Composite setupComp = toolkit.createComposite(comp);
		layout = new GridLayout();
		layout.verticalSpacing = 8;
		layout.horizontalSpacing = 15;
		setupComp.setLayout(layout);
		setupComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		
		// Vertical separator
		toolkit.createSeparator(comp, SWT.SEPARATOR | SWT.VERTICAL).setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		
		Composite projectComp = toolkit.createComposite(comp);
		layout = new GridLayout();
		layout.verticalSpacing = 8;
		layout.horizontalSpacing = 15;
		projectComp.setLayout(layout);
		projectComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		
		List<Button> buttons = new ArrayList<Button>();
		
		// Set up section
		Text setupLabel = toolkit.createText(setupComp, Messages.WelcomePageQuickStartSetUpLabel, SWT.READ_ONLY);
		setupLabel.setFont(medBoldFont);
		IDEUtil.paintBackgroundToMatch(setupLabel, setupComp);
		setupLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		
		Text stepLabel = toolkit.createText(setupComp, NLS.bind(Messages.WelcomePageQuickStartStep, 1), SWT.READ_ONLY);
		IDEUtil.paintBackgroundToMatch(stepLabel, setupComp);
		stepLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		
		if (isLocal) {
			Button dockerButton = new Button(setupComp, SWT.PUSH);
			dockerButton.setText(Messages.WelcomePageQuickStartInstallDockerButton);
			dockerButton.setFont(medFont);
			dockerButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
			buttons.add(dockerButton);
			
			dockerButton.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent event) {
					IDEUtil.openExternalBrowser(UIConstants.DOCKER_INSTALL_URL);
				}
			});
			
			stepLabel = toolkit.createText(setupComp, NLS.bind(Messages.WelcomePageQuickStartStep, 2), SWT.READ_ONLY);
			IDEUtil.paintBackgroundToMatch(stepLabel, setupComp);
			stepLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
			
			imagesButton = new Button(setupComp, SWT.PUSH);
			imagesButton.setText(Messages.WelcomePageQuickStartInstallImagesButton);
			imagesButton.setFont(medFont);
			imagesButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
			buttons.add(imagesButton);
			
			imagesButton.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent event) {
					LocalConnection connection = (LocalConnection) CodewindConnectionManager.getLocalConnection();
					LocalDoubleClickAction.performInstall(connection, false);
				}
			});
		} else {
			Button connectionButton = new Button(setupComp, SWT.PUSH);
			connectionButton.setText(Messages.WelcomePageQuickStartNewConnectionButton);
			connectionButton.setFont(medFont);
			connectionButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
			buttons.add(connectionButton);
			
			connectionButton.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent event) {
					AddConnectionAction.openConnectionWizard();
				}
			});
		}
		
		// Project section
		Text projectLabel = toolkit.createText(projectComp, Messages.WelcomePageQuickStartProjectLabel, SWT.READ_ONLY);
		projectLabel.setFont(medBoldFont);
		IDEUtil.paintBackgroundToMatch(projectLabel, projectComp);
		projectLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		
		stepLabel = toolkit.createText(projectComp, NLS.bind(Messages.WelcomePageQuickStartStep, isLocal ? 3 : 2), SWT.READ_ONLY);
		IDEUtil.paintBackgroundToMatch(stepLabel, projectComp);
		stepLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		
		Button newProjectButton = new Button(projectComp, SWT.PUSH);
		newProjectButton.setText(Messages.WelcomePageQuickStartNewProjectButton);
		newProjectButton.setFont(medFont);
		newProjectButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		buttons.add(newProjectButton);
		
		newProjectButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				if (isLocal) {
					NewProjectAction.openNewProjectWizard(CodewindConnectionManager.getLocalConnection());
				} else {
					List<CodewindConnection> connections = CodewindConnectionManager.activeRemoteConnections();
					if (connections.isEmpty()) {
						IDEUtil.openInfoDialog(Messages.WelcomePageQuickStartNoConnectionTitle, Messages.WelcomePageQuickStartNoConnectionMsg);
					} else {
						NewProjectAction.openNewProjectWizard(connections);
					}
				}
			}
		});
		
		Text orLabel = toolkit.createText(projectComp, Messages.WelcomePageQuickStartOr, SWT.READ_ONLY);
		IDEUtil.paintBackgroundToMatch(orLabel, projectComp);
		orLabel.setLayoutData(new GridData(SWT.CENTER, SWT.FILL, false, false));
		
		Button addProjectButton = new Button(projectComp, SWT.PUSH);
		addProjectButton.setText(Messages.WelcomePageQuickStartAddProjectButton);
		addProjectButton.setFont(medFont);
		addProjectButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		buttons.add(addProjectButton);
		
		addProjectButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				if (isLocal) {
					BindAction.openBindProjectWizard(CodewindConnectionManager.getLocalConnection());
				} else {
					List<CodewindConnection> connections = CodewindConnectionManager.activeRemoteConnections();
					if (connections.isEmpty()) {
						IDEUtil.openInfoDialog(Messages.WelcomePageQuickStartNoConnectionTitle, Messages.WelcomePageQuickStartNoConnectionMsg);
					} else {
						BindAction.openBindProjectWizard(connections);
					}
				}
			}
		});
		
		List<Button> projectButtons = isLocal ? localProjectButtons : remoteProjectButtons;
		projectButtons.add(newProjectButton);
		projectButtons.add(addProjectButton);
		
		sizeButtons(buttons);
		
		return comp;
	}
	
	@Override
	public void update(UpdateType type, Object element) {
		if (element == null || element instanceof CodewindConnection) {
        	Display.getDefault().asyncExec(() -> updateButtons());
    	}
	}
	
	private void updateButtons() {
		if (imagesButton == null || imagesButton.isDisposed() || localProjectButtons.isEmpty() || remoteProjectButtons.isEmpty()) {
			return;
		}
		
		boolean installing = CodewindManager.getManager().getInstallerStatus() != null;
		
		// Install images button should only be enabled if an install is not in progress
		imagesButton.setEnabled(!installing);
			
		// Local project buttons should only be enabled if local Codewind is installed and running
		boolean running = CodewindManager.getManager().getInstallStatus().isStarted();
		localProjectButtons.stream().forEach(button -> {if (!button.isDisposed()) button.setEnabled(!installing && running);});
		
		// Remote project buttons should only be enabled if there is a remote connection
		boolean hasRemoteConnections = !CodewindConnectionManager.activeRemoteConnections().isEmpty();
		remoteProjectButtons.stream().forEach(button -> {if (!button.isDisposed()) button.setEnabled(hasRemoteConnections);});
	}
	
	private void sizeButtons(List<Button> buttons) {
		int width = 0;
		for (Button button : buttons) {
			int x = button.computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
			width = x > width ? x : width;
		}
		width = width + 10;
		for (Button button : buttons) {
			((GridData)button.getLayoutData()).widthHint = width;
		}
	}
	
	public interface LinkAction {
		public void execute();
	}
	
	@Override
	public void setFocus() {
		if (focusControl != null) {
			focusControl.setFocus();
		}
	}
}
