/*******************************************************************************
 * Copyright (c) 2018, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.codewind.core.internal.constants;

/**
 * Type and language of a project.
 */
public enum ProjectType {
	
	TYPE_LIBERTY("liberty", "MicroProfile / Java EE"),
	TYPE_SPRING("spring", "Spring"),
	TYPE_SWIFT("swift", "Swift"),
	TYPE_NODEJS("nodejs", "Node.js"),
	TYPE_DOCKER("docker", "Other (Basic Container)"),
	TYPE_UNKNOWN("unknown", "Unknown");

	private final String id;
	private final String displayName;
	
	private ProjectType(String id, String displayName) {
		this.id = id;
		this.displayName = displayName;
	}
	
	public String getId() {
		return id;
	}
	
	public String getDisplayName() {
		return displayName;
	}
	
	public static ProjectType getType(String name) {
		for (ProjectType type : ProjectType.values()) {
			if (type.id.equals(name)) {
				return type;
			}
		}
		return TYPE_UNKNOWN;
	}
	
	public static ProjectType getTypeFromLanguage(String language) {
		ProjectLanguage lang = ProjectLanguage.getLanguage(language);
		switch(lang) {
			case LANGUAGE_NODEJS:
				return TYPE_NODEJS;
			case LANGUAGE_SWIFT:
				return TYPE_SWIFT;
			case LANGUAGE_PYTHON:
				return TYPE_DOCKER;
			case LANGUAGE_GO:
				return TYPE_DOCKER;
			default:
				return null;
		}
	}
	
	public static String getDisplayName(String typeId) {
		if (typeId == null) {
			return ProjectType.TYPE_UNKNOWN.getDisplayName();
		}
		ProjectType type = ProjectType.getType(typeId);
		if (type != null && type != ProjectType.TYPE_UNKNOWN) {
			return type.getDisplayName();
		}
		return typeId;
	}
	
}
