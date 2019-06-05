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

package org.eclipse.codewind.test;

import org.eclipse.codewind.core.internal.CodewindApplication;
import org.eclipse.codewind.core.internal.constants.AppState;
import org.eclipse.codewind.core.internal.constants.StartMode;
import org.eclipse.codewind.test.util.CodewindUtil;
import org.eclipse.codewind.test.util.TestUtil;
import org.eclipse.core.runtime.IPath;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public abstract class BaseDebugTest extends BaseTest {
	
	protected static String currentText;
	protected static String newText;
	protected static String dockerfile;
	
    @Test
    public void test01_doSetup() throws Exception {
        TestUtil.print("Starting test: " + getName());
        doSetup();
    }
    
    @Test
    public void test02_checkApp() throws Exception {
    	checkApp(currentText);
    }
    
    @Test
    public void test03_switchToDebugMode() throws Exception {
    	switchMode(StartMode.DEBUG);
    	pingApp(currentText);
    	checkConsoles();
    }
    
    @Test
    public void test04_modifyJavaFile() throws Exception {
    	IPath path = connection.getWorkspacePath().append(projectName);
    	path = path.append(srcPath);
    	TestUtil.updateFile(path.toOSString(), currentText, newText);
    	currentText = newText;
    	buildIfWindows();
    	CodewindApplication app = connection.getAppByName(projectName);
    	// For Java builds the states can go by quickly so don't do an assert on this
    	CodewindUtil.waitForAppState(app, AppState.STOPPED, 120, 1);
    	assertTrue("App should be in started state", CodewindUtil.waitForAppState(app, AppState.STARTED, 120, 1));
    	pingApp(currentText);
    	checkMode(StartMode.DEBUG);
    	checkConsoles();
    }
    
    @Test
    public void test05_modifyDockerfile() throws Exception {
    	IPath path = connection.getWorkspacePath().append(projectName);
    	path = path.append(dockerfile);
    	TestUtil.prependToFile(path.toOSString(), "# no comment\n");
    	buildIfWindows();
    	CodewindApplication app = connection.getAppByName(projectName);
    	assertTrue("App should be in stopped state", CodewindUtil.waitForAppState(app, AppState.STOPPED, 120, 1));
    	assertTrue("App should be in starting state", CodewindUtil.waitForAppState(app, AppState.STARTING, 600, 1));
    	assertTrue("App should be in started state", CodewindUtil.waitForAppState(app, AppState.STARTED, 300, 1));
    	pingApp(currentText);
    	checkMode(StartMode.DEBUG);
    	checkConsoles();
    }
    
    @Test
    public void test06_switchToRunMode() throws Exception {
    	switchMode(StartMode.RUN);
    	pingApp(currentText);
    	checkConsoles();
    }
    
    @Test
    public void test99_tearDown() {
    	doTearDown();
    	TestUtil.print("Ending test: " + getName());
    }

}
