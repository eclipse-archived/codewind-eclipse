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

package org.eclipse.codewind.filewatchers.core;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Various utilities related to converting Windows-style paths (ex: c:\Users)
 * to/from our standardized Unix-style format (ex: /C/users), in order to
 * conform to our watcher API specification. See PathUtilsTest for additional
 * details.
 */
public class PathUtils {

	private static final FWLogger log = FWLogger.getInstance();

	/**
	 * A windows absolute path will begin with a letter followed by a colon: C:\
	 */
	private static boolean isWindowsAbsolutePath(String absolutePath) {
		if (absolutePath.length() < 2) {
			return false;
		}

		char char0 = absolutePath.charAt(0);

		if (!Character.isLetter(char0)) {
			return false;
		}

		if (absolutePath.charAt(1) != ':') {
			return false;
		}

		return true;
	}

	/** Ensure that the drive is lowercase for Unix-style paths from Windows. */
	public static String normalizeDriveLetter(String absolutePath) {

		if (absolutePath.contains("\\")) {
			throw new IllegalArgumentException("This function does not support Windows-style paths.");
		}
		if (absolutePath.length() < 2) {
			return absolutePath;
		}

		if (!absolutePath.startsWith("/")) {
			throw new IllegalArgumentException("Path should begin with forward slash: " + absolutePath);
		}

		char char0 = absolutePath.charAt(0);
		char char1 = absolutePath.charAt(1);

		// Special case the absolute path of only 2 characters.
		if (absolutePath.length() == 2) {
			if (char0 == '/' && Character.isLetter(char1) && Character.isUpperCase(char1)) {
				return "/" + Character.toLowerCase(char1);
			} else {
				return absolutePath;
			}

		}

		char char2 = absolutePath.charAt(2);
		if (char0 == '/' && char2 == '/' && Character.isLetter(char1) && Character.isUpperCase(char1)) {

			return "/" + Character.toLowerCase(char1) + char2 + absolutePath.substring(3);

		} else {
			return absolutePath;
		}
	}

	/** C:\helloThere -> /c/helloThere */
	public static String convertFromWindowsDriveLetter(String absolutePath) {

		if (!isWindowsAbsolutePath(absolutePath)) {
			return absolutePath;
		}

		absolutePath = absolutePath.replace("\\", "/");

		char char0 = absolutePath.charAt(0);

		// Strip first two characters
		absolutePath = absolutePath.substring(2);

		absolutePath = "/" + Character.toLowerCase(char0) + absolutePath;

		return absolutePath;

	}

	/** Same as below, but determine behaviour based on OS. */
	public static String convertAbsoluteUnixStyleNormalizedPathToLocalFile(String str) {
		if (File.separator.equals("/")) {
			// For Mac/Linux, nothing to do
			return str;
		}

		return convertAbsoluteUnixStyleNormalizedPathToLocalFile(str, true);
	}

	/* Convert /c/Users/Administrator to c:\Users\Administrator */
	public static String convertAbsoluteUnixStyleNormalizedPathToLocalFile(String str, boolean isWindows) {

		if (!isWindows) {
			return str;
		}

		if (!str.startsWith("/")) {
			throw new IllegalArgumentException("Parameters must begin with slash");
		}

		if (str.length() <= 1) {
			throw new IllegalArgumentException("Cannot convert string with length of 0 or 1: " + str);
		}

		char driveLetter = str.charAt(1);
		if (!Character.isLetter(driveLetter)) {
			throw new IllegalArgumentException("Missing drive letter: " + str);
		}

		if (str.length() == 2) {
			return driveLetter + ":\\";
		}

		char secondSlash = str.charAt(2);
		if (secondSlash != '/') {
			throw new IllegalArgumentException("Invalid path format: " + str);
		}

		return driveLetter + ":\\" + str.substring(3).replace("/", "\\");

	}

	public static String stripTrailingSlash(String str) {

		while (str.endsWith("/")) {

			str = str.substring(0, str.length() - 1);

		}

		return str;

	}

	// Strip project parent directory from path:
	// If pathToMonitor is: /home/user/codewind/project
	// and watchEventPath is: /home/user/codewind/project/some-file.txt
	// then this will convert watchEventPath to /some-file.txt
	public static Optional<String> convertAbsolutePathWithUnixSeparatorsToProjectRelativePath(String path,
			String rootPath) {

		if (rootPath.contains("\\")) {
			throw new IllegalArgumentException("Forward slashes are not supported.");
		}

		rootPath = stripTrailingSlash(rootPath);

		if (!path.startsWith(rootPath)) {
			// This shouldn't happen, and is thus severe
			log.logSevere("Watch event '" + path + "' does not match project path '" + rootPath + "'");
			return Optional.empty();
		}

		path = path.replace(rootPath, "");

		if (path.length() == 0) {

			path = "/";
		}

		return Optional.of(path);

	}

	/**
	 * *Take the OS-specific path format, and convert it to a standardized non-OS
	 * specific format. This method should only effect Windows paths.
	 */
	public static String normalizePath(String pathParam) {
		String absPath = pathParam.replace("\\", "/");

		absPath = PathUtils.convertFromWindowsDriveLetter(absPath);
		absPath = PathUtils.normalizeDriveLetter(absPath);

		return stripTrailingSlash(absPath);

	}

	/** "/moo/cow" => [ "/moo/cow", "/moo"] */
	public static List<String> splitRelativeProjectPathIntoComponentPaths(final String path) {
		List<String> result = new ArrayList<>();

		String currPath = path;
		while (true) {

			if (currPath.length() == 1) {
				break;
			}

			result.add(currPath);

			int index = currPath.lastIndexOf("/");
			if (index <= 0) {
				break;
			}

			currPath = currPath.substring(0, index);
		}

		return result;
	}
}
