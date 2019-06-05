/*******************************************************************************
 * Copyright (c) 2018, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.codewind.ui.internal.actions;

import java.net.URL;

import org.eclipse.codewind.core.internal.Logger;
import org.eclipse.codewind.core.internal.CoreUtil;
import org.eclipse.codewind.core.internal.CodewindApplication;
import org.eclipse.codewind.core.internal.constants.AppState;
import org.eclipse.codewind.core.internal.constants.CoreConstants;
import org.eclipse.codewind.ui.internal.messages.Messages;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.SelectionProviderAction;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;

/**
 * Action to open the application monitor in a browser.
 */
public class OpenAppMonitorAction extends SelectionProviderAction {

    protected CodewindApplication app;
    
	public OpenAppMonitorAction(ISelectionProvider selectionProvider) {
        super(selectionProvider, Messages.ActionOpenAppMonitor);
        selectionChanged(getStructuredSelection());
    }


    @Override
    public void selectionChanged(IStructuredSelection sel) {
        if (sel.size() == 1) {
            Object obj = sel.getFirstElement();
            if (obj instanceof CodewindApplication) {
            	app = (CodewindApplication)obj;
            	setEnabled(app.isAvailable() && app.getAppState() == AppState.STARTED);
            	return;
            }
        }
        setEnabled(false);
    }

    @Override
    public void run() {
        if (app == null) {
        	// should not be possible
        	Logger.logError("OpenAppMonitorAction ran but no application was selected"); //$NON-NLS-1$
			return;
		}

        if (!app.isRunning()) {
        	CoreUtil.openDialog(true, Messages.OpenAppAction_CantOpenNotRunningAppTitle,
        			Messages.OpenAppAction_CantOpenNotRunningAppMsg);
        	return;
        }

        URL url = app.getMetricsUrl();
		if (url == null) {
			// this should not happen
			Logger.logError("OpenAppMonitorAction ran but could not construct the url"); //$NON-NLS-1$
			return;
		}

        // Use the app's ID as the browser ID so that if this is called again on the same app,
        // the browser will be re-used

		try {
			IWorkbenchBrowserSupport browserSupport = PlatformUI.getWorkbench().getBrowserSupport();
			IWebBrowser browser = browserSupport
					.createBrowser(IWorkbenchBrowserSupport.NAVIGATION_BAR | IWorkbenchBrowserSupport.LOCATION_BAR,
							app.projectID + "_" + CoreConstants.VIEW_MONITOR, app.name, NLS.bind(Messages.BrowserTooltipAppMonitor, app.name));

	        browser.openURL(url);
		} catch (PartInitException e) {
			Logger.logError("Error opening the app monitor in browser", e); //$NON-NLS-1$
		}
    }
    
    public boolean showAction() {
    	return app != null && app.getMetricsAvailable() && app.projectType.getMetricsRoot() != null;
    }
}
