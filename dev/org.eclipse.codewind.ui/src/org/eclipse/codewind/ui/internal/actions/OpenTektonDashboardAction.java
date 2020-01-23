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

package org.eclipse.codewind.ui.internal.actions;

import java.net.URL;

import org.eclipse.codewind.core.internal.CoreUtil;
import org.eclipse.codewind.core.internal.Logger;
import org.eclipse.codewind.core.internal.connection.ConnectionEnv.TektonDashboard;
import org.eclipse.codewind.core.internal.connection.RemoteConnection;
import org.eclipse.codewind.ui.internal.messages.Messages;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.SelectionProviderAction;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;

/**
 * Action to open the Tekton dashboard in a browser.
 */
public class OpenTektonDashboardAction extends SelectionProviderAction {

    protected RemoteConnection conn;
    
	public OpenTektonDashboardAction(ISelectionProvider selectionProvider) {
        super(selectionProvider, Messages.ActionOpenTektonDashboard);
        selectionChanged(getStructuredSelection());
    }

    @Override
    public void selectionChanged(IStructuredSelection sel) {
        if (sel.size() == 1) {
            Object obj = sel.getFirstElement();
            if (obj instanceof RemoteConnection) {
            	conn = (RemoteConnection)obj;
            	setEnabled(conn.isConnected());
            	return;
            }
        }
        setEnabled(false);
    }

    @Override
    public void run() {
        if (conn == null) {
        	// should not be possible
        	Logger.logError("OpenTektonDashboardAction ran but no remote connection was selected"); //$NON-NLS-1$
			return;
		}
        
        TektonDashboard tekton = conn.getTektonDashboard();
        if (tekton == null) {
        	// Should not happen since the action should not show if there is no dashboard
        	Logger.logError("OpenTektonDashboardAction ran but there is no tekton dashboard in the environment"); //$NON-NLS-1$
        	return;
        }
        
        if (!tekton.hasTektonDashboard()) {
        	Logger.logError("Tekton dashboard is not available: " + tekton.getTektonMessage()); //$NON-NLS-1$
        	String errorMsg = tekton.isNotInstalled() ? Messages.ActionOpenTektonDashboardNotInstalled : Messages.ActionOpenTektonDashboardOtherError;
        	MessageDialog.openError(Display.getDefault().getActiveShell(), Messages.ActionOpenTektonDashboardErrorDialogTitle, errorMsg);
        	return;
        }

        URL url = tekton.getTektonUrl();
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
								conn.getConid() + "_tektonDashboard", conn.getName(), NLS.bind(Messages.BrowserTooltipTektonDashboard, conn.getName()));
			}

			browser.openURL(url);
		} catch (PartInitException e) {
			Logger.logError("Error opening the Tekton dashboard in browser", e); //$NON-NLS-1$
		}
    }
    
    public boolean showAction() {
    	return conn != null && conn.isConnected() && conn.getTektonDashboard() != null;
    }
}
