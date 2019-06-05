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

import java.io.IOException;

import org.eclipse.codewind.core.CodewindCorePlugin;
import org.eclipse.codewind.core.internal.Logger;
import org.eclipse.codewind.core.internal.CodewindApplication;
import org.eclipse.codewind.core.internal.connection.CodewindSocket;
import org.eclipse.codewind.core.internal.messages.Messages;
import org.eclipse.ui.console.IOConsole;
import org.eclipse.ui.console.IOConsoleOutputStream;

public class SocketConsole extends IOConsole {

	public final CodewindApplication app;
	public final ProjectLogInfo logInfo;
	private final CodewindSocket socket;

	private IOConsoleOutputStream outputStream;
	private boolean isInitialized = false;
	private boolean showOnUpdate = false;

	public SocketConsole(String consoleName, ProjectLogInfo logInfo, CodewindApplication app) {
		super(consoleName, CodewindConsoleFactory.CODEWIND_CONSOLE_TYPE,
				CodewindCorePlugin.getIcon(CodewindCorePlugin.DEFAULT_ICON_PATH),
				true);

		this.app = app;
		this.logInfo = logInfo;
		this.outputStream = newOutputStream();
		this.socket = app.connection.getSocket();
		socket.registerSocketConsole(this);

		try {
			this.outputStream.write(Messages.LogFileInitialMsg);
			app.connection.requestEnableLogStream(app, logInfo);
		} catch (IOException e) {
			Logger.logError("Error opening console output stream for: " + this.getName(), e);
		}
	}

	public void update(String contents, boolean reset) throws IOException {
		if (!isInitialized || reset) {
			clearConsole();
			isInitialized = true;
		}

		Logger.log("Appending contents to log: " + this.getName());		// $NON-NLS-1$
		outputStream.write(contents);
		if (showOnUpdate) {
			activate();
		}
	}

	@Override
	protected void dispose() {
		Logger.log("Dispose console " + getName()); //$NON-NLS-1$

		socket.deregisterSocketConsole(this);

		try {
			app.connection.requestDisableLogStream(app, logInfo);
			outputStream.close();
		} catch (IOException e) {
			Logger.logError("Error closing console output stream for: " + this.getName(), e); //$NON-NLS-1$
		}

		super.dispose();
	}
	
	public void setShowOnUpdate(boolean value) {
		showOnUpdate = value;
	}
}
