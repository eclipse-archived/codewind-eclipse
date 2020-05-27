/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.codewind.core.internal.connection;

import org.eclipse.codewind.core.internal.Logger;
import org.json.JSONException;
import org.json.JSONObject;

public class ExtensionConfig extends JSONObjectResult {
	
	private static final String EXTENSION_KEY = "extension";
	private static final String CONFIG_KEY = "config";
	private static final String CONTAINER_APP_ROOT_KEY = "containerAppRoot";
	
	public ExtensionConfig(JSONObject extensionConfig) {
		super(extensionConfig, "extension config");
	}
	
	public static ExtensionConfig getExtensionConfig(JSONObject projectInfo) {
		try {
			if (projectInfo.has(EXTENSION_KEY) && !projectInfo.isNull(EXTENSION_KEY)) {
				JSONObject ext = projectInfo.getJSONObject(EXTENSION_KEY);
				if (ext.has(CONFIG_KEY) && !ext.isNull(CONFIG_KEY)) {
					return new ExtensionConfig(ext.getJSONObject(CONFIG_KEY));
				}
			}
		} catch (JSONException e) {
			Logger.logError("The project info format is not valid", e);
		}
		return null;
	}
	
	public String getContainerAppRoot() {
		return getString(CONTAINER_APP_ROOT_KEY);
	}

}
