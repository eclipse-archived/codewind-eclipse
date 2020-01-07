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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeoutException;

import org.eclipse.codewind.core.internal.Logger;
import org.eclipse.codewind.core.internal.ProcessHelper;
import org.eclipse.codewind.core.internal.ProcessHelper.ProcessResult;
import org.eclipse.codewind.core.internal.connection.ProjectTemplateInfo;
import org.eclipse.codewind.core.internal.connection.RepositoryInfo;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.json.JSONArray;
import org.json.JSONException;

public class TemplateUtil {
	
	private static final String TEMPLATES_CMD = "templates";
	private static final String LIST_OPTION = "list";
	private static final String REPOS_OPTION = "repos";
	private static final String[] LIST_CMD = new String[] {TEMPLATES_CMD, LIST_OPTION};
	private static final String[] REPO_LIST_CMD = new String[] {TEMPLATES_CMD, REPOS_OPTION, LIST_OPTION};
	private static final String[] REPO_ADD_CMD = new String[] {TEMPLATES_CMD, REPOS_OPTION, "add"};
	private static final String[] REPO_REMOVE_CMD = new String[] {TEMPLATES_CMD, REPOS_OPTION, "remove"};
	private static final String[] REPO_ENABLE_CMD = new String[] {TEMPLATES_CMD, REPOS_OPTION, "enable"};
	private static final String[] REPO_DISABLE_CMD = new String[] {TEMPLATES_CMD, REPOS_OPTION, "disable"};
	
	private static final String ENABLED_ONLY_OPTION = "--showEnabledOnly";
	private static final String URL_OPTION = "--url";
	private static final String NAME_OPTION = "--name";
	private static final String DESCRIPTION_OPTION = "--description";

	public static List<ProjectTemplateInfo> listTemplates(boolean enabledOnly, String conid, IProgressMonitor monitor) throws IOException, JSONException, TimeoutException {
		SubMonitor mon = SubMonitor.convert(monitor, 100);
		Process process = null;
		String[] options = enabledOnly ? new String[] {ENABLED_ONLY_OPTION, CLIUtil.CON_ID_OPTION, conid} : new String[] {CLIUtil.CON_ID_OPTION, conid};
		try {
			process = CLIUtil.runCWCTL(CLIUtil.GLOBAL_INSECURE, LIST_CMD, options);
			ProcessResult result = ProcessHelper.waitForProcess(process, 500, 60, mon);
			if (result.getExitValue() != 0) {
				Logger.logError("List templates failed with rc: " + result.getExitValue() + " and error: " + result.getErrorMsg()); //$NON-NLS-1$ //$NON-NLS-2$
				throw new IOException(result.getErrorMsg());
			}
			if (result.getOutput() == null || result.getOutput().trim().isEmpty()) {
				// This should not happen
				Logger.logError("List templates had 0 return code but the output is empty"); //$NON-NLS-1$
				throw new IOException("The output from list templates is empty."); //$NON-NLS-1$
			}
			JSONArray templateArray = new JSONArray(result.getOutput().trim());
			List<ProjectTemplateInfo> templates = new ArrayList<ProjectTemplateInfo>();
			for (int i = 0; i < templateArray.length(); i++) {
				templates.add(new ProjectTemplateInfo(templateArray.getJSONObject(i)));
			}
			return templates;
		} finally {
			if (process != null && process.isAlive()) {
				process.destroy();
			}
		}
	}
	
	public static List<RepositoryInfo> listTemplateSources(String conid, IProgressMonitor monitor) throws IOException, JSONException, TimeoutException {
		SubMonitor mon = SubMonitor.convert(monitor, 100);
		Process process = null;
		try {
			process = CLIUtil.runCWCTL(CLIUtil.GLOBAL_INSECURE, REPO_LIST_CMD, new String[] {CLIUtil.CON_ID_OPTION, conid});
			ProcessResult result = ProcessHelper.waitForProcess(process, 500, 60, mon);
			if (result.getExitValue() != 0) {
				Logger.logError("List templates sources failed with rc: " + result.getExitValue() + " and error: " + result.getErrorMsg()); //$NON-NLS-1$ //$NON-NLS-2$
				throw new IOException(result.getErrorMsg());
			}
			if (result.getOutput() == null || result.getOutput().trim().isEmpty()) {
				// This should not happen
				Logger.logError("List template sources had 0 return code but the output is empty"); //$NON-NLS-1$
				throw new IOException("The output from list template sources is empty."); //$NON-NLS-1$
			}
			JSONArray repoArray = new JSONArray(result.getOutput());
			List<RepositoryInfo> repos = new ArrayList<RepositoryInfo>();
			for (int i = 0; i < repoArray.length(); i++) {
				repos.add(new RepositoryInfo(repoArray.getJSONObject(i)));
			}
			return repos;
		} finally {
			if (process != null && process.isAlive()) {
				process.destroy();
			}
		}
	}
	
	public static void addTemplateSource(String url, String name, String description, String conid, IProgressMonitor monitor) throws IOException, JSONException, TimeoutException {
		runTemplateSourceCmd(REPO_ADD_CMD, new String[] {URL_OPTION, url, NAME_OPTION, name, DESCRIPTION_OPTION, description, CLIUtil.CON_ID_OPTION, conid}, null, monitor);
	}
	
	public static void removeTemplateSource(String url, String conid, IProgressMonitor monitor) throws IOException, JSONException, TimeoutException {
		runTemplateSourceCmd(REPO_REMOVE_CMD, new String[] {URL_OPTION, url, CLIUtil.CON_ID_OPTION, conid}, null, monitor);
	}
	
	public static void enableTemplateSource(boolean enable, String url, String conid, IProgressMonitor monitor) throws IOException, JSONException, TimeoutException {
		String[] cmd = enable ? REPO_ENABLE_CMD : REPO_DISABLE_CMD;
		runTemplateSourceCmd(cmd, new String[] {CLIUtil.CON_ID_OPTION, conid}, new String[] {url}, monitor);
	}
	
	private static void runTemplateSourceCmd(String[] command, String[] options, String[] args, IProgressMonitor monitor) throws IOException, JSONException, TimeoutException {
		SubMonitor mon = SubMonitor.convert(monitor, 100);
		Process process = null;
		try {
			process = CLIUtil.runCWCTL(CLIUtil.GLOBAL_INSECURE, command, options, args);
			ProcessResult result = ProcessHelper.waitForProcess(process, 500, 60, mon);
			if (result.getExitValue() != 0) {
				Logger.logError("The " + Arrays.toString(command) + " command with options " + Arrays.toString(options) + " failed with rc: " + result.getExitValue() + " and error: " + result.getErrorMsg()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
				throw new IOException(result.getErrorMsg());
			}
		} finally {
			if (process != null && process.isAlive()) {
				process.destroy();
			}
		}
	}
}
