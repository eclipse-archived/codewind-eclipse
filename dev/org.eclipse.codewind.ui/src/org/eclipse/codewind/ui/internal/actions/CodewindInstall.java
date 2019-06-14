/*******************************************************************************
e * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *	 IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.codewind.ui.internal.actions;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.TimeoutException;

import org.eclipse.codewind.core.internal.InstallUtil;
import org.eclipse.codewind.core.internal.Logger;
import org.eclipse.codewind.core.internal.ProcessHelper.ProcessResult;
import org.eclipse.codewind.ui.CodewindUIPlugin;
import org.eclipse.codewind.ui.internal.messages.Messages;
import org.eclipse.codewind.ui.internal.wizards.NewCodewindProjectWizard;
import org.eclipse.codewind.ui.internal.wizards.WizardLauncher;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;


public class CodewindInstall {
		
	public static boolean isCodewindInstalled() throws InvocationTargetException {
						
			try {
				ProcessResult result = InstallUtil.statusCodewind();

				if (result.getExitValue() == 0) {
					return false;
				} else {
					return true;
				}
				
			} catch (IOException e) {
				throw new InvocationTargetException(e, "An error occurred trying to determine Codewind status: " + e.getMessage()); //$NON-NLS-1$
			} catch (TimeoutException e) {
				throw new InvocationTargetException(e, "Codewind did not return status in the expected time: " + e.getMessage()); //$NON-NLS-1$
			}
		
	}



	public static void codewindInstallerDialog() {
		Shell shell = new Shell();
		MessageBox dialog =
			    new MessageBox(shell, SWT.ICON_QUESTION | SWT.YES| SWT.NO);
			dialog.setText(Messages.InstallCodewindDialogTitle);
			dialog.setMessage(Messages.InstallCodewindDialogMessage);

			int rc = dialog.open();

		    switch (rc) {
		    case SWT.YES:
				installCodewind();
			 break;
		    }
	}
	
	public static void installCodewind() { 

		try {
			Job job = new Job(Messages.InstallCodewindJobLabel) {
			    @Override
				protected IStatus run(IProgressMonitor monitor) {

					try {
						
						ProcessResult result = InstallUtil.installCodewind(monitor);
						
						if (monitor.isCanceled()) {
							removeCodewind();
							return Status.CANCEL_STATUS;
						}
						
						if (result.getExitValue() != 0) {
							return new Status(IStatus.ERROR, CodewindUIPlugin.PLUGIN_ID, "There was a problem trying to install Codewind: " + result.getError());
						}
						
						if (result.getExitValue() == 0) {
							startingCodewind();
						}
						
					} catch (IOException e) {
						return new Status(IStatus.ERROR, CodewindUIPlugin.PLUGIN_ID, "An error occurred trying to install Codewind.", e);
					} catch (TimeoutException e) {
						return new Status(IStatus.ERROR, CodewindUIPlugin.PLUGIN_ID, "Codewind did not install in the expected time.", e);
					}

					return Status.OK_STATUS;
					
				}
			};
			job.schedule();
		} catch (Exception e) {
			Logger.logError("An error occurred installing Codewind: ", e); //$NON-NLS-1$
		}
		
	}
	
	public static void startingCodewind() { 

		try {
			Job job = new Job(Messages.InstalledCodewindJobLabel) {
			    @Override
				protected IStatus run(IProgressMonitor monitor) {

					try {
						
						ProcessResult result = InstallUtil.startCodewind(monitor);
						
						if (result.getExitValue() != 0) {
							return new Status(IStatus.ERROR, CodewindUIPlugin.PLUGIN_ID, "There was a problem trying to start Codewind: " + result.getError());
						}
						
						Display.getDefault().asyncExec(new Runnable() {
							public void run() {
								promptUserAfterInstall();
					        }
					    });
					 
					} catch (IOException e) {
						return new Status(IStatus.ERROR, CodewindUIPlugin.PLUGIN_ID, "An error occurred trying to start Codewind.", e);
					} catch (TimeoutException e) {
						return new Status(IStatus.ERROR, CodewindUIPlugin.PLUGIN_ID, "Codewind did not start in the expected time.", e);
					}

					return Status.OK_STATUS;
					
				}
			};
			job.schedule();
		} catch (Exception e) {
			Logger.logError("An error occurred activating connection: ", e); //$NON-NLS-1$
		}
		
	}
	
	public static void promptUserAfterInstall() {
	    Shell shell = new Shell();
		MessageBox dialog =
			    new MessageBox(shell, SWT.ICON_QUESTION | SWT.YES| SWT.NO);
			dialog.setText(Messages.InstallCodewindDialogTitle);
			dialog.setMessage(Messages.InstallCodewindAfterDialogMessage);

			int rc = dialog.open();

		    switch (rc) {
		    case SWT.YES:

		   	Wizard wizard = new NewCodewindProjectWizard();
		   	WizardLauncher.launchWizardWithoutSelection(wizard);
		   		
			 break;
		    }
	}
	
	public static void removeCodewind() {

		try {
			Job job = new Job(Messages.RemovingCodewindJobLabel) {
			    @Override
				protected IStatus run(IProgressMonitor monitor) {

					try {
						ProcessResult result = InstallUtil.removeCodewind(monitor);
						
						if (result.getExitValue() != 0) {
							return new Status(IStatus.ERROR, CodewindUIPlugin.PLUGIN_ID, "There was a problem trying to remove Codewind images: " + result.getError());
						}
						
						
					} catch (IOException e) {
						return new Status(IStatus.ERROR, CodewindUIPlugin.PLUGIN_ID, "An error occurred trying to remove Codewind.", e);
					} catch (TimeoutException e) {
						return new Status(IStatus.ERROR, CodewindUIPlugin.PLUGIN_ID, "Codewind did not remove in the expected time.", e);
					}

					return Status.OK_STATUS;
					
				}
			};
			job.schedule();
		} catch (Exception e) {
			Logger.logError("An error occurred removing Codewind images: ", e); //$NON-NLS-1$
		}
		
	}
	
	
}
