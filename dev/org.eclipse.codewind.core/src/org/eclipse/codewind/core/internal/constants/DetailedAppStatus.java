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

package org.eclipse.codewind.core.internal.constants;

import org.eclipse.codewind.core.internal.Logger;
import org.eclipse.codewind.core.internal.connection.JSONObjectResult;
import org.eclipse.codewind.core.internal.messages.Messages;
import org.json.JSONObject;

public class DetailedAppStatus extends JSONObjectResult {
	
	private static final String SEVERITY_KEY = "severity";
	private static final String MESSAGE_KEY = "message";
	private static final String NOTIFY_KEY = "notify";
	private static final String NOTIFICATION_ID_KEY = "notificationID";
	
	public enum Severity {
		ERROR("ERROR", Messages.SeverityError),
		WARNING("WARN", Messages.SeverityWarning),
		INFO("INFO", Messages.SeverityInfo);

		public final String level;
		public final String displayString;

		private Severity(String level, String displayString) {
			this.level = level;
			this.displayString = displayString;
		}
		
		public static Severity get(String severity) {
			for (Severity sev : Severity.values()) {
				if (sev.level.equals(severity)) {
					return sev;
				}
			}
			Logger.logError("Unrecognized severity: " + severity);
			return null;
		}

		public String getDisplayString(StartMode mode) {
			return displayString;
		}
	};
	
	public DetailedAppStatus(JSONObject connectionInfo) {
		super(connectionInfo, "detailed app status");
	}
	
	public Severity getSeverity() {
		String severity = getString(SEVERITY_KEY);
		if (severity != null) {
			return Severity.get(severity);
		}
		return null;
	}
	
	public String getMessage() {
		String msg = getString(MESSAGE_KEY);
		return msg == null || msg.isEmpty() ? null : msg;
	}
	
	public boolean getNotify() {
		return getBoolean(NOTIFY_KEY);
	}

	public String getNotificationID() {
		String id = getString(NOTIFICATION_ID_KEY);
		return id == null || id.isEmpty() ? null : id;
	}
}
