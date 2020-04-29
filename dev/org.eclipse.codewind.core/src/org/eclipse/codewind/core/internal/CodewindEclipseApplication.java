/*******************************************************************************
 * Copyright (c) 2018, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.codewind.core.internal;

import java.net.MalformedURLException;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.codewind.core.CodewindCorePlugin;
import org.eclipse.codewind.core.internal.connection.CodewindConnection;
import org.eclipse.codewind.core.internal.console.ProjectLogInfo;
import org.eclipse.codewind.core.internal.console.SocketConsole;
import org.eclipse.codewind.core.internal.constants.ProjectLanguage;
import org.eclipse.codewind.core.internal.constants.ProjectType;
import org.eclipse.codewind.core.internal.launch.CodewindLaunchConfigDelegate;
import org.eclipse.codewind.core.internal.messages.Messages;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;

/**
 * Eclipse specific code for a Codewind application.  Anything related to Eclipse 
 * (launches, consoles, connecting the debugger, etc.) should go here and not in the
 * CodewindApplication class.
 */
public class CodewindEclipseApplication extends CodewindApplication {
	
	// Validation marker
	public static final String MARKER_TYPE = CodewindCorePlugin.PLUGIN_ID + ".validationMarker";
	public static final String CONNECTION_URL = "connectionUrl";
	public static final String PROJECT_ID = "projectId";
	public static final String QUICK_FIX_ID = "quickFixId";
	public static final String QUICK_FIX_DESCRIPTION = "quickFixDescription";
	
	// in seconds
	public static final int DEFAULT_DEBUG_CONNECT_TIMEOUT = 3;
	
	// New consoles
	private Set<SocketConsole> activeConsoles = new HashSet<SocketConsole>();
	
	// Debug launch, null if not debugging
	private ILaunch launch = null;
	private boolean debugPortNotify = false;

	CodewindEclipseApplication(CodewindConnection connection, String id, String name,
			ProjectType projectType, ProjectLanguage language, IPath localPath)
					throws MalformedURLException {
		super(connection, id, name, projectType, language, localPath);
	}
	
	public synchronized void addConsole(SocketConsole console) {
		activeConsoles.add(console);
	}
	
	public synchronized SocketConsole getConsole(ProjectLogInfo logInfo) {
		for (SocketConsole console : activeConsoles) {
			if (console.logInfo.isThisLogInfo(logInfo)) {
				return console;
			}
		}
		return null;
	}
	
	public synchronized void removeConsole(SocketConsole console) {
		if (console != null) {
			activeConsoles.remove(console);
		}
	}
	
	public synchronized void setLaunch(ILaunch launch) {
		this.launch = launch;
	}
	
	public synchronized ILaunch getLaunch() {
		return launch;
	}
	
	public synchronized void setDebugPortNotify(boolean value) {
		debugPortNotify = value;
	}
	
	@Override
	public void clearDebugger() {
		if (launch != null) {
			IDebugTarget debugTarget = launch.getDebugTarget();
			if (debugTarget != null && !debugTarget.isDisconnected()) {
				try {
					debugTarget.disconnect();
				} catch (DebugException e) {
					Logger.logError("An error occurred while disconnecting the debugger for project: " + name, e); //$NON-NLS-1$
				}
			}
		}
		setLaunch(null);
	}

	@Override
	public void connectDebugger() {
		if (!canInitiateDebugSession()) {
			if (debugPortNotify) {
				debugPortNotify = false;
				CoreUtil.openDialog(CoreUtil.DialogType.INFO, NLS.bind(Messages.DebugPortNotifyTitle, name), NLS.bind(Messages.DebugPortNotifyMsg, new String[] {name, String.valueOf(getDebugConnectPort())}));
			}
			return;
		}
		final CodewindEclipseApplication app = this;
		Job job = new Job(Messages.ConnectDebugJob) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					if (app.projectLanguage.isJava()) {
						ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
				        ILaunchConfigurationType launchConfigurationType = launchManager.getLaunchConfigurationType(CodewindLaunchConfigDelegate.LAUNCH_CONFIG_ID);
				        ILaunchConfigurationWorkingCopy workingCopy = launchConfigurationType.newInstance((IContainer) null, app.name);
				        CodewindLaunchConfigDelegate.setConfigAttributes(workingCopy, app);
				        ILaunchConfiguration launchConfig = workingCopy.doSave();
			            ILaunch launch = launchConfig.launch(ILaunchManager.DEBUG_MODE, monitor);
			            app.setLaunch(launch);
			            return Status.OK_STATUS;
					} else {
						IDebugLauncher launcher = CodewindCorePlugin.getDebugLauncher(app.projectLanguage.getId());
						if (launcher != null) {
							return launcher.launchDebugger(app);
						}
					}
				} catch (Exception e) {
					Logger.logError("An error occurred while trying to launch the debugger for project: " + app.name, e); //$NON-NLS-1$
					return new Status(IStatus.ERROR, CodewindCorePlugin.PLUGIN_ID,
							NLS.bind(Messages.DebugLaunchError, app.name), e);
				}
				return Status.CANCEL_STATUS;
			}
		};
		job.setPriority(Job.LONG);
		job.schedule();
	}

	@Override
	public void reconnectDebugger() {
		// First check if there is a launch and it is registered
		if (launch != null) {
			ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
			for (ILaunch launchItem : launchManager.getLaunches()) {
				if (launch.equals(launchItem)) {
					// Check if the debugger is still attached (for Liberty, a small change to the app does not require a server restart)
					IDebugTarget debugTarget = launch.getDebugTarget();
					if (debugTarget == null || debugTarget.isDisconnected()) {
						// Clean up
						clearDebugger();
						// Reconnect the debugger
						connectDebugger();
					}
				}
			}
		}
	}
	
	public boolean canAttachDebugger() {
		if (projectLanguage.isJava()) {
			IDebugTarget debugTarget = getDebugTarget();
			return (debugTarget == null || debugTarget.isDisconnected());
		} else {
			IDebugLauncher launcher = CodewindCorePlugin.getDebugLauncher(projectLanguage.getId());
			if (launcher != null) {
				return launcher.canAttachDebugger(this);
			}
		}
		return false;
		
	}
	
	public void attachDebugger() {
		// Check to see if already attached
		if (launch != null) {
			IDebugTarget debugTarget = launch.getDebugTarget();
			if (debugTarget != null && !debugTarget.isDisconnected()) {
				// Already attached
				return;
			}
		}
		clearDebugger();
		connectDebugger();
	}
	
	public IDebugTarget getDebugTarget() {
		if (launch != null) {
			ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
			for (ILaunch launchItem : launchManager.getLaunches()) {
				if (launch.equals(launchItem)) {
					return launch.getDebugTarget();
				}
			}
		}
		return null;
	}
	
	@Override
	public synchronized void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		if (!enabled) {
			// Clean up the launch
			clearDebugger();
			
			// Clean up the consoles
			if (!activeConsoles.isEmpty()) {
				IConsoleManager consoleManager = ConsolePlugin.getDefault().getConsoleManager();
				consoleManager.removeConsoles(activeConsoles.toArray(new IConsole[activeConsoles.size()]));
				activeConsoles.clear();
			}
		}
	}

	@Override
	public void dispose() {
		setEnabled(false);
		
		// Start project delete if requested
		if (getDeleteContents()) {
			deleteProject();
		}
		
		super.dispose();
	}
	
	@Override
	public void resetValidation() {
		// Delete all Codewind markers for a project
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(name);
		if (project != null && project.isAccessible()) {
			try {
				project.deleteMarkers(MARKER_TYPE, true, IResource.DEPTH_INFINITE);
			} catch (CoreException e) {
				Logger.logError("Failed to delete existing markers for the " + name + " project.", e); //$NON-NLS-1$
			}
		}
	}
	
	@Override
	public void validationError(String filePath, String message, String quickFixId, String quickFixDescription) {
		validationEvent(IMarker.SEVERITY_ERROR, filePath, message, quickFixId, quickFixDescription);
	}
	
	@Override
	public void validationWarning(String filePath, String message, String quickFixId, String quickFixDescription) {
		validationEvent(IMarker.SEVERITY_WARNING, filePath, message, quickFixId, quickFixDescription);
	}
	
    private void validationEvent(int severity, String filePath, String message, String quickFixId, String quickFixDescription) {
        // Create a marker and quick fix (if available) on the specific file if there is one or the project if not.
    	try {
        	IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(name);
        	if (project != null && project.isAccessible()) {
	        	IResource resource = project;
	        	if (filePath != null && !filePath.isEmpty()) {
		        	IPath path = new Path(filePath);
		        	if (filePath.startsWith(project.getName())) {
		        		path = path.removeFirstSegments(1);
		        	}
		        	IFile file = project.getFile(path);
		        	if (file != null && file.exists()) {
		        		resource = file;
		        	}
	        	}
	            final IMarker marker = resource.createMarker(MARKER_TYPE);
	            marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
	            marker.setAttribute(IMarker.MESSAGE, message);
//	            if (quickFixId != null && !quickFixId.isEmpty()) {
//	            	marker.setAttribute(CONNECTION_URL, connection.baseUrl.toString());
//	            	marker.setAttribute(PROJECT_ID, projectID);
//	            	marker.setAttribute(QUICK_FIX_ID, quickFixId);
//	            	marker.setAttribute(QUICK_FIX_DESCRIPTION, quickFixDescription);
//	            }
        	}
        } catch (CoreException e) {
            Logger.logError("Failed to create a marker for the " + name + " application: " + message, e); //$NON-NLS-1$
        }
    }

	public boolean canInitiateDebugSession() {
		// Only supported for certain languages
		if (projectLanguage.isJava() || projectLanguage.isJavaScript()) {
			// And only if the project supports it
			return supportsDebug();
		}
		return false;
	}

	@Override
	public void buildComplete() {
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(name);
    	if (project != null && project.isAccessible()) {
    		Job job = new Job(NLS.bind(Messages.RefreshResourceJobLabel, project.getName())) {
				@Override
				protected IStatus run(IProgressMonitor monitor) {
					try {
						project.refreshLocal(IResource.DEPTH_INFINITE, monitor);
			            return Status.OK_STATUS;
					} catch (Exception e) {
						Logger.logError("An error occurred while refreshing the resource: " + project.getLocation()); //$NON-NLS-1$
						return new Status(IStatus.ERROR, CodewindCorePlugin.PLUGIN_ID,
								NLS.bind(Messages.RefreshResourceError, project.getLocation()), e);
					}
				}
			};
			job.setPriority(Job.LONG);
			job.schedule();
    	}
	}

	private void deleteProject() {
		Job job = new Job(NLS.bind(Messages.DeleteProjectJobLabel, name)) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					IProject project = CoreUtil.getEclipseProject(CodewindEclipseApplication.this);
					if (project != null) {
						project.delete(true, true, monitor);
					} else if (fullLocalPath.toFile().exists()) {
						FileUtil.deleteDirectory(fullLocalPath.toOSString(), true);
					} else {
						Logger.log("No project contents were found to delete for application: " + name);
					}
				} catch (Exception e) {
					Logger.logError("Error deleting project contents: " + name, e); //$NON-NLS-1$
					return new Status(IStatus.ERROR, CodewindCorePlugin.PLUGIN_ID, NLS.bind(Messages.DeleteProjectError, name), e);
				}
				if (monitor.isCanceled()) {
					return Status.CANCEL_STATUS;
				}
				return Status.OK_STATUS;
			}
		};
		job.schedule();
	}
    
}
