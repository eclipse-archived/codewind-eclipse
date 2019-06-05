/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.codewind.core.internal;

import org.eclipse.codewind.core.CodewindCorePlugin;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.service.debug.DebugOptions;
import org.eclipse.osgi.service.debug.DebugOptionsListener;

/**
 * Logging utility
 */
public class Logger implements DebugOptionsListener {

	private static final ILog logger = CodewindCorePlugin.getDefault().getLog();

	private static Logger instance;

	private Logger() {
		instance = this;
	}

	/**
	 * This should only be used by the Activator. Call this class's methods statically.
	 * @return
	 */
	public static Logger instance() {
		if (instance == null) {
			instance = new Logger();
		}
		return instance;
	}

	private static boolean
			logInfo;

	private static final String
			INFO_LEVEL = "/debug/info"; //$NON-NLS-1$

	@Override
	public void optionsChanged(DebugOptions debugOptions) {
		// Note that we have to register this class with the debug options service in Activator.start

		// Always log errors.
		// logError = debugOptions.getBooleanOption(org.eclipse.codewind.core.Activator.PLUGIN_ID + ERROR_LEVEL, true);

		// Toggle info logging by creating a .options file in your Eclipse installation directory with this content:
		// org.eclipse.codewind.core/debug/info=true
		// and then passing eclipse the '-debug' option

		logInfo = debugOptions.getBooleanOption(CodewindCorePlugin.PLUGIN_ID + INFO_LEVEL, false);
	}

	public static void log(String msg) {
		writeLog(msg, false, null);
	}

	public static void logError(String msg) {
		writeLog(msg, true, null);
	}

	public static void logError(Throwable t) {
		logError("Exception occurred:", t); //$NON-NLS-1$
	}

	public static void logError(String msg, Throwable t) {
		writeLog(msg, true, t);
	}

	/**
	 * Log the given message to stdout or stderr, depending on isError.
	 * The message is prepended with a timestamp, as well as the caller's class name, method name, and line number.
	 */
	private static void writeLog(String msg, boolean isError, Throwable t) {
		if (!isError && !logInfo) {
			// Not logging info at this time; do nothing.
			return;
		}

		StackTraceElement[] ste = Thread.currentThread().getStackTrace();
		StackTraceElement callingMethod = null;
		for (int x = 0; x < ste.length; x++) {
			if (ste[x].getMethodName().equals("writeLog")) { //$NON-NLS-1$
				callingMethod = ste[x++];
				// Skip over logging methods, we want to print their callers.
				while (callingMethod.getMethodName().equals("writeLog") //$NON-NLS-1$
						|| callingMethod.getMethodName().equals("log") //$NON-NLS-1$
						|| callingMethod.getMethodName().equals("logError")) { //$NON-NLS-1$
					callingMethod = ste[x++];
				}
				break;
			}
		}

		// String time = TIME_SDF.format(Calendar.getInstance().getTime());

		String callerInfo = "unknown"; //$NON-NLS-1$

		if (callingMethod != null) {
			String className = callingMethod.getClassName();
			String simpleClassName = className.substring(className.lastIndexOf('.') + 1);

			callerInfo = String.format("%s.%s:%s", //$NON-NLS-1$
					simpleClassName, callingMethod.getMethodName(), callingMethod.getLineNumber());
		}

		String type = isError ? "ERROR" : "INFO"; //$NON-NLS-1$ //$NON-NLS-2$
		String fullMessage = String.format("[%s %s] %s", type, callerInfo, msg); //$NON-NLS-1$

		int level = isError ? IStatus.ERROR : IStatus.INFO;
		IStatus status;

		if (t != null) {
			status = new Status(level, CodewindCorePlugin.PLUGIN_ID, fullMessage, t);
		}
		else {
			status = new Status(level, CodewindCorePlugin.PLUGIN_ID, fullMessage);
		}

		logger.log(status);
	}
}
