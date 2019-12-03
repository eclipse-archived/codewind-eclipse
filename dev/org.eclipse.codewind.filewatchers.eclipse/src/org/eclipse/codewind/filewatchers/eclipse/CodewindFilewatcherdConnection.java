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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.eclipse.codewind.filewatchers.JavaNioWatchService;
import org.eclipse.codewind.filewatchers.core.FWLogger;
import org.eclipse.codewind.filewatchers.core.Filewatcher;
import org.eclipse.codewind.filewatchers.core.IAuthTokenProvider;
import org.eclipse.codewind.filewatchers.core.WatchEventEntry;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;

/**
 * This class is responsible for kicking off the Filewatcher core code (via the
 * Filewatcher constructor), adding the resource change listener to workspace,
 * and receiving events from that Eclipse resource change listener then passing
 * those events to the core Filewatcher logic.
 * 
 * Only a single instance of this class should exist per Codewind server. This
 * class is threadsafe.
 */
public class CodewindFilewatcherdConnection {

	@SuppressWarnings("unused")
	private final String baseHttpUrl;

	private final Filewatcher fileWatcher;

	private final EclipseResourceWatchService platformWatchService;

	private final CodewindResourceChangeListener listener;

	private final ICodewindProjectTranslator translator;

	private final String clientUuid;

	public CodewindFilewatcherdConnection(String baseHttpUrl, File pathToCwctl, ICodewindProjectTranslator translator,
			IAuthTokenProvider authTokenProvider /* nullable */) {

		if (pathToCwctl == null) {
			throw new RuntimeException("A valid path to the Codewind CLI is required: " + pathToCwctl);
		}

		if (!pathToCwctl.exists() || !pathToCwctl.canExecute()) {
			throw new RuntimeException(this.getClass().getSimpleName() + " was passed an invalid installer path: "
					+ pathToCwctl.getPath());
		}

		this.clientUuid = UUID.randomUUID().toString();

		String url = baseHttpUrl;
		if (!url.startsWith("http://") && !url.startsWith("https://")) {
			throw new IllegalArgumentException("Argument should begin with http:// or https://.");
		}

		// Log to the workspace .metadata directory, rather than to the console.
		File metadataDirectory = new File(ResourcesPlugin.getWorkspace().getRoot().getLocation().toFile().getPath(),
				".metadata");
		FWLogger logger = FWLogger.getInstance();
		logger.setOutputLogsToScreen(false);
		logger.setRollingFileLoggerOutputDir(metadataDirectory);

		this.translator = translator;

		listener = new CodewindResourceChangeListener(this);

		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		workspace.addResourceChangeListener(listener);

		// We use the native Eclipse-resource-listener-based watch service for projects
		// inside the workspace, and the Java-NIO-JVM-based watch service for folders
		// outside the workspace (eg the standalone Codewind settings directory).
		this.platformWatchService = new EclipseResourceWatchService();
		this.fileWatcher = new Filewatcher(url, clientUuid, platformWatchService, new JavaNioWatchService(),
				pathToCwctl.getPath(), authTokenProvider);

		this.baseHttpUrl = url;

	}

	public String getClientUuid() {
		return clientUuid;
	}

	public void dispose() {
		try {
			platformWatchService.dispose();

			IWorkspace workspace = ResourcesPlugin.getWorkspace();
			workspace.removeResourceChangeListener(listener);

		} catch (Exception e) {
			/* ignore */
		}
		try {
			fileWatcher.dispose();
		} catch (Exception e) {
			/* ignore */
		}
	}

	/**
	 * Called by the resource change listener with a list of changes, which we pass
	 * along to the core file watcher.
	 */
	void handleResourceChanges(List<FileChangeEntryEclipse> result) {
		if (result == null || result.size() == 0) {
			return;
		}

		// Sort the changes by project ID (which we get from the translator)
		HashMap<String /* project id */, List<WatchEventEntry>> entries = new HashMap<>();
		result.forEach(e -> {
			Optional<String> o = translator.getProjectId(e.getProject());
			if (o.isPresent()) {
				String projectId = o.get();

				List<WatchEventEntry> weeList = entries.computeIfAbsent(projectId, f -> new ArrayList<>());
				weeList.add(e.convertToWatchEvent());
			} else {
				// Ignore: probably not a Codewind project.
			}
		});

		// Pass the results to the watch service
		entries.entrySet().forEach((e) -> {
			platformWatchService.receiveWatchEntries(e.getKey(), e.getValue());
		});

	}

	/**
	 * The CodewindResourceChangeListener converts file/folder changes into
	 * instances of this class, which are then converted to WatchEventEntry (above)
	 * to be passed to the core Filewatcher plugin logic.
	 * 
	 * The main difference between this class and a WatchEventEntry is the IProject.
	 */
	public static class FileChangeEntryEclipse {

		private final File f;
		private final ChangeEntryEventType eventType;
		private final boolean isDirectory;

		private final IProject project;

		public static enum ChangeEntryEventType {
			CREATE, MODIFY, DELETE
		};

		public FileChangeEntryEclipse(File f, ChangeEntryEventType eventType, boolean isDirectory, IProject project) {
			this.f = f;
			this.eventType = eventType;
			this.isDirectory = isDirectory;
			this.project = project;
		}

		public WatchEventEntry convertToWatchEvent() {

			if (this.eventType == ChangeEntryEventType.CREATE) {
				return new WatchEventEntry(WatchEventEntry.EventType.CREATE, f.toPath(), isDirectory);

			} else if (this.eventType == ChangeEntryEventType.DELETE) {
				return new WatchEventEntry(WatchEventEntry.EventType.DELETE, f.toPath(), isDirectory);

			} else if (this.eventType == ChangeEntryEventType.MODIFY) {
				return new WatchEventEntry(WatchEventEntry.EventType.MODIFY, f.toPath(), isDirectory);

			} else {
				throw new IllegalArgumentException("Unknown eventType: " + this.eventType);
			}
		}

		public IProject getProject() {
			return project;
		}

		@Override
		public String toString() {
			return convertToWatchEvent().toString();
		}

	}

}
