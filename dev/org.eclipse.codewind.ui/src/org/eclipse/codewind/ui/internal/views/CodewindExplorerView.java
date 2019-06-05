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

package org.eclipse.codewind.ui.internal.views;

import org.eclipse.codewind.ui.internal.actions.CreateConnectionAction;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.navigator.CommonNavigator;
import org.eclipse.ui.navigator.CommonViewer;

/**
 * Codewind explorer view.  Shows connections to Codewind instances and
 * the applications for each instance.
 */
public class CodewindExplorerView extends CommonNavigator {
    public static final String VIEW_ID = "org.eclipse.codewind.ui.explorerView";

    @Override
    protected CommonViewer createCommonViewerObject(Composite parent) {
    	CreateConnectionAction.setupConnection();
        CommonViewer viewer = super.createCommonViewerObject(parent);
        viewer.setAutoExpandLevel(2);
        return viewer;
    }
}
