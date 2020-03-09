/*******************************************************************************
 * Copyright (c) 2018, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.codewind.core.internal;

import org.eclipse.codewind.core.internal.connection.CodewindConnection;

public interface IUpdateHandler {
	
	public void updateAll();
	
	public void updateConnection(CodewindConnection connection);
	
	public void updateApplication(CodewindApplication application);
	
	public void removeConnection(CodewindConnection connection);
	
	public void removeApplication(CodewindApplication application);

}
