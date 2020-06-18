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

import java.net.URI;
import java.net.URISyntaxException;

import org.eclipse.codewind.ui.internal.IDEUtil;
import org.eclipse.codewind.ui.internal.messages.Messages;
import org.eclipse.codewind.ui.internal.prefs.RepositoryManagementComposite.RepoEntry;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;

public class AddTemplateSourceWizard extends Wizard implements INewWizard {

	protected TemplateSourceURLPage urlPage;
	protected TemplateSourceDetailsPage detailsPage;

	public AddTemplateSourceWizard() {
		setNeedsProgressMonitor(true);
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		// Empty
	}

	@Override
	public void addPages() {
		setWindowTitle(Messages.AddRepoDialogShell);
		urlPage = new TemplateSourceURLPage(Messages.AddRepoDialogShell, Messages.AddRepoDialogTitle);
		addPage(urlPage);
		detailsPage = new TemplateSourceDetailsPage(Messages.AddRepoDialogShell, Messages.AddRepoDialogTitle);
		addPage(detailsPage);
	}

	@Override
	public IWizardPage getNextPage(IWizardPage page) {
		if (page == urlPage) {
			// Check that the URL is valid
			try {
				new URI(urlPage.getTemplateSourceUrl());
			} catch (URISyntaxException e) {
				IDEUtil.openInfoDialog(Messages.AddRepoDialogInvalidUrlTitle, NLS.bind(Messages.AddRepoDialogInvalidUrlError, e.toString()));
				urlPage.setErrorMessage(Messages.AddRepoDialogInvalidUrlMsg);
				return urlPage;
			}
			
			detailsPage.updatePage(urlPage.getTemplateSourceUrl(), urlPage.getAuthInfo());
			return detailsPage;
		}
		return super.getNextPage(page);
	}

	@Override
	public boolean canFinish() {
		return urlPage.canFinish() && detailsPage.canFinish();
	}

	@Override
	public boolean performCancel() {
		return true;
	}

	@Override
	public boolean performFinish() {
		if (!canFinish()) {
			return false;
		}
		return true;
	}
	
	RepoEntry getRepoEntry() {
		String url = urlPage.getTemplateSourceUrl();
		String name = detailsPage.getTemplateSourceName();
		String description = detailsPage.getTemplateSourceDescription();
		if (name != null && !name.isEmpty() &&
			description != null && !description.isEmpty() &&
			url != null && !url.isEmpty()) {
			return new RepoEntry(url, urlPage.getAuthRequired(), urlPage.getUsername(), urlPage.getPassword(), urlPage.getToken(), name, description);
		}
		return null;
	}
}
