/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.codewind.core.internal;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.StringTokenizer;

public class FileUtil {
	
    public static boolean makeDir(String path) {
        boolean result = true;

        if (path != null) {
            try {
                File fp = new File(path);
                if (!fp.exists() || !fp.isDirectory()) {
                    // Create the directory.
                    result = fp.mkdirs();
                }
            } catch (Exception e) {
                Logger.logError("Failed to create directory: " + path, e);
                result = false;
            }
        }
        return result;
    }
    
    public static void copyFile(InputStream inStream, String path) throws IOException, FileNotFoundException {
    	FileOutputStream outStream = null;
    	try {
    		outStream = new FileOutputStream(path);
    		byte[] bytes = new byte[1024];
    		int bytesRead = 0;
    		while ((bytesRead = inStream.read(bytes)) > 0) {
    			outStream.write(bytes, 0, bytesRead);
    		}
    	} finally {
    		if (outStream != null) {
    			try {
					outStream.close();
				} catch (IOException e) {
					// Ignore
				}
    		}
    	}
    }
    
    /**
     * Removes the given directory.
     * If recursive is true, any files and subdirectories within
     * the directory will also be deleted; otherwise the
     * operation will fail if the directory is not empty.
     */
    public static void deleteDirectory(String dir, boolean recursive) throws IOException {
        if (dir == null || dir.length() <= 0) {
            return;
        }

        // Safety feature. Prevent to remove directory from the root
        // of the drive, i.e. directory with less than 2 file separator.
        if ((new StringTokenizer(dir.replace(File.separatorChar, '/'), "/")).countTokens() < 2) {
            return;
        }

        File fp = new File(dir);
        if (!fp.exists() || !fp.isDirectory())
            throw new IOException("Directory does not exist: " + fp.toString());

        if (recursive) {
            // Remove the contents of the given directory before delete.
            String[] fileList = fp.list();
            if (fileList != null) {
                String curBasePath = dir + File.separator;
                for (int i = 0; i < fileList.length; i++) {
                    // Remove each file one at a time.
                    File curFp = new File(curBasePath + fileList[i]);
                    if (curFp.exists()) {
                        if (curFp.isDirectory()) {
                            // Remove the directory and sub directories;
                            deleteDirectory(dir + File.separator + fileList[i], recursive);
                        } else {
                            if (!curFp.delete())
                                Logger.log("Could not delete " + curFp.getName());
                        }
                    }
                }
            }
        }
        boolean isSuccess = fp.delete();

        if (!isSuccess) {
            throw new IOException("Directory cannot be removed.");
        }
    }
    
    public static String getCanonicalPath(String path) {
        String canonicalPath = path;
        try {
            canonicalPath = (new File(path)).getCanonicalPath();
        } catch (Exception e) {
            Logger.log("Failed to get the canonical path for: " + path + ". " + e.getMessage());
        }
        return canonicalPath;
    }

}
