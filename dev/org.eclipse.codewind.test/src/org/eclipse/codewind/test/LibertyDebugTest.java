/*******************************************************************************
 * Copyright (c) 2018, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.codewind.test;

public class LibertyDebugTest extends BaseDebugTest {

	@Override
	protected void doSetup() throws Exception {
		projectName = "libertydebugtest";
		templateId = JAVA_MICROPROFILE_ID;
		relativeURL = "/v1/example";
		srcPath = "src/main/java/application/rest/v1/Example.java";
		currentText = "Congratulations";
		newText = "Hello";
		dockerfile = "Dockerfile";
		super.doSetup();
	}
}
