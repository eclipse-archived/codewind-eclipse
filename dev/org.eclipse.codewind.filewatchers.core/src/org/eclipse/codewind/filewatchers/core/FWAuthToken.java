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

package org.eclipse.codewind.filewatchers.core;

/**
 * Immutable authorization token for use in HTTP(S) requests and WebSocket
 * connections
 */
public class FWAuthToken {

	private final String accessToken;

	private final String tokenType;

	public FWAuthToken(String token, String tokenType) {
		if (token == null || tokenType == null) {
			throw new IllegalArgumentException("Null args in FWAuthToken: " + token + " " + tokenType);
		}

		this.accessToken = token;
		this.tokenType = tokenType;
	}

	public String getAccessToken() {
		return accessToken;
	}

	public String getTokenType() {
		return tokenType;
	}

	private String key() {
		return accessToken + "/" + tokenType;
	}

	@Override
	public String toString() {
		return key();
	}

	@Override
	public int hashCode() {
		return key().hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof FWAuthToken)) {
			return false;
		}

		FWAuthToken other = (FWAuthToken) obj;

		return other.key().equals(this.key());
	}

}
