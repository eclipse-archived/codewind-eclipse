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

public class AppsodyJavaSpringAutoBuildTest extends BaseBuildTest {

	static {
		projectName = "appsodyjavaspringautobuildtest";
		templateId = APPSODY_JAVA_SPRING_ID;
		relativeURL = "/actuator/liveness";
		srcPath = "src/main/java/application/Main.java";
		text1 = "UP";
		text2 = "ALIVE";
	}
}