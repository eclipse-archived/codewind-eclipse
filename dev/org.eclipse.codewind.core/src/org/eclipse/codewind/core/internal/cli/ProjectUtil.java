/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
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
import java.util.ArrayList;
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
	private static final String[] CREATE_CMD = new String[] {PROJECT_CMD, "create"};
	private static final String[] BIND_CMD = new String[] {PROJECT_CMD, "bind"};
	private static final String[] REMOVE_CMD = new String[] {PROJECT_CMD, "remove"};
	private static final String[] VALIDATE_CMD = new String[] {PROJECT_CMD, "validate"};
	private static final String[] RESTART_CMD = new String[] {PROJECT_CMD, "restart"};
	
	private static final String URL_OPTION = "--url";
	private static final String NAME_OPTION = "--name";
	private static final String LANGUAGE_OPTION = "--language";
	private static final String TYPE_OPTION = "--type";
	private static final String PATH_OPTION = "--path";
	private static final String PROJECT_ID_OPTION = "--id";
	private static final String DELETE_OPTION = "--delete";
	private static final String STARTMODE_OPTION = "--startmode";

	public static void createProject(String name, String path, String url, String conid, IProgressMonitor monitor) throws IOException, JSONException, TimeoutException {
		SubMonitor mon = SubMonitor.convert(monitor, NLS.bind(Messages.CreateProjectTaskLabel, name), 100);
		Process process = null;
		try {
			process = CLIUtil.runCWCTL(CLIUtil.GLOBAL_JSON_INSECURE, CREATE_CMD, new String[] {PATH_OPTION, path, URL_OPTION, url, CLIUtil.CON_ID_OPTION, conid});
			ProcessResult result = ProcessHelper.waitForProcess(process, 500, 600, mon);
			CLIUtil.checkResult(CREATE_CMD, result, true);
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
	
	public static void bindProject(String name, String path, String language, String projectType, String conid, IProgressMonitor monitor) throws IOException, TimeoutException {
		SubMonitor mon = SubMonitor.convert(monitor, NLS.bind(Messages.BindingProjectTaskLabel, name), 100);
		Process process = null;
		try {
			String[] options = new String[] {NAME_OPTION, name, LANGUAGE_OPTION, language, TYPE_OPTION, projectType, PATH_OPTION, path, CLIUtil.CON_ID_OPTION, conid};
			process = CLIUtil.runCWCTL(CLIUtil.GLOBAL_JSON_INSECURE, BIND_CMD, options);
			ProcessResult result = ProcessHelper.waitForProcess(process, 500, 300, mon);
			CLIUtil.checkResult(BIND_CMD, result, false);
		} finally {
			if (process != null && process.isAlive()) {
				process.destroy();
			}
		}
	}
	
	public static ProjectInfo validateProject(String name, String path, String hint, String conid, IProgressMonitor monitor) throws IOException, JSONException, TimeoutException {
		SubMonitor mon = SubMonitor.convert(monitor, NLS.bind(Messages.ValidateProjectTaskLabel, name), 100);
		Process process = null;
		try {
			process = (hint == null) ?
					CLIUtil.runCWCTL(CLIUtil.GLOBAL_JSON_INSECURE, VALIDATE_CMD, new String[] {PATH_OPTION, path, CLIUtil.CON_ID_OPTION, conid}) :
					CLIUtil.runCWCTL(CLIUtil.GLOBAL_JSON_INSECURE, VALIDATE_CMD, new String[] {TYPE_OPTION, hint, PATH_OPTION, path, CLIUtil.CON_ID_OPTION, conid});
			ProcessResult result = ProcessHelper.waitForProcess(process, 500, 300, mon);
			CLIUtil.checkResult(VALIDATE_CMD, result, true);
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
	
	public static void removeProject(String name, String projectId, IProgressMonitor monitor) throws IOException, TimeoutException {
		removeProject(name, projectId, false, monitor);
	}
	
	public static void removeProject(String name, String projectId, boolean delete, IProgressMonitor monitor) throws IOException, TimeoutException {
		SubMonitor mon = SubMonitor.convert(monitor, NLS.bind(Messages.RemoveProjectTaskLabel, name), 100);
		Process process = null;
		List<String> options = new ArrayList<String>();
		if (delete) {
			options.add(DELETE_OPTION);
		}
		options.add(PROJECT_ID_OPTION);
		options.add(projectId);
		try {
			process = CLIUtil.runCWCTL(CLIUtil.GLOBAL_JSON_INSECURE, REMOVE_CMD, options.toArray(new String[options.size()]));
			ProcessResult result = ProcessHelper.waitForProcess(process, 500, 300, mon);
			CLIUtil.checkResult(REMOVE_CMD, result, false);
		} finally {
			if (process != null && process.isAlive()) {
				process.destroy();
			}
		}
	}
	
	public static void restartProject(String name, String projectId, String startMode, String conid, IProgressMonitor monitor) throws IOException, TimeoutException {
		SubMonitor mon = SubMonitor.convert(monitor, NLS.bind(Messages.RestartProjectTaskLabel, name), 100);
		Process process = null;
		try {
			String[] options = new String[] {PROJECT_ID_OPTION, projectId, STARTMODE_OPTION, startMode, CLIUtil.CON_ID_OPTION, conid};
			process = CLIUtil.runCWCTL(CLIUtil.GLOBAL_JSON_INSECURE, RESTART_CMD, options);
			ProcessResult result = ProcessHelper.waitForProcess(process, 500, 300, mon);
			CLIUtil.checkResult(RESTART_CMD, result, false);
		} finally {
			if (process != null && process.isAlive()) {
				process.destroy();
			}
		}
	}
}
