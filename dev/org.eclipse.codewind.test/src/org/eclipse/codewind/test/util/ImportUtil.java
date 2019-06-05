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

package org.eclipse.codewind.test.util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.MavenModelManager;
import org.eclipse.m2e.core.project.LocalProjectScanner;
import org.eclipse.m2e.core.project.MavenProjectInfo;
import org.eclipse.m2e.core.project.ProjectImportConfiguration;
import org.eclipse.m2e.core.ui.internal.wizards.ImportMavenProjectsJob;
import org.eclipse.ui.IWorkingSet;

@SuppressWarnings("restriction")
public class ImportUtil {
	
	public static IProject importExistingMavenProjects(IPath path, String projectName) throws Exception {
		File root = path.toFile();
		String location = path.toOSString();
		MavenModelManager modelManager = MavenPlugin.getMavenModelManager();
		LocalProjectScanner scanner = new LocalProjectScanner(root, location, false, modelManager);
		scanner.run(new NullProgressMonitor());
		List<MavenProjectInfo> infos = new ArrayList<MavenProjectInfo>();
		infos.addAll(scanner.getProjects());
		for(MavenProjectInfo info : scanner.getProjects()){
			infos.addAll(info.getProjects());
		}
		ImportMavenProjectsJob job = new ImportMavenProjectsJob(infos, new ArrayList<IWorkingSet>(), new ProjectImportConfiguration());
		job.setRule(MavenPlugin.getProjectConfigurationManager().getRule());
		job.schedule();
		IProject project = waitForProject(projectName);
		return project;
	}
	
	public static IProject waitForProject(String projectName) throws Exception {
		TestUtil.waitForJobs(300, 5);
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
		return project;
	}

}
