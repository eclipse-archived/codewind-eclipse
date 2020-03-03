/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.codewind.filewatchers.core.internal;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

import org.eclipse.codewind.filewatchers.core.FWLogger;
import org.eclipse.codewind.filewatchers.core.Filewatcher;
import org.eclipse.codewind.filewatchers.core.WatchEventEntry;
import org.eclipse.codewind.filewatchers.core.WatchEventEntry.EventType;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * When file/directory change events occur in quick succession (within
 * milliseconds of each other), it tends to imply that they are related. For
 * example, if you were refactoring Java code, that refactoring operation might
 * touch a bunch of source files at once. This means, in order to avoid
 * performing extra builds, we should batch together changes that occur within
 * close temporal proximity.
 *
 * However, we don't want to wait TOO long for new events, otherwise this
 * introduces latency between when the user makes a change, and when their build
 * actually starts.
 *
 * This class implements an algorithm that groups together changes that occur
 * within TIME_TO_WAIT_FOR_NO_NEW_EVENTS_IN_MSECS milliseconds of each other.
 *
 * The algorithm is: After at least one event is received, wait for there to be
 * be no more events in the stream of events (within eg 1000 msecs) before
 * sending them to the server. If an event is seen within 1000 msecs, the timer
 * is reset and a new 1000 msec timer begins. Batch together events seen since
 * within a given timeframe, and send them as a single request.
 *
 * This class receives file change events from the watch service, and forwards
 * batched groups of events to the HTTP POST output queue.
 *
 */
public class FileChangeEventBatchUtil {

	/** Synchronized on lock when accessing */
	private final List<ChangedFileEntry> files_synch_lock = new ArrayList<>();

	/** Synchronize on lock when accessing */
	private Timer timer_synch_lock = null;

	public boolean disposed_synch_lock = false;

	private final Object lock = new Object();

	private final Filewatcher parent;

	private final String projectId;

	private static final int TIME_TO_WAIT_FOR_NO_NEW_EVENTS_IN_MSECS = 1000;

	private static final int MAX_REQUEST_SIZE_IN_PATHS = 625;

	private static final FWLogger log = FWLogger.getInstance();

	private static final boolean DEBUG_PRINT_COMPRESSION_RATIO = false;

	private final boolean DISABLE_CWCTL_CLI_SYNC; // Enable this for debugging purposes.

	public FileChangeEventBatchUtil(Filewatcher parent, String projectId) {
		this.parent = parent;
		this.projectId = projectId;

		String val = System.getenv("DISABLE_CWCTL_CLI_SYNC");

		DISABLE_CWCTL_CLI_SYNC = val != null && val.trim().equalsIgnoreCase("true");

	}

	/**
	 * When files have changed, add them to the list and reset the timer task ahead
	 * X milliseconds.
	 */
	public void addChangedFiles(List<ChangedFileEntry> changedFileEntries) {
		synchronized (lock) {
			if (disposed_synch_lock) {
				return;
			}

			files_synch_lock.addAll(changedFileEntries);

			Date scheduledTime = new Date(System.currentTimeMillis() + TIME_TO_WAIT_FOR_NO_NEW_EVENTS_IN_MSECS);

			if (timer_synch_lock == null) {
				timer_synch_lock = new Timer();
			} else {
				timer_synch_lock.cancel();
				timer_synch_lock = new Timer();
			}

			timer_synch_lock.schedule(new EventProcessingTimerTask(), scheduledTime);

		}

	}

	/**
	 * For any given path: If there are multiple entries of the same type in a row,
	 * then remove all but the first.
	 **/
	static final void removeDuplicateEventsOfType(List<ChangedFileEntry> changedFileList,
			WatchEventEntry.EventType eventType) {

		if (eventType == EventType.MODIFY) {
			throw new IllegalArgumentException("Unsupported event type: " + eventType);
		}

		Map<String /* path */, Boolean /* not used */> containsPath = new HashMap<>();

		for (Iterator<ChangedFileEntry> it = changedFileList.iterator(); it.hasNext();) {
			ChangedFileEntry cfe = it.next();

			String path = cfe.getPath();

			if (cfe.getType() == eventType) {

				if (containsPath.containsKey(path)) {
					if (log.isDebug()) {
						log.logDebug("Removing duplicate event: " + cfe.toString());
					}
					it.remove();
				} else {
					containsPath.put(path, true);
				}

			} else {
				containsPath.remove(path);
			}

		}

	}

	@SuppressWarnings("unused")
	private final static byte[] compressString(String str) {

		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		DeflaterOutputStream dos = new DeflaterOutputStream(baos, new Deflater(Deflater.BEST_SPEED));

		int uncompressedSize;
		try {
			byte[] strBytes = str.getBytes();
			dos.write(strBytes);
			dos.close();
			baos.close();
		} catch (IOException e) {
			log.logSevere("Unable to compress string contents", e, null);
			throw new RuntimeException(e);
		}

		byte[] result = baos.toByteArray();

		return result;
	}

	/**
	 * Output the first 256 characters of the change list, as a summary of the full
	 * list of changes. This means the change list is not necessary a complete list,
	 * and is only what fits into the given length.
	 */
	private static final String generateChangeListSummaryForDebug(List<ChangedFileEntry> entries) {
		StringBuilder fileChangeSummaryList = new StringBuilder();
		fileChangeSummaryList.append("[ ");

		for (ChangedFileEntry cfe : entries) {

			if (cfe.getType() == EventType.CREATE) {
				fileChangeSummaryList.append("+");
			} else if (cfe.getType() == EventType.DELETE) {
				fileChangeSummaryList.append("-");
			} else if (cfe.getType() == EventType.MODIFY) {
				fileChangeSummaryList.append(">");
			} else {
				fileChangeSummaryList.append("?");
			}

			String filename = cfe.getPath();
			int index = cfe.getPath().lastIndexOf("/");
			if (index != -1) {
				filename = filename.substring(index + 1);

				if (filename.length() == 0) {
					filename = "/"; // Handle event on the root project directory as "/"
				}
			}

			fileChangeSummaryList.append(filename);
			fileChangeSummaryList.append(" ");

			if (fileChangeSummaryList.length() > 256) {
				break;
			}
		}

		if (fileChangeSummaryList.length() > 256) {
			fileChangeSummaryList.append(" (...) ");
		}

		fileChangeSummaryList.append("]");

		return fileChangeSummaryList.toString();
	}

	public void dispose() {
		synchronized (lock) {
			if (disposed_synch_lock) {
				return;
			}
			log.logInfo("dispose() called on " + this.getClass().getSimpleName());
			disposed_synch_lock = true;

		}
	}

	/**
	 * This logic runs after TIME_TO_WAIT_FOR_NO_NEW_EVENTS_IN_MSECS has elapsed. At
	 * this point, the assumption is that all events that will occur HAVE occurred,
	 * and thus all events currently in the list can be grouped together and sent.
	 */
	private class EventProcessingTimerTask extends TimerTask {

		public EventProcessingTimerTask() {
		}

		@Override
		public void run() {
			List<ChangedFileEntry> entries = new ArrayList<>();

			synchronized (lock) {
				// When the timer task has triggered, we pull all the entries out of the file
				// list and reset the timer.
				entries.addAll(files_synch_lock);
				files_synch_lock.clear();

				timer_synch_lock.cancel();
				timer_synch_lock = null;

				if (entries.size() == 0) {
					return;
				}

			}

			// Sort ascending by timestamp, remove duplicate entries, then flip to
			// descending
			Collections.sort(entries);
			removeDuplicateEventsOfType(entries, EventType.CREATE);
			removeDuplicateEventsOfType(entries, EventType.DELETE);
			Collections.reverse(entries);

			if (entries.size() == 0) {
				return;
			}

			long mostRecentEntryTimestamp = entries.get(0).getTimestamp();

			String changeSummary = generateChangeListSummaryForDebug(entries);
			log.logInfo(
					"Batch change summary for " + projectId + "@ " + mostRecentEntryTimestamp + ": " + changeSummary);

			if (!DISABLE_CWCTL_CLI_SYNC) {
				// Use CWCTL CLI SYNC command
				parent.internal_informCwctlOfFileChanges(projectId);

			} else {

				// Use the old way of communicating file values.

				// TODO: Remove this entire else block once CWCTL sync is mature.

				// Split the entries into separate requests (chunks), to ensure that each
				// request is no larger then a given size.
				List<JSONArray> fileListsToSend = new ArrayList<>();
				while (entries.size() > 0) {

					// Remove at most MAX_REQUEST_SIZE_IN_PATHS paths from paths
					List<JSONObject> currList = new ArrayList<>();
					while (currList.size() < MAX_REQUEST_SIZE_IN_PATHS && entries.size() > 0) {

						// Oldest entries will be at the end of the list, and we want to send those
						// first.
						ChangedFileEntry nextPath = entries.remove(entries.size() - 1);
						try {
							currList.add(nextPath.toJsonObject());
						} catch (JSONException e1) {
							log.logSevere("Unable to convert changed file entry to json", e1, projectId);
						}
					}

					if (currList.size() > 0) {
						fileListsToSend.add(new JSONArray(currList));
					}

				}

				// Compress, convert to base64, then send
				List<String> base64Compressed = new ArrayList<>();
				for (JSONArray array : fileListsToSend) {

					String json = array.toString();

					byte[] compressedData = compressString(json);
					base64Compressed.add(Base64.getEncoder().encodeToString(compressedData));

					if (DEBUG_PRINT_COMPRESSION_RATIO) {
						int uncompressedSize = json.getBytes().length;
						int compressedSize = compressedData.length;
						System.out.println("Compression ratio: " + uncompressedSize + " -> " + compressedSize
								+ " (ratio: " + (int) ((100 * compressedSize) / uncompressedSize) + ") [per path: "
								+ (compressedSize / array.length()) + "]");
					}

				}

				if (base64Compressed.size() > 0) {
					parent.internal_sendBulkFileChanges(projectId, mostRecentEntryTimestamp, base64Compressed);
				}

			}

		}
	}

	/**
	 * Simple representation of a single change: the file/dir path that changed,
	 * what type of change, and when. These are then consumed by the batch
	 * processing utility.
	 */
	public static class ChangedFileEntry implements Comparable<ChangedFileEntry> {

		/** Path is in normalized form, with project base as the root. */
		private final String path;

		private final WatchEventEntry.EventType type;
		private final long timestamp;

		private final boolean directory;

		public ChangedFileEntry(String path, boolean directory, EventType type, long timestamp) {
			if (path == null || type == null || timestamp <= 0) {
				throw new IllegalArgumentException(
						"Invalid parameter '" + path + "' '" + type + "' '" + timestamp + "'");
			}

			this.path = path;
			this.type = type;
			this.timestamp = timestamp;
			this.directory = directory;
		}

		public long getTimestamp() {
			return timestamp;
		}

		public String getPath() {
			return path;
		}

		public WatchEventEntry.EventType getType() {
			return type;
		}

		public JSONObject toJsonObject() throws JSONException {
			JSONObject result = new JSONObject();
			result.put("path", path);
			result.put("timestamp", timestamp);
			result.put("type", type.name());
			result.put("directory", directory);
			return result;
		}

		@Override
		public String toString() {
			try {
				return toJsonObject().toString();
			} catch (JSONException e) {
				return path;
			}
		}

		@Override
		public int compareTo(ChangedFileEntry other) {
			// Sort ascending by timestamp
			long val = this.getTimestamp() - other.getTimestamp();
			if (val > 0) {
				return 1;
			} else if (val < 0) {
				return -1;
			} else {
				return 0;
			}

		}
	}
}
