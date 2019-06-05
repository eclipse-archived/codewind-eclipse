/*******************************************************************************
 * Copyright (c) 2018, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.codewind.core.internal.constants;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.codewind.core.internal.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Represents the capabilities of a project.
 */
public class ProjectCapabilities {
	
	public static final ProjectCapabilities emptyCapabilities = new ProjectCapabilities();
	
	private final Set<StartMode> startModes = new HashSet<StartMode>();
	private final Set<ControlCommand> controlCommands = new HashSet<ControlCommand>();
	
	public enum StartMode {
		RUN("run"),
		DEBUG("debug"),
		DEBUG_NO_INIT("debugNoInit");
		
		private final String name;
		
		private StartMode(String name) {
			this.name = name;
		}
		
		public String getName() {
			return name;
		}
		
		public static StartMode getStartMode(String name) {
			for (StartMode mode : StartMode.values()) {
				if (mode.name.equals(name)) {
					return mode;
				}
			}
			return null;
		}
	};
	
	public enum ControlCommand {
		RESTART("restart");
		
		private final String name;
		
		private ControlCommand(String name) {
			this.name = name;
		}
		
		public String getName() {
			return name;
		}
		
		public static ControlCommand getControlCommand(String name) {
			for (ControlCommand command : ControlCommand.values()) {
				if (command.name.equals(name)) {
					return command;
				}
			}
			return null;
		}
	}
	
	public ProjectCapabilities(JSONObject capabilities) {
		try {
			if (capabilities.has(CoreConstants.KEY_START_MODES)) {
				JSONArray modes = capabilities.getJSONArray(CoreConstants.KEY_START_MODES);
				for (int i = 0; i < modes.length(); i++) {
					StartMode startMode = StartMode.getStartMode(modes.getString(i));
					if (startMode != null) {
						this.startModes.add(startMode);
					}
				}
			}
		} catch (JSONException e) {
			Logger.logError("Failed to parse the start mode capabilities.", e);
		}
		
		try {
			if (capabilities.has(CoreConstants.KEY_CONTROL_COMMANDS)) {
				JSONArray commands = capabilities.getJSONArray(CoreConstants.KEY_CONTROL_COMMANDS);
				for (int i = 0; i < commands.length(); i++) {
					ControlCommand controlCommand = ControlCommand.getControlCommand(commands.getString(i));
					if (controlCommand != null) {
						this.controlCommands.add(controlCommand);
					}
				}
			}
		} catch (JSONException e) {
			Logger.logError("Failed to parse the control command capabilities.", e);
		}
	}
	
	private ProjectCapabilities() {
		// Intentionally empty
	}
	
	public boolean canRestart() {
		return controlCommands.contains(ControlCommand.RESTART);
	}
	
	public boolean supportsDebugMode() {
		return startModes.contains(StartMode.DEBUG);
	}
	
	public boolean supportsDebugNoInitMode() {
		return startModes.contains(StartMode.DEBUG_NO_INIT);
	}

}
