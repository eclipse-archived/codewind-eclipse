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

import org.eclipse.codewind.filewatchers.core.FWLogger;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;

/**
 * Implements the Java WebSocket Endpoint API using the Jetty WS Client library.
 * 
 * On connection success, this class informs of its parent of the success.
 * 
 * On connection failure, this class informs its parent so that a new connection
 * can be established.
 * 
 * When a message is received, this message is passed to it's parent, who will
 * use that message (likely containing watch changes) to update the
 * ProjectToWatch list.
 */
@WebSocket
public class JettyClientEndpoint {

	private static final FWLogger log = FWLogger.getInstance();
	private final WebSocketManagerThread parent;

	/** For debug purposes. */
	private final String wsUrl;

	public JettyClientEndpoint(WebSocketManagerThread parent, String wsUrl) {
		this.parent = parent;
		this.wsUrl = wsUrl;
	}

	@OnWebSocketConnect
	public void onConnect(Session session) {
		log.logInfo("WebSocket connection opened for " + wsUrl);
		if (parent != null) {
			parent.setSessionFromEndpoint(session);
		}
	}

	@OnWebSocketClose
	public void onClose(int statusCode, String reason) {
		log.logInfo("WebSocket connection closed with reason: " + statusCode + " " + reason + ", for url: " + wsUrl);
		if (parent != null) {
			parent.setSessionFromEndpoint(null);
			parent.queueEstablishConnection();
			parent.informConnectionFail();
		}
	}

	@OnWebSocketMessage
	public void onMessage(String msg) {

		if (parent != null) {
			parent.receiveMessage(msg);
		}

	}

	@OnWebSocketError
	public void onError(Throwable thr) {

		if (thr instanceof ConnectException && thr.getMessage().contains("Connection refused")) {
			log.logError("WebSocket onError throwable: " + thr + " for url: " + wsUrl);
			// Don't print stack trace for known message
		} else {
			log.logSevere("WebSocket onError throwable: " + thr + " for url: " + wsUrl);
			thr.printStackTrace();
		}
	}

}
