/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.codewind.filewatchers;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.UUID;

import org.eclipse.codewind.filewatchers.core.FWLogger;
import org.eclipse.codewind.filewatchers.core.Filewatcher;
import org.eclipse.codewind.filewatchers.core.FilewatcherUtils;
import org.eclipse.codewind.filewatchers.core.IPlatformWatchService;

public class Main {

	public static void main(String[] args) throws IOException, URISyntaxException, InterruptedException {

		String url;

		if (args.length == 0) {
			url = "https://localhost:9090";

		} else if (args.length == 1) {
			url = args[0];
			if (!url.startsWith("http://") && !url.startsWith("https://")) {
				System.err.println("Argument should begin with http:// or https://.");
				return;
			}

		} else {
			System.err.println("Argument should be URL to server instance.");
			return;
		}

		File logOutputDir = new File(System.getProperty("user.home"), ".codewind");
		logOutputDir.mkdir();

		FWLogger logger = FWLogger.getInstance();
		logger.setOutputLogsToScreen(true);
		logger.setRollingFileLoggerOutputDir(logOutputDir);

		IPlatformWatchService platformWatchService = new JavaNioWatchService();

		String pathToCli = System.getenv("MOCK_CWCTL_INSTALLER_PATH");

		Filewatcher fw = new Filewatcher(url, UUID.randomUUID().toString(), platformWatchService, null, pathToCli,
				null);

		while (true) {
			FilewatcherUtils.sleepIgnoreInterrupt(1000);
		}
	}
}
