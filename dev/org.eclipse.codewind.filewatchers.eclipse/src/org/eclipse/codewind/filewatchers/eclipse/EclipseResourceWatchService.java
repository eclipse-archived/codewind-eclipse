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

package org.eclipse.codewind.filewatchers.eclipse;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.codewind.filewatchers.core.FWLogger;
import org.eclipse.codewind.filewatchers.core.FilewatcherUtils;
import org.eclipse.codewind.filewatchers.core.IPlatformWatchService;
import org.eclipse.codewind.filewatchers.core.PathFilter;
import org.eclipse.codewind.filewatchers.core.PathUtils;
import org.eclipse.codewind.filewatchers.core.ProjectToWatch;
import org.eclipse.codewind.filewatchers.core.WatchEventEntry;

public class EclipseResourceWatchService implements IPlatformWatchService {

	/**
	 * NOTE: Any variable ending with '_synch' suffix should be synchronized on when
	 * accessing.
	 */

	private final Map<String /* project Id to Watched Path */, WatchedPath> projIdToWatchedPaths_synch = new HashMap<>();

	private final List<IPlatformWatchListener> listeners_synch = new ArrayList<>();

	private static final FWLogger log = FWLogger.getInstance();

	private final AtomicBoolean disposed_synch = new AtomicBoolean(false);

	public EclipseResourceWatchService() {
	}

	@Override
	public void addListener(IPlatformWatchListener gwl) {
		log.logDebug("Listener added to " + this.getClass().getSimpleName());

		synchronized (listeners_synch) {
			listeners_synch.add(gwl);
		}
	}

	@Override
	public void addPath(File f, ProjectToWatch ptw) throws IOException {

		synchronized (disposed_synch) {
			if (disposed_synch.get()) {
				return;
			}
		}

		log.logInfo("Path '" + f.getPath() + "' added to " + this.getClass().getSimpleName());

		String key = ptw.getProjectId();

		synchronized (projIdToWatchedPaths_synch) {
			WatchedPath value = projIdToWatchedPaths_synch.get(key);
			if (value != null) {
				value.stopWatching();
			}
			projIdToWatchedPaths_synch.put(key, new WatchedPath(f, ptw, this));

		}

	}

	@Override
	public void removePath(File f, ProjectToWatch ptw) {

		synchronized (disposed_synch) {
			if (disposed_synch.get()) {
				return;
			}
		}

		String key = ptw.getProjectId();

		synchronized (projIdToWatchedPaths_synch) {
			WatchedPath value = projIdToWatchedPaths_synch.remove(key);
			if (value != null) {
				log.logInfo("Path '" + f.getPath() + "' removed from " + this.getClass().getSimpleName());
				value.stopWatching();
			} else {
				log.logError("Path '" + f.getPath() + "' attempted to be removed, but could not be found, from"
						+ this.getClass().getSimpleName());
			}
		}
	}

	@Override
	public void dispose() {

		// Only dispose once.
		synchronized (disposed_synch) {
			if (disposed_synch.get()) {
				return;
			}

			disposed_synch.set(true);
		}

		log.logInfo("dispose() called on " + this.getClass().getSimpleName());

		List<WatchedPath> toDispose = new ArrayList<>();

		synchronized (projIdToWatchedPaths_synch) {
			toDispose.addAll(projIdToWatchedPaths_synch.values());
			projIdToWatchedPaths_synch.clear();
		}

		toDispose.forEach(e -> {
			FilewatcherUtils.newThread(() -> {
				e.stopWatching();
			});
		});

	}

	public void receiveWatchEntries(String cwProjectId, List<WatchEventEntry> entries) {

		// Locate the watched path corresponding to the given project ID
		WatchedPath wp;
		synchronized (projIdToWatchedPaths_synch) {

			wp = projIdToWatchedPaths_synch.get(cwProjectId);
			if (wp == null) {
				// TODO: If this is printed for projects that are not managed by Codewind, then
				// just comment this out.
				System.err.println("Could not find project with ID '" + cwProjectId + "' in list.");
				return;
			}
		}

		wp.receiveFileChanges(entries);

	}

	private static class WatchedPath {

		private final PathFilter pathFilter;

		private final EclipseResourceWatchService parent;

		private final String pathInNormalizedForm;

		private final File pathRoot;

		public WatchedPath(File pathRoot, ProjectToWatch projectToWatch, EclipseResourceWatchService parent)
				throws IOException {

			this.pathInNormalizedForm = PathUtils.normalizePath(pathRoot.getPath());

			this.pathFilter = new PathFilter(projectToWatch);
			this.parent = parent;

			this.pathRoot = pathRoot;

			List<IPlatformWatchListener> listeners = new ArrayList<>();
			synchronized (parent.listeners_synch) {
				listeners.addAll(parent.listeners_synch);
			}

			for (IPlatformWatchListener pw : listeners) {
				pw.watchAdded(projectToWatch, true);
			}

		}

		private void receiveFileChanges(List<WatchEventEntry> entries) {

			List<WatchEventEntry> newEvents = new ArrayList<>();

			for (WatchEventEntry wee : entries) {

				String relativePath = PathUtils.convertAbsolutePathWithUnixSeparatorsToProjectRelativePath(
						wee.getAbsolutePathWithUnixSeparators(), pathInNormalizedForm).orElse(null);

				// Ignore events that are filtered by the watch filter.
				if (!isFilteredOut(relativePath, pathFilter)) {

					newEvents.add(wee);
				}

			}

			if (newEvents.size() > 0) {
				// Copy listeners from parent and changes to the listeners
				List<IPlatformWatchListener> listeners = new ArrayList<>();
				synchronized (parent.listeners_synch) {
					listeners.addAll(parent.listeners_synch);
				}

				// Inform listeners of changes
				for (IPlatformWatchListener gwl : listeners) {
					gwl.changeDetected(newEvents);
				}
			}

		}

		/**
		 * Returns true if matched by a filter (and therefore should be filtered out),
		 * false otherwise.
		 */
		private static boolean isFilteredOut(String relativePath, PathFilter pathFilter) {

			if (relativePath == null || relativePath.trim().length() <= 1) {
				return false;
			}

			if (pathFilter.isFilteredOutByFilename(relativePath)) {
				return true;
			}

			List<String> pathsToProcess = PathUtils.splitRelativeProjectPathIntoComponentPaths(relativePath);

			for (String path : pathsToProcess) {

				// Ignore events that are filtered by the watch filter.
				if (pathFilter.isFilteredOutByPath(path)) {
					return true;
				}

			}

			return false;

		}

		public void stopWatching() {
			/** No disposal needed. */
		}

		public File getPathRoot() {
			return pathRoot;
		}

	}

	@Override
	public String generateDebugState() {

		StringBuilder result = new StringBuilder();

		synchronized (projIdToWatchedPaths_synch) {

			projIdToWatchedPaths_synch.forEach((k, v) -> {
				result.append("- " + k + " " + v.getPathRoot().getPath() + "\n");
			});

		}

		return result.toString();
	}
}
