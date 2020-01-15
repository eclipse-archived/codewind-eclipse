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

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.eclipse.codewind.filewatchers.core.FWLogger;
import org.eclipse.codewind.filewatchers.core.Filewatcher;
import org.eclipse.codewind.filewatchers.core.FilewatcherUtils;
import org.eclipse.codewind.filewatchers.core.FilewatcherUtils.ExponentialBackoffUtil;
import org.eclipse.codewind.filewatchers.core.ProjectToWatch.ProjectToWatchFromWebSocket;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import okhttp3.ConnectionSpec;
import okhttp3.OkHttpClient;
import okhttp3.OkHttpClient.Builder;
import okhttp3.Request;
import okhttp3.WebSocket;

/**
 * The purpose of this class is to initiate and maintain the WebSocket
 * connection between the filewatcher and the server.
 * 
 * Since the OKHttp WebSocket API is non-blocking, we use the WSClientEndpoint
 * to inform us if a connection succeeded or failed, and act on that information
 * to re-establish a connection if applicable.
 * 
 * This class also sends a simple "keep alive" packet every X seconds (eg 25).
 */
public class WebSocketManagerThread extends Thread {

	private final WSClientEndpoint endpoint;

	private final String wsUrl;

	private final Filewatcher watcher;

	private final AtomicBoolean disposed = new AtomicBoolean(false);

	private final FWLogger log = FWLogger.getInstance();

	private final static int SEND_KEEPALIVE_EVERY_X_SECONDS = 25;

	private final OkHttpClient okClient;

	@SuppressWarnings("unused")
	private final AuthTokenWrapper authTokenWrapper;

	private final ExponentialBackoffUtil exponentialBackoffUtil_synch = new ExponentialBackoffUtil(50, 4000, 2f);

	private final AtomicInteger numberOfConsecutiveFailures = new AtomicInteger(0);

	private final List<WebSocket> activeWebSockets_synch = new ArrayList<>();

	public WebSocketManagerThread(String wsUrl, Filewatcher watcher) {
		setDaemon(true);
		setName(WebSocketManagerThread.class.getName());
		this.endpoint = new WSClientEndpoint(this, wsUrl);
		this.wsUrl = wsUrl;
		this.watcher = watcher;

		this.authTokenWrapper = watcher.internal_getAuthTokenWrapper();

		// Ignore invalid certificates until we have project infrastructure to better
		// support this scenario
		X509TrustManager tm = new X509TrustManager() {
			public void checkClientTrusted(X509Certificate[] xcs, String str) throws CertificateException {
				// Do nothing
			}

			public void checkServerTrusted(X509Certificate[] xcs, String str) throws CertificateException {
				// Do nothing
			}

			public X509Certificate[] getAcceptedIssuers() {
				return new X509Certificate[] {};
			}
		};

		// Don't bother to verify that hostname resolves correctly, until we have
		// project infrastructure to better support this scenario.
		HostnameVerifier hostnameVerifier = new HostnameVerifier() {
			@Override
			public boolean verify(String hostname, SSLSession session) {
				return true;
			}
		};

		// TLS/SSL setup
		SSLContext ctx;
		try {
			ctx = SSLContext.getInstance("TLSv1.2");
			ctx.init(null, new TrustManager[] { tm }, new java.security.SecureRandom());
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		} catch (KeyManagementException e) {
			throw new RuntimeException(e);
		}

		Builder b = new OkHttpClient.Builder()
				.connectionSpecs(Arrays.asList(ConnectionSpec.MODERN_TLS, ConnectionSpec.COMPATIBLE_TLS,
						ConnectionSpec.CLEARTEXT))
				.sslSocketFactory(ctx.getSocketFactory(), tm).hostnameVerifier(hostnameVerifier);

		OkHttpClient client = b.build();

		okClient = client;
	}

	@Override
	public void run() {
		try {

			Long nextPingInNanos = null;

			log.logInfo("Web socket manager thread for '" + wsUrl + "' begun.");

			while (!disposed.get()) {

				try {

					// Ensure that we are always attempting to establish a connection
					synchronized (activeWebSockets_synch) {
						if (activeWebSockets_synch.size() == 0) {
							establishOrReestablishConnection();
						}
					}

					// Send a ping packet to keep the connection alive, every X seconds (eg 25).
					if (nextPingInNanos == null) {
						nextPingInNanos = System.nanoTime()
								+ TimeUnit.NANOSECONDS.convert(SEND_KEEPALIVE_EVERY_X_SECONDS, TimeUnit.SECONDS);
					}

					if (nextPingInNanos != null && System.nanoTime() > nextPingInNanos) {
						try {

							for (WebSocket ws : activeWebSockets_synch) {
								ws.send("{}");
							}

						} catch (Throwable t) {
							/* ignore: session is probably closed or dead, so no ping needed. */
						}
						nextPingInNanos = null;

					}

				} catch (Throwable t) {
					// Prevent this thread from dying unnaturally
					log.logSevere("Unexpected exception occurred.", t, null);
				}
				FilewatcherUtils.sleepIgnoreInterrupt(1000);

			} // end while
		} finally {
			log.logInfo("Web socket thread for '" + wsUrl + "' ended.");
		}
	}

	public void informConnectionSuccess(WebSocket webSocket) {
		if (disposed.get()) {
			return;
		}

		log.logInfo("WebSocket successfully connected to:" + wsUrl);
		numberOfConsecutiveFailures.getAndSet(0);

		synchronized (exponentialBackoffUtil_synch) {
			exponentialBackoffUtil_synch.successReset();
		}

		watcher.refreshWatchStatus();
	}

	/**
	 * If the WebSocket connection closes or fails, we should issue a new GET
	 * request to ensure we have the latest state.
	 */
	public void informConnectionClosedOrFailed(WebSocket ws) {

		ws.close(1000, "Ensure WebSocket resource is disposed.");

		synchronized (activeWebSockets_synch) {
			boolean wsFound = activeWebSockets_synch.remove(ws);
			if (!wsFound && !disposed.get()) {
				log.logError("Unable to locate WebSocket in active websockets.");
			}
		}

		if (disposed.get()) {
			return;
		}

		numberOfConsecutiveFailures.incrementAndGet();
		synchronized (exponentialBackoffUtil_synch) {
			exponentialBackoffUtil_synch.failIncrease();
		}

		// Each time we fail to connect, ask the filewatcher to issue a new GET request
		// to refresh the watch state (in lieu of any WebSocket events we may be
		// missing.)
		watcher.refreshWatchStatus();

		log.logInfo("WebSocket failed to connect to '" + wsUrl + "', establishOrReestablishConnection to retry.");

		establishOrReestablishConnection();
	}

	public void dispose() {
		if (disposed.get()) {
			return;
		}

		disposed.set(true);

		log.logInfo("disposed() called in " + this.getClass().getSimpleName());

		FilewatcherUtils.newThread(() -> {

			synchronized (activeWebSockets_synch) {
				for (WebSocket curr : activeWebSockets_synch) {
					try {
						curr.close(1000, "Disposing of WebSocket.");
					} catch (Exception e) {
						/* ignore */
					}
				}

			}

			okClient.dispatcher().executorService().shutdown();

		});

	}

	private void establishOrReestablishConnection() {

		if (disposed.get()) {
			return;
		}

		synchronized (activeWebSockets_synch) {
			if (activeWebSockets_synch.size() > 0) {
				return;
			}

			if (numberOfConsecutiveFailures.get() > 0) {
				exponentialBackoffUtil_synch.sleepIgnoreInterrupt();
			}

			log.logInfo("Attempting to establish connection to WebSocket, attempt #"
					+ (numberOfConsecutiveFailures.get() + 1));

			String url = wsUrl + "/websockets/file-changes/v1";

			// This request is queued to the OKHttp thread pool, as the OKHttp API is
			// non-blocking.
			Request request = new Request.Builder().url(url).build();
			WebSocket ws = okClient.newWebSocket(request, endpoint);

			activeWebSockets_synch.add(ws);
		}

	}

	/** Called by WSClientEndpoint when a message is received from the socket. */
	void receiveMessage(String s) {

		if (disposed.get()) {
			return;
		}

		try {
			JSONObject jo = new JSONObject(s);
			String type = jo.getString("type"); // ignored, only one type for now.

			if (type.equals("debug")) {
				handleDebugMessage(jo);
				return;
			}

			log.logInfo("Received watch change message from WebSocket: " + s);

			JSONArray projects = jo.getJSONArray("projects");

			List<ProjectToWatchFromWebSocket> ptwList = new ArrayList<>();

			for (int x = 0; x < projects.length(); x++) {

				JSONObject projectToWatchJson = projects.getJSONObject(x);
				String changeType = projectToWatchJson.getString("changeType");

				ProjectToWatchFromWebSocket ptw = new ProjectToWatchFromWebSocket(projectToWatchJson, changeType);

				ptwList.add(ptw);
			}

			String projectList = ptwList.stream()
					.map(e -> "[" + e.getProjectId() + " in " + e.getPathToMonitor() + "]" + ", ")
					.reduce((a, b) -> (a + b)).get().trim();
			projectList = projectList.substring(0, projectList.length() - 1); // strip comma

			log.logInfo("Watch list update received for { " + projectList + " }");

			watcher.internal_updateFileWatchStateFromWebSocket(ptwList);

		} catch (JSONException e) {
			log.logSevere("Unexpected JSON exception when trying to parse message", e, null);
		} catch (IOException e) {
			log.logSevere("IOException from file watch state updater", e, null);
		}
	}

	private void handleDebugMessage(JSONObject jsonObj) {
		try {
			String msg = jsonObj.getString("msg");

			log.logInfo("------------------------------------------------------------");
			log.logInfo("[Server-Debug] " + msg);
			log.logInfo("------------------------------------------------------------");
		} catch (Exception e) {
			/* ignore */
		}

	}
}
