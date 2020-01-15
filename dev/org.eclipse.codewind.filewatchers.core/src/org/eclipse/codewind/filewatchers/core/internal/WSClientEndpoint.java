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

import java.io.EOFException;
import java.net.SocketException;

import org.eclipse.codewind.filewatchers.core.FWLogger;

import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

/**
 * On connection success, this class informs of its parent of the success.
 * 
 * On connection failure, this class informs its parent so that a new connection
 * can be established.
 * 
 * When a message is received, this message is passed to it's parent, who will
 * use that message (likely containing watch changes) to update the
 * ProjectToWatch list.
 */
public class WSClientEndpoint extends WebSocketListener {

	private static final FWLogger log = FWLogger.getInstance();
	private final WebSocketManagerThread parent;

	/** For debug purposes. */
	private final String wsUrl;

	public WSClientEndpoint(WebSocketManagerThread parent, String wsUrl) {
		this.parent = parent;
		this.wsUrl = wsUrl;
	}

	@Override
	public void onOpen(WebSocket webSocket, Response response) {
		log.logInfo("WebSocket connection opened for " + wsUrl);
		if (parent != null) {
			parent.informConnectionSuccess(webSocket);
		}
	}

	@Override
	public void onClosing(WebSocket webSocket, int code, String reason) {
		log.logInfo("WebSocket connection closed with reason: " + code + " " + reason + ", for url: " + wsUrl);

		if (parent != null) {
			parent.informConnectionClosedOrFailed(webSocket);
		}
	}

	@Override
	public void onMessage(WebSocket webSocket, String msg) {

		if (parent != null) {
			parent.receiveMessage(msg);
		}

	}

	@Override
	public void onFailure(WebSocket webSocket, Throwable thr, Response response) {

		// Only print the throwable if we don't recognize it as a standard failure
		// scenario.
		boolean printThrowable = true;

		if (thr.getMessage() != null) {
			if (thr.getMessage().contains("Failed to connect to ")) {
				printThrowable = false;
			}
		}

		if (thr instanceof EOFException || thr instanceof SocketException) {
			printThrowable = false;
		}

		if (printThrowable) {
			log.logSevere("WebSocket onError throwable: " + thr + " for url: " + wsUrl);
			thr.printStackTrace();
		} else {
			log.logError("WebSocket onError - Unable to connect for url: " + wsUrl);

		}

		if (parent != null) {
			parent.informConnectionClosedOrFailed(webSocket);
		}

	}

}
