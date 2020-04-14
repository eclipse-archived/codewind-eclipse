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

package org.eclipse.codewind.ui.internal.wizards;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.eclipse.codewind.core.internal.Logger;
import org.eclipse.codewind.core.internal.cli.ProjectUtil;
import org.eclipse.codewind.core.internal.cli.RegistryUtil;
import org.eclipse.codewind.core.internal.connection.CodewindConnection;
import org.eclipse.codewind.core.internal.connection.ImagePushRegistryInfo;
import org.eclipse.codewind.core.internal.connection.RegistryInfo;
import org.eclipse.codewind.core.internal.constants.ProjectInfo;
import org.eclipse.codewind.ui.CodewindUIPlugin;
import org.eclipse.codewind.ui.internal.IDEUtil;
import org.eclipse.codewind.ui.internal.messages.Messages;
import org.eclipse.codewind.ui.internal.prefs.RegistryManagementDialog;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;

public class ProjectValidationPage extends WizardPage {

	private CodewindConnection connection;
	private IProject project;
	private IPath projectPath;
	private ProjectInfo projectInfo;
	private Text validateMsg;
	private Label typeLabel, languageLabel;
	private Text typeText, languageText;
	private Font boldFont;
	private Composite manageRegistriesComp;

	protected ProjectValidationPage(CodewindConnection connection, IProject project) {
		super(Messages.ProjectValidationPageName);
		setTitle(Messages.ProjectValidationPageTitle);
		setDescription(Messages.ProjectValidationPageDescription);
		this.connection = connection;
		this.project = project;
		if (project != null) {
			this.projectPath = project.getLocation();
		}
	}

	@Override
	public void createControl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.horizontalSpacing = 5;
		layout.verticalSpacing = 7;
		layout.numColumns = 2;
		composite.setLayout(layout);
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL));
		
		validateMsg = new Text(composite, SWT.READ_ONLY | SWT.MULTI | SWT.WRAP);
		validateMsg.setText("");
		GridData data = new GridData(GridData.FILL, GridData.CENTER, true, false, 2, 1);
		data.widthHint = 200;
		validateMsg.setLayoutData(data);
		IDEUtil.normalizeBackground(validateMsg, composite);
		
		new Label(composite, SWT.NONE).setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 2, 1));
		
		Composite typeComp = new Composite(composite, SWT.NONE);
		layout = new GridLayout();
		layout.marginTop = 10;
		layout.horizontalSpacing = 5;
		layout.verticalSpacing = 7;
		layout.numColumns = 2;
		typeComp.setLayout(layout);
		typeComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 2, 1));
		
		boldFont = IDEUtil.newFont(getShell(), getFont(), SWT.BOLD);
        
		typeLabel = new Label(typeComp, SWT.NONE);
		typeLabel.setText("Type:");
		data = new GridData(GridData.END, GridData.CENTER, false, false);
		data.horizontalIndent = 20;
		typeLabel.setLayoutData(data);
		
		typeText = new Text(typeComp, SWT.READ_ONLY);
		typeText.setFont(boldFont);
		typeText.setText("");
		typeText.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, false));
		IDEUtil.normalizeBackground(typeText, typeComp);
		
		languageLabel = new Label(typeComp, SWT.NONE);
		languageLabel.setText("Language:");
		data = new GridData(GridData.END, GridData.CENTER, false, false);
		data.horizontalIndent = 20;
		languageLabel.setLayoutData(data);
		
		languageText = new Text(typeComp, SWT.READ_ONLY);
		languageText.setFont(boldFont);
		languageText.setText("");
		languageText.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, false));
		IDEUtil.normalizeBackground(languageText, typeComp);

		// Manage registries link
		new Label(composite, SWT.NONE).setLayoutData(new GridData(GridData.FILL, GridData.FILL, false, false, 2, 1));
		manageRegistriesComp = new Composite(composite, SWT.NONE);
		manageRegistriesComp.setLayout(new GridLayout(1, false));
		manageRegistriesComp.setLayoutData(new GridData(GridData.BEGINNING, GridData.END, false, false, 2, 1));
		
		Link manageRegistriesLink = new Link(manageRegistriesComp, SWT.NONE);
		manageRegistriesLink.setText(Messages.ManageRegistriesLinkLabel + " <a>" + Messages.ManageRegistriesLinkText + "</a>");
		manageRegistriesLink.setToolTipText(Messages.ManageRegistriesLinkTooltip);
		manageRegistriesLink.setLayoutData(new GridData(GridData.BEGINNING, GridData.CENTER, false, false));

		manageRegistriesLink.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				try {
					List<RegistryInfo> regList = RegistryUtil.listRegistrySecrets(connection.getConid(), new NullProgressMonitor());
					ImagePushRegistryInfo pushReg = connection.requestGetPushRegistry();
					RegistryManagementDialog regDialog = new RegistryManagementDialog(getShell(), connection, regList, pushReg);
					if (regDialog.open() == Window.OK) {
						if (regDialog.hasChanges()) {
							IRunnableWithProgress runnable = new IRunnableWithProgress() {
								@Override
								public void run(IProgressMonitor monitor) throws InvocationTargetException {
									SubMonitor mon = SubMonitor.convert(monitor, Messages.RegUpdateTask, 100);
									IStatus status = regDialog.updateRegistries(mon.split(75));
									if (!status.isOK()) {
										throw new InvocationTargetException(status.getException(), status.getMessage());
									}
									if (mon.isCanceled()) {
										return;
									}
								}
							};
							try {
								getWizard().getContainer().run(true, true, runnable);
							} catch (InvocationTargetException e) {
								MessageDialog.openError(getShell(), Messages.RegUpdateErrorTitle, e.getMessage());
								return;
							} catch (InterruptedException e) {
								// The user cancelled the operation
								return;
							}
						}
					}
				} catch (Exception e) {
					MessageDialog.openError(getShell(), Messages.RegListErrorTitle, NLS.bind(Messages.RegListErrorMsg, e));
				}
			}
		});
		
		updatePage(false);

		setControl(composite);
		
		// Add Context Sensitive Help
		PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), CodewindUIPlugin.MAIN_CONTEXTID);
	}

	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible) {
			validateMsg.setFocus();
		}
	}

	@Override
	public void dispose() {
		if (boldFont != null) {
			boldFont.dispose();
		}
		super.dispose();
	}

	@Override
	public boolean canFlipToNextPage() {
		return canFinish();
	}

	public boolean canFinish() {
		return projectInfo != null;
	}
	
	public boolean isActivePage() {
		return isCurrentPage();
	}
	
	public ProjectInfo getProjectInfo() {
		return projectInfo;
	}
	
	public void setConnection(CodewindConnection connection) {
		this.connection = connection;
		updatePage(true);
	}
	
	public void setProject(IProject project, IPath projectPath) {
		this.project = project;
		this.projectPath = projectPath;
		updatePage(true);
	}

	private void updatePage(boolean fromPrevPage) {
		if (connection == null || projectPath == null) {
			return;
		}
		this.projectInfo = null;
		
		IRunnableWithProgress runnable = new IRunnableWithProgress() {
			@Override
			public void run(IProgressMonitor monitor) throws InvocationTargetException {
				SubMonitor mon = SubMonitor.convert(monitor, NLS.bind(Messages.ProjectValidationTask, getProjectName()), 100);
				projectInfo = getProjectInfo(mon.split(100));
			}
		};
			
		try {
			if (fromPrevPage && getWizard() != null && getWizard().getContainer() != null) {
				getContainer().run(true, true, runnable);
			} else {
				PlatformUI.getWorkbench().getProgressService().busyCursorWhile(runnable);
			}
		} catch (InvocationTargetException e) {
			Logger.logError("An error occurred getting the project info for the project at: " + projectPath.toOSString(), e);
		} catch (InterruptedException e) {
			// The user cancelled the operation
		}
		if (projectInfo != null) {
			validateMsg.setText(NLS.bind(Messages.ProjectValidationPageMsg, getProjectName()));
			typeText.setText(projectInfo.type.getDisplayName());
			IDEUtil.normalizeBackground(typeText, typeText.getParent());
			typeLabel.setVisible(true);
			typeText.setVisible(true);
			
			if (!projectInfo.language.isUnknown()) {
				languageText.setText(projectInfo.language.getDisplayName());
				IDEUtil.normalizeBackground(languageText, languageText.getParent());
				languageLabel.setVisible(true);
				languageText.setVisible(true);
			} else {
				languageLabel.setVisible(false);
				languageText.setVisible(false);
			}
		} else {
			validateMsg.setText(Messages.ProjectValidationPageFailMsg);
			typeLabel.setVisible(false);
			typeText.setVisible(false);
			languageLabel.setVisible(false);
			languageText.setVisible(false);
		}
		
		int width = validateMsg.getParent().getClientArea().width;
		validateMsg.setSize(width, validateMsg.computeSize(width, SWT.DEFAULT).y);
	}

	private ProjectInfo getProjectInfo(IProgressMonitor monitor) {
		if (connection == null || projectPath == null) {
			return null;
		}

		try {
			return ProjectUtil.validateProject(getProjectName(), projectPath.toFile().getAbsolutePath(), null, connection.getConid(), monitor);
		} catch (Exception e) {
			Logger.logError("An error occurred trying to get the project type for the project at: " + projectPath.toOSString(), e); //$NON-NLS-1$
		}

		return null;
	}
	
	private String getProjectName() {
		return project != null ? project.getName() : projectPath.lastSegment();
	}
}
