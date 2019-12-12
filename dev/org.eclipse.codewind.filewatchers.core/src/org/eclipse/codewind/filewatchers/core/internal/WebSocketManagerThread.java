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
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

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
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * The purpose of this class is to initiate and maintain the WebSocket
 * connection between the filewatcher and the server.
 * 
 * After queueEstablishConnection(...) is called, we will keep trying to connect
 * to the server until it succeeds. If that connection ever goes down for any
 * reason, queueEstablishConnection() still start the reconnection process over
 * again.
 * 
 * This class also sends a simple "keep alive" packet every X seconds (eg 25).
 */
public class WebSocketManagerThread extends Thread {

	@SuppressWarnings("unused")
	private final JettyClientEndpoint endpoint;

	private final String wsUrl;

	private final Filewatcher watcher;

	private final AtomicBoolean disposed = new AtomicBoolean(false);

	private final FWLogger log = FWLogger.getInstance();

	private final static int SEND_KEEPALIVE_EVERY_X_SECONDS = 25;

	private final static int CONNECTION_WAIT_TIME_IN_SECONDS = 15;

	/** Synchronize on 'lock' when accessing this */
	private Session mostRecentSessionFromEndpoint_synch_lock = null;

	private final Object lock = new Object();

	private final WebSocketClient wsClient;

	private final AuthTokenWrapper authTokenWrapper;

	/**
	 * Synchronize on this before accessing. This will only ever contain 0 or 1
	 * items.
	 */
	private final List<Long /* nano time of connection request */> establishConnectionRequests_synch = new ArrayList<>();

	public WebSocketManagerThread(String wsUrl, Filewatcher watcher) {
		setDaemon(true);
		setName(WebSocketManagerThread.class.getName());
		this.endpoint = new JettyClientEndpoint(this, wsUrl);
		this.wsUrl = wsUrl;
		this.watcher = watcher;

		this.authTokenWrapper = watcher.internal_getAuthTokenWrapper();

		// Trust all TLS/SSL certificates and allow large messages

		SslContextFactory.Client ssl = new SslContextFactory.Client();
		ssl.setTrustAll(true);

		// Jetty does not like the IBM Java cipher suite names
		// (https://github.com/eclipse/jetty.project/issues/2921), so we clear the
		// client's exclude list and rely on the server to use a sane cipher set suite
		// list.
		ssl.setExcludeCipherSuites();

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
				return null;
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

			ssl.setHostnameVerifier(hostnameVerifier);
			ssl.setSslContext(ctx);

		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		} catch (KeyManagementException e) {
			throw new RuntimeException(e);
		}

		wsClient = new WebSocketClient(ssl);
		wsClient.setMaxTextMessageBufferSize(1024 * 1024);
		wsClient.setMaxIdleTimeout(24 * 60 * 60 * 1000);
		wsClient.getPolicy().setMaxTextMessageSize(1024 * 1024);
		try {
			wsClient.start();
		} catch (Exception e) {
			throw new RuntimeException(e); // Convert to unchecked
		}

	}

	@Override
	public void run() {
		try {

			Long nextPingInNanos = null;

			log.logInfo("Web socket manager thread for '" + wsUrl + "' begun.");

			while (!disposed.get()) {

				try {

					// Send a ping packet to keep the connection alive, every X seconds (eg 25).
					if (nextPingInNanos == null) {
						nextPingInNanos = System.nanoTime()
								+ TimeUnit.NANOSECONDS.convert(SEND_KEEPALIVE_EVERY_X_SECONDS, TimeUnit.SECONDS);
					}

					// Check if another thread is requesting that we (re)establish the WebSocket
					boolean establishConnection = false;
					synchronized (establishConnectionRequests_synch) {
						if (establishConnectionRequests_synch.size() > 0) {
							establishConnection = true;
						} else {
							establishConnectionRequests_synch.wait(100);
						}
					}

					if (establishConnection) {
						// Attempt to establish WS connection
						establishOrReestablishConnection();
						// At this point we will necessarily always have succeeded (or the thread is
						// dead)

						// Clear the connection requests after a success.
						synchronized (establishConnectionRequests_synch) {
							establishConnectionRequests_synch.clear();
						}
						nextPingInNanos = null;

					} else if (nextPingInNanos != null && System.nanoTime() > nextPingInNanos) {
						try {
							Session currSession;
							synchronized (lock) {
								currSession = mostRecentSessionFromEndpoint_synch_lock;
							}
							if (currSession != null) {
								currSession.getRemote().sendString("{}");
							}
						} catch (Throwable t) {
							/* ignore: session is probably closed or dead, so no ping needed. */
						}
						nextPingInNanos = null;

					}

				} catch (Throwable t) {
					// Prevent this thread from dying unnaturally
					log.logSevere("Unexpected exception occurred.", t, null);
					FilewatcherUtils.sleepIgnoreInterrupt(1000);
				}

			} // end while
		} finally {
			log.logInfo("Web socket thread for '" + wsUrl + "' ended.");
		}
	}

	/**
	 * If the websocket connection fails, we should issue a new GET request to
	 * ensure we have the latest state.
	 */
	public void informConnectionFail() {
		if (disposed.get()) {
			return;
		}
		watcher.refreshWatchStatus();
	}

	public void queueEstablishConnection() {
		if (disposed.get()) {
			return;
		}

		// Only queue if it's empty
		synchronized (establishConnectionRequests_synch) {
			if (establishConnectionRequests_synch.size() == 0) {
				establishConnectionRequests_synch.add(System.nanoTime());
				establishConnectionRequests_synch.notify();

				log.logInfo("Establish connection queued for '" + wsUrl + "', and accepted.");
			} else {
				log.logInfo("Establish connection queued for '" + wsUrl + "', but ignored.");
			}
		}
	}

	public void dispose() {
		if (disposed.get()) {
			return;
		}

		disposed.set(true);

		log.logInfo("disposed() called in " + this.getClass().getSimpleName());

		FilewatcherUtils.newThread(() -> {

			Session sess = null;
			synchronized (lock) {
				sess = mostRecentSessionFromEndpoint_synch_lock;
			}
			if (sess != null) {
				try {
					sess.close();
				} catch (Exception e) {
					/* ignore */
				}
			}

			try {
				wsClient.stop();
			} catch (Exception e) {
				/* ignore */
			}

		});

	}

	private void establishOrReestablishConnection() {
		boolean success = false;

		ExponentialBackoffUtil delay = FilewatcherUtils.getDefaultBackoffUtil(4000);

		int attemptNumber = 1;

		while (!disposed.get() && !success) {
			try {
				log.logInfo("Attempting to establish connection to web socket, attempt #" + attemptNumber);

				String url = wsUrl + "/websockets/file-changes/v1";

				JettyClientEndpoint clientEndpoint = new JettyClientEndpoint(this, url);
				ClientUpgradeRequest request = new ClientUpgradeRequest();

				Future<Session> session = wsClient.connect(clientEndpoint, new URI(url), request);

				try {
					Session sess = session.get(CONNECTION_WAIT_TIME_IN_SECONDS, TimeUnit.SECONDS);
					success = sess.isOpen();

					// TODO: We need to send a message in both the auth and non-auth case, to make
					// the server implementation easier. See issue for details.

					// TODO: Re-enable this once FW WebSocket has authentication
					// (https://github.com/eclipse/codewind/issues/1342)
//					FWAuthToken token = authTokenWrapper.getLatestToken().orElse(null);
//					if (token != null) {
//
//						JSONObject obj = new JSONObject();
//						obj.put("token", token.getAccessToken());
//						sess.getRemote().sendString(obj.toString());
//					}

				} catch (Exception e) {

					String msg = "Unable to connect to web socket: " + e.getClass().getSimpleName() + " (attempt #"
							+ attemptNumber + "): " + e.getMessage();
					log.logError(msg);

					if (e.getCause() != null && (e.getCause() instanceof ConnectException)
							&& ((ConnectException) e.getCause()).getMessage().contains("Connection refused")) {
						// Catch TimeoutException or NPE
					} else {
						e.printStackTrace();

					}
					success = false;

				}

				if (success) {
					log.logInfo("Established connection to web socket, after attempt #" + attemptNumber);
					delay.successReset();

					watcher.refreshWatchStatus();

				} else {
					log.logError("Unable to establish connection to web socket on attempt #" + attemptNumber);
				}

			} catch (Throwable t) {
				String msg = "Unable to connect to web socket: " + t.getClass().getSimpleName() + " (attempt #"
						+ attemptNumber + "): " + t.getMessage();
				log.logError(msg);

				success = false;

				if (t instanceof ConnectException && t.getMessage().contains("Connection refused")) {
					// Ignore, this is a standard 'can't connect' exception
				} else {
					// Otherwise, print the full trace.
					t.printStackTrace();
				}

			}

			if (!success && !disposed.get()) {
				delay.sleepIgnoreInterrupt();
				delay.failIncrease();
				attemptNumber++;
			}

		}

	}

	void setSessionFromEndpoint(Session s) {
		synchronized (lock) {
			mostRecentSessionFromEndpoint_synch_lock = s;
		}
	}

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
