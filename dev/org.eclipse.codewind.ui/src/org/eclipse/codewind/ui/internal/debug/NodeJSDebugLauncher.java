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

package org.eclipse.codewind.ui.internal.debug;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.codewind.core.CodewindCorePlugin;
import org.eclipse.codewind.core.internal.CodewindApplication;
import org.eclipse.codewind.core.internal.CodewindEclipseApplication;
import org.eclipse.codewind.core.internal.HttpUtil;
import org.eclipse.codewind.core.internal.HttpUtil.HttpResult;
import org.eclipse.codewind.core.internal.IDebugLauncher;
import org.eclipse.codewind.core.internal.KubeUtil;
import org.eclipse.codewind.core.internal.KubeUtil.PortForwardInfo;
import org.eclipse.codewind.core.internal.Logger;
import org.eclipse.codewind.core.internal.PlatformUtil;
import org.eclipse.codewind.core.internal.RemoteEclipseApplication;
import org.eclipse.codewind.ui.CodewindUIPlugin;
import org.eclipse.codewind.ui.internal.messages.Messages;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.RuntimeProcess;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.internal.browser.BrowserManager;
import org.eclipse.ui.internal.browser.IBrowserDescriptor;
import org.json.JSONArray;
import org.json.JSONObject;

@SuppressWarnings("restriction") //$NON-NLS-1$
public class NodeJSDebugLauncher implements IDebugLauncher {
	
	private static final String NODEJS_LAUNCH_CONFIG_ID = "org.eclipse.wildwebdeveloper.launchConfiguration.nodeDebugAttach";
	private static final String ADDRESS_ATTR = "address";
	private static final String PORT_ATTR = "port";
	private static final String LOCAL_ROOT_ATTR = "localRoot";
	private static final String REMOTE_ROOT_ATTR = "remoteRoot";
	
	
	private static final String DEBUG_INFO = "/json/list";
	private static final String DEVTOOLS_URL_FIELD = "devtoolsFrontendUrl";
	
	private Optional<ILaunchConfigurationType> launchConfigType = null;
	
	public IStatus launchDebugger(CodewindEclipseApplication app, IProgressMonitor monitor) {
		PortForwardInfo pfInfo = null;
		if (app instanceof RemoteEclipseApplication && app.getDebugConnectPort() == -1) {
			int port = PlatformUtil.findFreePort();
			if (port <= 0) {
				String msg = "Could not find a free port for port forwarding the debug port for project: " + app.name; //$NON-NLS-1$
				Logger.logError(msg);
				return new Status(IStatus.ERROR, CodewindUIPlugin.PLUGIN_ID, NLS.bind(Messages.NodeJSDebugPortForwardError, app.name), new IOException(msg));
			}
			try {
				if (getLaunchConfigType().isPresent()) {
					pfInfo = KubeUtil.startPortForward(app, port, app.getContainerDebugPort());
				} else {
					pfInfo = KubeUtil.launchPortForward(app, port, app.getContainerDebugPort());
				}
				((RemoteEclipseApplication)app).setDebugPFInfo(pfInfo);
			} catch (Exception e) {
				return new Status(IStatus.ERROR, CodewindUIPlugin.PLUGIN_ID, NLS.bind(Messages.NodeJSDebugPortForwardError, app.name), e);
			}
		}
		
		IStatus status;
		if (getLaunchConfigType().isPresent()) {
			status = launchInternalDebugSession(app, pfInfo, monitor);
		} else {
			status = launchBrowserDebugSession(app);
		}

		if (status == Status.CANCEL_STATUS && pfInfo != null) {
			// Terminate the port forward if the user canceled
			pfInfo.terminateAndRemove();
		}
		return status;
	}
	
	private IStatus launchInternalDebugSession(CodewindEclipseApplication app, PortForwardInfo pfInfo, IProgressMonitor monitor) {
		try {
			ILaunchConfigurationWorkingCopy workingCopy = getLaunchConfigType().get().newInstance((IContainer) null, app.name);
			workingCopy.setAttribute(ADDRESS_ATTR, app.getDebugConnectHost());
			workingCopy.setAttribute(PORT_ATTR, app.getDebugConnectPort());
			workingCopy.setAttribute(LOCAL_ROOT_ATTR, app.fullLocalPath.toOSString());
			workingCopy.setAttribute(REMOTE_ROOT_ATTR, "/app");
			ILaunchConfiguration launchConfig = workingCopy.doSave();
			ILaunch launch = launchConfig.launch(ILaunchManager.DEBUG_MODE, monitor);
			if (pfInfo != null && pfInfo.process != null) {
				Map<String, String> attributes = new HashMap<String, String>();
				attributes.put(IProcess.ATTR_PROCESS_TYPE, "codewind.utility");
				String title = NLS.bind(Messages.PortForwardTitle, pfInfo.localPort + ":" + app.getContainerDebugPort());
				launch.addProcess(new RuntimeProcess(launch, pfInfo.process, title, attributes));
			}
			app.setLaunch(launch);
		} catch (CoreException e) {
			return e.getStatus();
		}
		return Status.OK_STATUS;
	}

	private IStatus launchBrowserDebugSession(CodewindApplication app) {
		String urlString = null;
		Exception e = null;
		try {
			urlString = getDebugURL(app);
		} catch (Exception e1) {
			e = e1;
		}
		if (urlString == null) {
			Logger.logError("Failed to get the debug URL for the " + app.name + " application.", e); //$NON-NLS-1$ //$NON-NLS-2$
			return new Status(IStatus.ERROR, CodewindUIPlugin.PLUGIN_ID, NLS.bind(Messages.NodeJSDebugURLError, app.name), e);
		}

		IStatus status = openNodeJSDebugger(urlString);
		return status;
	}
	
	private Optional<ILaunchConfigurationType> getLaunchConfigType() {
		if (launchConfigType == null) {
			ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
	        launchConfigType = Optional.ofNullable(launchManager.getLaunchConfigurationType(NODEJS_LAUNCH_CONFIG_ID));
		}
		return launchConfigType;
	}
	
	@Override
	public boolean canAttachDebugger(CodewindEclipseApplication app) {
		if (getLaunchConfigType().isPresent()) {
			ILaunch launch = app.getLaunch();
			IDebugTarget debugTarget = launch == null ? null : launch.getDebugTarget();
			return (debugTarget == null || debugTarget.isDisconnected());
		}
		
		String host = app.getDebugConnectHost();
		int debugPort = app.getDebugConnectPort();
		
		if (app instanceof RemoteEclipseApplication && debugPort == -1) {
			// If the port forward is not running then the debugger cannot already be attached
			return true;
		}
		
		// If a debugger is already attached then the devtools url field will not be included in the result
		try {
			URI uri = new URI("http", null, host, debugPort, DEBUG_INFO, null, null); //$NON-NLS-1$
			HttpResult result = HttpUtil.get(uri);
			if (result.isGoodResponse) {
				String response = result.response;
				JSONArray array = new JSONArray(response);
				JSONObject info = array.getJSONObject(0);
				if (info.has(DEVTOOLS_URL_FIELD)) {
					String url = info.getString(DEVTOOLS_URL_FIELD);
					if (url != null && !url.isEmpty()) {
						return true;
					}
				}
			}
		} catch (Exception e) {
			Logger.log("Failed to retrieve the debug information for the " + app.name + " app: " + e.getMessage()); //$NON-NLS-1$  //$NON-NLS-2$
		}
		
		return false;
	}

	private String getDebugURL(CodewindApplication app) throws Exception {
		IPreferenceStore prefs = CodewindCorePlugin.getDefault().getPreferenceStore();
		int debugTimeout = prefs.getInt(CodewindCorePlugin.DEBUG_CONNECT_TIMEOUT_PREFSKEY);
		
		String host = app.getDebugConnectHost();
		int debugPort = app.getDebugConnectPort();
		
		URI uri = new URI("http", null, host, debugPort, DEBUG_INFO, null, null); //$NON-NLS-1$
		
		Exception e = null;
		HttpResult result = null;
		
		for (int i = 0; i <= debugTimeout; i++) {
			try {
				result = HttpUtil.get(uri);
				if (result.isGoodResponse) {
					String response = result.response;
					JSONArray array = new JSONArray(response);
					JSONObject info = array.getJSONObject(0);
					String url = info.getString(DEVTOOLS_URL_FIELD);
					
					// Replace the host and port
					int start = url.indexOf("ws=") + 3; //$NON-NLS-1$
					int end = url.indexOf("/", start); //$NON-NLS-1$
					String subString = url.substring(start, end);
					url = url.replace(subString, host + ":" + Integer.toString(debugPort));
					return url;
				}
			} catch (Exception e1) {
				e = e1;
			}
			
			if (i <= debugTimeout) {
				try {
					Thread.sleep(1000);
				} catch (Exception e1) {
					// Ignore
				}
			}
		}
		
		if (result != null && !result.isGoodResponse) {
		    Logger.logError("Error getting debug information for the " + app.name + " application. Error code: " + result.responseCode + ", error: " + result.error + "."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		} else {
			Logger.logError("An exception occurred trying to retrieve the debug information for the " + app.name + " application.", e); //$NON-NLS-1$ //$NON-NLS-2$
		}
		
		if (e != null) {
		    throw e;
		}
		return null;
	}
	
	private IStatus openNodeJSDebugger(final String chromeDevToolsUrl) {
		IPreferenceStore prefs = CodewindCorePlugin.getDefault().getPreferenceStore();
		String browserName = prefs.getString(CodewindCorePlugin.NODEJS_DEBUG_BROWSER_PREFSKEY);
		if (browserName != null){
			Logger.log("Using the " + browserName + " browser from the preferences.");  //$NON-NLS-1$ //$NON-NLS-2$
			// If the previously saved browser is not valid, so load the message dialog again
			if (!foundValidBrowser(browserName)){
				browserName = null;
			}					
		}
		
		if (browserName == null) {
			final AtomicInteger returnCode =new AtomicInteger(0);
			final String[] result = new String[1];
			Display.getDefault().syncExec(new Runnable() {
				@Override
				public void run() {
					WebBrowserSelectionDialog browserSelection = new WebBrowserSelectionDialog(Display.getDefault().getActiveShell(), 
							Messages.BrowserSelectionTitle, 
							null, 
							Messages.BrowserSelectionDescription, 
							MessageDialog.CONFIRM, 
							new String[] { IDialogConstants.OK_LABEL, IDialogConstants.CANCEL_LABEL }, 
							0);
					
					browserSelection.create();
					browserSelection.open();
					returnCode.set(browserSelection.getReturnCode());
					if (returnCode.get() == Window.OK) {
						result[0] = browserSelection.getBrowserName();
						boolean isNodejsDefaultBrowserSet = browserSelection.isNodejsDefaultBrowserSet();
						if (isNodejsDefaultBrowserSet) {
							if (browserSelection.getBrowserName() != null) {
								IPreferenceStore prefs = CodewindCorePlugin.getDefault().getPreferenceStore();
								prefs.setValue(CodewindCorePlugin.NODEJS_DEBUG_BROWSER_PREFSKEY, browserSelection.getBrowserName());
					        }
						}
					}
				}
			});
			
			if (returnCode.get() == Window.OK){
				browserName = result[0];
			} else {
				// If it is cancel, then do not continue
				return Status.CANCEL_STATUS;
			}
		}
		return openBrowserDialog(chromeDevToolsUrl, browserName);
	}
	
	private IStatus openBrowserDialog(final String chromeDevToolsUrl, final String browserName ) {
		final AtomicInteger returnCode =new AtomicInteger(0);
		Display.getDefault().syncExec(new Runnable() {
			@Override
			public void run() {
				NodeJsBrowserDialog nodeDialog = new NodeJsBrowserDialog(Display.getDefault().getActiveShell(), 
						Messages.NodeJSOpenBrowserTitle, 
						null, 
						Messages.NodeJSOpenBrowserDesc, 
						MessageDialog.CONFIRM, 
						new String[] { IDialogConstants.OK_LABEL}, 
						0, chromeDevToolsUrl, browserName);
				nodeDialog.open();
				returnCode.set(nodeDialog.getReturnCode());
			}
		});
		
		if (returnCode.get() != Window.OK){
			// Cancelled
			return Status.CANCEL_STATUS;
		}		

		return Status.OK_STATUS;
	}
	
	private boolean foundValidBrowser(String browserName){
		BrowserManager bm = BrowserManager.getInstance();
        if (bm != null){
	        List<IBrowserDescriptor> browserList = bm.getWebBrowsers();
	        if (browserList != null){
		        
		        int len = browserList.size();
		        
		        for (int i=0;i<len;i++){
		        	IBrowserDescriptor tempBrowser = browserList.get(i);
		        	if (tempBrowser != null && tempBrowser.getLocation() != null && 
		        			!tempBrowser.getLocation().trim().equals("")){ //$NON-NLS-1$
		        		// The location is not empty
		        		String name = tempBrowser.getName();
		        		if (name != null && name.equalsIgnoreCase(browserName)){
		        			return true;
		        		}
		        	}
		        }
	        }
        }
        return false;
	}

}
