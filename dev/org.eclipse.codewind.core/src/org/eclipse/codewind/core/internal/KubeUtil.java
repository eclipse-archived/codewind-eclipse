/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.codewind.core.internal;

public class KubeUtil {
	
	private static final String KUBECTL_CMD = "kubectl"; //$NON-NLS-1$
	private static final String KUBECTL_EXE = "kubectl.exe"; //$NON-NLS-1$
	private static final String OC_CMD = "oc"; //$NON-NLS-1$
	private static final String OC_EXE = "oc.exe"; //$NON-NLS-1$
	
	private static String kubeCommand = null;
	
	public static String getCommand() {
		if (kubeCommand != null) {
			return kubeCommand;
		}
		boolean isWindows = CoreUtil.isWindows();
		String exec = CoreUtil.getExecutablePath(isWindows ? KUBECTL_EXE : KUBECTL_CMD);
		if (exec == null) {
			exec = CoreUtil.getExecutablePath(isWindows ? OC_EXE : OC_CMD);
		}
		kubeCommand = exec;
		return kubeCommand;
	}

}
