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

import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.codewind.core.internal.CodewindApplication;
import org.eclipse.codewind.core.internal.CodewindEclipseApplication;
import org.eclipse.codewind.core.internal.CodewindManager;
import org.eclipse.codewind.core.internal.HttpUtil;
import org.eclipse.codewind.core.internal.HttpUtil.HttpResult;
import org.eclipse.codewind.core.internal.ProcessHelper.ProcessResult;
import org.eclipse.codewind.core.internal.cli.InstallUtil;
import org.eclipse.codewind.core.internal.cli.ProjectUtil;
import org.eclipse.codewind.core.internal.cli.TemplateUtil;
import org.eclipse.codewind.core.internal.connection.CodewindConnection;
import org.eclipse.codewind.core.internal.connection.CodewindConnectionManager;
import org.eclipse.codewind.core.internal.connection.ProjectTemplateInfo;
import org.eclipse.codewind.core.internal.console.CodewindConsoleFactory;
import org.eclipse.codewind.core.internal.console.ProjectLogInfo;
import org.eclipse.codewind.core.internal.console.SocketConsole;
import org.eclipse.codewind.core.internal.constants.AppStatus;
import org.eclipse.codewind.core.internal.constants.CoreConstants;
import org.eclipse.codewind.core.internal.constants.ProjectInfo;
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
import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.TextConsole;
import org.eclipse.ui.ide.IDE;

import junit.framework.TestCase;

public abstract class BaseTest extends TestCase {
	
	protected static final String APPSODY_PROJECT_TYPE="appsodyExtension";
	protected static final String LAGOM_ID = "lagomJavaTemplate";
	protected static final String GO_ID = "goTemplate";
	protected static final String JAVA_MICROPROFILE_ID = "javaMicroProfileTemplate";
	protected static final String NODE_EXPRESS_ID = "nodeExpressTemplate";
	protected static final String PYTHON_ID = "pythonTemplate";
	protected static final String SPRING_JAVA_ID = "springJavaTemplate";
	protected static final String APPSODY_JAVA_MICROPROFILE_ID = "java-microprofile.*default";
	protected static final String APPSODY_NODE_EXPRESS_ID = "nodejs-express.*simple";
	protected static final String APPSODY_JAVA_SPRING_ID = "spring-boot2.*default";

	protected static final String MARKER_TYPE = "org.eclipse.codewind.core.validationMarker";
	
	protected static final String RESOURCE_PATH = "resources";
	
	protected static Boolean origAutoBuildSetting = null;
	
	public void setup() {
		// Disable workspace auto build
    	origAutoBuildSetting = setWorkspaceAutoBuild(false);
	}

	public void cleanup() {
		// Restore workspace auto build setting
		if (origAutoBuildSetting != null) {
			setWorkspaceAutoBuild(origAutoBuildSetting.booleanValue());
		}
	}
	
	public CodewindConnection getLocalConnection() throws Exception {
		// Check that Codewind is installed
    	CodewindManager.getManager().refreshInstallStatus(new NullProgressMonitor());
    	if (!CodewindManager.getManager().getInstallStatus().isInstalled()) {
    		installCodewind();
    		startCodewind();
    	} else if (!CodewindManager.getManager().getInstallStatus().isStarted()) {
    		startCodewind();
    	}
    	assertTrue("Codewind must be installed and started before the tests can be run", CodewindManager.getManager().getInstallStatus().isStarted());
    	
    	// Get the local Codewind connection
        CodewindConnection conn = CodewindConnectionManager.getLocalConnection();
        if (conn == null) {
            IJobManager jobManager = Job.getJobManager();
            Job[] jobs = jobManager.find(CodewindConnectionManager.RESTORE_CONNECTIONS_FAMILY);
            for (Job job : jobs) {
            	job.join();
            }
            conn = CodewindConnectionManager.getLocalConnection();
        }
        assertNotNull("The connection should not be null.", conn);
        return conn;
	}
	
	public void cleanupConnection(CodewindConnection conn) {
		try {
			CodewindUtil.cleanup(conn);
		} catch (Exception e) {
			TestUtil.print("Cleanup failed for connection: " + conn.getName(), e);
		}
	}
	
	public CodewindApplication getApp(CodewindConnection conn, String projectName) {
		return conn.getAppByName(projectName);
	}
    
    protected URL getAppURL(CodewindApplication app, String relativeURL) throws Exception {
    	URL url = app.getRootUrl();
    	if (relativeURL != null && !relativeURL.isEmpty()) {
    		url = new URL(url.toExternalForm() + relativeURL);
    	}
    	return url;
    }
    
    public void checkApp(CodewindApplication app, String relativeURL, String text) throws Exception {
    	assertTrue("App should be in started state.  Current state is: " + app.getAppStatus(), CodewindUtil.waitForAppState(app, AppStatus.STARTED, 120, 2));
    	pingApp(app, relativeURL, text);
    	checkMode(app, StartMode.RUN);
    	showConsoles(app);
    	checkConsoles(app);
    }
    
    public void checkAppUnavailable(String projectName, URL url) throws Exception {
    	try {
    		assertTrue("The application URL should not be available: " + url, !pingUnavailableURL(url).isGoodResponse);
    	} catch (IOException e) {
    		// This is expected if the application is not available
    	}
    	checkNoConsoles(projectName);
    }
    
    protected void pingApp(CodewindApplication app, String relativeURL, String expectedText) throws Exception {
    	URL url = getAppURL(app, relativeURL);
    	HttpUtil.HttpResult result = pingURL(url);
    	assertTrue("The response code should be 200: " + result.responseCode, result.responseCode == 200);
    	assertTrue("The response should contain the expected text: " + expectedText, result.response != null && result.response.contains(expectedText));   	
    }

    protected void checkMode(CodewindApplication app, StartMode mode) throws Exception {
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
    
    protected void switchMode(CodewindApplication app, StartMode mode) throws Exception {
    	TestUtil.print("Switching mode for " + app.name + " to: " + mode);
    	ProjectUtil.restartProject(app.name, app.projectID, mode.startMode, app.connection.getConid(), new NullProgressMonitor());
    	// For Java builds the states can go by quickly so don't do an assert on this
    	CodewindUtil.waitForAppState(app, AppStatus.STOPPED, 30, 1);
    	assertTrue("App should be in started state instead of: " + app.getAppStatus(), CodewindUtil.waitForAppState(app, AppStatus.STARTED, 120, 1));
    	checkMode(app, mode);
    }
    
    protected void showConsoles(CodewindApplication cwApp) throws Exception {
    	TestUtil.print("Opening consoles for " + cwApp.name);
    	CodewindEclipseApplication app = (CodewindEclipseApplication) cwApp;
		for (ProjectLogInfo logInfo : app.getLogInfos()) {
    		if (app.getConsole(logInfo) == null) {
    			SocketConsole console = CodewindConsoleFactory.createLogFileConsole(app, logInfo);
    			app.addConsole(console);
    		}
    	}
    }

    protected void checkConsoles(CodewindApplication app) throws Exception {
    	Set<String> expectedConsoles = new HashSet<String>();
    	Set<String> foundConsoles = new HashSet<String>();
		for (ProjectLogInfo logInfo : app.getLogInfos()) {
			expectedConsoles.add(logInfo.logName);
		}
    	
    	IConsoleManager manager = ConsolePlugin.getDefault().getConsoleManager();
    	for (IConsole console : manager.getConsoles()) {
    		if (console.getName().contains(app.name)) {
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
    
    protected void checkNoConsoles(String projectName) throws Exception {
    	IConsoleManager manager = ConsolePlugin.getDefault().getConsoleManager();
    	boolean hasConsoles = false;
    	for (IConsole console : manager.getConsoles()) {
    		if (console.getName().contains(projectName)) {
    			TestUtil.print("Found unexpected console for the " + projectName + " project: " + console.getName());
    			hasConsoles = true;
    		}
    	}
    	assertFalse("The " + projectName + " project should not have any consoles.", hasConsoles);
    }
    
    protected void checkDashboards(CodewindApplication app) throws Exception {
    	if (app.hasMetricsDashboard()) {
    		URL url = app.getMetricsDashboardUrl();
    		assertTrue("Expecting a good response code for ping of metrics dashboard: " + url.toString(), pingURL(url).isGoodResponse);
    	}
    	if (app.hasPerfDashboard()) {
    		URL url = app.getPerfDashboardUrl();
    		assertTrue("Expecting a good response code for ping of performance dashboard: " + url.toString(), pingURL(url).isGoodResponse);
    	}
    }
    
    protected HttpResult pingURL(URL url) throws Exception {
    	HttpUtil.HttpResult result = HttpUtil.get(url.toURI());
    	for (int i = 0; i < 15 && !result.isGoodResponse; i++) {
    		Thread.sleep(1000);
    		result = HttpUtil.get(url.toURI());
    	}
    	if (!result.isGoodResponse) {
    		String msg = result.error == null || result.error.isEmpty() ? result.response : result.error;
    		TestUtil.print("Ping of " + url.toString() + " gave response code: " + result.responseCode + " and output: " + msg);
    	} else {
    		TestUtil.print("Ping of " + url.toString() + " was successful");
    	}
    	return result;
    }
    
    protected HttpResult pingUnavailableURL(URL url) throws Exception {
    	HttpUtil.HttpResult result = HttpUtil.get(url.toURI());
    	for (int i = 0; i < 15 && result.isGoodResponse; i++) {
    		Thread.sleep(1000);
    		result = HttpUtil.get(url.toURI());
    	}
    	if (result.isGoodResponse) {
    		TestUtil.print("The URL is not supposed to be available: " + url.toString());
    	}
    	return result;
    }
    
	protected void build(CodewindApplication app) throws Exception {
		TestUtil.print("Starting manual build for " + app.name);
		app.connection.requestProjectBuild(app, CoreConstants.VALUE_ACTION_BUILD);
	}
    
    protected void setAutoBuild(CodewindApplication app, boolean enabled) throws Exception {
    	TestUtil.print("Setting auto build enabled for " + app.name + " to: " + enabled);
    	String actionKey = enabled ? CoreConstants.VALUE_ACTION_ENABLEAUTOBUILD : CoreConstants.VALUE_ACTION_DISABLEAUTOBUILD;
		app.connection.requestProjectBuild(app, actionKey);
    }
    
    protected IMarker[] getMarkers(IResource resource) throws Exception {
    	return resource.findMarkers(MARKER_TYPE, false, IResource.DEPTH_ONE);
    }
    
    protected void runValidation(CodewindApplication app) throws Exception {
		app.connection.requestValidate(app);
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

	protected CodewindApplication createProject(CodewindConnection conn, String type, String id, String name) throws Exception {
		TestUtil.print("Creating project: " + name + ", on connection " + conn.getName());
		ProjectTemplateInfo templateInfo = null;
		List<ProjectTemplateInfo> templates = TemplateUtil.listTemplates(true, conn.getConid(), new NullProgressMonitor());
		for (ProjectTemplateInfo template : templates) {
			if ((type == null || type.equals(template.getProjectType())) && 
					template.getUrl().toLowerCase().matches(".*" + id.toLowerCase() + ".*")) {
				templateInfo = template;
				break;
			}
		}
		assertNotNull("No template found that matches the id: " + id, templateInfo);
		IPath path = ResourcesPlugin.getWorkspace().getRoot().getLocation().append(name);
		ProjectUtil.createProject(name, path.toOSString(), templateInfo.getUrl(), conn.getConid(), new NullProgressMonitor());
		ProjectUtil.bindProject(name, path.toOSString(), templateInfo.getLanguage(), templateInfo.getProjectType(), conn.getConid(), new NullProgressMonitor());
		assertTrue("The application " + name + " should be created", CodewindUtil.waitForProject(conn, name, 300, 5));
		return getApp(conn, name);
	}
	
	protected IProject importProject(CodewindApplication app) throws Exception {
		// Import the application into eclipse
		TestUtil.print("Importing " + app.name + " into Eclipse");
        ImportProjectAction.importProject(app);
        IProject project = ImportUtil.waitForProject(app.name);
        assertNotNull("The " + app.name + " project should be imported in eclipse", project);
        return project;
	}
	
	protected void disableProject(CodewindApplication app) throws Exception {
		TestUtil.print("Disabling project: " + app.name);
		app.connection.requestProjectOpenClose(app, false);
		assertTrue("The " + app.name + " project should be disabled", TestUtil.wait(() -> !app.isEnabled(), 15, 1));
	}
	
	protected void enableProject(CodewindApplication app) throws Exception {
		TestUtil.print("Enabling project: " + app.name);
		app.connection.requestProjectOpenClose(app, true);
		assertTrue("The " + app.name + " project should be enabled", TestUtil.wait(() -> app.isEnabled(), 15, 1));
		assertTrue("The application " + app.name + " should be running", CodewindUtil.waitForAppState(app, AppStatus.STARTED, 600, 5));
	}
	
	protected void removeProject(CodewindApplication app) throws Exception {
		TestUtil.print("Removing project: " + app.name);
		ProjectUtil.removeProject(app.name, app.projectID, new NullProgressMonitor());
		assertTrue("The " + app.name + " project should be removed from Codewind", TestUtil.wait(() -> getApp(app.connection, app.name) == null, 15, 1));
	}
	
	protected CodewindApplication addProject(IProject project, String projectType, CodewindConnection conn) throws Exception {
		TestUtil.print("Adding project: " + project.getName());
		ProjectInfo info = ProjectUtil.validateProject(project.getName(), project.getLocation().toOSString(), null, conn.getConid(), new NullProgressMonitor());
		assertTrue("Validation result for " + project.getName() + " should not be null.", info != null && info.type != null);
		assertTrue("Project type should be the same as when the project was created. Expected: " + projectType + ", Actual: " + info.type.getId(), info.type.getId().equals(projectType));
		ProjectUtil.bindProject(project.getName(), project.getLocation().toOSString(), info.language.getId(), info.type.getId(), conn.getConid(), new NullProgressMonitor());
		assertTrue("The application " + project.getName() + " should be created", CodewindUtil.waitForProject(conn, project.getName(), 300, 5));
		return getApp(conn, project.getName());
	}
	
	protected void refreshProject(IProject project) throws CoreException {
		project.refreshLocal(IProject.DEPTH_INFINITE, new NullProgressMonitor());
	}
	
	protected void copyFile(String resourcesRelPath, IPath destPath) throws Exception {
		IPath srcPath = CodewindTestPlugin.getInstallLocation();
		srcPath = srcPath.append(RESOURCE_PATH).append(resourcesRelPath);
		TestUtil.copyFile(srcPath, destPath);
	}

	protected void installCodewind() throws Exception {
		TestUtil.print("Installing Codewind version: " + InstallUtil.getVersion());
		ProcessResult result = InstallUtil.installCodewind(InstallUtil.getVersion(), new NullProgressMonitor());
		checkResult(result);
		assertTrue("Codewind should be installed", CodewindManager.getManager().getInstallStatus().isInstalled());
	}
	
	protected void startCodewind() throws Exception {
		TestUtil.print("Starting Codewind version: " + InstallUtil.getVersion());
		ProcessResult result = InstallUtil.startCodewind(InstallUtil.getVersion(), new NullProgressMonitor());
		checkResult(result);
		assertTrue("Codewind should be started", CodewindManager.getManager().getInstallStatus().isStarted());
	}
	
	protected void checkResult(ProcessResult result) throws Exception {
		assertTrue("The process failed with exit code: " + result.getExitValue() + ", and error: " + result.getErrorMsg(), result.getExitValue() == 0);
	}
}
