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

package org.eclipse.codewind.filewatchers.eclipse;

import java.util.Optional;

import org.eclipse.core.resources.IProject;

/**
 * Implement this in order to provide the Eclipse filewatcher with the ability
 * to translate IProjects into the corresponding Codewind projectId.
 */
public interface ICodewindProjectTranslator {

	public Optional<String> getProjectId(IProject project);
}
