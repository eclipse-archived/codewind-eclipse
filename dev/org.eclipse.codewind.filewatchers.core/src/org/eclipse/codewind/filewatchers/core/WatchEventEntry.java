/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.codewind.filewatchers.core;

import java.nio.file.Path;

/**
 * Corresponds to an file/directory change event from the operating system: a
 * creation/modification/deletion of a file, or a creation/deletion of a
 * directory.
 */
public class WatchEventEntry {

	public static enum EventType {
		CREATE, MODIFY, DELETE
	};

	private final EventType eventType;

	private final Path path;

	private final String absolutePath;

	private final boolean directory;

	public WatchEventEntry(EventType eventType, Path path, boolean directory) {
		this.eventType = eventType;
		this.path = path;
		this.directory = directory;

		// Take the OS-specific path format, and convert it to a standardized non-OS
		// specific format. This should only effect Windows paths.
		absolutePath = PathUtils.normalizePath(path.toFile().getPath());

	}

	public EventType getEventType() {
		return eventType;
	}

	public String getAbsolutePathWithUnixSeparators() {
		return absolutePath;
	}

	public Path getLocalOSSpecificPath() {
		return path;
	}

	public boolean isDirectory() {
		return directory;
	}

	// For debug purposes only
	@Override
	public String toString() {
		return "[" + eventType.name() + "] " + absolutePath;
	}
}
