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
import org.eclipse.codewind.ui.internal.IDEUtil;
import org.eclipse.codewind.ui.internal.messages.Messages;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;

public class ProjectValidationPage extends WizardPage {

	private BindProjectWizard wizard;
	private CodewindConnection connection;
	private IPath projectPath;
	private ProjectInfo projectInfo;
	private Text validateMsg;
	private StyledText typeText;

	protected ProjectValidationPage(BindProjectWizard wizard, CodewindConnection connection, IPath projectPath) {
		super(Messages.ProjectValidationPageName);
		setTitle(Messages.ProjectValidationPageTitle);
		setDescription(Messages.ProjectValidationPageDescription);
		this.wizard = wizard;
		this.connection = connection;
		this.projectPath = projectPath;
	}

	@Override
	public void createControl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.horizontalSpacing = 5;
		layout.verticalSpacing = 7;
		composite.setLayout(layout);
		composite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		validateMsg = new Text(composite, SWT.READ_ONLY | SWT.MULTI | SWT.WRAP);
		validateMsg.setText("");
		GridData data = new GridData(GridData.FILL, GridData.CENTER, true, false);
		data.widthHint = 200;
		validateMsg.setLayoutData(data);
		IDEUtil.normalizeBackground(validateMsg, composite);
		
		new Label(composite, SWT.NONE).setLayoutData(new GridData(GridData.BEGINNING, GridData.CENTER, false, false));
		
		typeText = new StyledText(composite, SWT.READ_ONLY);
		typeText.setText("");
		data = new GridData(GridData.FILL, GridData.CENTER, true, false);
		data.horizontalIndent = 20;
		typeText.setLayoutData(data);
		IDEUtil.normalizeBackground(typeText, composite);
		
		setProjectPath(projectPath, false);

		validateMsg.setFocus();
		setControl(composite);
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

	public void setProjectPath(IPath projectPath, boolean fromPrevPage) {
		this.projectPath = projectPath;
		this.projectInfo = null;
		if (projectPath == null) {
			return;
		}
		
		IRunnableWithProgress runnable = new IRunnableWithProgress() {
			@Override
			public void run(IProgressMonitor monitor) throws InvocationTargetException {
				SubMonitor mon = SubMonitor.convert(monitor, NLS.bind(Messages.ProjectValidationTask, projectPath.lastSegment()), 100);
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
			Logger.logError("An error occurred getting the project info for: " + projectPath.lastSegment(), e);
		} catch (InterruptedException e) {
			// The user cancelled the operation
		}
		wizard.setProjectInfo(projectInfo);
		if (projectInfo != null) {
			validateMsg.setText(NLS.bind(Messages.ProjectValidationPageMsg, projectPath.lastSegment()));
			typeText.setText(projectInfo.type.getDisplayName());
			IDEUtil.setBold(typeText);
			IDEUtil.normalizeBackground(typeText, typeText.getParent());
			typeText.setVisible(true);
		} else {
			validateMsg.setText(Messages.ProjectValidationPageFailMsg);
			typeText.setVisible(false);
		}
		
		int width = validateMsg.getParent().getClientArea().width;
		validateMsg.setSize(width, validateMsg.computeSize(width, SWT.DEFAULT).y);
	}

	private ProjectInfo getProjectInfo(IProgressMonitor monitor) {
		if (connection == null || projectPath == null) {
			return null;
		}

		try {
			return ProjectUtil.validateProject(projectPath.lastSegment(), projectPath.toFile().getAbsolutePath(), monitor);
		} catch (Exception e) {
			Logger.logError("An error occurred trying to get the project type for project: " + projectPath.lastSegment(), e); //$NON-NLS-1$
		}

		return null;
	}
}
