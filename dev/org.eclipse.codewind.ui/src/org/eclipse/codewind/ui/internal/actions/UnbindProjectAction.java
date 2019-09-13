/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *	 IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.codewind.ui.internal.actions;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.codewind.core.internal.CodewindEclipseApplication;
import org.eclipse.codewind.core.internal.FileUtil;
import org.eclipse.codewind.core.internal.Logger;
import org.eclipse.codewind.ui.CodewindUIPlugin;
import org.eclipse.codewind.ui.internal.messages.Messages;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.LayoutConstants;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.actions.SelectionProviderAction;

/**
 * Action for unbinding a Codewind project.
 */
public class UnbindProjectAction extends SelectionProviderAction {
	
	List<CodewindEclipseApplication> apps;
	
	public UnbindProjectAction(ISelectionProvider selectionProvider) {
		super(selectionProvider, Messages.UnbindActionLabel);
		selectionChanged(getStructuredSelection());
	}

	@Override
	public void selectionChanged(IStructuredSelection sel) {
		boolean enabled = false;
		apps = new ArrayList<CodewindEclipseApplication>();
		for (Object obj : sel.toList()) {
			if (obj instanceof CodewindEclipseApplication) {
				apps.add((CodewindEclipseApplication) obj);
				enabled = true;
			} else {
				enabled = false;
				break;
			}
		}
		setEnabled(enabled);
	}

	@Override
	public void run() {
		if (apps == null || apps.isEmpty()) {
			// should not be possible
			Logger.logError("UnbindProjectAction ran but no application was selected"); //$NON-NLS-1$
			return;
		}
		
		String msg;
		if (apps.size() == 1) {
			msg = NLS.bind(Messages.UnbindActionMessage, apps.get(0).name);
		} else {
			msg = NLS.bind(Messages.UnbindActionMultipleMessage, apps.size());
		}
		final RemoveDialog removeDialog = new RemoveDialog(Display.getDefault().getActiveShell(), msg, apps);
		final int[] result = new int[1]; // Result is the index of the button pressed
		Display.getDefault().syncExec(new Runnable() {
			@Override
			public void run() {
				result[0] = removeDialog.open();
			}
		});
		if (result[0] == 0) {  // OK button is 0 index
			final boolean deleteContent = removeDialog.getDeleteContent();
			String jobTitle;
			if (apps.size() == 1) {
				jobTitle = NLS.bind(Messages.UnbindActionJobTitle, apps.get(0).name);
			} else {
				jobTitle = NLS.bind(Messages.UnbindActionMultipleJobTitle, apps.size());
			}
			Job job = new Job(jobTitle) {
				@Override
				protected IStatus run(IProgressMonitor monitor) {
					for (CodewindEclipseApplication app : apps) {
						try {
							app.connection.requestProjectUnbind(app.projectID);
							if (deleteContent) {
								IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(app.name);
								if (project != null && project.exists() && project.getLocation().toFile().equals(app.fullLocalPath.toFile())) {
									project.delete(true, true, monitor);
								} else if (app.fullLocalPath.toFile().exists()) {
									FileUtil.deleteDirectory(app.fullLocalPath.toOSString(), true);
								}
							}
						} catch (Exception e) {
							Logger.logError("Error requesting application remove: " + app.name, e); //$NON-NLS-1$
							return new Status(IStatus.ERROR, CodewindUIPlugin.PLUGIN_ID, NLS.bind(Messages.UnbindActionError, app.name), e);
						}
					}
					return Status.OK_STATUS;
				}
			};
			job.schedule();
		}
	}
	
	public static class RemoveDialog extends MessageDialog {
		
		List<CodewindEclipseApplication> apps;
		boolean deleteContent = false;
		
		public RemoveDialog(Shell parentShell, String msg, List<CodewindEclipseApplication> apps) {
			super(parentShell, Messages.UnbindActionTitle, null, msg, MessageDialog.QUESTION,
					0, IDialogConstants.OK_LABEL, IDialogConstants.CANCEL_LABEL);
			this.apps = apps;
		}

		@Override
		protected Control createCustomArea(Composite parent) {
			Point defaultSpacing = LayoutConstants.getSpacing();
			
			Composite composite = new Composite(parent, SWT.NONE);
			composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			GridLayout layout = new GridLayout(1, false);
			layout.horizontalSpacing = defaultSpacing.x * 2;
			layout.verticalSpacing = defaultSpacing.y;
			composite.setLayout(layout);
			
			Button deleteContentButton = new Button(composite, SWT.CHECK);
			deleteContentButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
			deleteContentButton.setText(Messages.UnbindActionDeleteContentsLabel);
			deleteContentButton.setFocus();
			deleteContentButton.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					deleteContent = deleteContentButton.getSelection();
				}
			});
			
			Text locationLabel = new Text(composite, SWT.READ_ONLY);
			GridData labelData = new GridData(SWT.FILL, SWT.FILL, true, false);
			labelData.verticalIndent = 5;
			locationLabel.setLayoutData(labelData);
			if (apps.size() == 1) {
				locationLabel.setText(Messages.UnbindActionLocationLabel);
			} else {
				locationLabel.setText(NLS.bind(Messages.UnbindActionMultipleLocationLabel, apps.size()));
			}
			locationLabel.setForeground(composite.getForeground());
			locationLabel.setBackground(composite.getBackground());
			
			int style= SWT.MULTI | SWT.READ_ONLY | SWT.WRAP | SWT.V_SCROLL;
			if (apps.size() > 1) {
				style |= SWT.BORDER;
			}
			StyledText locationList = new StyledText(composite, style);
			locationList.setAlwaysShowScrollBars(false);
			labelData.horizontalIndent = locationList.getLeftMargin();
			GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
			data.heightHint= convertHeightInCharsToPixels(Math.min(apps.size(), 5));
			locationList.setLayoutData(data);
			locationList.setForeground(composite.getForeground());
			locationList.setBackground(composite.getBackground());
			
			StringBuilder buffer = new StringBuilder();
			for (int i= 0; i < apps.size(); i++) {
				String location = apps.get(i).fullLocalPath.toOSString();
				if (location != null) {
					if (buffer.length() > 0) {
						buffer.append('\n');
					}
					buffer.append(location);
				}
			}
			locationList.setText(buffer.toString());
			
			return composite;
		}
		
		public boolean getDeleteContent() {
			return deleteContent;
		}
		
	}
}
