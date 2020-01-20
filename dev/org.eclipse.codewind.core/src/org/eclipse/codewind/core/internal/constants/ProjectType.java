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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.codewind.core.internal.messages.Messages;
import org.json.JSONObject;

/**
 * Project type. For known types provides a user friendly name.
 */
public class ProjectType {
	
	public static final ProjectType TYPE_LIBERTY = new ProjectType("liberty", "MicroProfile / Java EE");
	public static final ProjectType TYPE_SPRING = new ProjectType("spring", "Spring");
	public static final ProjectType TYPE_SWIFT = new ProjectType("swift", "Swift");
	public static final ProjectType TYPE_NODEJS = new ProjectType("nodejs", "Node.js");
	public static final ProjectType TYPE_DOCKER = new ProjectType("docker", Messages.DockerTypeDisplayName);
	public static final ProjectType TYPE_UNKNOWN = new ProjectType("unknown", Messages.GenericUnknown);
	
	private static final Map<String, ProjectType> defaultTypes = new HashMap<String, ProjectType>();
	static {
		defaultTypes.put("liberty", TYPE_LIBERTY);
		defaultTypes.put("spring", TYPE_SPRING);
		defaultTypes.put("swift", TYPE_SWIFT);
		defaultTypes.put("nodejs", TYPE_NODEJS);
		defaultTypes.put("docker", TYPE_DOCKER);
		defaultTypes.put("unknown", TYPE_UNKNOWN);
	}
	
	private final String typeId;
	private final String displayName;
	private final JSONObject extension;
	
	private ProjectType(String typeId, String displayName) {
		this(typeId, displayName, null);
	}
	
	private ProjectType(String typeId, String displayName, JSONObject extension) {
		this.typeId = typeId;
		this.displayName = displayName;
		this.extension = extension;
	}
	
	public static ProjectType getType(String typeId, JSONObject extension) {
		ProjectType type = defaultTypes.get(typeId);
		if (type == null) {
			String name = null;
			if (extension != null) {
				name = getExtDisplayName(typeId);
			}
			type = new ProjectType(typeId, name, extension);
		}
		return type;
	}
	
	private static String getExtDisplayName(String typeId) {
		String name = typeId.toLowerCase();
		if (name.endsWith("extension")) {
			name = name.substring(0, name.length() - "extension".length());
		}
		return name.substring(0, 1).toUpperCase() + name.substring(1);
	}
	
	public String getId() {
		return typeId;
	}
	
	public String getDisplayName() {
		if (displayName != null) {
			return displayName;
		}
		return typeId;
	}
	
	public boolean isExtension() {
		return extension != null;
	}
	
	public static boolean isCodewindStyle(String typeId) {
		return !"appsodyExtension".equals(typeId) && !"odo".equals(typeId);
	}
	
	public static String getDisplayName(String typeId) {
		if (typeId == null) {
			return Messages.GenericUnknown;
		}
		ProjectType type = defaultTypes.get(typeId);
		if (type != null) {
			return type.getDisplayName();
		}
		return getExtDisplayName(typeId);
	}
}
