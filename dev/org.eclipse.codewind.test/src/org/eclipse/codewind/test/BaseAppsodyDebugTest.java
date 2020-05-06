/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
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
import org.eclipse.codewind.core.internal.connection.CodewindConnection;
import org.eclipse.codewind.core.internal.constants.AppStatus;
import org.eclipse.codewind.core.internal.constants.StartMode;
import org.eclipse.codewind.test.util.CodewindUtil;
import org.eclipse.codewind.test.util.TestUtil;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public abstract class BaseAppsodyDebugTest extends BaseTest {
	
	protected static CodewindConnection conn;
	protected static CodewindApplication app;
	protected static IProject project;
	protected static String projectType;
	
	// Must be set by the test case in doSetup
	protected static String projectName;
	protected static String templateId;
	protected static String relativeURL;
	protected static String srcPath;
	protected static String currentText;
	protected static String newText;
	
	protected void doSetup() throws Exception {
        setup();
        conn = getLocalConnection();
        
        app = createProject(conn, projectType, templateId, projectName);
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
    	checkApp(app, relativeURL, currentText);
    }
    
    @Test
    public void test03_switchToDebugMode() throws Exception {
    	switchMode(app, StartMode.DEBUG);
    	pingApp(app, relativeURL, currentText);
    	checkConsoles(app);
    }
    
    @Test
    public void test04_modifyJavaFile() throws Exception {
    	IPath path = project.getLocation();
    	path = path.append(srcPath);
    	TestUtil.updateFile(path.toOSString(), currentText, newText);
    	refreshProject(project);
    	currentText = newText;
    	CodewindApplication app = conn.getAppByName(projectName);
    	// For Java builds the states can go by quickly so don't do an assert on this
    	CodewindUtil.waitForAppState(app, AppStatus.STOPPED, 120, 1);
    	assertTrue("App should be in started state", CodewindUtil.waitForAppState(app, AppStatus.STARTED, 120, 1));
    	pingApp(app, relativeURL, currentText);
    	checkMode(app, StartMode.DEBUG);
    	checkConsoles(app);
    }

    @Test
    public void test05_switchToRunMode() throws Exception {
    	switchMode(app, StartMode.RUN);
    	pingApp(app, relativeURL, currentText);
    	checkConsoles(app);
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
    }
}
