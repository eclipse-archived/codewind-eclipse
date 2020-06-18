/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.codewind.ui.internal.prefs;

import org.eclipse.codewind.ui.internal.messages.Messages;
import org.eclipse.codewind.ui.internal.prefs.RepositoryManagementComposite.RepoEntry;

public class EditTemplateSourceWizard extends AddTemplateSourceWizard {
	
	private final RepoEntry repo;

	public EditTemplateSourceWizard(RepoEntry repo) {
		super();
		this.repo = repo;
	}

	@Override
	public void addPages() {
		setWindowTitle(Messages.EditRepoDialogShell);
		urlPage = new TemplateSourceURLPage(Messages.EditRepoDialogShell, Messages.EditRepoDialogTitle, repo.url, repo.requiresAuthentication, repo.isLogonMethod(), repo.username);
		addPage(urlPage);
		detailsPage = new TemplateSourceDetailsPage(Messages.EditRepoDialogShell, Messages.EditRepoDialogTitle, repo.name, repo.description);
		addPage(detailsPage);
	}
}
