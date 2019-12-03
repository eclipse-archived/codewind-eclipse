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

public class RegistryInfo extends JSONObjectResult {
	
	public static final String URL_KEY = "url";
	public static final String USERNAME_KEY = "username";
	
	private boolean isPushReg = false;
	
	public RegistryInfo(JSONObject repo) {
		super(repo, "registry");
	}

	public String getURL() {
		return getString(URL_KEY);
	}
	
	public String getUsername() {
		List<String> usernames = getStringArray(USERNAME_KEY);
		if (!usernames.isEmpty()) {
			return usernames.get(0);
		}
		return null;
	}
	
	public String getNamespace() {
		return null;
	}
	
	public boolean isPushReg() {
		return isPushReg;
	}
	
	public void setIsPushReg(boolean isPushReg) {
		this.isPushReg = isPushReg;
	}
}
