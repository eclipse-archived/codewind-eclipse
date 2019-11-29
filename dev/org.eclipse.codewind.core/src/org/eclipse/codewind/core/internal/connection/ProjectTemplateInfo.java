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

import org.json.JSONObject;

public class ProjectTemplateInfo extends JSONObjectResult {
	
	public static final String LABEL_KEY = "label";
	public static final String DESCRIPTION_KEY = "description";
	public static final String URL_KEY = "url";
	public static final String LANGUAGE_KEY = "language";
	public static final String PROJECT_TYPE_KEY = "projectType";
	public static final String SOURCE_KEY = "source";
	public static final String PROJECT_STYLE_KEY = "projectStyle";
	
	public static final String CODEWIND_STYLE = "Codewind";
	
	public ProjectTemplateInfo(JSONObject projectInfo) {
		super(projectInfo, "project template");
	}
	
	public String getLabel() {
		return getString(LABEL_KEY);
	}
	
	public String getDescription() {
		return getString(DESCRIPTION_KEY);
	}
	
	public String getLanguage() {
		return getString(LANGUAGE_KEY);
	}
	
	public String getUrl() {
		return getString(URL_KEY);
	}
	
	public String getProjectType() {
		return getString(PROJECT_TYPE_KEY);
	}
	
	public String getProjectStyle() {
		String style = getString(PROJECT_STYLE_KEY);
		if (style == null || style.isEmpty()) {
			style = CODEWIND_STYLE;
		}
		return style;
	}
	
	public String getSource() {
		return getString(SOURCE_KEY);
	}
	
	public boolean isCodewindStyle() {
		return CODEWIND_STYLE.equals(getProjectStyle());
	}
}
