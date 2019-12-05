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
 * The Codewind IDE classes should extends this interface to provide
 * filewatcherd with secure authentication tokens from the IDE.
 * 
 * This class was created as part of issue codewind/1309.
 */
public interface IAuthTokenProvider {

	/**
	 * Return the latest auth token; return null if an auth token is not available.
	 * 
	 * This method should be non-blocking (eg it should return null or stale data,
	 * rather then block on issuing a new I/O or CWCTL request to acquire a new
	 * token.)
	 */
	public FWAuthToken getLatestAuthToken();

	/**
	 * Inform the IDE that the server told us that our current token is invalid, at
	 * which point the IDE should then re-acquire/refresh it on a separate thread.
	 * After FWd informs the IDE, the new value should be available to the FWd via a
	 * call to getLatestAuthToken() after some short period of time.
	 * 
	 * Filewatcher to call `informReceivedInvalidAuthToken(...)` ONLY ONCE for a
	 * given invalid token (eg NOT every time the server informs us.)
	 */
	public void informReceivedInvalidAuthToken(FWAuthToken badToken);

}