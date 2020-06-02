/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *	 IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.codewind.ui.internal.prefs;

import java.lang.reflect.InvocationTargetException;
import java.net.URL;

import org.eclipse.codewind.core.internal.HttpUtil;
import org.eclipse.codewind.core.internal.HttpUtil.HttpResult;
import org.eclipse.codewind.core.internal.IAuthInfo;
import org.eclipse.codewind.core.internal.Logger;
import org.eclipse.codewind.ui.CodewindUIPlugin;
import org.eclipse.codewind.ui.internal.IDEUtil;
import org.eclipse.codewind.ui.internal.messages.Messages;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.ProgressMonitorPart;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.json.JSONObject;

public class TemplateSourceDetailsPage extends WizardPage {

	public static final String DETAILS_FILE_NAME = "templates.json"; //$NON-NLS-1$
	public static final String NAME_KEY = "name"; //$NON-NLS-1$
	public static final String DESCRIPTION_KEY = "description"; //$NON-NLS-1$

	private Text nameText, descriptionText;
	private String nameValue, descriptionValue;
	private String[] defaultValues = new String[2];
	private Button resetButton;
	private ProgressMonitorPart progressMon;

	protected TemplateSourceDetailsPage(String shellTitle, String pageTitle) {
		super(shellTitle);
		setTitle(pageTitle);
		setDescription(Messages.AddRepoDetailsPageMessage);
	}
	
	protected TemplateSourceDetailsPage(String shellTitle, String pageTitle, String name, String description) {
		this(shellTitle, pageTitle);
		nameValue = name;
		descriptionValue = description;
	}

	@Override
	public void createControl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.horizontalSpacing = 5;
		layout.verticalSpacing = 7;
		composite.setLayout(layout);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		composite.setLayoutData(data);

		Label label = new Label(composite, SWT.NONE);
		label.setText(Messages.AddRepoDialogNameLabel);
		label.setLayoutData(new GridData(GridData.BEGINNING, GridData.CENTER, false, false));

		nameText = new Text(composite, SWT.BORDER);
		nameText.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false));
		if (nameValue != null) {
			nameText.setText(nameValue);
		}

		label = new Label(composite, SWT.NONE);
		label.setText(Messages.AddRepoDialogDescriptionLabel);
		label.setLayoutData(new GridData(GridData.BEGINNING, GridData.CENTER, false, false));

		descriptionText = new Text(composite, SWT.BORDER);
		descriptionText.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false));
		if (descriptionValue != null) {
			descriptionText.setText(descriptionValue);
		}

		// Button to auto fill the name and description fields for the template source
		resetButton = new Button(composite, SWT.PUSH);
		resetButton.setText(Messages.AddRepoDialogResetButtonLabel);
		resetButton.setToolTipText(Messages.AddRepoDialogResetButtonTooltip);
		resetButton.setLayoutData(new GridData(GridData.END, GridData.FILL, false, false, 2, 1));

		// Progress monitor widget
		progressMon = new ProgressMonitorPart(parent, layout, true);
		progressMon.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		progressMon.setVisible(false);

		nameText.addModifyListener((event) -> {
			validate(false);
		});
		descriptionText.addModifyListener((event) -> {
			validate(false);
		});

		resetButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				try {
					nameText.setText(defaultValues[0] == null ? "" : defaultValues[0]);
					descriptionText.setText(defaultValues[1] == null ? "" : defaultValues[1]);
					validate(false);
				} catch (Exception e) {
					Logger.logError("An error occurred trying to get the template source details", e); //$NON-NLS-1$
				}
			}
		});

		resetButton.setVisible(false);

		// Add Context Sensitive Help
		PlatformUI.getWorkbench().getHelpSystem().setHelp(composite, CodewindUIPlugin.MAIN_CONTEXTID);

		setControl(composite);
	}

	public void updatePage(String url, IAuthInfo authInfo) {
		try {
			getContainer().run(true, true, getDetailsRunnable(url, authInfo));
			validate(true);
		} catch (Exception e) {
			Logger.logError("An error occurred trying to get the template source details", e); //$NON-NLS-1$
		}
	}

	private IRunnableWithProgress getDetailsRunnable(final String url, final IAuthInfo authInfo) {
		return new IRunnableWithProgress() {
			@Override
			public void run(IProgressMonitor monitor) throws InvocationTargetException {
				try {
					monitor.beginTask(Messages.AddRepoDialogAutoFillTaskLabel, IProgressMonitor.UNKNOWN);

					// Construct the URL for the details file from the template source URL
					URL repoUrl = new URL(url);
					String path = repoUrl.getPath();
					path = path.substring(0, path.lastIndexOf("/") + 1) + DETAILS_FILE_NAME;
					URL detailsUrl = new URL(repoUrl.getProtocol(), repoUrl.getHost(), path);

					// Try to get the template source details
					HttpResult result = HttpUtil.get(detailsUrl.toURI(), authInfo);
					if (result.isGoodResponse && result.response != null && !result.response.isEmpty()) {
						JSONObject jsonObj = new JSONObject(result.response);
						String name = jsonObj.has(NAME_KEY) ? jsonObj.getString(NAME_KEY) : null;
						String description = jsonObj.has(DESCRIPTION_KEY) ? jsonObj.getString(DESCRIPTION_KEY) : null;
						Display.getDefault().syncExec(() -> {
							// The name should at least be set
							if (name == null || name.isEmpty()) {
								Logger.logError("Found the template source information but the name is null or empty: " + detailsUrl);
								setErrorMessage(Messages.AddRepoDialogAutoFillNotAvailableMsg);
							} else {
								defaultValues[0] = name;
								defaultValues[1] = description;
								if (nameValue == null) {
									nameText.setText(name);
								}
								if (descriptionValue == null) {
									descriptionText.setText(description == null ? "" : description);
								}
							}
						});
					} else {
						// Don't log this as an error as the template source may not provide details
						Logger.log("Got error code " + result.error //$NON-NLS-1$
								+ " trying to retrieve the template source details for url: " + detailsUrl //$NON-NLS-1$
								+ ", and error: " + result.error); //$NON-NLS-1$
						setErrorMessage(Messages.AddRepoDialogAutoFillNotAvailableMsg);
					}
				} catch (Exception e) {
					Logger.logError("An error occurred trying to retrieve the template source details for URL: " + url, e); //$NON-NLS-1$
					setErrorMessage(Messages.AddRepoDialogAutoFillNotAvailableMsg);
				} finally {
					monitor.done();
				}
			}
		};
	}

	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible) {
			nameText.setFocus();
		}
	}

	private void validate(boolean init) {
		String errorMsg = null;
		nameValue = IDEUtil.getTextValue(nameText);
		descriptionValue = IDEUtil.getTextValue(descriptionText);
		resetButton.setVisible(defaultValues[0] != null);
		if (resetButton.getVisible()) {
			resetButton.setEnabled((nameValue == null && defaultValues[0] != null) || (nameValue != null && !nameValue.equals(defaultValues[0])) ||
					(descriptionValue == null && defaultValues[1] != null) || (descriptionValue != null && !descriptionValue.equals(defaultValues[1])));
		}
		if (nameValue == null) {
			errorMsg = Messages.AddRepoDialogNoName;
		} else if (descriptionValue == null) {
			errorMsg = Messages.AddRepoDialogNoDescription;
		}
		if (init) {
			// Errors should not show when the page is first opened
			setErrorMessage(null);
		} else {
			setErrorMessage(errorMsg);
		}
		getContainer().updateButtons();
	}

	@Override
	public boolean canFlipToNextPage() {
		return false;
	}

	boolean isActivePage() {
		return isCurrentPage();
	}

	boolean canFinish() {
		return nameValue != null && descriptionValue != null;
	}
	
	String getTemplateSourceName() {
		return nameValue;
	}
	
	String getTemplateSourceDescription() {
		return descriptionValue;
	}
}
