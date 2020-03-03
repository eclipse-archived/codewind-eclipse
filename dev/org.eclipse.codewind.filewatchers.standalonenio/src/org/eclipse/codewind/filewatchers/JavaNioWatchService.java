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

package org.eclipse.codewind.filewatchers;

import java.io.File;
import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.eclipse.codewind.filewatchers.core.FWLogger;
import org.eclipse.codewind.filewatchers.core.FilewatcherUtils;
import org.eclipse.codewind.filewatchers.core.IPlatformWatchService;
import org.eclipse.codewind.filewatchers.core.PathFilter;
import org.eclipse.codewind.filewatchers.core.PathUtils;
import org.eclipse.codewind.filewatchers.core.ProjectToWatch;
import org.eclipse.codewind.filewatchers.core.WatchEventEntry;

/**
 * This class uses the directory/file monitoring functionality that is built
 * into the JVM as part of java.nio.file.WatchService.
 * 
 * See IPlatformWatchService for more information.
 */
public class JavaNioWatchService implements IPlatformWatchService {

	/**
	 * NOTE: Any variable ending with '_synch' suffix should be synchronized on when
	 * accessing.
	 */

	private final Map<String /* project id */, WatchedPath> watchedProjects_synch = new HashMap<>();

	private final List<IPlatformWatchListener> listeners_synch = new ArrayList<>();

	private static final FWLogger log = FWLogger.getInstance();

	private final static boolean DEBUG = log.isDebug();

	private AtomicBoolean disposed_synch = new AtomicBoolean(false);

	public JavaNioWatchService() {
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

		synchronized (watchedProjects_synch) {
			WatchedPath value = watchedProjects_synch.get(key);
			if (value != null) {
				value.stopWatching();
			}
			watchedProjects_synch.put(key, new WatchedPath(f, ptw, this));
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

		synchronized (watchedProjects_synch) {
			WatchedPath value = watchedProjects_synch.remove(key);
			if (value != null) {
				log.logInfo("Path '" + f.getPath() + "' removed from " + this.getClass().getSimpleName()
						+ " for project " + ptw.getProjectId());
				value.stopWatching();
			} else {
				log.logError("Path '" + f.getPath() + "' attempted to be removed, but could not be found, from"
						+ this.getClass().getSimpleName() + " for project " + ptw.getProjectId());
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

		synchronized (watchedProjects_synch) {
			toDispose.addAll(watchedProjects_synch.values());
			watchedProjects_synch.clear();
		}

		toDispose.forEach(e -> {
			FilewatcherUtils.newThread(() -> {
				e.stopWatching();
			});
		});

	}

	@Override
	public String generateDebugState() {

		StringBuilder result = new StringBuilder();

		synchronized (watchedProjects_synch) {

			watchedProjects_synch.forEach((k, v) -> {
				result.append("- " + k + " | " + v.getPathRoot().getPath() + "\n");
			});

		}

		return result.toString();
	}

	/**
	 * Each WatchedPath has a corresponding thread that listens for file/directory
	 * changes.
	 */
	private static class WatchedPathThread extends Thread {
		private static final FWLogger log = FWLogger.getInstance();

		private final WatchedPath watchedPath;

		private WatchedPathThread(WatchedPath watchedPath) {
			this.watchedPath = watchedPath;
		}

		@Override
		public void run() {
			try {
				if (JavaNioWatchService.DEBUG) {
					log.logDebug("Generic watch service thread for '" + watchedPath.pathRoot + "' started.");
				}
				watchedPath.eventLoopCatchAll();
			} catch (Throwable t) {

				log.logSevere("WatchPathThreadDied", t, null);

			} finally {
				if (JavaNioWatchService.DEBUG) {
					log.logDebug("Generic watch service thread for '" + watchedPath.pathRoot + "' ended.");
				}
			}
		}

	}

	/**
	 * The entry in WatchService for an individual directory to (recursively) watch.
	 * There should be a 1-1 relationship between WatchPath objects and projects
	 * that are monitored on behalf of the server.
	 * 
	 * Change events are passed to any attached listeners.
	 */
	private static class WatchedPath {
		private final File pathRoot;

		private final String pathInNormalizedForm;

		private final WatchService watchService;
		private final Map<WatchKey, Path> keys = new HashMap<>();

		private final Map<Path, Boolean> watchedPaths = new HashMap<>();

		private final WatchedPathThread thread;

		private final JavaNioWatchService parent;

		private final FWLogger log = FWLogger.getInstance();

		private final PathFilter pathFilter;

		private final ProjectToWatch projectToWatch;

		private boolean threadActive = true;

		public WatchedPath(File pathRoot, ProjectToWatch projectToWatch, JavaNioWatchService parent)
				throws IOException {
			this.pathRoot = pathRoot;

			this.pathInNormalizedForm = PathUtils.normalizePath(pathRoot.getPath());
			this.parent = parent;
			this.projectToWatch = projectToWatch;

			this.pathFilter = new PathFilter(projectToWatch);

			Path path = pathRoot.toPath();

			watchService = path.getFileSystem().newWatchService();

			thread = new WatchedPathThread(this);
			thread.start();

		}

		/** Add a watch for the directory, then add the files found inside of it */
		private void addDirectory(Path path, List<File> filesFound) throws IOException {

			if (watchedPaths.containsKey(path)) {
				return;
			}

			String normalizedPath = PathUtils.normalizePath(path.toFile().getPath());

			String relativePath = PathUtils
					.convertAbsolutePathWithUnixSeparatorsToProjectRelativePath(normalizedPath, pathInNormalizedForm)
					.orElse(null);

			if (relativePath != null) {
				if (pathFilter.isFilteredOutByFilename(relativePath)) {
					log.logDebug("Filtering out " + path + " due to filename");
					return;
				} else if (pathFilter.isFilteredOutByPath(relativePath)) {
					log.logDebug("Filtering out " + path + " due to path.");
					return;
				}
			}

			watchedPaths.put(path, true);

			WatchKey key = path.register(watchService, StandardWatchEventKinds.ENTRY_CREATE,
					StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_DELETE);

			keys.put(key, path);

			// Any files found in the directory should also be reported as created.
			File[] farr = path.toFile().listFiles();
			if (farr == null) {
				log.logDebug("Added directory: " + path + " files found: " + farr + " " + path.toFile().exists());
				return;
			}

			for (File f : farr) {
				filesFound.add(f);
				if (f.isDirectory()) {
					addDirectory(f.toPath(), filesFound);
				}
			}

			log.logDebug("Added directory: " + path + " files found: " + farr.length);

		}

		private void addDirectoryRecursive(final Path path, List<File> filesFound) throws IOException {
			if (JavaNioWatchService.DEBUG) {
				log.logDebug("Recursively adding directory: " + path);
			}

			addDirectory(path, filesFound);

			if (JavaNioWatchService.DEBUG) {
				log.logDebug("Completed recursively adding directory: " + path);
			}

		}

		public void stopWatching() {
			threadActive = false;
			FilewatcherUtils.newThread(() -> {
				try {
					thread.interrupt();
					watchService.close();
				} catch (IOException e) {
					/* ignore */
				}
			});
		}

		/**
		 * Wait for the watch path to become available, then wrap the main event loop
		 * and prevent it from terminating the thread.
		 */
		private void eventLoopCatchAll() {

			// Wait for the directory to exist, and be valid.
			if (!waitForWatchedPathSuccess()) {
				return;
			}

			while (threadActive) {

				try {

					eventLoop();

				} catch (InterruptedException e) {
					// This is expected, and likely due to stopWatching(...)
					log.logDebug("Watch service interupted");
					return;
				} catch (Exception e) {

					if (e instanceof ClosedWatchServiceException && !threadActive) {
						// Ignore CWSE in the dispose case.
						return;
					}

					// Catch the exception, log it, and continue.
					log.logSevere("Unexpected event loop exception in " + this.getClass().getSimpleName(), e, null);
				}
			}

		}

		@SuppressWarnings({ "rawtypes", "unchecked" })
		private void eventLoop() throws InterruptedException {

			while (threadActive) {

				WatchKey key;
				key = watchService.take();

				Path dir = keys.get(key);
				if (dir == null) {
					System.err.println("WatchKey not recognized!!");
					continue;
				}

				List<WatchEventEntry> newEvents = new ArrayList<>();

				for (WatchEvent<?> event : key.pollEvents()) {
					WatchEvent.Kind kind = event.kind();

					// Context for directory entry event is the file name of entry
					Path name = ((WatchEvent<Path>) event).context();
					Path child = dir.resolve(name);

					boolean isDirectory = Files.isDirectory(child);

					WatchEventEntry we = null;
					if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE) {
						we = new WatchEventEntry(WatchEventEntry.EventType.CREATE, child, isDirectory);
					} else if (event.kind() == StandardWatchEventKinds.ENTRY_DELETE) {
						we = new WatchEventEntry(WatchEventEntry.EventType.DELETE, child, isDirectory);
					} else if (event.kind() == StandardWatchEventKinds.ENTRY_MODIFY) {

						// Only process MODIFY events for files, not for directories.
						if (!Files.isDirectory(child)) {
							we = new WatchEventEntry(WatchEventEntry.EventType.MODIFY, child, isDirectory);
						}

					}

					// if directory is created, and watching recursively, then register it
					// and its sub-directories
					if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
						try {
							if (Files.isDirectory(child)) {
								List<File> filesFound = new ArrayList<>();
								addDirectoryRecursive(child, filesFound);

								for (File e : filesFound) {
									newEvents.add(new WatchEventEntry(WatchEventEntry.EventType.CREATE, e.toPath(),
											e.isDirectory()));
								}
							}
						} catch (IOException x) {
							log.logSevere("Error during recursive directory add", x, null);
						}
					}

					if (we != null) {
						newEvents.add(we);
					}

				} // end for

				// Reset key and remove from set if directory no longer accessible
				boolean valid = key.reset();
				if (!valid) {
					keys.remove(key);
					watchedPaths.remove(dir);

					if (!dir.toFile().exists()) {
						newEvents.add(new WatchEventEntry(WatchEventEntry.EventType.DELETE, dir, true));
					}

					// All directories are inaccessible
					if (keys.isEmpty()) {
						if (this.pathRoot.exists()) {
							log.logSevere(
									"The watch service has nothing to watch, but the path root still exists. This should never happen. "
											+ projectToWatch.getProjectId());
						} else {
							log.logInfo(
									"The watch service has nothing to watch, so the thread is stopping in 30 seconds. "
											+ projectToWatch.getProjectId());
							FilewatcherUtils.newThread(() -> {
								FilewatcherUtils.sleepIgnoreInterrupt(30 * 1000);
								log.logInfo("The watch service has nothing to watch, so the thread is now stopping: "
										+ projectToWatch.getProjectId());
								stopWatching();
							});
						}
					}
				}

				// Remove events that are filtered out by the filters
				newEvents = newEvents.stream().filter(e -> {

					String relativePath = PathUtils.convertAbsolutePathWithUnixSeparatorsToProjectRelativePath(
							e.getAbsolutePathWithUnixSeparators(), pathInNormalizedForm).orElse(null);

					// Ignore events that are filtered by the watch filter.
					return relativePath == null || (!pathFilter.isFilteredOutByFilename(relativePath)
							&& !pathFilter.isFilteredOutByPath(relativePath));

				}).collect(Collectors.toList());

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

			} // end while
		}

		private boolean waitForWatchedPathSuccess() {
			long expireTimeInNanos = System.nanoTime() + TimeUnit.NANOSECONDS.convert(5, TimeUnit.MINUTES);

			// Wait for the directory to exist, and be valid.
			boolean watchSuccess = false;

			Long nextStatusPrintInNanos = null;

			while (threadActive) {
				boolean success = pathRoot.exists() && pathRoot.isDirectory() && pathRoot.canRead();

				if (success) {
					watchSuccess = true;
					break;
				} else {

					if (nextStatusPrintInNanos == null) {
						nextStatusPrintInNanos = System.nanoTime() + TimeUnit.NANOSECONDS.convert(1, TimeUnit.SECONDS);
					} else if (System.nanoTime() > nextStatusPrintInNanos) {
						nextStatusPrintInNanos = null;
						log.logInfo("Waiting for " + pathRoot + " to exist, and be accessible.");
					}

					FilewatcherUtils.sleep(50);
				}

				if (System.nanoTime() > expireTimeInNanos) {
					watchSuccess = false;
					break;
				}
			}

			if (!threadActive) {
				return false;
			}

			if (watchSuccess) {
				try {
					addDirectoryRecursive(pathRoot.toPath(), new ArrayList<File>());
				} catch (IOException e) {
					log.logError("Unable to watch directory: " + pathRoot.getPath(), e);
					watchSuccess = false;
				}
			}

			// Inform the listeners if the watch has succeeded or failed.
			List<IPlatformWatchListener> listeners = new ArrayList<>();
			synchronized (parent.listeners_synch) {
				listeners.addAll(parent.listeners_synch);
			}
			for (IPlatformWatchListener pw : listeners) {
				pw.watchAdded(projectToWatch, watchSuccess);
			}

			if (watchSuccess) {
				log.logInfo("Watch succeeded on " + pathRoot.getPath() + " for " + projectToWatch.getProjectId());
			} else {
				log.logError("Watch failed on " + pathRoot.getPath() + " for " + projectToWatch.getProjectId());
			}

			return watchSuccess;
		}

		public File getPathRoot() {
			return pathRoot;
		}
	}

}
