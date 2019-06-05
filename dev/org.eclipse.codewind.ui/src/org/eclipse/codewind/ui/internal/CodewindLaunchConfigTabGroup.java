/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.codewind.ui.internal;

import org.eclipse.debug.ui.AbstractLaunchConfigurationTabGroup;
import org.eclipse.debug.ui.CommonTab;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.eclipse.debug.ui.sourcelookup.SourceLookupTab;

/**
 * A tab group for editing a Codewind Launch Configuration.
 * Right now there isn't really any obvious reason a user would need to use this,
 * but without it you get an error message when clicking on the launch config in the Configurations menu.
 */
public class CodewindLaunchConfigTabGroup extends AbstractLaunchConfigurationTabGroup {

	@Override
    public void createTabs(ILaunchConfigurationDialog dialog, String mode) {
        ILaunchConfigurationTab[] tabs = new ILaunchConfigurationTab[2];

        SourceLookupTab sourceLookupTab = new SourceLookupTab();
        sourceLookupTab.setLaunchConfigurationDialog(dialog);
        tabs[0] = sourceLookupTab;

        tabs[1] = new CommonTab();
        tabs[1].setLaunchConfigurationDialog(dialog);
        setTabs(tabs);
    }
}
