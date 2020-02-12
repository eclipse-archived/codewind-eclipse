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

package org.eclipse.codewind.ui.internal.views;

import java.util.List;

import org.eclipse.codewind.core.internal.CodewindApplication;
import org.eclipse.codewind.core.internal.CodewindManager;
import org.eclipse.codewind.core.internal.Logger;
import org.eclipse.codewind.core.internal.connection.CodewindConnection;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.navigator.CommonNavigator;
import org.eclipse.ui.navigator.CommonViewer;

/**
 * Utilities for refreshing the Codewind view.
 */
public class ViewHelper {
	
	public static void openCodewindExplorerView() {
		openNavigatorView(CodewindExplorerView.VIEW_ID);
	}
	
	public static void refreshCodewindExplorerView(Object element) {
		final Object obj = element == null ? CodewindManager.getManager() : element;
        refreshNavigatorView(CodewindExplorerView.VIEW_ID, obj);
	}
	
	public static void expandConnection(CodewindConnection connection) {
		if (connection == null) {
			return;
		}
		List<CodewindApplication> apps = connection.getApps();
		if (!apps.isEmpty()) {
			Display.getDefault().asyncExec(new Runnable() {
	            @Override
	            public void run() {
					IViewPart view = getViewPart(CodewindExplorerView.VIEW_ID);
					if (view instanceof CommonNavigator) {
						CommonViewer viewer = ((CommonNavigator)view).getCommonViewer();
				
						if (!viewer.getExpandedState(connection)) {
							viewer.expandToLevel(AbstractTreeViewer.ALL_LEVELS);
						}
		            }
	 			}
			});
		}
	}
	
	public static void toggleExpansion(Object element) {
		final Object obj = element == null ? CodewindManager.getManager() : element;
		Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
				IViewPart view = getViewPart(CodewindExplorerView.VIEW_ID);
				if (view instanceof CommonNavigator) {
					CommonViewer viewer = ((CommonNavigator)view).getCommonViewer();
			
					if (!viewer.getExpandedState(obj)) {
						viewer.expandToLevel(obj, AbstractTreeViewer.ALL_LEVELS);
					} else {
						viewer.collapseToLevel(obj, AbstractTreeViewer.ALL_LEVELS);
					}
	            }
 			}
		});
	}
	
    public static void openNavigatorView(String viewId) {
    	Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
		        IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		        if (window != null) {
		            IWorkbenchPage page = window.getActivePage();
		            if (page != null) {
	                    try {
	                        page.showView(viewId);
	                    } catch (PartInitException e) {
	                        Logger.logError("An error occurred when trying to open the navigator view: " + viewId, e);
	                    }
		            }
		        }
            }
    	});
    }

    public static void refreshNavigatorView(String viewId, Object element) {
    	Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
		    	IViewPart part = getViewPart(viewId);
		        if (part != null) {
		            if (part instanceof CommonNavigator) {
		                CommonNavigator v = (CommonNavigator) part;
		                v.getCommonViewer().refresh(element);
		                v.getCommonViewer().expandToLevel(element, AbstractTreeViewer.ALL_LEVELS);
		            }
		        }
            }
    	});
    }
    
	public static IViewPart getViewPart(String viewId) {
    	for (IWorkbenchWindow window : PlatformUI.getWorkbench().getWorkbenchWindows()) {
    		for (IWorkbenchPage page : window.getPages()) {
    			for (IViewReference reference : page.getViewReferences()) {
    				if (viewId.equals(reference.getId())) {
    					return reference.getView(false);
    				}
    			}
    		}
    	}
    	return null;
	}
}
