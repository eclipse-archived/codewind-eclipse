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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * This class is responsible for taking the filename/path filters for a project
 * on the watched projects list, and applying those filters against a given path
 * string (returning true if a filter should be ignored).
 */
public class PathFilter {

	private static final FWLogger log = FWLogger.getInstance();

	private final List<Pattern> filenameExcludePatterns = new ArrayList<>();
	private final List<Pattern> pathExcludePatterns = new ArrayList<>();

	public PathFilter(ProjectToWatch ptw) {

		if (ptw.getIgnoredFilenames() != null) {
			ptw.getIgnoredFilenames().forEach(e -> {

				if (e.contains("/") || e.contains("\\")) {
					log.logSevere("Ignored filenames may not contain path separators: " + e);
					return;
				}

				String text = e.replace("*", ".*");
				filenameExcludePatterns.add(Pattern.compile(text));
			});
		}

		if (ptw.getIgnoredPaths() != null) {
			ptw.getIgnoredPaths().forEach(e -> {
				if (e.contains("\\")) {
					log.logSevere("Ignore paths may not contain Windows-style path separators: " + e);
					return;
				}

				String text = e.replace("*", ".*");
				pathExcludePatterns.add(Pattern.compile(text));
			});
		}

	}

	/**
	 * File parameter should be relative path from project root, rather than an
	 * absolute path.
	 */
	public boolean isFilteredOutByFilename(String path) {

		if (path.contains("\\")) {
			log.logSevere("Parameter cannot contain Window-style file paths");
			return false;
		}

		for (String name : path.split("/")) {

			for (Pattern p : filenameExcludePatterns) {
				if (p.matcher(name).matches()) {
					return true;
				}
			}

		}

		return false;
	}

	/**
	 * File parameter should be relative path from project root, rather than an
	 * absolute path.
	 */
	public boolean isFilteredOutByPath(String path) {

		if (path.contains("\\")) {
			log.logSevere("Parameter cannot contain Window-style file paths");
			return false;
		}

		for (Pattern p : pathExcludePatterns) {
			if (p.matcher(path).matches()) {
				return true;
			}
		}

		return false;
	}

}
