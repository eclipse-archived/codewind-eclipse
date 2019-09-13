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

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeoutException;

import org.eclipse.codewind.core.internal.CodewindApplication;
import org.eclipse.codewind.core.internal.CodewindEclipseApplication;
import org.eclipse.codewind.core.internal.CodewindManager;
import org.eclipse.codewind.core.internal.HttpUtil;
import org.eclipse.codewind.core.internal.InstallUtil;
import org.eclipse.codewind.core.internal.connection.CodewindConnection;
import org.eclipse.codewind.core.internal.connection.ProjectTemplateInfo;
import org.eclipse.codewind.core.internal.console.CodewindConsoleFactory;
import org.eclipse.codewind.core.internal.console.ProjectLogInfo;
import org.eclipse.codewind.core.internal.console.SocketConsole;
import org.eclipse.codewind.core.internal.constants.AppStatus;
import org.eclipse.codewind.core.internal.constants.CoreConstants;
import org.eclipse.codewind.core.internal.constants.StartMode;
import org.eclipse.codewind.test.util.CodewindUtil;
import org.eclipse.codewind.test.util.Condition;
import org.eclipse.codewind.test.util.ImportUtil;
import org.eclipse.codewind.test.util.TestUtil;
import org.eclipse.codewind.ui.internal.actions.ImportProjectAction;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.TextConsole;
import org.eclipse.ui.ide.IDE;
import org.json.JSONException;

import junit.framework.TestCase;

public abstract class BaseTest extends TestCase {
	
	protected static final String LAGOM_ID = "lagomJavaTemplate";
	protected static final String GO_ID = "microclimateGoTemplate";
	protected static final String JAVA_MICROPROFILE_ID = "javaMicroProfileTemplate";
	protected static final String NODE_EXPRESS_ID = "nodeExpressTemplate";
	protected static final String PYTHON_ID = "SVTPythonTemplate";
	protected static final String SPRING_JAVA_ID = "springJavaTemplate";
	protected static final String APPSODY_JAVA_MICROPROFILE_ID = "codewind-appsody-java-microprofile";
	protected static final String APPSODY_NODE_EXPRESS_ID = "codewind-appsody-nodejs-express";
	protected static final String APPSODY_JAVA_SPRING_ID = "codewind-appsody-java-spring-boot2";

	protected static final String MARKER_TYPE = "org.eclipse.codewind.core.validationMarker";
	
	protected static final String RESOURCE_PATH = "resources";
	
	protected static CodewindConnection connection;
	protected static IProject project;
	
	protected static String projectName;
	protected static String templateId;
	protected static String relativeURL;
	protected static String srcPath;
	
	protected static Boolean origAutoBuildSetting = null;
	
    public void doSetup() throws Exception {
    	// Check that Codewind is installed
    	assertTrue("Codewind must be installed and started before the tests can be run", CodewindManager.getManager().getInstallStatus().isStarted());
    	
    	// Disable workspace auto build
    	origAutoBuildSetting = setWorkspaceAutoBuild(false);
    	
        // Create a Codewind connection
        connection = CodewindManager.getManager().getLocalConnection();
        if (connection == null) {
        	connection = CodewindManager.getManager().createLocalConnection();
        }
        assertNotNull("The connection should not be null.", connection);
        
        // Create a new microprofile project
        createProject(templateId, projectName);
        
        // Wait for the project to be created
        assertTrue("The application " + projectName + " should be created", CodewindUtil.waitForProject(connection, projectName, 300, 5));
        
        // Wait for the project to be started
        assertTrue("The application " + projectName + " should be running", CodewindUtil.waitForProjectStart(connection, projectName, 600, 5));
        
        // Import the application into eclipse
        CodewindApplication app = connection.getAppByName(projectName);
        ImportProjectAction.importProject(app);
        project = ImportUtil.waitForProject(projectName);
        assertNotNull("The " + projectName + " project should be imported in eclipse", project);
    }
    
	public void doTearDown() {
		try {
			CodewindUtil.cleanup(connection);
		} catch (Exception e) {
			TestUtil.print("Test case cleanup failed", e);
		}
    	
		// Restore workspace auto build setting
		if (origAutoBuildSetting != null) {
			setWorkspaceAutoBuild(origAutoBuildSetting.booleanValue());
		}
	}
    
    public void checkApp(String text) throws Exception {
    	CodewindApplication app = connection.getAppByName(projectName);
    	assertTrue("App should be in started state.  Current state is: " + app.getAppStatus(), CodewindUtil.waitForAppState(app, AppStatus.STARTED, 120, 2));
    	pingApp(text);
    	checkMode(StartMode.RUN);
    	showConsoles();
    	checkConsoles();
    }
    
    protected void pingApp(String expectedText) throws Exception {
    	CodewindApplication app = connection.getAppByName(projectName);
    	URL url = app.getRootUrl();
    	url = new URL(url.toExternalForm() + relativeURL);
    	HttpUtil.HttpResult result = HttpUtil.get(url.toURI());
    	for (int i = 0; i < 15 && !result.isGoodResponse; i++) {
    		Thread.sleep(1000);
    		result = HttpUtil.get(url.toURI());
    	}
    	assertTrue("The response code should be 200: " + result.responseCode, result.responseCode == 200);
    	assertTrue("The response should contain the expected text: " + expectedText, result.response != null && result.response.contains(expectedText));   	
    }
    
    protected void checkMode(StartMode mode) throws Exception {
    	CodewindApplication app = connection.getAppByName(projectName);
    	for (int i = 0; i < 5 && app.getStartMode() != mode; i++) {
    		Thread.sleep(1000);
    	}
    	assertTrue("App is in " + app.getStartMode() + " when it should be in " + mode + " mode.", app.getStartMode() == mode);
    	ILaunch launch = ((CodewindEclipseApplication)app).getLaunch();
    	if (StartMode.DEBUG_MODES.contains(mode)) {
    		assertNotNull("There should be a launch for the app", launch);
        	IDebugTarget debugTarget = launch.getDebugTarget();
	    	assertNotNull("The launch should have a debug target", debugTarget);
	    	assertTrue("The debug target should have threads", debugTarget.hasThreads());
    	} else {
    		assertNull("There should be no launch when in run mode", launch);
    	}
    }
    
    protected void switchMode(StartMode mode) throws Exception {
    	CodewindApplication app = connection.getAppByName(projectName);
    	connection.requestProjectRestart(app, mode.startMode);
    	// For Java builds the states can go by quickly so don't do an assert on this
    	CodewindUtil.waitForAppState(app, AppStatus.STOPPED, 30, 1);
    	assertTrue("App should be in started state instead of: " + app.getAppStatus(), CodewindUtil.waitForAppState(app, AppStatus.STARTED, 120, 1));
    	checkMode(mode);
    }
    
    protected void showConsoles() throws Exception {
    	CodewindEclipseApplication app = (CodewindEclipseApplication) connection.getAppByName(projectName);
		for (ProjectLogInfo logInfo : app.getLogInfos()) {
    		if (app.getConsole(logInfo) == null) {
    			SocketConsole console = CodewindConsoleFactory.createLogFileConsole(app, logInfo);
    			app.addConsole(console);
    		}
    	}
    }

    protected void checkConsoles() throws Exception {
    	CodewindApplication app = connection.getAppByName(projectName);
    	Set<String> expectedConsoles = new HashSet<String>();
    	Set<String> foundConsoles = new HashSet<String>();
		for (ProjectLogInfo logInfo : app.getLogInfos()) {
			expectedConsoles.add(logInfo.logName);
		}
    	
    	IConsoleManager manager = ConsolePlugin.getDefault().getConsoleManager();
    	for (IConsole console : manager.getConsoles()) {
    		if (console.getName().contains(projectName)) {
    			TestUtil.print("Found console: " + console.getName());
    			assertTrue("The " + console.getName() + " console should be a TextConsole", console instanceof TextConsole);
    			TestUtil.wait(new Condition() {
    				@Override
    				public boolean test() {
    					return ((TextConsole)console).getDocument().getLength() > 0;
    				}
    			}, 20, 1);
    			assertTrue("The " + console.getName() + " console should not be empty", ((TextConsole)console).getDocument().getLength() > 0);
    			for (String name : expectedConsoles) {
    				if (console.getName().contains(name)) {
    					foundConsoles.add(name);
    					break;
    				}
    			}
    		}
    	}
    	assertTrue("Did not find all expected consoles", foundConsoles.size() == expectedConsoles.size());
    }
    
    protected void build() throws Exception {
    	CodewindApplication app = connection.getAppByName(projectName);
		connection.requestProjectBuild(app, CoreConstants.VALUE_ACTION_BUILD);
    }
    
    protected void setAutoBuild(boolean enabled) throws Exception {
    	String actionKey = enabled ? CoreConstants.VALUE_ACTION_ENABLEAUTOBUILD : CoreConstants.VALUE_ACTION_DISABLEAUTOBUILD;
    	CodewindApplication app = connection.getAppByName(projectName);
		connection.requestProjectBuild(app, actionKey);
    }
    
    protected IMarker[] getMarkers(IResource resource) throws Exception {
    	return resource.findMarkers(MARKER_TYPE, false, IResource.DEPTH_ONE);
    }
    
    protected void runValidation() throws Exception {
    	CodewindApplication app = connection.getAppByName(projectName);
		connection.requestValidate(app);
    }
    
    protected void runQuickFix(IResource resource) throws Exception {
    	IMarker[] markers = getMarkers(resource);
    	assertTrue("There should be at least one marker for " + resource.getName() + ": " + markers.length, markers.length > 0);

        IMarkerResolution[] resolutions = IDE.getMarkerHelpRegistry().getResolutions(markers[0]);
        assertTrue("Did not get any marker resolutions.", resolutions.length > 0);
        resolutions[0].run(markers[0]);
        TestUtil.waitForJobs(10, 1);
    }
    
	public static Boolean setWorkspaceAutoBuild(boolean enabled) {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IWorkspaceDescription wsDescription = workspace.getDescription();
		boolean origEnabled = wsDescription.isAutoBuilding();
		if (enabled != origEnabled) {
			try {
				wsDescription.setAutoBuilding(enabled);
				workspace.setDescription(wsDescription);
				return origEnabled ? Boolean.TRUE : Boolean.FALSE;
			} catch (CoreException e) {
				TestUtil.print("Failed to set workspace auto build enabled to: " + enabled, e);
			}
		}
		return null;
	}
	
	protected void createProject(String id, String name) throws IOException, JSONException, URISyntaxException, TimeoutException {
		ProjectTemplateInfo templateInfo = null;
		List<ProjectTemplateInfo> templates = connection.requestProjectTemplates(true);
		for (ProjectTemplateInfo template : templates) {
			if (template.getUrl().toLowerCase().contains(id.toLowerCase())) {
				templateInfo = template;
				break;
			}
		}
		assertNotNull("No template found that matches the id: " + id, templateInfo);
		IPath path = connection.getWorkspacePath().append(name);
		InstallUtil.createProject(name, path.toOSString(), templateInfo.getUrl(), new NullProgressMonitor());
		connection.requestProjectBind(name, connection.getWorkspacePath() + "/" + name, templateInfo.getLanguage(), templateInfo.getProjectType());

	}
	
	protected void refreshProject() throws CoreException {
		project.refreshLocal(IProject.DEPTH_INFINITE, new NullProgressMonitor());
	}
	
	protected void copyFile(String resourcesRelPath, IPath destPath) throws Exception {
		IPath srcPath = CodewindTestPlugin.getInstallLocation();
		srcPath = srcPath.append(RESOURCE_PATH).append(resourcesRelPath);
		TestUtil.copyFile(srcPath, destPath);
	}

}
