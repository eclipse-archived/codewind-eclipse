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

package org.eclipse.codewind.core.internal.constants;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.codewind.core.internal.messages.Messages;

public class ProjectLanguage {
	
	public static final ProjectLanguage LANGUAGE_JAVA = new ProjectLanguage("java", "Java");
	public static final ProjectLanguage LANGUAGE_JAVASCRIPT = new ProjectLanguage("javascript", "JavaScript");
	public static final ProjectLanguage LANGUAGE_NODEJS = new ProjectLanguage("nodejs", "Node.js");
	public static final ProjectLanguage LANGUAGE_SWIFT = new ProjectLanguage("swift", "Swift");
	public static final ProjectLanguage LANGUAGE_GO = new ProjectLanguage("go", "Go");
	public static final ProjectLanguage LANGUAGE_PYTHON = new ProjectLanguage("python", "Python");
	public static final ProjectLanguage LANGUAGE_BASH = new ProjectLanguage("bash", "Bash");
	public static final ProjectLanguage LANGUAGE_UNKNOWN = new ProjectLanguage("unknown", Messages.GenericUnknown);
	
	private static final Map<String, ProjectLanguage> defaultLanguages = new HashMap<String, ProjectLanguage>();
	
	static {
		defaultLanguages.put("java", LANGUAGE_JAVA);
		defaultLanguages.put("javascript", LANGUAGE_JAVASCRIPT);
		defaultLanguages.put("nodejs", LANGUAGE_NODEJS);
		defaultLanguages.put("swift", LANGUAGE_SWIFT);
		defaultLanguages.put("go", LANGUAGE_GO);
		defaultLanguages.put("python", LANGUAGE_PYTHON);
		defaultLanguages.put("bash", LANGUAGE_BASH);
		defaultLanguages.put("unknown", LANGUAGE_UNKNOWN);
	}
	
	private final String id;
	private final String displayName;
	
	private ProjectLanguage(String id, String displayName) {
		this.id = id;
		this.displayName = displayName;
	}
	
	public String getId() {
		return id;
	}
	
	public String getDisplayName() {
		return displayName;
	}
	
	public static ProjectLanguage getLanguage(String id) {
		ProjectLanguage language = id == null ? LANGUAGE_UNKNOWN : defaultLanguages.get(id);
		if (language == null) {
			language = new ProjectLanguage(id, id);
		}
		return language;
	}
	
	public boolean isUnknown() {
		return "unknown".equals(id);
	}
	
	public boolean isJavaScript() {
		return "javascript".equals(id) || "nodejs".equals(id);
	}
	
	public boolean isJava() {
		return "java".equals(id);
	}
	
	public boolean isSwift() {
		return "swift".equals(id);
	}
	
	public boolean isGo() {
		return "go".equals(id);
	}
	
	public boolean isPython() {
		return "python".equals(id);
	}

	public static String getDisplayName(String id) {
		if (id == null) {
			return Messages.GenericUnknown;
		}
		ProjectLanguage language = defaultLanguages.get(id);
		if (language != null) {
			return language.getDisplayName();
		}
		return id;
	}
};