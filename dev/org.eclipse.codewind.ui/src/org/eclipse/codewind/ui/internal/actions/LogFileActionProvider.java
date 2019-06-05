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

import org.eclipse.codewind.core.internal.CodewindEclipseApplication;
import org.eclipse.codewind.core.internal.console.ProjectLogInfo;
import org.eclipse.codewind.ui.internal.messages.Messages;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.navigator.CommonActionProvider;
import org.eclipse.ui.navigator.ICommonActionExtensionSite;
import org.eclipse.ui.navigator.ICommonMenuConstants;
import org.eclipse.ui.navigator.ICommonViewerSite;

/**
 * Action provider for application log files in the Codewind view.
 */
public class LogFileActionProvider extends CommonActionProvider {
	
	private ShowAllLogsAction showAllLogsAction;
	private HideAllLogsAction hideAllLogsAction;
	
	
    @Override
    public void init(ICommonActionExtensionSite aSite) {
        super.init(aSite);
        showAllLogsAction = new ShowAllLogsAction();
        hideAllLogsAction = new HideAllLogsAction();
    }
    
    @Override
    public void fillContextMenu(IMenuManager menu) {
    	final ICommonViewerSite viewSite = getActionSite().getViewSite();
    	ISelection selection = viewSite.getSelectionProvider().getSelection();
    	if (!(selection instanceof IStructuredSelection)) {
    		return;
    	}

        IStructuredSelection sel = (IStructuredSelection) selection;
        if (sel.size() == 1) {
        	Object obj = sel.getFirstElement();
        	if (obj instanceof CodewindEclipseApplication) {
        		final CodewindEclipseApplication app = (CodewindEclipseApplication)obj;
    			if (app.isAvailable() && app.getLogInfos() != null && !app.getLogInfos().isEmpty()) {
    				MenuManager menuMgr = new MenuManager(Messages.ShowLogFilesMenu, "ShowLogFiles");
    				showAllLogsAction.setApp(app);
    				menuMgr.add(showAllLogsAction);
    				hideAllLogsAction.setApp(app);
    				menuMgr.add(hideAllLogsAction);
    				menuMgr.add(new Separator());
    				for (ProjectLogInfo logInfo : app.getLogInfos()) {
    					menuMgr.add(new LogFileAction(app, logInfo, viewSite));
    				}
    				menu.appendToGroup(ICommonMenuConstants.GROUP_SHOW, menuMgr);
    			}
        	}
        }
	}
}
