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

public class AppsodyJavaSpringAutoBuildTest extends BaseAppsodyAutoBuildTest {

	@Override
	protected void doSetup() throws Exception {
		projectName = "appsodyjavaspringautobuildtest";
		projectType = APPSODY_PROJECT_TYPE;
		templateId = APPSODY_JAVA_SPRING_ID;
		relativeURL = "/actuator/liveness";
		srcPath = "src/main/java/application/LivenessEndpoint.java";
		text1 = "UP";
		text2 = "ALIVE";
		super.doSetup();
	}
}