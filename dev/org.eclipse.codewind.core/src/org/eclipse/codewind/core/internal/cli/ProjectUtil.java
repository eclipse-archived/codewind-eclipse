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

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeoutException;

import org.eclipse.codewind.core.internal.Logger;
import org.eclipse.codewind.core.internal.ProcessHelper;
import org.eclipse.codewind.core.internal.ProcessHelper.ProcessResult;
import org.eclipse.codewind.core.internal.constants.CoreConstants;
import org.eclipse.codewind.core.internal.constants.ProjectInfo;
import org.eclipse.codewind.core.internal.messages.Messages;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.osgi.util.NLS;
import org.json.JSONException;
import org.json.JSONObject;

public class ProjectUtil {
	

	private static final String PROJECT_CMD = "project";
	private static final String CREATE_OPTION = "create";
	private static final String BIND_OPTION = "bind";
	
	private static final String URL_OPTION = "--url";
	private static final String NAME_OPTION = "--name";
	private static final String LANGUAGE_OPTION = "--language";
	private static final String TYPE_OPTION = "--type";
	private static final String PATH_OPTION = "--path";
	private static final String DEP_ID_OPTION = "--depid";

	public static void createProject(String name, String path, String url, IProgressMonitor monitor) throws IOException, JSONException, TimeoutException {
		SubMonitor mon = SubMonitor.convert(monitor, NLS.bind(Messages.CreateProjectTaskLabel, name), 100);
		Process process = null;
		try {
			process = CLIUtil.runCWCTL(PROJECT_CMD, CREATE_OPTION, path, URL_OPTION, url);
			ProcessResult result = ProcessHelper.waitForProcess(process, 500, 300, mon);
			if (result.getExitValue() != 0) {
				Logger.logError("Project create failed with rc: " + result.getExitValue() + " and error: " + result.getErrorMsg()); //$NON-NLS-1$ //$NON-NLS-2$
				throw new IOException(result.getErrorMsg());
			}
			if (result.getOutput() == null || result.getOutput().trim().isEmpty()) {
				// This should not happen
				Logger.logError("Project create had 0 return code but the output is empty"); //$NON-NLS-1$
				throw new IOException("The output from project create is empty."); //$NON-NLS-1$
			}
			JSONObject resultJson = new JSONObject(result.getOutput());
			if (!CoreConstants.VALUE_STATUS_SUCCESS.equals(resultJson.getString(CoreConstants.KEY_STATUS))) {
				String msg = "Project create failed for project: " + name + " with output: " + result.getOutput(); //$NON-NLS-1$ //$NON-NLS-2$
				Logger.logError(msg);
				throw new IOException(msg);
			}
		} finally {
			if (process != null && process.isAlive()) {
				process.destroy();
			}
		}
	}
	
	public static void bindProject(String name, String path, String language, String projectType, String depId, IProgressMonitor monitor) throws IOException, TimeoutException {
		SubMonitor mon = SubMonitor.convert(monitor, NLS.bind(Messages.BindingProjectTaskLabel, name), 100);
		Process process = null;
		try {
			List<String> options = Arrays.asList(new String[] {BIND_OPTION, NAME_OPTION, name, LANGUAGE_OPTION, language, TYPE_OPTION, projectType, PATH_OPTION, path});
			if (depId != null) {
				options.add(DEP_ID_OPTION);
				options.add(depId);
			}
			process = CLIUtil.runCWCTL(PROJECT_CMD, options.toArray(new String[options.size()]));
			ProcessResult result = ProcessHelper.waitForProcess(process, 500, 300, mon);
			if (result.getExitValue() != 0) {
				Logger.logError("Project bind failed with rc: " + result.getExitValue() + " and error: " + result.getErrorMsg()); //$NON-NLS-1$ //$NON-NLS-2$
				throw new IOException(result.getErrorMsg());
			}
		} finally {
			if (process != null && process.isAlive()) {
				process.destroy();
			}
		}
	}
	
	public static ProjectInfo validateProject(String name, String path, String hint, IProgressMonitor monitor) throws IOException, JSONException, TimeoutException {
		SubMonitor mon = SubMonitor.convert(monitor, NLS.bind(Messages.ValidateProjectTaskLabel, name), 100);
		Process process = null;
		try {
			process = (hint == null) ? 
					CLIUtil.runCWCTL(PROJECT_CMD, CREATE_OPTION, path) : CLIUtil.runCWCTL(PROJECT_CMD, CREATE_OPTION, path, "-t", hint);
			ProcessResult result = ProcessHelper.waitForProcess(process, 500, 300, mon);
			if (result.getExitValue() != 0) {
				Logger.logError("Project validate failed with rc: " + result.getExitValue() + " and error: " + result.getErrorMsg()); //$NON-NLS-1$ //$NON-NLS-2$
				throw new IOException(result.getErrorMsg());
			}
			if (result.getOutput() == null || result.getOutput().trim().isEmpty()) {
				// This should not happen
				Logger.logError("Project validate had 0 return code but the output is empty"); //$NON-NLS-1$
				throw new IOException("The output from project validate is empty."); //$NON-NLS-1$
			}
		    
			JSONObject resultJson = new JSONObject(result.getOutput());
			if (CoreConstants.VALUE_STATUS_SUCCESS.equals(resultJson.getString(CoreConstants.KEY_STATUS))) {
				if (resultJson.has(CoreConstants.KEY_RESULT)) {
					JSONObject typeJson = resultJson.getJSONObject(CoreConstants.KEY_RESULT);
					String language = typeJson.getString(CoreConstants.KEY_LANGUAGE);
					String projectType = typeJson.getString(CoreConstants.KEY_PROJECT_TYPE);
					return new ProjectInfo(projectType, language);
				}
			}
			String msg = "Validation failed for project: " + name + " with output: " + result.getOutput(); //$NON-NLS-1$ //$NON-NLS-2$
			Logger.logError(msg);
			throw new IOException(msg);
		} finally {
			if (process != null && process.isAlive()) {
				process.destroy();
			}
		}
	}
	
	public static ProjectInfo validateProject(String name, String path, IProgressMonitor monitor) throws IOException, JSONException, TimeoutException {
		return validateProject(name, path, null, monitor);
	}
}
