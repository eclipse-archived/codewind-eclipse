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

public class LagomAutoBuildTest extends BaseAutoBuildTest {
	
	@Override
	protected void doSetup() throws Exception {
		projectName = "lagomautobuildtest";
		templateId = LAGOM_ID;
		relativeURL = "";
		srcPath = "hello-impl/src/main/java/com/example/rp/test/lagomendpoints/impl/HelloServiceImpl.java";
		text1 = "hello:";
		text2 = "hi there:";
		text3 = "hola:";
		super.doSetup();
	}
}
