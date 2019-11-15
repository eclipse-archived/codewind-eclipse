/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *	 IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.codewind.core.internal.cli;

import org.eclipse.codewind.core.internal.connection.JSONObjectResult;
import org.eclipse.codewind.core.internal.messages.Messages;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class UpgradeResult extends JSONObjectResult {
	
	private static String FAILED_KEY = "failed";
	private static String MIGRATED_KEY = "migrated";
	private static String ERROR_KEY = "error";
	private static String PROJECT_NAME_KEY = "projectName";
	
	public UpgradeResult(JSONObject result) {
		super(result, "workspace upgrade");
	}
	
	public String getFormattedResult() throws JSONException {
		JSONArray migrated = getArray(MIGRATED_KEY);
		JSONArray failed = getArray(FAILED_KEY);
		if ((migrated == null || migrated.length() == 0) && (failed == null || failed.length() == 0)) {
			return null;
		}
		
		StringBuilder builder = new StringBuilder();
		
		if (migrated != null && migrated.length() > 0) {
			builder.append(Messages.UpgradeResultMigrated + "\n");
			builder.append("  ");
			boolean start = true;
			for (int i = 0; i < migrated.length(); i++) {
				if (start) {
					start = false;
				} else {
					builder.append(", ");
				}
				builder.append(migrated.get(i));
			}
		}
		
		if (failed != null && failed.length() > 0) {
			if (builder.length() > 0) {
				builder.append("\n");
			}
			builder.append(Messages.UpgradeResultNotMigrated + "\n");
			builder.append("  ");
			boolean start = true;
			for (int i = 0; i < failed.length(); i++) {
				if (start) {
					start = false;
				} else {
					builder.append("\n  ");
				}
				JSONObject obj = failed.getJSONObject(i);
				String name = obj.getString(PROJECT_NAME_KEY);
				String error = obj.getString(ERROR_KEY);
				builder.append(name + " (" + error + ")");
			}
		}
		
		return builder.toString();
	}

}
