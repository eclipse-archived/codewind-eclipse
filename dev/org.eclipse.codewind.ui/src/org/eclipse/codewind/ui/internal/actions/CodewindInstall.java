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
import org.eclipse.codewind.core.internal.CodewindManager.InstallerStatus;
import org.eclipse.codewind.core.internal.InstallStatus;
import org.eclipse.codewind.core.internal.InstallUtil;
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
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.json.JSONException;


public class CodewindInstall {
	
	public static boolean ENABLE_STOP_APPS_OPTION = false;
		
	public static boolean isCodewindInstalled() throws InvocationTargetException {
			try {
				InstallStatus status = InstallUtil.getInstallStatus();
				return status != null && status.isInstalled();
			} catch (IOException e) {
				throw new InvocationTargetException(e, "An error occurred trying to determine Codewind status: " + e.getMessage()); //$NON-NLS-1$
			} catch (TimeoutException e) {
				throw new InvocationTargetException(e, "Codewind did not return status in the expected time: " + e.getMessage()); //$NON-NLS-1$
			} catch (JSONException e) {
				throw new InvocationTargetException(e, "The Codewind status format is not recognized: " + e.getMessage()); //$NON-NLS-1$
			}
	}

	public static void codewindInstallerDialog() {
		codewindInstallerDialog(getNewProjectPrompt());
	}
	
	public static void codewindInstallerDialog(IProject project) {
		codewindInstallerDialog(addExistingProjectPrompt(project));
	}
	
	private static void codewindInstallerDialog(Runnable prompt) {
		InstallStatus status = CodewindManager.getManager().getInstallStatus(false);
		Shell shell = Display.getDefault().getActiveShell();
		if (status.hasInstalledVersions()) {
			if (MessageDialog.openQuestion(shell, Messages.InstallCodewindDialogTitle, Messages.UpgradeCodewindDialogMessage)) {
				updateCodewind(InstallUtil.getVersion(), true, prompt);
			}
		} else {
			if (MessageDialog.openQuestion(shell, Messages.InstallCodewindDialogTitle, Messages.InstallCodewindDialogMessage)) {
				installCodewind(InstallUtil.getVersion(), prompt);
			}
		}
	}
	
	public static void installerActiveDialog(InstallerStatus status) {
		if (status == null) {
			// This should not happen
			Logger.logError("The installerActiveDialog method is invoked but the installer status is null"); //$NON-NLS-1$
			return;
		}
		String msg = null;
		switch(status) {
			case INSTALLING:
			case STARTING:
				msg = Messages.InstallCodewindInstallingMessage;
				break;
			case UNINSTALLING:
			case STOPPING:
				msg = Messages.InstallCodewindUninstallingMessage;
				break;
			default:
				Logger.logError("The installerActiveDialog method is invoked but the installer status is not recognized: " + status); //$NON-NLS-1$
				return;
		}
		MessageDialog.openError(Display.getDefault().getActiveShell(), Messages.InstallCodewindDialogTitle, msg);
	}
	
	public static void installCodewind(String version, Runnable prompt) { 

		try {
			Job job = new Job(Messages.InstallCodewindJobLabel) {
			    @Override
				protected IStatus run(IProgressMonitor progressMon) {
			    	try {
						SubMonitor mon = SubMonitor.convert(progressMon, 100);
						mon.setTaskName(Messages.InstallingCodewindTask);
						
						ProcessResult result;
						try {
							result = InstallUtil.installCodewind(version, mon.split(95));
							
							if (mon.isCanceled()) {
								removeCodewind(version);
								return Status.CANCEL_STATUS;
							}
							
							if (result.getExitValue() != 0) {
								return getErrorStatus(result, Messages.CodewindInstallFail);
							}
						} catch (TimeoutException e) {
							return getErrorStatus(Messages.CodewindInstallTimeout, e);
						}
					
						mon.setTaskName(Messages.StartingCodewindJobLabel);
						
						try {
							result = InstallUtil.startCodewind(version, mon.split(5));
							
							if (mon.isCanceled()) {
								removeCodewind(version);
								return Status.CANCEL_STATUS;
							}
							
							if (result.getExitValue() != 0) {
								return getErrorStatus(result, Messages.CodewindStartFail);
							}
						} catch (TimeoutException e) {
							return getErrorStatus(Messages.CodewindStartTimeout, e);
						}

						if (prompt != null) {
							Display.getDefault().asyncExec(prompt);
						}
			    	} catch (IOException e) {
			    		return getErrorStatus(Messages.CodewindInstallError, e);
			    	}

					ViewHelper.refreshCodewindExplorerView(null);
					return Status.OK_STATUS;
					
				}
			};
			job.setPriority(Job.LONG);
			job.schedule();
		} catch (Exception e) {
			Logger.logError("An error occurred installing Codewind: ", e); //$NON-NLS-1$
		}
		
	}
	
	public static void startCodewind(String version, Runnable prompt) { 

		try {
			Job job = new Job(Messages.StartingCodewindJobLabel) {
			    @Override
				protected IStatus run(IProgressMonitor monitor) {

					try {
						
						ProcessResult result = InstallUtil.startCodewind(version, monitor);

						if (monitor.isCanceled()) {
							return Status.CANCEL_STATUS;
						}

						if (result.getExitValue() != 0) {
							return getErrorStatus(result, Messages.CodewindStartFail);
						}
						
						if (prompt != null) {
							Display.getDefault().asyncExec(prompt);
						}
					 
					} catch (IOException e) {
						return getErrorStatus(Messages.CodewindStartError, e);
					} catch (TimeoutException e) {
						return getErrorStatus(Messages.CodewindStartTimeout, e);
					}

					ViewHelper.refreshCodewindExplorerView(null);
					return Status.OK_STATUS;
					
				}
			};
			job.setPriority(Job.LONG);
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
							return getErrorStatus(result, Messages.CodewindStopFail);
						}
					 
					} catch (IOException e) {
						return getErrorStatus(Messages.CodewindStopError, e);
					} catch (TimeoutException e) {
						return getErrorStatus(Messages.CodewindStopTimeout, e);
					}

					ViewHelper.refreshCodewindExplorerView(null);
					return Status.OK_STATUS;
					
				}
			};
			job.setPriority(Job.LONG);
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
						Logger.logError("Codewind not installed or has unknown status when trying to bind project: " + project.getName()); //$NON-NLS-1$
						return;
					}
				}
			}
		};
	}
	
	public static void updateCodewind(String version, boolean removeOldVersion, Runnable prompt) {
		try {
			Job job = new Job(Messages.UpdatingCodewindJobLabel) {
				@Override
				protected IStatus run(IProgressMonitor progressMon) {
					SubMonitor mon = SubMonitor.convert(progressMon, 200);
					try {
						// Stop if necessary
						InstallStatus status = InstallUtil.getInstallStatus();
						if (status.hasStartedVersions()) {
							mon.setTaskName(Messages.StoppingCodewindJobLabel);
							try {
								ProcessResult result = InstallUtil.stopCodewind(true, mon.split(80));
								if (mon.isCanceled()) {
									return Status.CANCEL_STATUS;
								}
								if (result.getExitValue() != 0) {
									return getErrorStatus(result, Messages.CodewindStopFail);
								}
							} catch (TimeoutException e) {
								return getErrorStatus(Messages.CodewindStopTimeout, e);
							}
						}
						mon.setWorkRemaining(120);
						
						// Remove if requested
						if (removeOldVersion) {
							mon.setTaskName(Messages.UninstallingCodewindTask);
							try {
								ProcessResult result = InstallUtil.removeCodewind(null, mon.split(20));
								if (mon.isCanceled()) {
									return Status.CANCEL_STATUS;
								}
								if (result.getExitValue() != 0) {
									return getErrorStatus(result, Messages.CodewindUninstallFail);
								}
							} catch (TimeoutException e) {
								return getErrorStatus(Messages.CodewindUninstallTimeout, e);
							}
						}
						mon.setWorkRemaining(100);
						
						// Install
						mon.setTaskName(Messages.InstallingCodewindTask);
						try {
							ProcessResult result = InstallUtil.installCodewind(version, mon.split(95));
							if (mon.isCanceled()) {
								removeCodewind(version);
								return Status.CANCEL_STATUS;
							}
							if (result.getExitValue() != 0) {
								return getErrorStatus(result, Messages.CodewindInstallFail);
							}
						} catch (TimeoutException e) {
							return getErrorStatus(Messages.CodewindInstallTimeout, e);
						}
						
						// Start
						mon.setTaskName(Messages.StartingCodewindJobLabel);
						try {
							ProcessResult result = InstallUtil.startCodewind(version, mon.split(5));
							if (mon.isCanceled()) {
								removeCodewind(version);
								return Status.CANCEL_STATUS;
							}
							if (result.getExitValue() != 0) {
								return getErrorStatus(result, Messages.CodewindStartFail);
							}
						} catch (TimeoutException e) {
							return getErrorStatus(Messages.CodewindStartTimeout, e);
						}
						
						// Display prompt
						if (prompt != null) {
							Display.getDefault().asyncExec(prompt);
						}
					} catch (Exception e) {
						return getErrorStatus(Messages.CodewindUpdateError, e);
					}
					
					ViewHelper.refreshCodewindExplorerView(null);
					return Status.OK_STATUS;
				}
			};
			job.setPriority(Job.LONG);
			job.schedule();
		} catch (Exception e) {
			Logger.logError("An error occurred updating Codewind images: ", e); //$NON-NLS-1$
		}
	}
	
	public static void removeCodewind(String version) {

		try {
			Job job = new Job(Messages.RemovingCodewindJobLabel) {
			    @Override
				protected IStatus run(IProgressMonitor progressMon) {
			    	SubMonitor mon = SubMonitor.convert(progressMon, 100);
					try {
						InstallStatus status = InstallUtil.getInstallStatus();
						if (status.isStarted()) {
							// Stop Codewind before uninstalling
							// All containers must be stopped or uninstall won't work
							mon.setTaskName(Messages.StoppingCodewindJobLabel);
							try {
								ProcessResult result = InstallUtil.stopCodewind(true, mon.split(80));
								
								if (mon.isCanceled()) {
									return Status.CANCEL_STATUS;
								}
								
								if (result.getExitValue() != 0) {
									return getErrorStatus(result, Messages.CodewindStopFail);
								}
							} catch (TimeoutException e) {
								return getErrorStatus(Messages.CodewindStopTimeout, e);
							}
						}
						
						if (mon.isCanceled()) {
							return Status.CANCEL_STATUS;
						}
						
						mon.setTaskName(Messages.UninstallingCodewindTask);
						mon.setWorkRemaining(20);
						ProcessResult result = InstallUtil.removeCodewind(version, mon.split(20));
						
						if (mon.isCanceled()) {
							return Status.CANCEL_STATUS;
						}
						
						if (result.getExitValue() != 0) {
							return getErrorStatus(result, Messages.CodewindUninstallFail);
						}
						
						
					} catch (TimeoutException e) {
						return getErrorStatus(Messages.CodewindUninstallTimeout, e);
					} catch (Exception e) {
						return getErrorStatus(Messages.CodewindUninstallError, e);
					}
					
					ViewHelper.refreshCodewindExplorerView(null);
					return Status.OK_STATUS;
					
				}
			};
			job.setPriority(Job.LONG);
			job.schedule();
		} catch (Exception e) {
			Logger.logError("An error occurred removing Codewind images: ", e); //$NON-NLS-1$
		}
		
	}
	
	private static IStatus getErrorStatus(ProcessResult result, String msg) {
		Logger.logError("Installer failed with return code: " + result.getExitValue() + ", output: " + result.getOutput() + ", error: " + result.getError()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		String errorText = result.getError() != null && !result.getError().isEmpty() ? result.getError() : result.getOutput();
		if (errorText == null || errorText.trim().isEmpty()) {
			errorText = NLS.bind(Messages.InstallCodewindFailNoMessage, result.getExitValue());
		}
		return getErrorStatus(NLS.bind(msg, errorText), null);
	}
	
	private static IStatus getErrorStatus(String msg, Throwable t) {
		Logger.logError(msg, t);
		ViewHelper.refreshCodewindExplorerView(null);
		return new Status(IStatus.ERROR, CodewindUIPlugin.PLUGIN_ID, msg, t);
	}
	
	private static boolean getStopAll(IProgressMonitor monitor) {
		if (!ENABLE_STOP_APPS_OPTION) {
			return true;
		}
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
