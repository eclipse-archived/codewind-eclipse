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

package org.eclipse.codewind.core.internal.constants;

import java.util.EnumSet;

public enum ProjectLanguage {
	LANGUAGE_JAVA("java", "Java"),
	LANGUAGE_JAVASCRIPT("javascript", "JavaScript"),
	LANGUAGE_NODEJS("nodejs", "Node.js"),
	LANGUAGE_SWIFT("swift", "Swift"),
	LANGUAGE_PYTHON("python", "Python"),
	LANGUAGE_GO("go", "Go"),
	LANGUAGE_BASH("bash", "Bash"),
	LANGUAGE_UNKNOWN("unknown", "Unknown");
	
	public static final EnumSet<ProjectLanguage> ALWAYS_HAS_APP_MONITOR = EnumSet.of(LANGUAGE_JAVA, LANGUAGE_NODEJS);
	
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
	
	public static ProjectLanguage getLanguage(String name) {
		for (ProjectLanguage language : ProjectLanguage.values()) {
			if (language.id.equals(name)) {
				return language;
			}
		}
		return LANGUAGE_UNKNOWN;
	}
	
	public String getMetricsRoot() {
		switch(this) {
			case LANGUAGE_NODEJS:
				return "appmetrics-dash/?theme=dark";
			case LANGUAGE_SWIFT:
				return "swiftmetrics-dash/?theme=dark";
			case LANGUAGE_JAVA:
				return "javametrics-dash/?theme=dark";
			default:
				return null;
		}
	}
	
	public static boolean alwaysHasAppMonitor(ProjectLanguage lang) {
		if (lang == null) {
			return false;
		}
		return ALWAYS_HAS_APP_MONITOR.contains(lang);
	}
	
	public static String getDisplayName(String languageId) {
		if (languageId == null) {
			return ProjectLanguage.LANGUAGE_UNKNOWN.getDisplayName();
		}
		ProjectLanguage language = ProjectLanguage.getLanguage(languageId);
		if (language != null && language != ProjectLanguage.LANGUAGE_UNKNOWN) {
			return language.getDisplayName();
		}
		return languageId;
	}
};