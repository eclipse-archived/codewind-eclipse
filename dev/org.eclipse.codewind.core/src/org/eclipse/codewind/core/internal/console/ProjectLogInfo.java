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

package org.eclipse.codewind.core.internal.console;

public class ProjectLogInfo {
	
	public final String type;
	public final String logName;
	public final String workspaceLogPath;
	
	public ProjectLogInfo(String type, String logName, String workspaceLogPath) {
		this.type = type;
		this.logName = logName;
		this.workspaceLogPath = workspaceLogPath;
	}
	
	public boolean isThisLogInfo(String type, String logName) {
		return (this.type.equals(type) && this.logName.equals(logName));
	}
	
	public boolean isThisLogInfo(ProjectLogInfo logInfo) {
		return (this.type.equals(logInfo.type) && this.logName.equals(logInfo.logName));
	}

}
