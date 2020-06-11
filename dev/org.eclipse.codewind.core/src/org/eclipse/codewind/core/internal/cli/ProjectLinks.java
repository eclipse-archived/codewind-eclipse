/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *	 IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.codewind.core.internal.cli;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.codewind.core.internal.connection.CodewindConnection;
import org.eclipse.codewind.core.internal.connection.JSONObjectResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ProjectLinks {
	
	private List<LinkInfo> links = new ArrayList<LinkInfo>();
	
	public class LinkInfo extends JSONObjectResult {
		
		private static final String PROJECT_ID_KEY = "projectID";
		private static final String ENV_NAME_KEY = "envName";
		private static final String PROJECT_URL_KEY = "projectURL";
		private static final String PROJECT_NAME_KEY = "projectName";

		public LinkInfo(JSONObject linkInfo) {
			super(linkInfo, "link info");
		}
		
		public String getProjectId() {
			return getString(PROJECT_ID_KEY);
		}
		
		public String getEnvVar() {
			return getString(ENV_NAME_KEY);
		}
		
		public String getProjectURL() {
			return getString(PROJECT_URL_KEY);
		}
		
		public String getProjectName() {
			return getString(PROJECT_NAME_KEY);
		}
	}
	
	public ProjectLinks(JSONArray linksArray) throws JSONException {
		for (int i = 0; i < linksArray.length(); i++) {
			links.add(new LinkInfo(linksArray.getJSONObject(i)));
		}
	}
	
	public List<LinkInfo> getLinks() {
		return Collections.unmodifiableList(links);
	}
	
	// Return the list of infos for any links to the given project
	public List<LinkInfo> getLinkedProjects(String projectId) {
		return links.stream().filter(link -> link.getProjectId().equals(projectId)).collect(Collectors.toList());
	}
	
	// Return the list of infos for any links to projects that don't exist
	public List<LinkInfo> getBrokenLinks(CodewindConnection conn) {
		return links.stream().filter(link -> conn.getAppByID(link.getProjectId()) == null).collect(Collectors.toList());
	}

}
