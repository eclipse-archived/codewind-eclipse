/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.codewind.filewatchers.core.internal;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.eclipse.codewind.filewatchers.core.FWLogger;
import org.eclipse.codewind.filewatchers.core.Filewatcher;
import org.eclipse.codewind.filewatchers.core.FilewatcherUtils;
import org.eclipse.codewind.filewatchers.core.PathUtils;
import org.eclipse.codewind.filewatchers.core.WatchEventEntry;
import org.eclipse.codewind.filewatchers.core.WatchEventEntry.EventType;
import org.eclipse.codewind.filewatchers.core.internal.FileChangeEventBatchUtil.ChangedFileEntry;

/**
 * This class is used to watch a small number of individual files, for example,
 * linked files defined in the 'refPaths' field of a watched project. For a
 * large number of files to watch, the watch service should be used instead.
 * 
 * Files watched by this class do not need to exist
 * 
 * A single instance of this class will exist per filewatcher (eg it is not per
 * project).
 * 
 * This class was introduced as part of 'Project sync support for reference to
 * files outside of project folder ' (codewind/1399).
 */
public class IndividualFileWatchService {

	private static final FWLogger log = FWLogger.getInstance();

	private final Map<String /* project id */, Map<String /* absolute path */, PollEntry> /* linked files */> filesToWatchMap_synch = new HashMap<>();

	private final IndivFileWatchThread thread = new IndivFileWatchThread();

	private final AtomicBoolean disposed = new AtomicBoolean(false);

	private final Filewatcher parent;

	public IndividualFileWatchService(Filewatcher parent) {

		this.parent = parent;

		thread.setName(IndivFileWatchThread.class.getSimpleName());
		thread.setDaemon(true);
		thread.start();

	}

	/**
	 * This method is called by the filewatcher to inform us of potential changes to
	 * the list of files to watch. In this method we synchronize the paths that FW
	 * is telling us we should be watching, with what we are currently watching.
	 */
	public void setFilesToWatch(String projectId, List<String> pathsFromPtw) {

		List<Path> paths = pathsFromPtw.stream()
				.map(e -> PathUtils.convertAbsoluteUnixStyleNormalizedPathToLocalFile(e)).map(e -> Paths.get(e))
				.collect(Collectors.toList());

		paths = paths.stream().filter(e -> {
			boolean isDirectory = Files.exists(e) && Files.isDirectory(e);

			if (isDirectory) {
				log.logError(
						"Project '" + projectId + "' was asked to watch a directory, which is not supported: " + e);
			}

			return !isDirectory;
		}).collect(Collectors.toList());

		synchronized (filesToWatchMap_synch) {

			// Handle project removal (indicated by empty path list) or empty paths
			if (paths.size() == 0) {
				filesToWatchMap_synch.remove(projectId);
				filesToWatchMap_synch.notify();
				return;
			}

			boolean mapUpdated = false;

			Map<String, PollEntry> currProjectState = filesToWatchMap_synch.get(projectId);
			if (currProjectState == null) {
				// This is a new project we haven't seen.

				Map<String, PollEntry> newFiles = new HashMap<>();

				paths.stream().map(e -> new PollEntry(PollEntry.Status.RECENTLY_ADDED, e, null)).forEach(e -> {

					log.logInfo("Files to watch - recently added for new project: " + e.absolutePath + "");
					newFiles.put(e.absolutePath.toString(), e);
				});
				mapUpdated = true;

				filesToWatchMap_synch.put(projectId, newFiles);

			} else {
				// This is an existing project with at least one file we are currently
				// monitoring.

				for (Path paramPath : paths) {

					String paramPathString = paramPath.toString();
					PollEntry pe = currProjectState.get(paramPathString);
					if (pe == null) {

						log.logInfo("Files to watch - recently added for existing project: " + paramPath);

						// The path is not in current state, so add it
						currProjectState.put(paramPathString,
								new PollEntry(PollEntry.Status.RECENTLY_ADDED, paramPath, null));

						mapUpdated = true;
					} else {
						/* Ignore: the path is in both maps -- no change. */
					}

				}

				HashSet<String> pathsInParam = new HashSet<>(
						paths.stream().map(e -> e.toString()).collect(Collectors.toList()));

				// Look for values that are in curr project state, but not in the parameter
				// list. These are files that we WERE watching, but are no longer.
				for (Iterator<Entry<String, PollEntry>> it = currProjectState.entrySet().iterator(); it.hasNext();) {

					Entry<String /* path */, PollEntry> currProjStateEntry = it.next();
					String pathInCurrentState = currProjStateEntry.getKey();

					if (!pathsInParam.contains(pathInCurrentState)) {
						// pathInCurrentState is no longer in the the path list, so remove it our
						// internal state of files to watch for this project.
						it.remove();
						log.logInfo("Files to watch - removing from watch list: " + pathInCurrentState + "");
						mapUpdated = true;
					}

				}

				// If we're not watching anything anymore, remove the project from the state
				// list.
				if (currProjectState.isEmpty()) {
					filesToWatchMap_synch.remove(projectId);
				}

			} // end 'existing project' else

			if (mapUpdated) {
				filesToWatchMap_synch.notify();

			}

		} // end synchronized

	} // end set files

	public void dispose() {

		if (disposed.get()) {
			return;
		}

		disposed.set(true);

		FilewatcherUtils.newThread(() -> {
			synchronized (filesToWatchMap_synch) {
				filesToWatchMap_synch.clear();
				filesToWatchMap_synch.notify();
			}
		});

	}

	/* A single instance of this thread exists per Codewind server connection. */
	private class IndivFileWatchThread extends Thread {

		@Override
		public void run() {

			while (!disposed.get()) {

				try {
					innerRun();

					synchronized (filesToWatchMap_synch) {
						filesToWatchMap_synch.wait(1000);
					}

				} catch (Throwable t) {
					log.logError("Unexpected error thrown in polling thread, ignoring.", t);
				}

			}

		}

		private void innerRun() {
			Map<String /* project id */, Set<ChangedFileEntry>> fileChangesDetected = new HashMap<>();

			Map<String /* project id */, List<PollEntry>> localMap = new HashMap<>();

			synchronized (filesToWatchMap_synch) {

				filesToWatchMap_synch.forEach((projectId, watchFileState) -> {
					localMap.put(projectId, new ArrayList<PollEntry>(watchFileState.values()));
				});

			}

			// For each project we are monitoring...
			localMap.forEach((projectId, filesToWatch) -> {

				// For each watched file in that project...
				for (PollEntry fileToWatch : filesToWatch) {

					boolean fileExists = Files.exists(fileToWatch.absolutePath);
					PollEntry.Status newStatus = fileExists ? PollEntry.Status.EXISTS : PollEntry.Status.DOES_NOT_EXIST;

					Long fileModifiedTime = null;
					try {
						if (fileExists) {
							fileModifiedTime = Files.getLastModifiedTime(fileToWatch.absolutePath).toMillis();
						}
					} catch (IOException e) {
						log.logError("Unexpected error on retrieving last modified time for '"
								+ fileToWatch.absolutePath + "', ignoring.", e);
					}

					if (fileToWatch.lastObservedStatus != PollEntry.Status.RECENTLY_ADDED) {

						if (fileToWatch.lastObservedStatus != newStatus) {

							WatchEventEntry.EventType type;

							if (fileExists) {
								// ADDED: Last time we saw this file it did not exist, but now it does.
								log.logInfo("Watched file now exists: " + fileToWatch.absolutePath);
								type = EventType.CREATE;
							} else {
								// DELETED: Last time we saw this file it did exist, but it no longer does.
								log.logInfo("Watched file has been deleted: " + fileToWatch.absolutePath);
								type = EventType.DELETE;
							}
							Set<ChangedFileEntry> changedFiles = fileChangesDetected.computeIfAbsent(projectId,
									e -> new HashSet<>());

							changedFiles.add(new ChangedFileEntry(fileToWatch.absolutePath.toString(), false, type,
									System.currentTimeMillis()));

						}

						if (fileModifiedTime != null && fileToWatch.lastModifiedDate != null
								&& !fileModifiedTime.equals(fileToWatch.lastModifiedDate)) {
							// CHANGED: Last time we same this file it had a different modified time.
							log.logInfo("Watched file change detected: " + fileToWatch.absolutePath + " "
									+ fileModifiedTime + " " + fileToWatch.lastModifiedDate);

							Set<ChangedFileEntry> changedFiles = fileChangesDetected.computeIfAbsent(projectId,
									e -> new HashSet<>());

							changedFiles.add(new ChangedFileEntry(fileToWatch.absolutePath.toString(), false,
									WatchEventEntry.EventType.MODIFY, System.currentTimeMillis()));

						}

					}

					fileToWatch.lastObservedStatus = newStatus;
					fileToWatch.lastModifiedDate = fileModifiedTime;

				}

			}); // end localMap foreach

			fileChangesDetected.forEach((projectId, paths) -> {
				if (paths.size() == 0) {
					return;
				}
				parent.internal_receiveIndividualChangesFileList(projectId, paths);
			});

		}
	}

	/**
	 * Struct class containing the most recent observed state of a file we have been
	 * told to watch
	 */
	private static class PollEntry {

		enum Status {
			/**
			 * We were recently told to watch this file and thus have not yet observed a
			 * state for it
			 */
			RECENTLY_ADDED,

			/** File exists, last time we checked it */
			EXISTS,

			/** File did not exist, last time we checked it */
			DOES_NOT_EXIST
		}

		public PollEntry(Status lastObservedStatus, Path absolutePath, Long lastModifiedDate) {
			this.lastObservedStatus = lastObservedStatus;
			this.absolutePath = absolutePath;
			this.lastModifiedDate = lastModifiedDate;
		}

		Status lastObservedStatus;

		final Path absolutePath;

		// Null if the file doesn't exist, or if the status is RECENTLY_ADDED
		Long lastModifiedDate;

	}

}
