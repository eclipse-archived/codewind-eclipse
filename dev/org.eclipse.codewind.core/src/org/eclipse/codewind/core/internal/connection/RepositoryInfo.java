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

package org.eclipse.codewind.core.internal.connection;

import java.util.List;

import org.json.JSONObject;

public class RepositoryInfo extends JSONObjectResult {
	
	public static final String URL_KEY = "url";
	public static final String DESCRIPTION_KEY = "description";
	public static final String ENABLED_KEY = "enabled";
	public static final String PROTECTED_KEY = "protected";
	public static final String STYLES_KEY = "projectStyles";
	
	public RepositoryInfo(JSONObject repo) {
		super(repo, "repository");
	}

	public String getURL() {
		return getString(URL_KEY);
	}
	
	public String getDescription() {
		return getString(DESCRIPTION_KEY);
	}
	
	public boolean getEnabled() {
		return getBoolean(ENABLED_KEY);
	}
	
	public boolean isProtected() {
		return getBoolean(PROTECTED_KEY);
	}
	
	public List<String> getStyles() {
		return getStringArray(STYLES_KEY);
	}
}
