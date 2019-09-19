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

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
	// Auto build tests
	GoAutoBuildTest.class,
	LagomAutoBuildTest.class,
	NodeAutoBuildTest.class,
	PythonAutoBuildTest.class,
	SpringAutoBuildTest.class,
	AppsodyJavaMicroprofileAutoBuildTest.class,
	AppsodyJavaSpringAutoBuildTest.class,
	AppsodyNodeExpressAutoBuildTest.class,
	
	// Debug tests
	LibertyDebugTest.class,
	SpringDebugTest.class,
	
	// Validation tests (not supported yet)
//	NodeValidationTest.class
})

public class CodewindTests {
	// intentionally empty
}