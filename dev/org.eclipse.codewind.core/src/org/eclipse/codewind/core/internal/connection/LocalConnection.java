/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
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

public class LocalConnection extends CodewindConnection {
	
	public static final String CONNECTION_ID = "local";
	
	public LocalConnection(String name, URI uri) {
		super(name, uri, CONNECTION_ID, null);
	}
	
	@Override
	public boolean isLocal() {
		return true;
	}

}
