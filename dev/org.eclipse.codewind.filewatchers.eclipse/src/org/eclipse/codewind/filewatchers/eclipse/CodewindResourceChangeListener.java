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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.codewind.filewatchers.eclipse.CodewindFilewatcherdConnection.FileChangeEntryEclipse;
import org.eclipse.codewind.filewatchers.eclipse.CodewindFilewatcherdConnection.FileChangeEntryEclipse.ChangeEntryEventType;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.runtime.CoreException;

public class CodewindResourceChangeListener implements IResourceChangeListener {

	private final CodewindFilewatcherdConnection parent;

	public CodewindResourceChangeListener(CodewindFilewatcherdConnection parent) {
		this.parent = parent;
	}

	@Override
	public void resourceChanged(IResourceChangeEvent event) {
		IResourceDelta delta = event.getDelta();

		CodewindResourceDeltaVisitor visitor = new CodewindResourceDeltaVisitor();

		try {
			delta.accept(visitor);

			visitor.getResult().forEach(e -> {
				System.out.println(e);
			});

			parent.handleResourceChanges(visitor.getResult());

		} catch (CoreException e1) {
			// TODO: Log me.
			e1.printStackTrace();
		}

	}

	private class CodewindResourceDeltaVisitor implements IResourceDeltaVisitor {

		private final List<FileChangeEntryEclipse> result = new ArrayList<>();

		public CodewindResourceDeltaVisitor() {
		}

		@Override
		public boolean visit(IResourceDelta delta) throws CoreException {
			IResource resource = delta.getResource();

			if (resource == null) {
				return false;
			}
			// Exclude parent folder or project
			if (delta.getKind() == IResourceDelta.CHANGED && delta.getFlags() == 0) {
				return true;
			}

			ChangeEntryEventType ceet = null;

			switch (delta.getKind()) {
			case IResourceDelta.ADDED:
				ceet = ChangeEntryEventType.CREATE;
				break;
			case IResourceDelta.REMOVED:
				ceet = ChangeEntryEventType.DELETE;
				break;
			case IResourceDelta.CHANGED:
				ceet = ChangeEntryEventType.MODIFY;
				break;
			}

			File resourceFile = resource.getLocation().toFile();

			IProject project = resource.getProject();

			FileChangeEntryEclipse fcee = new FileChangeEntryEclipse(resourceFile, ceet,
					resource.getType() == IResource.FOLDER, project);

			result.add(fcee);

			return true;
		}

		public List<FileChangeEntryEclipse> getResult() {
			return result;
		}
	}
}
