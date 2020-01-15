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

import java.lang.reflect.InvocationTargetException;

import org.eclipse.codewind.core.internal.Logger;
import org.eclipse.codewind.core.internal.cli.ProjectUtil;
import org.eclipse.codewind.core.internal.connection.CodewindConnection;
import org.eclipse.codewind.core.internal.constants.ProjectInfo;
import org.eclipse.codewind.core.internal.constants.ProjectLanguage;
import org.eclipse.codewind.ui.internal.IDEUtil;
import org.eclipse.codewind.ui.internal.messages.Messages;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;

public class ProjectValidationPage extends WizardPage {

	private BindProjectWizard wizard;
	private CodewindConnection connection;
	private IProject project;
	private IPath projectPath;
	private ProjectInfo projectInfo;
	private Text validateMsg;
	private Text typeText, languageText;
	private Font boldFont;

	protected ProjectValidationPage(BindProjectWizard wizard, CodewindConnection connection, IProject project) {
		super(Messages.ProjectValidationPageName);
		setTitle(Messages.ProjectValidationPageTitle);
		setDescription(Messages.ProjectValidationPageDescription);
		this.wizard = wizard;
		this.connection = connection;
		this.project = project;
		if (project != null) {
			this.projectPath = project.getLocation();
		}
	}

	@Override
	public void createControl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.horizontalSpacing = 5;
		layout.verticalSpacing = 7;
		layout.numColumns = 2;
		composite.setLayout(layout);
		composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		validateMsg = new Text(composite, SWT.READ_ONLY | SWT.MULTI | SWT.WRAP);
		validateMsg.setText("");
		GridData data = new GridData(GridData.FILL, GridData.CENTER, true, false, 2, 1);
		data.widthHint = 200;
		validateMsg.setLayoutData(data);
		IDEUtil.normalizeBackground(validateMsg, composite);
		
		new Label(composite, SWT.NONE).setLayoutData(new GridData(GridData.BEGINNING, GridData.CENTER, false, false, 2, 1));
		
		boldFont = IDEUtil.getBoldFont(getShell(), getFont());
        
		Label typeLabel = new Label(composite, SWT.NONE);
		typeLabel.setText("Type:");
		data = new GridData(GridData.END, GridData.CENTER, false, false);
		data.horizontalIndent = 20;
		typeLabel.setLayoutData(data);
		
		typeText = new Text(composite, SWT.READ_ONLY);
		typeText.setFont(boldFont);
		typeText.setText("");
		typeText.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, false));
		IDEUtil.normalizeBackground(typeText, composite);
		
		Label languageLabel = new Label(composite, SWT.NONE);
		languageLabel.setText("Language:");
		data = new GridData(GridData.END, GridData.CENTER, false, false);
		data.horizontalIndent = 20;
		languageLabel.setLayoutData(data);
		
		languageText = new Text(composite, SWT.READ_ONLY);
		languageText.setFont(boldFont);
		languageText.setText("");
		languageText.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, false));
		IDEUtil.normalizeBackground(languageText, composite);
		
		setProject(project, projectPath, false);

		validateMsg.setFocus();
		setControl(composite);
	}

	@Override
	public void dispose() {
		if (boldFont != null) {
			boldFont.dispose();
		}
		super.dispose();
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

	public void setProject(IProject project, IPath projectPath, boolean fromPrevPage) {
		this.project = project;
		this.projectPath = projectPath;
		this.projectInfo = null;
		if (projectPath == null) {
			return;
		}
		
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
		wizard.setProjectInfo(projectInfo);
		if (projectInfo != null) {
			validateMsg.setText(NLS.bind(Messages.ProjectValidationPageMsg, getProjectName()));
			typeText.setText(projectInfo.type.getDisplayName());
			IDEUtil.normalizeBackground(typeText, typeText.getParent());
			typeText.setVisible(true);
			
			if (projectInfo.language != ProjectLanguage.LANGUAGE_UNKNOWN) {
				languageText.setText(projectInfo.language.getDisplayName());
				IDEUtil.normalizeBackground(languageText, languageText.getParent());
				languageText.setVisible(true);
			} else {
				languageText.setVisible(false);
			}
		} else {
			validateMsg.setText(Messages.ProjectValidationPageFailMsg);
			typeText.setVisible(false);
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
