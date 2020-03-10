/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.codewind.test;

import java.lang.reflect.Modifier;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.codewind.core.internal.HttpUtil;
import org.eclipse.codewind.core.internal.HttpUtil.HttpResult;
import org.eclipse.codewind.test.util.TestUtil;
import org.eclipse.codewind.ui.internal.UIConstants;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import junit.framework.TestCase;

/**
 * Test that all of the external links are valid
 * 
 * Set the org.eclipse.codewind.docBaseUrl environment variable to test with a
 * different base for the Codewind documentation links.
 * 
 * @see org.eclipse.codewind.ui.internal.UIConstants
 */

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ExternalLinkTest extends TestCase {
	
	@Test
    public void test01_doSetup() throws Exception {
        TestUtil.print("Starting test: " + getName());
    }
	
	@Test
    public void test02_pingLinks() throws Exception {
		List<String> failedLinks = new ArrayList<String>();
		Arrays.stream(UIConstants.class.getDeclaredFields())
			.filter(field -> Modifier.isPublic(field.getModifiers()))
			.forEach(field -> {
				String value = null;
				try {
					value = (String)field.get(null);
					HttpResult result = HttpUtil.get(new URI(value));
					if (!result.isGoodResponse || result.response == null || result.response.isEmpty()) {
						failedLinks.add(value);
						TestUtil.print("Failed to ping the external link: " + value);
					}
				} catch (Exception e) {
					if (value == null || value.isEmpty()) {
						failedLinks.add("(" + field.getName() + ")");
						TestUtil.print("Failed to get the external link for: " + field.getName(), e);
					} else {
						failedLinks.add(value);
						TestUtil.print("Failed to ping the external link: " + value, e);
					}
				}
			});
		assertTrue("Failed to ping the external links: " + Arrays.toString(failedLinks.toArray()), failedLinks.isEmpty());
    }
	
    @Test
    public void test99_tearDown() {
    	TestUtil.print("Ending test: " + getName());
    }

}
