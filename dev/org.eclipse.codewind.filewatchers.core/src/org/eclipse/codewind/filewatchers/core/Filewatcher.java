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
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.eclipse.codewind.filewatchers.core.FilewatcherUtils.ExponentialBackoffUtil;
import org.eclipse.codewind.filewatchers.core.IPlatformWatchService.IPlatformWatchListener;
import org.eclipse.codewind.filewatchers.core.ProjectToWatch.ProjectToWatchFromWebSocket;
import org.eclipse.codewind.filewatchers.core.internal.DebugTimer;
import org.eclipse.codewind.filewatchers.core.internal.FileChangeEventBatchUtil;
import org.eclipse.codewind.filewatchers.core.internal.FileChangeEventBatchUtil.ChangedFileEntry;
import org.eclipse.codewind.filewatchers.core.internal.HttpGetStatusThread;
import org.eclipse.codewind.filewatchers.core.internal.HttpPostOutputQueue;
import org.eclipse.codewind.filewatchers.core.internal.HttpUtil;
import org.eclipse.codewind.filewatchers.core.internal.HttpUtil.HttpResult;
import org.eclipse.codewind.filewatchers.core.internal.WebSocketManagerThread;
import org.json.JSONObject;

/**
 * This class maintains information about the projects being watch, and is
 * otherwise the "glue" between the other components. This class maintains
 * references to the other utilities (post queue, WebSocket connection, watch
 * service, etc) and forwards communication between them.
 * 
 * Only one instance of this object will exist per server.
 */
public class Filewatcher {

	private static final FWLogger log = FWLogger.getInstance();

	/** Synchronize on me while accessing */
	private final HashMap<String /* project id */, ProjectObject> projectsMap_synch = new HashMap<>();

	private final HttpPostOutputQueue outputQueue;

	/** URL of Codewind service */
	private final String url;

	/** WebSocket URL of Codewind service */
	private final String wsUrl;

	private final IPlatformWatchService internalWatchService;
	private final IPlatformWatchService externalWatchService;

	private final HttpGetStatusThread getStatusThread;

	private final WebSocketManagerThread webSocketThread;

	private final AtomicBoolean disposed_synch = new AtomicBoolean();

	private final String clientUuid;

	public Filewatcher(String urlParam, String clientUuid, IPlatformWatchService internalWatchService,
			IPlatformWatchService externalWatchService) {

		this.url = FilewatcherUtils.stripTrailingSlash(urlParam);

		this.clientUuid = clientUuid;

		String calculatedWsUrl = this.url;
		{
			calculatedWsUrl = calculatedWsUrl.replace("http://", "ws://");
			calculatedWsUrl = calculatedWsUrl.replace("https://", "wss://");
		}

		this.wsUrl = calculatedWsUrl;
		this.outputQueue = new HttpPostOutputQueue(this.url);

		FilewatcherWatchListener fwl = new FilewatcherWatchListener(this);

		if (internalWatchService == null) {
			throw new IllegalArgumentException("internalWatchService param must be provided.");
		}

		this.internalWatchService = internalWatchService;
		this.internalWatchService.addListener(fwl);

		if (externalWatchService != null) {

			if (internalWatchService == externalWatchService) {
				this.externalWatchService = null;
			} else {
				this.externalWatchService = externalWatchService;
				this.externalWatchService.addListener(fwl);
			}
		} else {
			this.externalWatchService = null;
		}

		this.getStatusThread = new HttpGetStatusThread(this.url, this);
		getStatusThread.start();
		getStatusThread.queueStatusUpdate();

		this.webSocketThread = new WebSocketManagerThread(wsUrl, this);
		webSocketThread.start();
		webSocketThread.queueEstablishConnection();

		new DebugTimer(this);
	}

	public void refreshWatchStatus() {
		getStatusThread.queueStatusUpdate();
	}

	public void internal_updateFileWatchStateFromWebSocket(List<ProjectToWatchFromWebSocket> ptwList)
			throws IOException {

		log.logInfo("Examining received file watch state from WebSocket");

		for (ProjectToWatchFromWebSocket ptw : ptwList) {

			if (ptw.getChangeType().equals("add") || ptw.getChangeType().equals("update")) {
				createOrUpdateProjectToWatch(ptw);
			}

			if (ptw.getChangeType().equals("delete")) {
				removeSingleProjectToWatch(ptw);
			}
		}

	}

	public void dispose() {
		synchronized (disposed_synch) {
			if (disposed_synch.get()) {
				return;
			}
			disposed_synch.set(true);
		}

		log.logInfo("disposed() called on " + this.getClass().getSimpleName());

		outputQueue.dispose();
		try {
			this.internalWatchService.dispose();
		} catch (Exception e) {
			/* ignore */
		}

		try {
			if (this.externalWatchService != null) {
				this.externalWatchService.dispose();
			}

		} catch (Exception e) {
			/* ignore */
		}

		getStatusThread.dispose();

		webSocketThread.dispose();

		synchronized (projectsMap_synch) {
			projectsMap_synch.values().forEach(e -> {
				e.getEventBatchUtil().dispose();
			});
		}

	}

	public void internal_updateFileWatchStateFromGetRequest(List<ProjectToWatch> latestState) throws IOException {

		log.logInfo("Examining received file watch state, from GET request");

		// First we remove old projects
		List<ProjectToWatch> removedProjects = new ArrayList<>();

		Map<String /* project id */, Boolean> projectIdInHttpResult = new HashMap<>();
		latestState.forEach((ProjectToWatch e) -> {

			if (projectIdInHttpResult.containsKey(e.getProjectId())) {
				log.logSevere("Multiple projects in the project list share the same project ID: " + e.getProjectId());
			}

			projectIdInHttpResult.put(e.getProjectId(), true);
		});

		synchronized (projectsMap_synch) {
			// For each of the projects in the local state map, if they aren't found
			// in the HTTP GET result, then they have been removed.

			for (ProjectObject po : projectsMap_synch.values()) {

				if (!projectIdInHttpResult.containsKey(po.getProjectToWatch().getProjectId())) {
					removedProjects.add(po.getProjectToWatch());
				}

			}
		}

		removedProjects.forEach(e -> {
			removeSingleProjectToWatch(e);
		});

		// Next we create new watches, or update existing watches
		for (ProjectToWatch ptw : latestState) {

			createOrUpdateProjectToWatch(ptw);

		}

	}

	private void removeSingleProjectToWatch(ProjectToWatch removedProject) {

		ProjectObject po;
		synchronized (projectsMap_synch) {
			po = projectsMap_synch.remove(removedProject.getProjectId());
		}

		if (po == null) {
			log.logError("Asked to remove a project that wasn't in the projects map: " + removedProject.getProjectId());
			return;
		}

		log.logInfo("Removing project from watch list: " + po.getProjectToWatch().getProjectId() + " "
				+ po.getProjectToWatch().getPathToMonitor());

		ProjectToWatch ptw = po.getProjectToWatch();

		File fileToMonitor = new File(
				PathUtils.convertAbsoluteUnixStyleNormalizedPathToLocalFile(ptw.getPathToMonitor()));
		log.logDebug("Calling watch service removePath with file: " + fileToMonitor.getPath());
		po.getWatchService().removePath(fileToMonitor, ptw);

	}

	private void createOrUpdateProjectToWatch(ProjectToWatch ptw) throws IOException {
		ProjectObject po;
		synchronized (projectsMap_synch) {
			po = projectsMap_synch.get(ptw.getProjectId());
		}

		// For Windows, the server will give us path in the form of
		// '/c/Users/Administrator', which we need to convert to
		// 'c:\Users\Administrator', below.
		File fileToMonitor = new File(
				PathUtils.convertAbsoluteUnixStyleNormalizedPathToLocalFile(ptw.getPathToMonitor()));

		if (po == null) {
			// If is a new project to watch...

			IPlatformWatchService watchService;

			// Determine which watch service to use, based on what was provided in the
			// constructor, and what is specified in the JSON object.
			{
				if (this.externalWatchService == null) {
					watchService = this.internalWatchService;
				} else {

					if (ptw.isExternal()) {
						watchService = this.externalWatchService;
					} else {
						watchService = this.internalWatchService;
					}
				}

				if (watchService == null) {
					log.logSevere("Watch service for the new project was null; this shouldn't happen. projectId: "
							+ ptw.getProjectId() + " path: " + ptw.getPathToMonitor());
					return;
				}
			}

			po = new ProjectObject(ptw.getProjectId(), ptw, this, watchService);

			synchronized (projectsMap_synch) {
				projectsMap_synch.put(ptw.getProjectId(), po);
			}

			watchService.addPath(fileToMonitor, ptw);
			log.logInfo("Added new project with path '" + ptw.getPathToMonitor()
					+ "' to watch list, with watch directory: '" + fileToMonitor.getPath() + "' with watch service "
					+ watchService.getClass().getSimpleName(), ptw.getProjectId());

		} else {

			ProjectToWatch oldProjectToWatch = po.getProjectToWatch();

			// If the watch has changed, then remove the path and update the PTW
			if (!oldProjectToWatch.getProjectWatchStateId().equals(ptw.getProjectWatchStateId())) {

				log.logInfo("The project watch state has changed: " + oldProjectToWatch.getProjectWatchStateId() + " "
						+ ptw.getProjectWatchStateId() + " for project " + ptw.getProjectId());

				// Update existing project to watch
				po.updateProjectToWatch(ptw);

				// Remove the old path
				po.getWatchService().removePath(fileToMonitor, oldProjectToWatch);
				log.logInfo(
						"From update, removed project with path '" + ptw.getPathToMonitor()
								+ "' from watch list, with watch directory: '" + fileToMonitor.getPath() + "'",
						ptw.getProjectId());

				// Added the new path and PTW
				po.getWatchService().addPath(fileToMonitor, ptw);
				log.logInfo(
						"From update, added new project with path '" + ptw.getPathToMonitor()
								+ "' to watch list, with watch directory: '" + fileToMonitor.getPath() + "'",
						ptw.getProjectId());

			} else {
				log.logInfo("The project watch state has not changed for project " + ptw.getProjectId()
						+ " based on the project watch state id.");
			}

		}

	}

	/** Called by event processing timer task */

	public void internal_sendBulkFileChanges(String projectId, long mostRecentEntryTimestamp,
			List<String> base64Compressed) {

		outputQueue.addToQueue(projectId, mostRecentEntryTimestamp, base64Compressed);

	}

	public Optional<String> generateDebugString() {

		synchronized (disposed_synch) {
			if (disposed_synch.get()) {
				return Optional.empty();
			}
		}

		String result = "";

		result += "---------------------------------------------------------------------------------------\n\n";

		if (internalWatchService != null) {
			result += "WatchService - " + internalWatchService.getClass().getSimpleName() + ":\n";
			result += internalWatchService.generateDebugState().trim() + "\n";
		}

		if (externalWatchService != null) {
			result += "WatchService - " + externalWatchService.getClass().getSimpleName() + ":\n";
			result += externalWatchService.generateDebugState().trim() + "\n";

		}

		result += "\n";

		synchronized (projectsMap_synch) {

			result += "Project list:\n";

			for (Map.Entry<String, ProjectObject> e : projectsMap_synch.entrySet()) {

				ProjectToWatch ptw = e.getValue().getProjectToWatch();

				result += "- " + e.getKey() + " | " + ptw.getPathToMonitor();

				if (ptw.getIgnoredPaths().size() > 0) {
					result += " | ignoredPaths: ";

					for (String path : ptw.getIgnoredPaths()) {
						result += "'" + path + "' ";
					}
				}

				result += "\n";
			}

		}

		result += "\nHTTP Post Output Queue:\n" + outputQueue.generateDebugString().trim() + "\n\n";

		result += "---------------------------------------------------------------------------------------\n\n";

		return Optional.of(result);

	}

	Optional<FileChangeEventBatchUtil> getEventProcessing(String projectId) {
		synchronized (projectsMap_synch) {

			ProjectObject po = projectsMap_synch.getOrDefault(projectId, null);

			if (po != null) {
				return Optional.of(po.getEventBatchUtil());
			}

			return Optional.empty();

		}
	}

	// Called by FilewatcherWatchListener
	void receiveNewWatchEventEntries(List<WatchEventEntry> watchEntries, long receivedAtInEpochMsecs) {

		Map<String /* project id */, List<WatchEventEntry> /* watch entries for this project */> projectIdToList = new HashMap<>();

		List<ProjectToWatch> projectsToWatch = new ArrayList<>();
		synchronized (projectsMap_synch) {
			projectsToWatch.addAll(
					projectsMap_synch.values().stream().map(e -> e.getProjectToWatch()).collect(Collectors.toList()));
		}

		/*
		 * Sort projectsToWatch by length, descending (this handles the case where a
		 * parent, and it's child, are both managed at the same time..
		 */
		Collections.sort(projectsToWatch, (a, b) -> {
			return b.getPathToMonitor().length() - a.getPathToMonitor().length();
		});

		/**
		 * Figure out which WatchEventEntries go with with project IDs, based on the
		 * path from the entry. Filter the results into projectIdToList.
		 */
		for (WatchEventEntry we : watchEntries) {
			if (log.isDebug()) {
				log.logDebug("Received event from watcher: " + we);
			}

			// This will be the absolute path on the local drive
			String fullLocalPath = we.getAbsolutePathWithUnixSeparators();

			boolean match = false;

			for (ProjectToWatch ptw : projectsToWatch) {

				// See if this watch event is related to the project
				if (fullLocalPath.startsWith(ptw.getPathToMonitor())) {
					List<WatchEventEntry> list = projectIdToList.computeIfAbsent(ptw.getProjectId(),
							e -> new ArrayList<WatchEventEntry>());
					list.add(we);
					match = true;
					break;
				}
			}

			if (!match) {
				log.logSevere("Could not find matching project for " + we);
			}
		}

		// Any event type (create, modify, delete) is acceptable.

		// Filter it, then pass it to FilechangeEventBatchUtil

		for (Map.Entry<String /* project id */, List<WatchEventEntry>> me : projectIdToList.entrySet()) {

			ProjectToWatch ptw = projectsToWatch.stream().filter(e -> e.getProjectId().equals(me.getKey())).findAny()
					.orElse(null);

			if (ptw == null) {
				continue;
			}

			PathFilter filter = new PathFilter(ptw);

			List<ChangedFileEntry> changedFileEntries = new ArrayList<>();

			List<WatchEventEntry> eventList = me.getValue();
			for (WatchEventEntry we : eventList) {

				// Path will necessarily already have lowercase Windows drive letter, if
				// applicable.
				Optional<String> initialPath = PathUtils.convertAbsolutePathWithUnixSeparatorsToProjectRelativePath(
						we.getAbsolutePathWithUnixSeparators(), ptw.getPathToMonitor());

				String path = initialPath.orElse(null);
				if (path == null) {
					continue;
				}

//				if (!path.startsWith(ptw.getPathToMonitor())) {
//					// This shouldn't happen, and is thus severe
//					log.logSevere(
//							"Watch event '" + path + "' does not match project path '" + ptw.getPathToMonitor() + "'");
//					continue;
//				}
//
//				// Strip project parent directory from path:
//				// If pathToMonitor is: /home/user/codewind/project
//				// and watchEventPath is: /home/user/codewind/project/some-file.txt
//				// then this will convert watchEventPath to /some-file.txt
//				path = path.replace(ptw.getPathToMonitor(), "");
//
//				if (path.length() == 0) {
//					// Ignore the empty case
//					continue;
//				}

				if (ptw.getIgnoredPaths() != null && filter.isFilteredOutByPath(path)) {
					log.logDebug("Filtering out " + path + " by path.");
					continue;
				}

				if (ptw.getIgnoredFilenames() != null && filter.isFilteredOutByFilename(path)) {
					log.logDebug("Filtering out " + path + " by filename.");
					continue;
				}

				log.logDebug("Adding " + path + " to change list.");

				changedFileEntries.add(new FileChangeEventBatchUtil.ChangedFileEntry(path, we.isDirectory(),
						we.getEventType(), receivedAtInEpochMsecs));
			}

			if (changedFileEntries.size() > 0) {
				FileChangeEventBatchUtil processing = getEventProcessing(ptw.getProjectId()).orElse(null);
				if (processing != null) {
					processing.addChangedFiles(changedFileEntries);
				} else {
					log.logSevere("Could not locate event processing for project id " + ptw.getProjectId());
				}
			}
		}
	}

	void receiveWatchSuccessStatus(ProjectToWatch ptw, boolean successParam) {

		// Start a new thread to inform the server that the watch has succeeded (or
		// failed). Keep trying until success.
		FilewatcherUtils.newThread(() -> {

			String url = Filewatcher.this.url + "/api/v1/projects/" + ptw.getProjectId() + "/file-changes/"
					+ ptw.getProjectWatchStateId() + "/status?clientUuid=" + clientUuid;

			ExponentialBackoffUtil backoffUtil = FilewatcherUtils.getDefaultBackoffUtil(4000);

			boolean success = false;
			while (!success) {

				try {
					JSONObject obj = new JSONObject();
					obj.put("success", successParam);

					log.logInfo("Issuing PUT request to '" + url + "' with body " + obj);

					HttpResult response = HttpUtil.put(new URI(url), obj, (e) -> {
						e.setConnectTimeout(10 * 1000);
						e.setReadTimeout(10 * 1000);
					});

					if (response.responseCode == 200) {
						success = true;
						backoffUtil.successReset();
					} else {
						success = false;
					}

				} catch (Throwable t) {
					log.logError("Unable to inform server of watch status for '" + ptw.getProjectWatchStateId() + "'",
							t);
					success = false;
					backoffUtil.sleepIgnoreInterrupt();
					backoffUtil.failIncrease();
				}

			}
		});

	}

	/**
	 * Information maintained for each project that is being monitor by the watcher.
	 * This includes information on what to watch/filter (the ProjectToWatch), and
	 * the batch util (one batch util object exists per project.)
	 */
	private static class ProjectObject {
		private final FileChangeEventBatchUtil batchUtil;

		// Synchronize on lock when accessing inside this class
		private ProjectToWatch project_synch_lock;

		private final IPlatformWatchService watchService;

		private final Object lock = new Object();

		public ProjectObject(String projectId, ProjectToWatch project, Filewatcher parent,
				IPlatformWatchService watchService) {

			if (projectId == null || project == null || watchService == null) {
				throw new IllegalArgumentException("Invalid arg: " + projectId + " " + project + " " + watchService);
			}

			this.project_synch_lock = project;
			this.batchUtil = new FileChangeEventBatchUtil(parent, projectId);
			this.watchService = watchService;
		}

		private FileChangeEventBatchUtil getEventBatchUtil() {
			return batchUtil;
		}

		public ProjectToWatch getProjectToWatch() {
			synchronized (lock) {
				return project_synch_lock;
			}
		}

		private void updateProjectToWatch(ProjectToWatch newProjectToWatch) {

			synchronized (lock) {
				ProjectToWatch existingProjectToWatch = project_synch_lock;
				if (!existingProjectToWatch.getPathToMonitor().equals(newProjectToWatch.getPathToMonitor())) {

					String msg = "The path to monitor of a project cannot be changed once it is set, for a particular project id";

					log.logSevere(msg, null, existingProjectToWatch.getProjectId());
				}

				this.project_synch_lock = newProjectToWatch;
			}

		}

		public IPlatformWatchService getWatchService() {
			return watchService;
		}
	}

	/**
	 * A simple shim that receives file changes from the watch service, and passes
	 * them to a method above for processing. Once received there, they will next be
	 * passed to the batch util.
	 * 
	 * This shim also passes along watch success/failure messages.
	 */
	private static class FilewatcherWatchListener implements IPlatformWatchListener {

		private final Filewatcher parent;

		private FilewatcherWatchListener(Filewatcher parent) {
			this.parent = parent;
		}

		@Override
		public void changeDetected(List<WatchEventEntry> entries) {

			long receivedAt = System.currentTimeMillis();

			parent.receiveNewWatchEventEntries(entries, receivedAt);

		}

		@Override
		public void watchAdded(ProjectToWatch ptw, boolean success) {
			parent.receiveWatchSuccessStatus(ptw, success);
		}

	}

}
