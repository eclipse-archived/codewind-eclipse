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

package org.eclipse.codewind.test;

import org.eclipse.codewind.core.internal.CodewindApplication;
import org.eclipse.codewind.core.internal.constants.AppState;
import org.eclipse.codewind.test.util.CodewindUtil;
import org.eclipse.codewind.test.util.TestUtil;
import org.eclipse.core.runtime.IPath;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public abstract class BaseAutoBuildTest extends BaseTest {
	
	protected static String text1, text2, text3;
	
    @Test
    public void test01_doSetup() throws Exception {
        TestUtil.print("Starting test: " + getName());
        doSetup();
    }
    
    @Test
    public void test02_checkApp() throws Exception {
    	checkApp(text1);
    }
    
    @Test
    public void test03_disableAutoBuild() throws Exception {
    	setAutoBuild(false);
    }
    
    @Test
    public void test04_modifyFile() throws Exception {
    	IPath path = connection.getWorkspacePath().append(projectName);
    	path = path.append(srcPath);
    	TestUtil.updateFile(path.toOSString(), text1, text2);
    	// Check that the old text is still there
    	pingApp(text1);
    	// Run a build
    	build();
    	CodewindApplication app = connection.getAppByName(projectName);
    	// For Java builds the states can go by quickly so don't do an assert on this
    	CodewindUtil.waitForAppState(app, AppState.STOPPED, 120, 1);
    	assertTrue("App should be in started state", CodewindUtil.waitForAppState(app, AppState.STARTED, 120, 1));
    	// Check for the new text
    	pingApp(text2);
    }
    
    @Test
    public void test05_enableAutoBuild() throws Exception {
    	setAutoBuild(true);
    }
    
    @Test
    public void test06_modifyFile() throws Exception {
    	IPath path = connection.getWorkspacePath().append(projectName);
    	path = path.append(srcPath);
    	TestUtil.updateFile(path.toOSString(), text2, text3);
    	// Check that build is started automatically
    	buildIfWindows();
    	CodewindApplication app = connection.getAppByName(projectName);
    	// For Java builds the states can go by quickly so don't do an assert on this
    	CodewindUtil.waitForAppState(app, AppState.STOPPED, 120, 1);
    	assertTrue("App should be in started state", CodewindUtil.waitForAppState(app, AppState.STARTED, 120, 1));
    	// Check for the new text
    	pingApp(text3);
    }
    
    @Test
    public void test99_tearDown() {
    	doTearDown();
    	TestUtil.print("Ending test: " + getName());
    }

}
