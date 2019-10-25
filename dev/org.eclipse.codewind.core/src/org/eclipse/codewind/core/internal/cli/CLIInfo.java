/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *	 IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.codewind.core.internal.cli;

import java.util.Map;

import org.eclipse.codewind.core.internal.PlatformUtil.OperatingSystem;

public class CLIInfo {
	private String installPath = null;
	private Map<OperatingSystem, String> osPathMap;
	private String name = null;
	
	public CLIInfo(String name, Map<OperatingSystem, String> osPathMap) {
		this.osPathMap = osPathMap;
		this.name = name;
	}
	
	/**
	 * Set the path of installation
	 * @param installPath full path of installation
	 */
	public void setInstallPath(String installPath) {
		this.installPath = installPath;
	}
	
	/**
	 * Get path of installation
	 * @return a string with the full path, null if not installed
	 */
	public String getInstallPath() {
		return installPath;
	}
	
	/**
	 * Get the operating system map for the paths
	 * @return a map with the operating system as keys and
	 * the paths as values
	 */
	public Map<OperatingSystem, String> getOSPathMap(){
		return osPathMap;
	}
	
	/**
	 * Get the name of the CLI info
	 * @return name of the CLI info
	 */
	public String getInstallName() {
		return name;
	}
}
