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

package org.eclipse.codewind.ui.internal.actions;

import java.net.URL;

import org.eclipse.codewind.core.internal.CodewindApplication;
import org.eclipse.codewind.core.internal.CoreUtil;
import org.eclipse.codewind.core.internal.Logger;
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
 * Action to open the Tekton dashboard in a browser.
 */
public class OpenTektonDashboardAction extends SelectionProviderAction {

    protected CodewindApplication app;
    
	public OpenTektonDashboardAction(ISelectionProvider selectionProvider) {
        super(selectionProvider, Messages.ActionOpenTektonDashboard);
        selectionChanged(getStructuredSelection());
    }

    @Override
    public void selectionChanged(IStructuredSelection sel) {
        if (sel.size() == 1) {
            Object obj = sel.getFirstElement();
            if (obj instanceof CodewindApplication) {
            	app = (CodewindApplication)obj;
            	setEnabled(app.isAvailable());
            	return;
            }
        }
        setEnabled(false);
    }

    @Override
    public void run() {
        if (app == null) {
        	// should not be possible
        	Logger.logError("OpenTektonDashboardAction ran but no application was selected"); //$NON-NLS-1$
			return;
		}

        URL url = app.connection.getTektonDashboardURL();
		if (url == null) {
			Logger.logError("OpenTektonDashboardAction ran but could not get the url"); //$NON-NLS-1$
			return;
		}
		
		try {
			IWebBrowser browser = null;
			IWorkbenchBrowserSupport browserSupport = PlatformUI.getWorkbench().getBrowserSupport();
			
			if (CoreUtil.isWindows()) {
				// Use the external browser if available since the dashboard does not display 
				// well in the internal browser on Windows
				browser = browserSupport.getExternalBrowser();
			}
			
			if (browser == null) {
				// Use the app's ID as the browser ID so that if this is called again on the same app,
				// the browser will be re-used
				browser = browserSupport
						.createBrowser(IWorkbenchBrowserSupport.NAVIGATION_BAR | IWorkbenchBrowserSupport.LOCATION_BAR,
								app.projectID + "_tektonDashboard", app.name, NLS.bind(Messages.BrowserTooltipTektonDashboard, app.name));
			}

			browser.openURL(url);
		} catch (PartInitException e) {
			Logger.logError("Error opening the Tekton dashboard in browser", e); //$NON-NLS-1$
		}
    }
    
    public boolean showAction() {
    	return app != null && app.connection.getTektonDashboardURL() != null;
    }
}
