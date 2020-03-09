/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.codewind.core.internal.connection;

import java.net.URI;

import org.eclipse.codewind.core.internal.CodewindManager;
import org.eclipse.codewind.core.internal.messages.Messages;
import org.eclipse.core.runtime.NullProgressMonitor;

public class LocalConnection extends CodewindConnection {
	
	public static final String DEFAULT_NAME = Messages.CodewindLocalConnectionName;
	public static final String DEFAULT_ID = "local";
	
	public LocalConnection(URI uri) {
		super(DEFAULT_NAME, uri, DEFAULT_ID);
	}
	
	@Override
	public boolean isLocal() {
		return true;
	}

	@Override
	public synchronized void onConnectionError() {
		CodewindManager.getManager().refreshInstallStatus(new NullProgressMonitor());
		super.onConnectionError();
	}

}
