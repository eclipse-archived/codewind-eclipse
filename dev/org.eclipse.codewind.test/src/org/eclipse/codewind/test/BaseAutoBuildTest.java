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

package org.eclipse.codewind.test;

import java.net.URL;

import org.eclipse.codewind.core.internal.CodewindApplication;
import org.eclipse.codewind.core.internal.constants.AppStatus;
import org.eclipse.codewind.core.internal.constants.BuildStatus;
import org.eclipse.codewind.test.util.CodewindUtil;
import org.eclipse.codewind.test.util.TestUtil;
import org.eclipse.core.runtime.IPath;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public abstract class BaseAutoBuildTest extends BaseTest {
	
	protected static String text1, text2, text3;
	protected static boolean extendedTest;
	
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
    public void test03_checkDashboards() throws Exception {
    	checkDashboards();
    }
    
    @Test
    public void test04_disableAutoBuild() throws Exception {
    	setAutoBuild(false);
    	// Some project types need to restart when auto build changes (node.js)
    	CodewindApplication app = connection.getAppByName(projectName);
    	CodewindUtil.waitForAppState(app, AppStatus.STOPPING, 5, 1);
    	assertTrue("App should be in started state", CodewindUtil.waitForAppState(app, AppStatus.STARTED, 120, 1));
    }
    
    @Test
    public void test05_modifyFile() throws Exception {
    	IPath path = project.getLocation();
    	path = path.append(srcPath);
    	TestUtil.updateFile(path.toOSString(), text1, text2);
    	refreshProject();
    	// Make sure the status doesn't change
    	CodewindApplication app = connection.getAppByName(projectName);
    	CodewindUtil.checkStableAppStatus(app, AppStatus.STARTED, 5, 1);
    	// Check that the old text is still there
    	pingApp(text1);
    	// Run a build
    	build();
    	CodewindUtil.waitForBuildState(app, BuildStatus.IN_PROGRESS, 30, 1);
    	assertTrue("Build should be successful", CodewindUtil.waitForBuildState(app, BuildStatus.SUCCESS, 300, 1));
    	assertTrue("App should be in started state", CodewindUtil.waitForAppState(app, AppStatus.STARTED, 120, 1));
    	// Check for the new text
    	pingApp(text2);
    }
    
    @Test
    public void test06_enableAutoBuild() throws Exception {
    	setAutoBuild(true);
    	// Some project types need to restart when auto build changes (node.js)
    	CodewindApplication app = connection.getAppByName(projectName);
    	CodewindUtil.waitForAppState(app, AppStatus.STOPPING, 5, 1);
    	assertTrue("App should be in started state", CodewindUtil.waitForAppState(app, AppStatus.STARTED, 120, 1));
    }
    
    @Test
    public void test07_modifyFile() throws Exception {
    	IPath path = project.getLocation();
    	path = path.append(srcPath);
    	TestUtil.updateFile(path.toOSString(), text2, text3);
    	refreshProject();
    	// Check that build is started automatically
    	CodewindApplication app = connection.getAppByName(projectName);
    	CodewindUtil.waitForBuildState(app, BuildStatus.IN_PROGRESS, 30, 1);
    	assertTrue("Build should be successful", CodewindUtil.waitForBuildState(app, BuildStatus.SUCCESS, 300, 1));
    	assertTrue("App should be in started state", CodewindUtil.waitForAppState(app, AppStatus.STARTED, 120, 1));
    	// Check for the new text
    	pingApp(text3);
    }
    
    @Test
    public void test08_disableProject() throws Exception {
    	if (!extendedTest) return;
    	URL appURL = getAppURL();
    	disableProject();
    	checkAppUnavailable(appURL);
    }
    
    @Test
    public void test09_enableProject() throws Exception {
    	if (!extendedTest) return;
    	enableProject();
    	checkApp(text3);
    }
    
    @Test
    public void test10_removeProject() throws Exception {
    	if (!extendedTest) return;
    	URL appURL = getAppURL();
    	removeProject();
    	checkAppUnavailable(appURL);
    }
    
    @Test
    public void test11_addProject() throws Exception {
    	if (!extendedTest) return;
    	addProject();
    	assertTrue("The application " + projectName + " should be running", CodewindUtil.waitForProjectStart(connection, projectName, 600, 5));
    	checkApp(text3);
    }

    @Test
    public void test99_tearDown() {
    	doTearDown();
    	TestUtil.print("Ending test: " + getName());
    }

}
