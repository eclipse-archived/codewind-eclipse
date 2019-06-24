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

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Utility class containing simple standalone utility methods that are used
 * throughout the code.
 */
public class FilewatcherUtils {

	/** Like Thread.sleep(), but buries the InterruptedException */
	public static void sleepIgnoreInterrupt(long sleepTimeInMsecs) {
		try {
			Thread.sleep(sleepTimeInMsecs);
		} catch (InterruptedException e) {
			// Ignore
		}
	}

	/** Like Thread.sleep(), but throws an unchecked exception */
	public static void sleep(long sleepTimeInMsecs) {
		try {
			Thread.sleep(sleepTimeInMsecs);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}

	}

	public static String stripTrailingSlash(String str) {

		while (str.trim().endsWith("/")) {

			str = str.trim();
			str = str.substring(0, str.length() - 1);

		}

		return str;

	}

	public static void newThread(Runnable r) {
		Thread t = new Thread(r);
		t.setDaemon(true);
		t.start();
	}

	public static ExponentialBackoffUtil getDefaultBackoffUtil(long maxFailureDelay) {
		return new ExponentialBackoffUtil(500, maxFailureDelay, 1.5f);

	}

	/**
	 * Implements an exponential backoff, which increases the delay between requests
	 * when a request failure occurs, and resets the delay on success.
	 */
	public static class ExponentialBackoffUtil {
		private final long minFailureDelay;

		private long failureDelay;

		private final long maxFailureDelay;

		private final float backoffExponent;

		public ExponentialBackoffUtil(long minFailureDelay, long maxFailureDelay, float backoffExponent) {
			this.minFailureDelay = minFailureDelay;
			this.maxFailureDelay = maxFailureDelay;
			this.failureDelay = minFailureDelay;
			this.backoffExponent = backoffExponent;
		}

		public void sleep() throws InterruptedException {
			Thread.sleep(failureDelay);
		}

		/** Like Thread.sleep(), but buries the InterruptedException */
		public void sleepIgnoreInterrupt() {
			FilewatcherUtils.sleepIgnoreInterrupt(failureDelay);
		}

		public void failIncrease() {
			failureDelay *= backoffExponent;
			if (failureDelay > maxFailureDelay) {
				failureDelay = maxFailureDelay;
			}
		}

		public void successReset() {
			failureDelay = minFailureDelay;
		}

	}

	public static String convertStackTraceToString(Throwable t) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		t.printStackTrace(pw);
		return sw.toString();
	}
}
