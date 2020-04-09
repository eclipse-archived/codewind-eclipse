/*******************************************************************************
 * Copyright (c) 2018, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.codewind.core.internal.constants;

import java.util.EnumSet;

import org.eclipse.codewind.core.internal.Logger;
import org.json.JSONObject;

/**
 * Project start modes.
 */
public enum StartMode {

	RUN("run"),
	DEBUG("debug"),
	DEBUG_NO_INIT("debugNoInit");
	
	public static final EnumSet<StartMode> DEBUG_MODES = EnumSet.of(DEBUG, DEBUG_NO_INIT);

	public final String startMode;

	private StartMode(String startMode) {
		this.startMode = startMode;
	}

	public boolean equals(String s) {
		return this.name().equals(s);
	}
	
	public boolean isDebugMode() {
		return DEBUG_MODES.contains(this);
	}
	
	public static StartMode get(String startMode) {
		for (StartMode mode : StartMode.values()) {
			if (mode.startMode.equals(startMode)) {
				return mode;
			}
		}
		return null;
	}
	
	public static StartMode get(JSONObject obj) {
		try {
			String mode = null;
			if (obj.has(CoreConstants.KEY_START_MODE)) {
				mode = obj.getString(CoreConstants.KEY_START_MODE);
			}
			if (mode == null) {
				Logger.log("No start mode was specified on JSON object");
				return StartMode.RUN;
			} else {
				StartMode startMode = StartMode.get(mode);
				if (startMode == null) {
					Logger.log("Unrecognized start mode: " + mode);
					return StartMode.RUN;
				}
				return startMode;
			}
		} catch (Exception e) {
			Logger.logError("Failed to get start mode", e);
		}
		return StartMode.RUN;
	}
}