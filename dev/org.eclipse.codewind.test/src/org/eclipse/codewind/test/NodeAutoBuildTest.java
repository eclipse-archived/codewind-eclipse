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

import org.eclipse.codewind.core.internal.CodewindApplication;
import org.eclipse.codewind.core.internal.constants.AppStatus;
import org.eclipse.codewind.core.internal.constants.BuildStatus;
import org.eclipse.codewind.test.util.CodewindUtil;
import org.eclipse.codewind.test.util.TestUtil;
import org.eclipse.core.runtime.IPath;

public class NodeAutoBuildTest extends BaseAutoBuildTest {

	static {
		projectName = "nodeautobuildtest";
		templateId = NODE_EXPRESS_ID;
		relativeURL = "/hello";
		srcPath = "server/server.js";
		text1 = "Planet";
		text2 = "Earth";
		text3 = "World";
		extendedTest = true;
	}

	@Override
	public void doSetup() throws Exception {
		super.doSetup();
		
		String origText = "// Add your code here";
		String newText = "app.get('/hello', (req, res) => res.send('Hello Planet!'));";
		
		IPath path = project.getLocation();
		path = path.append(srcPath);
		TestUtil.updateFile(path.toOSString(), origText, newText);
		refreshProject();
    	
		CodewindApplication app = connection.getAppByName(projectName);
		CodewindUtil.waitForBuildState(app, BuildStatus.IN_PROGRESS, 30, 1);
		assertTrue("Build should be successful", CodewindUtil.waitForBuildState(app, BuildStatus.SUCCESS, 300, 1));
		assertTrue("App should be in started state", CodewindUtil.waitForAppState(app, AppStatus.STARTED, 120, 1));
	}
	
	
}
