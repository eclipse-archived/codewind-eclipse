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
	private static final String USERNAME_OPTION = "--username";
	private static final String PASSWORD_OPTION = "--password";
	private static final String PERSONAL_ACCESS_TOKEN_OPTION = "--personalAccessToken";

	public static List<ProjectTemplateInfo> listTemplates(boolean enabledOnly, String conid, IProgressMonitor monitor) throws IOException, JSONException, TimeoutException {
		SubMonitor mon = SubMonitor.convert(monitor, 100);
		Process process = null;
		String[] options = enabledOnly ? new String[] {ENABLED_ONLY_OPTION, CLIUtil.CON_ID_OPTION, conid} : new String[] {CLIUtil.CON_ID_OPTION, conid};
		try {
			process = CLIUtil.runCWCTL(CLIUtil.GLOBAL_JSON_INSECURE, LIST_CMD, options);
			ProcessResult result = ProcessHelper.waitForProcess(process, 500, 60, mon);
			CLIUtil.checkResult(LIST_CMD, result, true);
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
			process = CLIUtil.runCWCTL(CLIUtil.GLOBAL_JSON_INSECURE, REPO_LIST_CMD, new String[] {CLIUtil.CON_ID_OPTION, conid});
			ProcessResult result = ProcessHelper.waitForProcess(process, 500, 60, mon);
			CLIUtil.checkResult(REPO_LIST_CMD, result, true);
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
	
	public static void addTemplateSource(String url, String username, String password, String accessToken, String name, String description, String conid, IProgressMonitor monitor) throws IOException, JSONException, TimeoutException {
		List<String> options = new ArrayList<String>();
		options.add(URL_OPTION);
		options.add(url);
		if (username != null && password != null) {
			options.add(USERNAME_OPTION);
			options.add(username);
			options.add(PASSWORD_OPTION);
			options.add(password);
		} else if (accessToken != null) {
			options.add(PERSONAL_ACCESS_TOKEN_OPTION);
			options.add(accessToken);
		}
		options.add(NAME_OPTION);
		options.add(name);
		options.add(DESCRIPTION_OPTION);
		options.add(description);
		options.add(CLIUtil.CON_ID_OPTION);
		options.add(conid);
		runTemplateSourceCmd(REPO_ADD_CMD, options.toArray(new String[options.size()]), null, monitor);
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
			process = CLIUtil.runCWCTL(CLIUtil.GLOBAL_JSON_INSECURE, command, options, args);
			ProcessResult result = ProcessHelper.waitForProcess(process, 500, 60, mon);
			CLIUtil.checkResult(command, result, false);
		} finally {
			if (process != null && process.isAlive()) {
				process.destroy();
			}
		}
	}
}
