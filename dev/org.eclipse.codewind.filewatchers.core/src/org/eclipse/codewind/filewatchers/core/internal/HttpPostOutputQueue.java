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

package org.eclipse.codewind.filewatchers.core.internal;

import java.net.ConnectException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.codewind.filewatchers.core.FWLogger;
import org.eclipse.codewind.filewatchers.core.FilewatcherUtils;
import org.eclipse.codewind.filewatchers.core.FilewatcherUtils.ExponentialBackoffUtil;
import org.eclipse.codewind.filewatchers.core.internal.HttpUtil.HttpResult;
import org.json.JSONObject;

/**
 * This class is responsible for informing the server (via HTTP post request) of
 * any file/directory changes that have occurred.
 * 
 * The FileChangeEventBatchUtil (indirectly) calls this class with a list of
 * base-64+compressed strings (containing the list of changes), and then this
 * class breaks the changes down into small chunks and sends them in the body of
 * individual HTTP POST requests.
 */
public class HttpPostOutputQueue {

	/** Synchronize on me when accessing, sorted ascending by timestamp */
	private final PriorityQueue<PostQueueChunkGroup> queue_synch = new PriorityQueue<>();

	/** Synchronize on 'lock' when accessing */
	private final List<OutputQueueWorkerThread> threads_sync_lock = new ArrayList<>();

	/** Synchronize on 'lock' when accessing */
	private final AtomicBoolean disposed_sync_lock = new AtomicBoolean(false);

	private final FWLogger log = FWLogger.getInstance();

	/** Keep track of the number of worker threads that are currently POST-ing. */
	private final AtomicInteger activeWorkerThreads_synch = new AtomicInteger();

	private final Object lock = new Object();

	private final String serverBaseUrl;

	public HttpPostOutputQueue(String url) {
		this.serverBaseUrl = url;

		for (int x = 0; x < 3; x++) {
			OutputQueueWorkerThread wt = new OutputQueueWorkerThread();
			wt.start();
			threads_sync_lock.add(wt);
		}
	}

	public void addToQueue(String projectId, long timestamp, List<String> base64Compressed) {
		synchronized (lock) {
			if (disposed_sync_lock.get()) {
				return;
			}
		}

		log.logDebug("Added file changes to queue: " + base64Compressed.size(), projectId);

		PostQueueChunkGroup chunkGroup = new PostQueueChunkGroup(timestamp, projectId, base64Compressed, this);

		synchronized (queue_synch) {
			queue_synch.offer(chunkGroup);
			informStateChange();
		}
	}

	public String generateDebugString() {

		String result = "- ";

		int activeWorkers = 0;
		synchronized (activeWorkerThreads_synch) {
			activeWorkers = activeWorkerThreads_synch.get();
		}

		synchronized (lock) {

			if (disposed_sync_lock.get()) {
				return result + "[disposed]";
			}

			result += "total-workers: " + threads_sync_lock.size() + "  active-workers:" + activeWorkers;
		}

		synchronized (queue_synch) {

			result += "  chunkGroupList-size: " + queue_synch.size() + "\n";

			if (queue_synch.size() > 0) {
				result += "\n";
				result += "- HTTP Post Chunk Group List:\n";

				for (PostQueueChunkGroup group : queue_synch) {
					result += "  - projectID: " + group.getProjectId() + "  timestamp: " + group.getTimestamp() + "\n";
				}

			}

		}

		return result;
	}

	/** Remove any chunk groups that have already sent all their chunks. */
	private void cleanupChunkGroups() {
		synchronized (queue_synch) {

			boolean changeMade = false;
			for (Iterator<PostQueueChunkGroup> it = queue_synch.iterator(); it.hasNext();) {

				PostQueueChunkGroup group = it.next();

				if (group.isGroupComplete()) {
					it.remove();
					changeMade = true;
				}
			}

			if (changeMade) {
				informStateChange();
			}
		}

	}

	void informStateChange() {
		synchronized (queue_synch) {
			queue_synch.notifyAll();
		}
	}

	private PostQueueChunk getOrWaitForNextPieceOfWork() throws InterruptedException {

		PostQueueChunk result = null;

		while (result == null) {
			synchronized (queue_synch) {

				cleanupChunkGroups();

				// If there is at least one chunk group available
				if (queue_synch.size() > 0) {

					// If there work available from the top chunk?
					PostQueueChunkGroup group = queue_synch.peek();
					Optional<PostQueueChunk> o = group.acquireNextChunkAvailableToSend();
					if (o.isPresent()) {
						result = o.get();
					}
				}

				// Wait if no work available
				if (result == null) {
					queue_synch.wait();
				}

			}

		}

		return result;
	}

	public void dispose() {
		synchronized (lock) {
			if (disposed_sync_lock.get()) {
				return;
			}

			disposed_sync_lock.set(true);

			threads_sync_lock.forEach(e -> {
				e.setThreadRunning(false);
			});

			threads_sync_lock.clear();
		}

	}

	/**
	 * Each post output queue can have up to X (eg 3) worker threads that are
	 * responsible for issuing post requests. This means that up to X (eg 3) HTTP
	 * connections may be active at a time. This significantly reduces latency for
	 * large requests.
	 */
	private class OutputQueueWorkerThread extends Thread {

		private boolean threadRunning = true;

		private final ExponentialBackoffUtil failureDelay = FilewatcherUtils.getDefaultBackoffUtil(4000);

		public OutputQueueWorkerThread() {
			setName(OutputQueueWorkerThread.class.getName());
			setDaemon(true);
		}

		public void setThreadRunning(boolean threadRunning) {
			this.threadRunning = threadRunning;
		}

		@Override
		public void run() {

			// Increase the length of the failure delay after each failure (to a maximum of
			// 2000 msecs); on success, reduce the failure back to default. (retry w/
			// exponential backoff)

			while (threadRunning) {

				try {
					pollAndSend();
				} catch (Throwable t) {
					// This should never happen, BUT OTOH we need to prevent this thread from EVER
					// unintentionally dying.
					log.logSevere("Exception in outer loop of pollAndSend", t, null);
				}

			}
		}

		private void pollAndSend() {

			boolean sendFailed = false;

			PostQueueChunk chunkToSend = null;
			try {

				chunkToSend = getOrWaitForNextPieceOfWork();

				if (chunkToSend != null && threadRunning) {
					synchronized (activeWorkerThreads_synch) {
						activeWorkerThreads_synch.incrementAndGet();
					}

					String url = serverBaseUrl + "/api/v1/projects/" + chunkToSend.getProjectId()
							+ "/file-changes?timestamp=" + chunkToSend.getTimestamp() + "&chunk="
							+ chunkToSend.getChunkId() + "&chunk_total=" + chunkToSend.getChunkTotal();

					JSONObject obj = new JSONObject();
					obj.put("msg", chunkToSend.getBase64Compressed());

					log.logInfo("Issuing POST request to '" + url + "', with payload size of "
							+ chunkToSend.getBase64Compressed().length());

					HttpResult response = HttpUtil.post(new URI(url), obj, (e) -> {
						HttpUtil.allowAllCerts(e);
						e.setConnectTimeout(10 * 1000);
						e.setReadTimeout(10 * 1000);
					});

					if (response == null || response.responseCode != 200) {
						sendFailed = true;
					} else {
						failureDelay.successReset();
						sendFailed = false;
					}

				}

			} catch (Throwable t) {
				if (t instanceof ConnectException && t.getMessage().contains("Connection refused")) {
					log.logError("Exception in pollAndSend");
				} else {
					log.logError("Exception in pollAndSend", t);
				}

				sendFailed = true;
			} finally {
				if (chunkToSend != null) {
					synchronized (activeWorkerThreads_synch) {
						activeWorkerThreads_synch.decrementAndGet();
					}
				}
			}

			if (!sendFailed && chunkToSend != null) {
				chunkToSend.getParentGroup().informChunkSent(chunkToSend);
			}

			// On fail, requeue the failed packet to be sent again
			if (sendFailed && threadRunning) {
				if (chunkToSend != null) {
					chunkToSend.getParentGroup().informChunkFailedToSend(chunkToSend);
				}

				// Exponential backoff with maximum
				failureDelay.sleepIgnoreInterrupt();
				failureDelay.failIncrease();

			}

		}

	}

	private static class PostQueueChunkGroup implements Comparable<PostQueueChunkGroup> {

		private enum ChunkStatus {
			AVAILABLE_TO_SEND, WAITING_FOR_ACK, COMPLETE
		}

		private final HashMap<Integer /* chunk id */, PostQueueChunk> chunkMap = new HashMap<>();

		private final HashMap<Integer /* chunk id */, ChunkStatus> chunkStatus_synch = new HashMap<>();

		private final long timestamp;

		private final HttpPostOutputQueue parent;

		private final FWLogger log = FWLogger.getInstance();

		private final String projectId;

		public PostQueueChunkGroup(long timestamp, String projectId, List<String> base64Compressed,
				HttpPostOutputQueue parent) {

			this.parent = parent;
			this.projectId = projectId;

			int chunkId = 1;
			for (String text : base64Compressed) {

				PostQueueChunk chunk = new PostQueueChunk(projectId, timestamp, text, chunkId, base64Compressed.size(),
						this);

				chunkMap.put(chunk.getChunkId(), chunk);
				chunkStatus_synch.put(chunk.getChunkId(), ChunkStatus.AVAILABLE_TO_SEND);

				chunkId++;
			}

			this.timestamp = timestamp;

		}

		/** A group is complete if every chunk is ChunkStatus.COMPLETE */
		public boolean isGroupComplete() {
			synchronized (chunkStatus_synch) {
				return !chunkStatus_synch.values().stream().anyMatch(e -> e != ChunkStatus.COMPLETE);
			}
		}

		public void informChunkSent(PostQueueChunk chunk) {
			synchronized (chunkStatus_synch) {
				ChunkStatus currStatus = chunkStatus_synch.get(chunk.getChunkId());
				if (currStatus != ChunkStatus.WAITING_FOR_ACK) {
					log.logSevere("Unexpected status of chunk, should be WAITING, but was:" + currStatus);
				}

				// Set the chunk back to complete, so no one else sends it
				chunkStatus_synch.put(chunk.getChunkId(), ChunkStatus.COMPLETE);
			}
			parent.informStateChange();
		}

		public void informChunkFailedToSend(PostQueueChunk chunk) {
			synchronized (chunkStatus_synch) {
				ChunkStatus currStatus = chunkStatus_synch.get(chunk.getChunkId());
				if (currStatus != ChunkStatus.WAITING_FOR_ACK) {
					log.logSevere("Unexpected status of chunk, should be WAITING, but was:" + currStatus);
				}

				// Reset the chunk back to AVAILABLE_TO_SEND, so someone else can send it
				chunkStatus_synch.put(chunk.getChunkId(), ChunkStatus.AVAILABLE_TO_SEND);
			}
			parent.informStateChange();
		}

		/**
		 * Returns the next chunk to be sent, or empty if none are currently available.
		 */
		public Optional<PostQueueChunk> acquireNextChunkAvailableToSend() {

			synchronized (chunkStatus_synch) {
				Map.Entry<Integer, ChunkStatus> entry = chunkStatus_synch.entrySet().stream()
						.filter(e -> e.getValue() == ChunkStatus.AVAILABLE_TO_SEND).findFirst().orElse(null);

				if (entry == null) {
					return Optional.empty();
				}

				chunkStatus_synch.put(entry.getKey(), ChunkStatus.WAITING_FOR_ACK);

				return Optional.of(chunkMap.get(entry.getKey()));

			}
		}

		/** Sort ascending by timestamp */
		@Override
		public int compareTo(PostQueueChunkGroup other) {
			int result;

			if (this.timestamp > other.timestamp) {
				result = 1;
			} else if (this.timestamp < other.timestamp) {
				result = -1;
			} else {
				result = 0;
			}

			return result;

		}

		String getProjectId() {
			return projectId;
		}

		long getTimestamp() {
			return timestamp;
		}
	}

	private static class PostQueueChunk {

		private final String projectId;
		private final long timestamp;
		private final String base64Compressed;
		private final int chunkId;
		private final int chunkTotal;

		private final PostQueueChunkGroup parentGroup;

		public PostQueueChunk(String projectId, long timestamp, String base64Compressed, int chunkId, int chunkTotal,
				PostQueueChunkGroup parentGroup) {
			this.projectId = projectId;
			this.timestamp = timestamp;
			this.base64Compressed = base64Compressed;
			this.chunkId = chunkId;
			this.chunkTotal = chunkTotal;
			this.parentGroup = parentGroup;
		}

		public String getProjectId() {
			return projectId;
		}

		public long getTimestamp() {
			return timestamp;
		}

		public String getBase64Compressed() {
			return base64Compressed;
		}

		public int getChunkId() {
			return chunkId;
		}

		public int getChunkTotal() {
			return chunkTotal;
		}

		public PostQueueChunkGroup getParentGroup() {
			return parentGroup;
		}
	}
}
