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

package org.eclipse.codewind.ui.internal.actions;

import java.net.URL;

import org.eclipse.codewind.core.internal.CodewindApplication;
import org.eclipse.codewind.core.internal.CoreUtil;
import org.eclipse.codewind.core.internal.Logger;
import org.eclipse.codewind.core.internal.constants.AppStatus;
import org.eclipse.codewind.ui.CodewindUIPlugin;
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
 * Action to open the application in a browser.
 */
public class OpenAppAction extends SelectionProviderAction {
	
	public static final String ACTION_ID = "org.eclipse.codewind.ui.openAppAction";

    protected CodewindApplication app;
    
	public OpenAppAction(ISelectionProvider selectionProvider) {
		super(selectionProvider, Messages.OpenAppAction_Label);
		setImageDescriptor(CodewindUIPlugin.getImageDescriptor(CodewindUIPlugin.OPEN_APP_ICON));
		selectionChanged(getStructuredSelection());
	}

    @Override
    public void selectionChanged(IStructuredSelection sel) {
        if (sel.size() == 1) {
            Object obj = sel.getFirstElement();
            if (obj instanceof CodewindApplication) {
            	app = (CodewindApplication)obj;
            	setEnabled(app.isAvailable() && (app.getAppStatus() == AppStatus.STARTING || app.getAppStatus() == AppStatus.STARTED));
            	return;
            }
        }
        setEnabled(false);
    }

    @Override
    public void run() {
        if (app == null) {
        	// should not be possible
        	Logger.logError("OpenAppAction ran but no application was selected"); //$NON-NLS-1$
			return;
		}

        if (!app.isRunning()) {
        	CoreUtil.openDialog(true, Messages.OpenAppAction_CantOpenNotRunningAppTitle,
        			Messages.OpenAppAction_CantOpenNotRunningAppMsg);
        	return;
        }
        
        openAppInBrowser(app);
    }
    
	public static void openAppInBrowser(CodewindApplication app) {
		URL appRootUrl = app.getRootUrl();
		if (appRootUrl == null) {
			Logger.logError("The application could not be opened in the browser because the url is null: " + app.name); //$NON-NLS-1$
			return;
		}
		try {
			// Use the app's ID as the browser ID so that if this is called again on the same app,
			// the browser will be re-used
			IWorkbenchBrowserSupport browserSupport = PlatformUI.getWorkbench().getBrowserSupport();
			IWebBrowser browser = browserSupport
					.createBrowser(IWorkbenchBrowserSupport.NAVIGATION_BAR | IWorkbenchBrowserSupport.LOCATION_BAR,
							app.projectID, app.name, NLS.bind(Messages.BrowserTooltipApp, app.name));

			browser.openURL(appRootUrl);
		} catch (PartInitException e) {
			Logger.logError("Error opening app in browser", e); //$NON-NLS-1$
		}
	}
}
