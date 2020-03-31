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

public class AppsodyNodeExpressAutoBuildTest extends BaseAppsodyAutoBuildTest {

	static {
		projectName = "appsodynodeexpressautobuildtest";
		projectType = APPSODY_PROJECT_TYPE;
		templateId = APPSODY_NODE_EXPRESS_ID;
		relativeURL = "";
		srcPath = "app.js";
		text1 = "Hello";
		text2 = "Hi there";
	}
}