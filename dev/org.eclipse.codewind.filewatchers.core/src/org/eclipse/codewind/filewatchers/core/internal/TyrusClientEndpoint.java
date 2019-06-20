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

import javax.websocket.CloseReason;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.Session;

import org.eclipse.codewind.filewatchers.core.FWLogger;

/**
 * Implements the Java WebSocket Endpoint API using the Tyrus WS Client library.
 * 
 * On connection success, this class informs of its parent of the success.
 * 
 * On connection failure, this class informs its parentso that a new connection
 * can be established.
 * 
 * When a message is received, this message is passed to it's parent, who will
 * use that message (likely containing watch changes) to update the
 * ProjectToWatch list.
 */
public class TyrusClientEndpoint extends Endpoint {

	private static final FWLogger log = FWLogger.getInstance();
	private final WebSocketManagerThread parent;

	/** For debug purposes. */
	private final String wsUrl;

	public TyrusClientEndpoint(WebSocketManagerThread parent, String wsUrl) {
		this.parent = parent;
		this.wsUrl = wsUrl;
	}

	@Override
	public void onOpen(Session session, EndpointConfig ec) {
		log.logInfo("WebSocket connection opened for " + wsUrl);
		session.addMessageHandler(new StringMessageHandler(this, session));
		if (parent != null) {
			parent.setSessionFromEndpoint(session);
		}
	}

	@Override
	public void onClose(Session session, CloseReason closeReason) {
		log.logInfo("WebSocket connection closed with reason: " + closeReason + ", for url: " + wsUrl);
		if (parent != null) {
			parent.setSessionFromEndpoint(null);
			parent.queueEstablishConnection();
			parent.informConnectionFail();
		}
	}

	@Override
	public void onError(Session session, Throwable thr) {
		log.logSevere("WebSocket onError throwable: " + thr + " for url: " + wsUrl);
	}

	/**
	 * Receive messages sent to our on this endpoint, and pass them back to our
	 * parent.
	 */
	private static class StringMessageHandler implements MessageHandler.Whole<String> {

		private static final FWLogger log = FWLogger.getInstance();

		final TyrusClientEndpoint endpoint;
		@SuppressWarnings("unused")
		final Session session;

		public StringMessageHandler(TyrusClientEndpoint parent, Session session) {
			this.endpoint = parent;
			this.session = session;
		}

		@Override
		public void onMessage(String s) {
			if (endpoint.parent == null) {
				return;
			}

			endpoint.parent.receiveMessage(s);
		}

	}

}
