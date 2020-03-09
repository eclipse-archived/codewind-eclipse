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

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

import org.eclipse.codewind.core.CodewindCorePlugin;
import org.eclipse.codewind.core.internal.connection.CodewindConnection;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Link;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;

/**
 * General utils that don't belong anywhere else
 */
public class CoreUtil {
	
	// Provide a way for users to override the path used for running commands
	private static final String ENV_PATH_PROPERTY = "org.eclipse.codewind.envPath";
	
	public enum DialogType {
		ERROR(MessageDialog.ERROR),
		WARN(MessageDialog.WARNING),
		INFO(MessageDialog.INFORMATION);
		
		private int value;

		private DialogType(int value) {
			this.value = value;
		}
		
		public int getValue() {
			return value;
		}
	};

	/**
	 * Open a dialog on top of the current active window. Can be called off the UI thread.
	 */
	public static void openDialog(boolean isError, String title, String msg) {
		openDialog(isError ? DialogType.ERROR : DialogType.INFO, title, msg);
	}
	
	public static void openDialog(DialogType type, String title, String msg) {
		Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {
				MessageDialog.open(type.getValue(), Display.getDefault().getActiveShell(), title, msg, 0);
			}
		});
	}

	public static void openDialogWithLink(DialogType type, String title, String msg, String linkLabel, String linkUrl) {
		Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {
				MessageDialog dialog = new MessageDialog(Display.getDefault().getActiveShell(), title, null, msg, type.getValue(), 0, IDialogConstants.OK_LABEL) {
					@Override
					protected Control createCustomArea(Composite parent) {
						Link link = new Link(parent, SWT.WRAP);
						link.setText("<a>" + linkLabel + "</a>"); //$NON-NLS-1$ //$NON-NLS-2$
						link.addSelectionListener(new SelectionAdapter() {
							@Override
							public void widgetSelected(SelectionEvent event) {
								try {
									IWorkbenchBrowserSupport browserSupport = PlatformUI.getWorkbench().getBrowserSupport();
									IWebBrowser browser = browserSupport.getExternalBrowser();
									URL url = new URL(linkUrl);
									browser.openURL(url);
								} catch (Exception e) {
									Logger.logError("An error occurred trying to open an external browser at: " + link, e); //$NON-NLS-1$
								}
							}
						});
						return link;
					}
				};
				dialog.open();
			}
		});
	}

	public static boolean openConfirmDialog(String title, String msg) {
		final boolean[] result = new boolean[1];
		Display.getDefault().syncExec(new Runnable() {
			@Override
			public void run() {
				result[0] = MessageDialog.open(MessageDialog.CONFIRM, Display.getDefault().getActiveShell(), title, msg, 0);
			}
		});
		return result[0];
	}


	public static String readAllFromStream(InputStream stream) {
		Scanner s = new Scanner(stream);
		// end-of-stream
		s.useDelimiter("\\A"); //$NON-NLS-1$
		String result = s.hasNext() ? s.next() : ""; //$NON-NLS-1$
		s.close();
		return result;
	}

	public static boolean isWindows() {
		String os = System.getProperty("os.name"); //$NON-NLS-1$
		return os != null && os.toLowerCase().startsWith("windows"); //$NON-NLS-1$
	}
	
	public static String getHostPath(String containerPath) {
		String hostPath = containerPath;
		if (isWindows() && containerPath.startsWith("/")) { //$NON-NLS-1$
			String device = containerPath.substring(1, 2);
			hostPath = device + ":" + containerPath.substring(2); //$NON-NLS-1$
		}
		return hostPath;
	}
	
	public static String getContainerPath(String hostPath) {
		String containerPath = hostPath;
		if (isWindows() && hostPath.indexOf(':') == 1) { //$NON-NLS-1$
			containerPath = "/" + hostPath.charAt(0) + hostPath.substring(2);
			containerPath = containerPath.replace("\\", "/"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return containerPath;
	}

	/**
	 * Append finish to start, removing the last segment of start if it is equal to the first segment of finish.
	 */
	public static IPath appendPathWithoutDupe(IPath start, String finish) {
		IPath finishPath = new Path(finish);
		if (start.lastSegment().equals(finishPath.segment(0))) {
			start = start.removeLastSegments(1);
		}
		return start.append(finishPath);
	}

	/**
	 * Splits the given Path into strings with a maximum length of the given length.
	 * Splits only across separators, so individual path segments will remain intact.
	 */
	public static List<String> splitPath(IPath path, int length) {
		List<String> result = new ArrayList<>();

		StringBuilder currentString = new StringBuilder().append(IPath.SEPARATOR);
		for (String segment : path.segments()) {
			if (currentString.length() + segment.length() > length) {
				result.add(currentString.toString());
				currentString = new StringBuilder();
			}
			currentString.append(segment).append(IPath.SEPARATOR);
		}

		if (currentString.length() > 0) {
			result.add(currentString.toString());
		}

		return result;
	}

	/**
	 * In: [ "Here", "Is", "Some Input" ]
	 * Separator: ", "
	 * Out: "Here, Is, Some Input"
	 */
	public static String formatString(String[] strArray, String separator) {
		return Arrays.stream(strArray).collect(Collectors.joining(separator));
	}
	
	public static int parsePort(String portStr) {
		try {
			return Integer.parseInt(portStr);
		}
		catch(NumberFormatException e) {
			Logger.logError(String.format("Couldn't parse port from \"%s\"", portStr), e); //$NON-NLS-1$
			return -1;
		}
	}
	
	/**
	 * Update everything in the Codewind explorer view
	 */
	public static void updateAll() {
		IUpdateHandler handler = CodewindCorePlugin.getUpdateHandler();
		if (handler != null) {
			handler.updateAll();
		}
	}
	
	/**
	 * Update the connection and its children in the Codewind explorer view
	 */
	public static void updateConnection(CodewindConnection connection) {
		IUpdateHandler handler = CodewindCorePlugin.getUpdateHandler();
		if (handler != null) {
			handler.updateConnection(connection);
		}
	}
	
	/**
	 * Remove the connection.
	 */
	public static void removeConnection(CodewindConnection conn) {
		IUpdateHandler handler = CodewindCorePlugin.getUpdateHandler();
		if (handler != null) {
			handler.removeConnection(conn);
		}
	}
	
	/**
	 * Update the application in the Codewind explorer view
	 */
	public static void updateApplication(CodewindApplication app) {
		IUpdateHandler handler = CodewindCorePlugin.getUpdateHandler();
		if (handler != null) {
			handler.updateApplication(app);
		}
	}
	
	/**
	 * Remove the application in the Codewind explorer view
	 */
	public static void removeApplication(CodewindApplication app) {
		IUpdateHandler handler = CodewindCorePlugin.getUpdateHandler();
		if (handler != null) {
			handler.removeApplication(app);
		}
	}

    public static String getOSName() {
        return (String)System.getProperty("os.name");
    }
    
    public static boolean isMACOS() {
    	String osName = getOSName();
    	if (osName != null && osName.toLowerCase().contains("mac")) {
    		return true;
    	}
    	return false;
    }
    
    public static String getEnvPath() {
    	String path = (String)System.getProperty(ENV_PATH_PROPERTY);
    	if (path == null || path.trim().isEmpty()) {
    		if (isMACOS()) {
    			// On MAC a full path is required for running commands
    			return "/usr/local/bin/";
    		}
    		return null;
    	}
    	path = path.trim();
    	path = path.replace("\\", "/");
    	if (!path.endsWith("/")) {
    		path = path + "/";
    	}
    	return path;
    }
    
	public static IPath getCodewindDataPath() {
		if (isWindows()) {
			return new Path("C:/codewind-data");
		}
		IPath userHome = getUserHome();
		if (userHome != null) {
			return userHome.append("codewind-data");
		}
		return null;
	}
	
	public static IPath getUserHome() {
		String homeDir = System.getProperty("user.home");
		if (homeDir != null && !homeDir.isEmpty()) {
			return new Path(homeDir);
		}
		// This should not happen
		Logger.logError("The user.home system property was null or empty.");
		return null;
	}
	
	public static IProject getEclipseProject(CodewindApplication app) {
		IContainer[] containers = ResourcesPlugin.getWorkspace().getRoot().findContainersForLocationURI(new File(app.fullLocalPath.toOSString()).toURI());
		for (IContainer container : containers) {
			if (container instanceof IProject && ((IProject)container).isAccessible()) {
				return (IProject)container;
			}
		}
		return null;
	}
    
}
