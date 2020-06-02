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

import org.eclipse.codewind.core.internal.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class JSONObjectResult {
	
	protected final JSONObject result;
	protected final String type;
	
	public JSONObjectResult(JSONObject result, String type) {
		this.result = result;
		this.type = type;
	}
	
	protected boolean hasKey(String key) {
		return result.has(key);
	}
	
	protected String getString(String key) {
		String value = null;
		if (result.has(key)) {
			try {
				value = result.getString(key);
			} catch (JSONException e) {
				Logger.logError("An error occurred retrieving the value from the " + type + " object for key: " + key, e);
			}
		} else {
			Logger.logError("The " + type + " object did not have the expected key: " + key);
		}
		return value;
	}
	
	protected boolean getBoolean(String key) {
		boolean value = false;
		if (result.has(key)) {
			try {
				value = result.getBoolean(key);
			} catch (JSONException e) {
				Logger.logError("An error occurred retrieving the value from the " + type + " object for key: " + key, e);
			}
		} else {
			Logger.logError("The " + type + " object did not have the expected key: " + key);
		}
		return value;
	}
	
	protected Integer getInt(String key) {
		Integer value = null;
		if (result.has(key)) {
			try {
				value = result.getInt(key);
			} catch (JSONException e) {
				Logger.logError("An error occurred retrieving the value from the " + type + " object for key: " + key, e);
			}
		} else {
			Logger.logError("The " + type + " object did not have the expected key: " + key);
		}
		return value;
	}
	
	protected List<String> getStringArray(String key) {
		List<String> list = new ArrayList<String>();
		if (result.has(key)) {
			try {
				JSONArray array = result.getJSONArray(key);
				for (int i = 0; i < array.length(); i++) {
					list.add(array.getString(i));
				}
			} catch (JSONException e) {
				Logger.logError("An error occurred retrieving the value from the " + type + " object for key: " + key, e);
			}
		} else {
			Logger.logError("The " + type + " object did not have the expected key: " + key);
		}
		return list;
	}
	
	protected JSONObject getObject(String key) {
		JSONObject value = null;
		if (result.has(key)) {
			try {
				value = result.getJSONObject(key);
			} catch (JSONException e) {
				Logger.logError("An error occurred retrieving the value from the " + type + " object for key: " + key, e);
			}
		} else {
			Logger.logError("The " + type + " object did not have the expected key: " + key);
		}
		return value;
	}
	
	protected JSONArray getArray(String key) {
		JSONArray value = null;
		if (result.has(key)) {
			try {
				value = result.getJSONArray(key);
			} catch (JSONException e) {
				Logger.logError("An error occurred retrieving the value from the " + type + " object for key: " + key, e);
			}
		} else {
			Logger.logError("The " + type + " object did not have the expected key: " + key);
		}
		return value;
	}

	@Override
	public String toString() {
		return result.toString();
	}
}
