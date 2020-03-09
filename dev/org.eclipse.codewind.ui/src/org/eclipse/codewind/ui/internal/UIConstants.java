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

package org.eclipse.codewind.ui.internal;

public class UIConstants {
	
	private UIConstants() {}
	
	// Codewind documentation links
	public static final String 
		DOC_BASE_URL = "https://www.eclipse.org/codewind",
		CWSETTINGS_INFO_URL = DOC_BASE_URL + "/project-settings.html",
		TEMPLATES_INFO_URL = DOC_BASE_URL + "/workingwithtemplates.html",
		REGISTRY_INFO_URL = DOC_BASE_URL + "/remote-setupregistries.html",
		REMOTE_SETUP_URL =  DOC_BASE_URL + "/remotedeploy-eclipse.html",
		COMMANDS_OVERVIEW_URL = DOC_BASE_URL + "/project-actions.html";
	
	// Eclipse Marketplace
	private static final String MKTPLACE_BASE_URL = "https://marketplace.eclipse.org/content";
	public static final String
		CODEWIND_OPENAPI_URL = MKTPLACE_BASE_URL + "/codewind-openapi-tools",
		DOCKER_TOOLS_URL = MKTPLACE_BASE_URL + "/eclipse-docker-tooling";
	
	// Docker
	public static final String DOCKER_INSTALL_URL = "https://docs.docker.com/install/";
}
