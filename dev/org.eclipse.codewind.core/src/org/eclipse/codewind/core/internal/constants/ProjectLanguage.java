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

public enum ProjectLanguage {
	LANGUAGE_JAVA("java", "Java"),
	LANGUAGE_NODEJS("nodejs", "Node.js"),
	LANGUAGE_SWIFT("swift", "Swift"),
	LANGUAGE_PYTHON("python", "Python"),
	LANGUAGE_GO("go", "Go"),
	LANGUAGE_UNKNOWN("unknown", "Unknown");
	
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
				return "appmetrics-dash";
			case LANGUAGE_SWIFT:
				return "swiftmetrics-dash";
			case LANGUAGE_JAVA:
				return "javametrics-dash";
			default:
				return null;
		}
	}

};