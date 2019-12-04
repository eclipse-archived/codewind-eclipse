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

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Optional;

import org.eclipse.codewind.filewatchers.core.FWAuthToken;
import org.eclipse.codewind.filewatchers.core.FWLogger;
import org.eclipse.codewind.filewatchers.core.IAuthTokenProvider;

/**
 * AuthTokenWrapper is the conduit through the internal filewatcher codebase
 * requests secure authentication tokens from the IDE. In cases where the
 * authTokenProvider is null (eg is secure auth is not required), the methods of
 * this class are no-ops.
 * 
 * This class is thread-safe.
 * 
 * This class was created as part of issue codewind/1309.
 */
public class AuthTokenWrapper {

	private static final int KEEP_LAST_X_STALE_KEYS = 10;

	private static final FWLogger log = FWLogger.getInstance();

	private final Object lock = new Object();

	/**
	 * Contains an ordered (descending by creation time) list of invalids keys, with
	 * at most KEEP_LAST_X_STALE_KEYS keys.
	 */
	private final Deque<FWAuthToken> recentInvalidKeysQueue_synch_lock = new ArrayDeque<>();

	/**
	 * Contains invalid keys; used as a fast path to determine if a given key is
	 * already invalid. The should be at most KEEP_LAST_X_STALE_KEYS here.
	 */
	private final HashSet<FWAuthToken> invalidKeysMap_synch_lock = new HashSet<>();

	/**
	 * Retrieve the auth token from the IDE, if available; null if the connection
	 * does not need an auth token.
	 */
	private final IAuthTokenProvider authTokenProvider;

	public AuthTokenWrapper(IAuthTokenProvider authTokenProvider /* nullable */ ) {

		this.authTokenProvider = authTokenProvider;
	}

	/** Retrieve the latest token from the IDE */
	public Optional<FWAuthToken> getLatestToken() {
		if (authTokenProvider == null) {
			return Optional.empty();
		}

		FWAuthToken token = authTokenProvider.getLatestAuthToken();
		if (token == null) {
			return Optional.empty();
		}

		log.logInfo("IDE returned a new security token to filewatcher: " + digest(token));

		return Optional.of(token);

	}

	/** Inform the IDE when a token is rejected. */
	public void informBadToken(FWAuthToken token) {

		if (authTokenProvider == null || token == null) {
			return;
		}

		synchronized (lock) {
			// We've already reported this key as invalid, so just return it
			if (invalidKeysMap_synch_lock.contains(token)) {
				log.logInfo("Filewatcher informed us of a bad token, but we've already reported it to the IDE: "
						+ digest(token));
				return;
			}

			// We have a new token that we have not previously reported as invalid.

			invalidKeysMap_synch_lock.add(token);
			recentInvalidKeysQueue_synch_lock.offer(token);

			while (recentInvalidKeysQueue_synch_lock.size() > KEEP_LAST_X_STALE_KEYS) {
				FWAuthToken keyToRemove = recentInvalidKeysQueue_synch_lock.poll();
				invalidKeysMap_synch_lock.remove(keyToRemove);
			}

		}

		authTokenProvider.informReceivedInvalidAuthToken(token);

		log.logInfo("Filewatcher informed us of a new invalid token, so we've already reported it to the IDE: "
				+ digest(token));

	}

	/**
	 * Return a representation of the token that is at most 32 characters long, so
	 * as not to overwhelm the log file.
	 */
	private static String digest(FWAuthToken token) {
		if (token == null) {
			return null;
		}

		String key = token.getAccessToken();
		if (key == null) {
			return null;
		}

		return key.substring(0, Math.min(key.length(), 32));
	}

}
