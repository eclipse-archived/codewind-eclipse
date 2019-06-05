/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.codewind.test;

import org.eclipse.codewind.core.internal.constants.ProjectType;

public class SpringDebugTest extends BaseDebugTest {

	static {
		projectName = "springdebugtest";
		projectType = new ProjectType(ProjectType.TYPE_SPRING, ProjectType.LANGUAGE_JAVA);
		relativeURL = "/v1";
		srcPath = "src/main/java/application/rest/v1/Example.java";
		currentText = "Congratulations";
		newText = "Hello";
		dockerfile = "Dockerfile";
	}
}
