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

public class PythonAutoBuildTest extends BaseAutoBuildTest {
	
	@Override
	protected void doSetup() throws Exception {
		projectName = "pythonautobuildtest";
		templateId = PYTHON_ID;
		relativeURL = "/";
		srcPath = "app.py";
		text1 = "World";
		text2 = "Planet";
		text3 = "Earth";
		super.doSetup();
	}
}
