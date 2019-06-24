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

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * This interface is an abstraction over platform-specific mechanisms for
 * detecting file/directory changes of a directory, in a lightweight fashion. On
 * Linux, this uses the inotify API, and Windows/Mac have similar
 * platform-specific APIs.
 * 
 * Java exposes this functionality in the java.nio.file.WatchService class, but
 * you may implement this IPlatformWatchService interface to hook into the
 * IDE-specific mechanisms for detecting class changes, such as Eclipse
 * resources listeners.
 * 
 * See JavaNioWatchService for an example implementation.
 */
public interface IPlatformWatchService {

	/**
	 * Add a path (and its subdirectories) to the list of directories to monitor.
	 * After this call, any changes to files under this path should call the
	 * IPlatformWatchListener listeners to be called.
	 * 
	 * Method is called by FileWatcher to inform the watch service of a new
	 * directory to watch.
	 */
	public void addPath(File f, ProjectToWatch ptw) throws IOException;

	/**
	 * Remove a path that was previously added with above path; ends monitoring on
	 * the directory (and its subdirectories). The watch service should no longer
	 * report any events under this path after this call.
	 * 
	 * Method is called by FileWatcher to inform the watch service that a watched
	 * directory should no longer be watched.
	 */
	public void removePath(File f, ProjectToWatch ptw);

	/**
	 * When a file change is detected, these listeners will be called with the list
	 * of changes. Implementing this interface is the responsibility of Filewatcher
	 * rather than the watch service.
	 * 
	 * Method is called by FileWatcher to allow FileWatcher to be informed of events
	 * from the watch service.
	 */
	public void addListener(IPlatformWatchListener gwl);

	/**
	 * Implementations of this interface receive a list of changes. Implementing
	 * this interface is the responsibility of Filewatcher rather than the watch
	 * service (but the watch service still needs to ensure that listeners are
	 * called at the appropriate times.)
	 */
	public static interface IPlatformWatchListener {

		/** Call this when one or more file changes are detected. */
		public void changeDetected(List<WatchEventEntry> entries);

		/** Call this when the watch succeeds or fails. */
		public void watchAdded(ProjectToWatch ptw, boolean success);
	}

	/**
	 * Dispose of any file watchers that are associated with this watch service, and
	 * any other associated resources (threads, buffers, etc).
	 * 
	 * Should be called by the watch service consumer when the Codewind connection
	 * is disposed (for example, if the workbench is stopping, or the user has
	 * deleted the server entry).
	 */
	void dispose();

	/** Generate an implementation-defined String representation of the internal state of the watcher. */
	public String generateDebugState();
}
