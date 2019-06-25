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

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.TimeoutException;

import org.eclipse.codewind.core.CodewindCorePlugin;
import org.eclipse.codewind.core.internal.CodewindManager;
import org.eclipse.codewind.core.internal.InstallUtil;
import org.eclipse.codewind.core.internal.InstallUtil.InstallStatus;
import org.eclipse.codewind.core.internal.Logger;
import org.eclipse.codewind.core.internal.ProcessHelper.ProcessResult;
import org.eclipse.codewind.core.internal.connection.CodewindConnection;
import org.eclipse.codewind.ui.CodewindUIPlugin;
import org.eclipse.codewind.ui.internal.messages.Messages;
import org.eclipse.codewind.ui.internal.views.ViewHelper;
import org.eclipse.codewind.ui.internal.wizards.BindProjectWizard;
import org.eclipse.codewind.ui.internal.wizards.NewCodewindProjectWizard;
import org.eclipse.codewind.ui.internal.wizards.WizardLauncher;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.osgi.util.NLS;
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
		Shell shell = Display.getDefault().getActiveShell();
		if (MessageDialog.openQuestion(shell, Messages.InstallCodewindDialogTitle,
				Messages.InstallCodewindDialogMessage)) {
			installCodewind(getNewProjectPrompt());
		}
	}
	
	public static void codewindInstallerDialog(IProject project) {
		Shell shell = Display.getDefault().getActiveShell();
		if (MessageDialog.openQuestion(shell, Messages.InstallCodewindDialogTitle,
				Messages.InstallCodewindDialogMessage)) {
			installCodewind(addExistingProjectPrompt(project));
		}
	}
	
	public static void installCodewind(Runnable prompt) { 

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
							return getErrorStatus("There was a problem trying to install Codewind: " + result.getError());
						}
						
						if (result.getExitValue() == 0) {
							startCodewind(prompt);
						}

					} catch (IOException e) {
						return getErrorStatus("An error occurred trying to install Codewind.", e);
					} catch (TimeoutException e) {
						return getErrorStatus("Codewind did not install in the expected time.", e);
					}

					ViewHelper.refreshCodewindExplorerView(null);
					return Status.OK_STATUS;
					
				}
			};
			job.schedule();
		} catch (Exception e) {
			Logger.logError("An error occurred installing Codewind: ", e); //$NON-NLS-1$
		}
		
	}
	
	public static void startCodewind(Runnable prompt) { 

		try {
			Job job = new Job(Messages.StartingCodewindJobLabel) {
			    @Override
				protected IStatus run(IProgressMonitor monitor) {

					try {
						
						ProcessResult result = InstallUtil.startCodewind(monitor);

						if (monitor.isCanceled()) {
							return Status.CANCEL_STATUS;
						}

						if (result.getExitValue() != 0) {
							return getErrorStatus("There was a problem trying to start Codewind: " + result.getError());
						}
						
						if (prompt != null) {
							Display.getDefault().asyncExec(prompt);
						}
					 
					} catch (IOException e) {
						return getErrorStatus("An error occurred trying to start Codewind.", e);
					} catch (TimeoutException e) {
						return getErrorStatus("Codewind did not start in the expected time.", e);
					}

					ViewHelper.refreshCodewindExplorerView(null);
					return Status.OK_STATUS;
					
				}
			};
			job.schedule();
		} catch (Exception e) {
			Logger.logError("An error occurred starting Codewind: ", e); //$NON-NLS-1$
		}
		
	}
	
	public static void stopCodewind() {

		try {
			Job job = new Job(Messages.StoppingCodewindJobLabel) {
			    @Override
				protected IStatus run(IProgressMonitor monitor) {

					try {
						
						boolean stopAll = getStopAll(monitor);
						if (monitor.isCanceled()) {
							return Status.CANCEL_STATUS;
						}
						
						ProcessResult result = InstallUtil.stopCodewind(stopAll, monitor);

						if (monitor.isCanceled()) {
							return Status.CANCEL_STATUS;
						}

						if (result.getExitValue() != 0) {
							return getErrorStatus("There was a problem trying to stop Codewind: " + result.getError());
						}
					 
					} catch (IOException e) {
						return getErrorStatus("An error occurred trying to stop Codewind.", e);
					} catch (TimeoutException e) {
						return getErrorStatus("Codewind did not stop in the expected time.", e);
					}

					ViewHelper.refreshCodewindExplorerView(null);
					return Status.OK_STATUS;
					
				}
			};
			job.schedule();
		} catch (Exception e) {
			Logger.logError("An error occurred stopping Codewind: ", e); //$NON-NLS-1$
		}
		
	}
	
	public static Runnable getNewProjectPrompt() {
		return new Runnable() {
			@Override
			public void run() {
				Shell shell = Display.getDefault().getActiveShell();
				if (MessageDialog.openQuestion(shell, Messages.InstallCodewindDialogTitle,
						Messages.InstallCodewindNewProjectMessage)) {
					Wizard wizard = new NewCodewindProjectWizard();
					WizardLauncher.launchWizardWithoutSelection(wizard);
				}
			}
		};
	}
	
	public static Runnable addExistingProjectPrompt(IProject project) {
		return new Runnable() {
			@Override
			public void run() {
				Shell shell = Display.getDefault().getActiveShell();
				if (MessageDialog.openQuestion(shell, Messages.InstallCodewindDialogTitle,
						NLS.bind(Messages.InstallCodewindAddProjectMessage, project.getName()))) {
					final CodewindManager manager = CodewindManager.getManager();
					CodewindConnection connection = manager.createLocalConnection();
					if (connection != null && connection.isConnected()) {
						Wizard wizard = new BindProjectWizard(connection, project);
						WizardLauncher.launchWizardWithoutSelection(wizard);
					} else {
						Logger.logError("In BindProjectAction run method and Codewind is not installed or has unknown status.");
						return;
					}
				}
			}
		};
	}
	
	public static void removeCodewind() {

		try {
			Job job = new Job(Messages.RemovingCodewindJobLabel) {
			    @Override
				protected IStatus run(IProgressMonitor mon) {
			    	SubMonitor monitor = SubMonitor.convert(mon, 100);
					try {
						InstallStatus status = InstallUtil.getInstallStatus();
						if (status == InstallStatus.RUNNING) {
							// Stop Codewind before uninstalling

							boolean stopAll = getStopAll(monitor);
							if (monitor.isCanceled()) {
								return Status.CANCEL_STATUS;
							}

							ProcessResult result = InstallUtil.stopCodewind(stopAll, monitor.split(20));
							
							if (monitor.isCanceled()) {
								return Status.CANCEL_STATUS;
							}
							
							if (result.getExitValue() != 0) {
								return getErrorStatus("There was a problem trying to stop Codewind: " + result.getError());
							}
						}
						
						if (monitor.isCanceled()) {
							return Status.CANCEL_STATUS;
						}
						
						monitor.setWorkRemaining(80);
						ProcessResult result = InstallUtil.removeCodewind(monitor);
						
						if (monitor.isCanceled()) {
							return Status.CANCEL_STATUS;
						}
						
						if (result.getExitValue() != 0) {
							return getErrorStatus("There was a problem trying to remove Codewind images: " + result.getError());
						}
						
						
					} catch (IOException e) {
						return getErrorStatus("An error occurred trying to remove Codewind.", e);
					} catch (TimeoutException e) {
						return getErrorStatus("Codewind did not remove in the expected time.", e);
					}
					
					ViewHelper.refreshCodewindExplorerView(null);
					return Status.OK_STATUS;
					
				}
			};
			job.schedule();
		} catch (Exception e) {
			Logger.logError("An error occurred removing Codewind images: ", e); //$NON-NLS-1$
		}
		
	}
	
	private static IStatus getErrorStatus(String msg) {
		return getErrorStatus(msg, null);
	}
	
	private static IStatus getErrorStatus(String msg, Throwable t) {
		ViewHelper.refreshCodewindExplorerView(null);
		return new Status(IStatus.ERROR, CodewindUIPlugin.PLUGIN_ID, msg, t);
	}
	
	private static boolean getStopAll(IProgressMonitor monitor) {
		IPreferenceStore prefs = CodewindCorePlugin.getDefault().getPreferenceStore();
		if (InstallUtil.STOP_APP_CONTAINERS_PROMPT.contentEquals(prefs.getString(InstallUtil.STOP_APP_CONTAINERS_PREFSKEY))) {
			if (!CodewindManager.getManager().hasActiveApplications()) {
				return false;
			}
			final boolean[] stopApps = new boolean[] {false};
			Display.getDefault().syncExec(new Runnable() {

				@Override
				public void run() {
					MessageDialogWithToggle stopAppsQuestion = MessageDialogWithToggle.openYesNoCancelQuestion(
							Display.getDefault().getActiveShell(), Messages.StopAllDialog_Title,
							Messages.StopAllDialog_Message,
							Messages.StopAllDialog_ToggleMessage, false, null, null);
					switch(stopAppsQuestion.getReturnCode()) {
						case IDialogConstants.YES_ID:
							stopApps[0] = true;
							break;
						case IDialogConstants.CANCEL_ID:
							monitor.setCanceled(true);
							break;
						default:
							break;
					}
					if (!monitor.isCanceled() && stopAppsQuestion.getToggleState()) {
						// Save the user's selection
						prefs.setValue(InstallUtil.STOP_APP_CONTAINERS_PREFSKEY,
								stopApps[0] ? InstallUtil.STOP_APP_CONTAINERS_ALWAYS : InstallUtil.STOP_APP_CONTAINERS_NEVER);
					}
				}
			});
			return stopApps[0];
		}

		return InstallUtil.STOP_APP_CONTAINERS_ALWAYS.contentEquals(prefs.getString(InstallUtil.STOP_APP_CONTAINERS_PREFSKEY));
	}
}
