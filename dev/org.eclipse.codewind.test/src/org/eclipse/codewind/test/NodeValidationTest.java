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

import org.eclipse.codewind.core.internal.constants.AppStatus;
import org.eclipse.codewind.test.util.CodewindUtil;
import org.eclipse.codewind.test.util.TestUtil;
import org.eclipse.core.runtime.IPath;

public class NodeValidationTest extends BaseValidationTest {

	static {
		projectName = "nodevalidationtest";
		templateId = NODE_EXPRESS_ID;
		relativeURL = "/hello";
		srcPath = "server/server.js";
		text = "Hello World!";
		dockerfile = "Dockerfile";
	}

	@Override
	public void doSetup() throws Exception {
		super.doSetup();
		IPath path = project.getLocation();
    	path = path.append(srcPath);
		TestUtil.updateFile(path.toOSString(), "// Add your code here", "app.get('/hello', (req, res) => res.send('Hello World!'));");
		build(app);
		// For Java builds the states can go by quickly so don't do an assert on this
		CodewindUtil.waitForAppState(app, AppStatus.STOPPED, 120, 1);
		assertTrue("App should be in started state", CodewindUtil.waitForAppState(app, AppStatus.STARTED, 300, 1));
	}
}
