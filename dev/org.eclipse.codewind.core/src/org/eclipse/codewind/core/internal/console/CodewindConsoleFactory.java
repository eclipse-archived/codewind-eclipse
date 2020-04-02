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

package org.eclipse.codewind.core.internal.console;

import org.eclipse.codewind.core.internal.Logger;
import org.eclipse.codewind.core.internal.CodewindApplication;
import org.eclipse.codewind.core.internal.messages.Messages;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.IOConsole;

public class CodewindConsoleFactory {

	static final String CODEWIND_CONSOLE_TYPE = "codewind-console"; //$NON-NLS-1$

	public static SocketConsole createLogFileConsole(CodewindApplication app, ProjectLogInfo logInfo) throws Exception {
		String consoleName;
		consoleName = NLS.bind(Messages.LogFileConsoleName, new String[] {app.name, app.connection.getName(), logInfo.logName});
		SocketConsole console = new SocketConsole(consoleName, logInfo, app);
		onNewConsole(console);
		return console;
	}

	private static void onNewConsole(IOConsole console) {
		IConsoleManager consoleManager = ConsolePlugin.getDefault().getConsoleManager();

		// See if a console exists matching this one and remove it if it does,
		// so that we don't have multiple of the same console (they would be identical anyway)
		IConsole[] existingMCConsoles = consoleManager.getConsoles();
		for (IConsole existingConsole : existingMCConsoles) {
			if (existingConsole.getName().equals(console.getName())) {
				consoleManager.removeConsoles(new IConsole[] { existingConsole } );
				break;
			}
		}
			
		Logger.log(String.format("Creating new application console: %s of type %s", 				//$NON-NLS-1$
				console.getName(), console.getClass().getSimpleName()));

		consoleManager.addConsoles(new IConsole[] { console });
	}
}
