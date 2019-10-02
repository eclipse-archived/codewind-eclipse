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

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ProjectTypeInfo {
	
	public static class ProjectSubtypeInfo {
		
		public String id;
		public String version;
		public String label;
		public String description;
		
		public ProjectSubtypeInfo(JSONObject json) throws JSONException {
			id = json.getString(ID_KEY);
			version = json.optString(VERSION_KEY);
			label = json.getString(LABEL_KEY);
			description = json.optString(DESCRIPTION_KEY);
		}

		@Override
		public int hashCode() {
			return id.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			
			if (this == obj)
				return true;
			
			if (obj instanceof ProjectSubtypeInfo)
				return id.equals(((ProjectSubtypeInfo) obj).id);
			
			return false;
		}
	}
	
	private static final String PROJECT_TYPE_KEY = "projectType";
	private static final String PROJECT_SUBTYPES_KEY = "projectSubtypes";
	
	private static final String LABEL_KEY = "label";
	private static final String ITEMS_KEY = "items";
	
	private static final String ID_KEY = "id";
	private static final String VERSION_KEY = "version";
	private static final String DESCRIPTION_KEY = "description";
	
	private String projectType;
	private String projectSubtypesLabel;
	private List<ProjectSubtypeInfo> projectSubtypes = new ArrayList<ProjectSubtypeInfo>();
	
	public ProjectTypeInfo(JSONObject json) throws JSONException {
		
		projectType = json.getString(PROJECT_TYPE_KEY);
		
		JSONObject subtypes = json.getJSONObject(PROJECT_SUBTYPES_KEY);
		projectSubtypesLabel = subtypes.optString(LABEL_KEY);
		
		JSONArray items = subtypes.getJSONArray(ITEMS_KEY);
		for (int i = 0; i < items.length(); i++) {
			JSONObject item = items.getJSONObject(i);
			projectSubtypes.add(new ProjectSubtypeInfo(item));
		}
	}
	
	public String getProjectType() {
		return projectType;
	}
	
	public String getProjectSubtypesLabel() {
		return projectSubtypesLabel;
	}
	
	public List<ProjectSubtypeInfo> getProjectSubtypes() {
		return projectSubtypes;
	}
	
	public void addProjectSubtypes(List<ProjectSubtypeInfo> projectSubtypes) {
		this.projectSubtypes.addAll(projectSubtypes);
	}
}
