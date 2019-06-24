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

import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.eclipse.codewind.filewatchers.core.FWLogger;
import org.eclipse.codewind.filewatchers.core.Filewatcher;
import org.eclipse.codewind.filewatchers.core.FilewatcherUtils;
import org.eclipse.codewind.filewatchers.core.FilewatcherUtils.ExponentialBackoffUtil;
import org.eclipse.codewind.filewatchers.core.ProjectToWatch;
import org.eclipse.codewind.filewatchers.core.internal.HttpUtil.HttpResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * This class is responsible for issuing a GET request to the server in order to
 * retrieve the latest list of projects to watch (including their path, and any
 * filters).
 * 
 * A new GET request will be sent by this class on startup, and then: whenever
 * the WebSocket connection fails, and otherwise once every 60 seconds.
 * 
 * WebSocketManagerThread is responsible for informing this class when the
 * WebSocket connection fails (input), and this class calls the Filewatcher
 * class with the data from the GET request (containing any project watch
 * updates received) as output.
 * 
 */
public class HttpGetStatusThread extends Thread {

	private final Filewatcher parent;
	private final String baseUrl;

	private boolean threadRunning = true;

	private final FWLogger log = FWLogger.getInstance();

	private static final int REFRESH_EVERY_X_SECONDS = 120;

	/**
	 * Synchronize on lock before accessing. This list will only ever contain zero
	 * or one timestamps.
	 */
	private final List<Long /* timestamp in nanos */> statusUpdateRequests_synch_lock = new ArrayList<>();

	private final Object lock = new Object();

	public HttpGetStatusThread(String url, Filewatcher parent) {
		this.parent = parent;
		this.baseUrl = url;
		setDaemon(true);
		setName(HttpGetStatusThread.class.getSimpleName());
	}

	@Override
	public void run() {
		try {
			log.logInfo(HttpGetStatusThread.class.getName() + " started, for '" + baseUrl + "'.");

			while (threadRunning) {

				try {
					innerLoop();
				} catch (Throwable t) {
					// Prevent this thread from ever dying due to an exception.
					log.logSevere("Unexpected exception when getting filewatcher status", t, null);

					FilewatcherUtils.sleepIgnoreInterrupt(1000);
				}

			}
		} finally {
			log.logInfo(HttpGetStatusThread.class.getName() + " terminated, from '" + baseUrl + "'.");
		}

	}

	public void queueStatusUpdate() {
		if (!threadRunning) {
			return;
		}

		boolean queued = false;
		synchronized (lock) {
			if (statusUpdateRequests_synch_lock.size() == 0) {
				statusUpdateRequests_synch_lock.add(System.nanoTime());
				lock.notify();
				queued = true;
			} else {
				queued = false;
			}
		}
		if (queued) {
			log.logDebug("Queue status update received for '" + baseUrl + "', and was queued.");
		} else {
			log.logDebug("Queue status update received for '" + baseUrl + "', but was ignored as already present.");
		}

	}

	private void innerLoop() throws InterruptedException, IOException {

		long nextWatchRefreshInNanos = 0;

		while (threadRunning) {

			boolean performSynch = false;

			// Wait for the caller to signal that we need another refresh
			synchronized (lock) {
				if (statusUpdateRequests_synch_lock.size() > 0) {
					performSynch = true;

				} else {
					lock.wait(10000);
				}
			}

			// Manually refresh every X (eg 60) seconds, whether we are asked to or not.
			if (nextWatchRefreshInNanos == 0) {
				nextWatchRefreshInNanos = System.nanoTime()
						+ TimeUnit.NANOSECONDS.convert(REFRESH_EVERY_X_SECONDS, TimeUnit.SECONDS);
			} else if (System.nanoTime() > nextWatchRefreshInNanos) {
				performSynch = true;
				nextWatchRefreshInNanos = 0;
			}

			// If the caller asked for a refresh, keep trying to get one until it succeeds.
			if (performSynch) {
				boolean success = false;

				List<ProjectToWatch> projectsToWatch = null;

				ExponentialBackoffUtil delay = FilewatcherUtils.getDefaultBackoffUtil(4000);

				// Keep trying until success
				while (!success && threadRunning) {
					try {
						projectsToWatch = doHttpRequest();
					} catch (Throwable t) {
						// Don't output the full exception if we recognize it
						if (t instanceof ConnectException && t.getMessage().contains("Connection refused")) {
							log.logError("Unable to issue get request to " + baseUrl + " "
									+ t.getClass().getSimpleName() + ": " + t.getMessage());
						} else {
							log.logError("Unable to issue get request to " + baseUrl, t);
						}

					}
					success = projectsToWatch != null;

					if (!success) {
						// On failure, wait then try again
						delay.sleep();
						delay.failIncrease();
					}

				}

				nextWatchRefreshInNanos = 0;

				// On success, clear any old queued requests.
				synchronized (lock) {
					statusUpdateRequests_synch_lock.clear();
				}

				if (projectsToWatch != null && projectsToWatch.size() > 0) {
					parent.internal_updateFileWatchStateFromGetRequest(projectsToWatch);
				}
			}

		}

	}

	/**
	 * Returns null if a request could not be successfully made to the watchlist, or
	 * a parsed JSON result otherwise.
	 * 
	 * @throws JSONException
	 */
	private List<ProjectToWatch> doHttpRequest() throws IOException, URISyntaxException, JSONException {

		String toGet = baseUrl + "/api/v1/projects/watchlist";

		HttpResult httpResult = null;
		try {

			log.logInfo("Initiating GET request to " + toGet);

			httpResult = HttpUtil.get(new URI(toGet), (e) -> {
				e.setConnectTimeout(15 * 1000);
				e.setReadTimeout(15 * 1000);
				HttpUtil.allowAllCerts(e);
			});

			if (httpResult == null || httpResult.responseCode != 200) {
				log.logError("Get response failed for " + toGet + ", "
						+ (httpResult != null ? httpResult.responseCode : "N/A"));
				return null;
			}

			if (httpResult.response == null) {
				log.logError("Get response was null for " + toGet);
				return null;
			}

		} finally {
			String responseStr = (httpResult != null && httpResult.response != null ? httpResult.response.trim()
					: "N/A");

			// Make the request fit on a single log line, use a pretty printer to restore.
			responseStr = responseStr.replace("\r", "");
			responseStr = responseStr.replace("\n", "");

			log.logInfo("GET request completed, for " + toGet + ". Response: " + responseStr);
		}

		JSONArray arr;
		try {

			JSONObject jo = new JSONObject(httpResult.response);
			arr = jo.getJSONArray("projects");

		} catch (JSONException je) {
			log.logSevere("Unable to parse JSON, response was: " + httpResult.response, je, null);
			throw je;
		}

		if (arr == null) {
			return null;
		}

		List<ProjectToWatch> result = new ArrayList<>();

		for (int x = 0; x < arr.length(); x++) {
			JSONObject projectToWatchJson = arr.getJSONObject(x);
			ProjectToWatch ptw = new ProjectToWatch(projectToWatchJson, false);
			result.add(ptw);
		}

		return result;

	}

	public void dispose() {

		if (!this.threadRunning) {
			return;
		}

		log.logInfo("dispose() called on " + this.getClass().getSimpleName());
		this.threadRunning = false;

		// Wake up the thread if needed.
		FilewatcherUtils.newThread(() -> {
			synchronized (lock) {
				lock.notifyAll();
			}
		});

	}

}
