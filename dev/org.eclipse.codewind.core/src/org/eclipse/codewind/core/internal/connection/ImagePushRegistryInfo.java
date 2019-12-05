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

import org.json.JSONObject;

public class ImagePushRegistryInfo extends JSONObjectResult {
	
	public static final String ADDRESS_KEY = "address";
	public static final String NAMESPACE_KEY = "namespace";
	
	public ImagePushRegistryInfo(JSONObject obj) {
		super(obj, "image push registry");
	}

	public String getAddress() {
		return getString(ADDRESS_KEY);
	}
	
	public String getNamespace() {
		return getString(NAMESPACE_KEY);
	}
}
