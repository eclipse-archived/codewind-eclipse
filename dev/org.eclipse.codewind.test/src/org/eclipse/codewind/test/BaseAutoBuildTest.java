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
import org.eclipse.codewind.core.internal.connection.CodewindConnection;
import org.eclipse.codewind.core.internal.constants.AppStatus;
import org.eclipse.codewind.core.internal.constants.BuildStatus;
import org.eclipse.codewind.test.util.CodewindUtil;
import org.eclipse.codewind.test.util.TestUtil;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public abstract class BaseAutoBuildTest extends BaseTest {
	
	protected static CodewindConnection conn;
	protected static CodewindApplication app;
	protected static IProject project;
	protected static String projectType;
	protected static boolean extendedTest = false;
	
	// Must be set by the test case in doSetup
	protected static String projectName;
	protected static String templateId;
	protected static String relativeURL;
	protected static String srcPath;
	protected static String text1, text2, text3;
	
	protected void doSetup() throws Exception {
        setup();
        conn = getLocalConnection();
        
        app = createProject(conn, projectType, templateId, projectName);
        TestUtil.print("Application created: " + projectName);
        projectType = app.projectType.getId();
        
        // Wait for the project to be started
        assertTrue("The application " + projectName + " should be running", CodewindUtil.waitForAppState(getApp(conn, projectName), AppStatus.STARTED, 600, 5));
        
        project = importProject(app);
	}
	
    @Test
    public void test01_doSetup() throws Exception {
    	TestUtil.print("Starting test: " + getClass().getSimpleName());
    	clearVariables();
        doSetup();
    }
    
    @Test
    public void test02_checkApp() throws Exception {
    	checkApp(app, relativeURL, text1);
    }
    
    @Test
    public void test03_checkDashboards() throws Exception {
    	checkDashboards(app);
    }
    
    @Test
    public void test04_disableAutoBuild() throws Exception {
    	setAutoBuild(app, false);
    	// Some project types need to restart when auto build changes (node.js)
    	CodewindUtil.waitForAppState(app, AppStatus.STOPPING, 5, 1);
    	assertTrue("App should be in started state", CodewindUtil.waitForAppState(app, AppStatus.STARTED, 120, 1));
    }
    
    @Test
    public void test05_modifyFile() throws Exception {
    	IPath path = project.getLocation();
    	path = path.append(srcPath);
    	TestUtil.updateFile(path.toOSString(), text1, text2);
    	refreshProject(project);
    	// Make sure the status doesn't change
    	CodewindUtil.checkStableAppStatus(app, AppStatus.STARTED, 5, 1);
    	// Check that the old text is still there
    	pingApp(app, relativeURL, text1);
    	// Run a build
    	build(app);
    	CodewindUtil.waitForBuildState(app, BuildStatus.IN_PROGRESS, 30, 1);
    	assertTrue("Build should be successful", CodewindUtil.waitForBuildState(app, BuildStatus.SUCCESS, 300, 1));
    	assertTrue("App should be in started state", CodewindUtil.waitForAppState(app, AppStatus.STARTED, 120, 1));
    	// Check for the new text
    	pingApp(app, relativeURL, text2);
    }
    
    @Test
    public void test06_enableAutoBuild() throws Exception {
    	setAutoBuild(app, true);
    	// Some project types need to restart when auto build changes (node.js)
    	CodewindUtil.waitForAppState(app, AppStatus.STOPPING, 5, 1);
    	assertTrue("App should be in started state", CodewindUtil.waitForAppState(app, AppStatus.STARTED, 120, 1));
    }
    
    @Test
    public void test07_modifyFile() throws Exception {
    	IPath path = project.getLocation();
    	path = path.append(srcPath);
    	TestUtil.updateFile(path.toOSString(), text2, text3);
    	refreshProject(project);
    	// Check that build is started automatically
    	CodewindUtil.waitForBuildState(app, BuildStatus.IN_PROGRESS, 30, 1);
    	assertTrue("Build should be successful", CodewindUtil.waitForBuildState(app, BuildStatus.SUCCESS, 300, 1));
    	assertTrue("App should be in started state", CodewindUtil.waitForAppState(app, AppStatus.STARTED, 120, 1));
    	// Check for the new text
    	pingApp(app, relativeURL, text3);
    }
    
    @Test 
    public void test08_appOverview() throws Exception {
    	if (!extendedTest) return;
    	TestUtil.print("Opening application overview page for: " + app.name);
    	openAppOverview(app);
    	assertTrue("App overview page should be open for: " + app.name, hasAppOverview(app));
    }
    
    @Test
    public void test09_disableProject() throws Exception {
    	if (!extendedTest) return;
    	URL appURL = getAppURL(app, relativeURL);
    	disableProject(app);
    	checkAppUnavailable(projectName, appURL);
    	// The app overview should still be there
    	assertTrue("App overview page should be open for: " + app.name, hasAppOverview(app));
    }
    
    @Test
    public void test10_enableProject() throws Exception {
    	if (!extendedTest) return;
    	enableProject(app);
    	checkApp(app, relativeURL, text3);
    	assertTrue("App overview page should be open for: " + app.name, hasAppOverview(app));
    }
    
    @Test
    public void test11_removeProject() throws Exception {
    	if (!extendedTest) return;
    	URL appURL = getAppURL(app, relativeURL);
    	removeProject(app);
    	checkAppUnavailable(projectName, appURL);
    	assertFalse("App overview page should be closed for: " + app.name, hasAppOverview(app));
    	app = null;
    }
    
    @Test
    public void test12_addProject() throws Exception {
    	if (!extendedTest) return;
    	app = addProject(project, projectType, conn);
    	assertTrue("The application " + projectName + " should be running", CodewindUtil.waitForAppState(getApp(conn, projectName), AppStatus.STARTED, 600, 5));
    	checkApp(app, relativeURL, text3);
    }

    @Test
    public void test99_tearDown() {
    	cleanupConnection(conn);
    	clearVariables();
    	cleanup();
    	TestUtil.print("Ending test: " + getClass().getSimpleName());
    }
    
    private void clearVariables() {
    	conn = null;
    	app = null;
    	project = null;
    	projectType = null;
    	extendedTest = false;
    }
}
