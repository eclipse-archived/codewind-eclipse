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

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;

import org.eclipse.codewind.core.internal.HttpUtil;
import org.eclipse.codewind.core.internal.HttpUtil.HttpResult;
import org.eclipse.codewind.ui.internal.IDEUtil;
import org.eclipse.codewind.ui.internal.messages.Messages;
import org.eclipse.codewind.ui.internal.prefs.RepositoryManagementComposite.RepoEntry;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;

public class AddTemplateSourceWizard extends Wizard implements INewWizard {

	protected TemplateSourceURLPage urlPage;
	protected TemplateSourceAuthPage authPage;
	protected TemplateSourceDetailsPage detailsPage;
	private boolean authPageIncluded = false;

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
		authPage = new TemplateSourceAuthPage(Messages.AddRepoDialogShell, Messages.AddRepoDialogTitle);
		addPage(authPage);
		detailsPage = new TemplateSourceDetailsPage(Messages.AddRepoDialogShell, Messages.AddRepoDialogTitle);
		addPage(detailsPage);
	}

	@Override
	public IWizardPage getNextPage(IWizardPage page) {
		if (page == urlPage) {
			// Check that the URL is valid
			URI uri;
			try {
				uri = new URI(urlPage.getTemplateSourceUrl());
			} catch (URISyntaxException e) {
				IDEUtil.openInfoDialog(Messages.AddRepoDialogInvalidUrlTitle, NLS.bind(Messages.AddRepoDialogInvalidUrlError, e.toString()));
				urlPage.setErrorMessage(Messages.AddRepoDialogInvalidUrlMsg);
				return urlPage;
			}
			
			// Check if the authentication required button was selected and return the auth page
			if (urlPage.getAuthRequired()) {
				authPageIncluded = true;
				authPage.updatePage(uri);
				return authPage;
			}
			
			// Check if authentication is required by the URL and return the auth page
			try {
				Boolean[] requiresAuth = new Boolean[1];
				requiresAuth[0] = Boolean.FALSE;
				IRunnableWithProgress runnable = new IRunnableWithProgress() {
					@Override
					public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
						try {
							SubMonitor.convert(monitor, NLS.bind(Messages.AddRepoDialogUrlPingTask, uri), IProgressMonitor.UNKNOWN);
							HttpResult result = HttpUtil.get(uri, null);
							if (!result.isGoodResponse) {
								if (result.responseCode == 401) {
									requiresAuth[0] = Boolean.TRUE;
								} else {
									String errorMsg = result.error;
									if (errorMsg == null || errorMsg.trim().isEmpty()) {
										errorMsg = NLS.bind(Messages.AddRepoDialogUrlPingFailedDefaultMsg, result.responseCode);
									}
									throw new InvocationTargetException(new IOException(errorMsg));
								}
							}
						} catch (IOException e) {
							throw new InvocationTargetException(e, e.toString());
						}
					}
				};
				getContainer().run(true, true, runnable);
				if (requiresAuth[0].booleanValue()) {
					authPageIncluded = true;
					authPage.updatePage(uri);
					return authPage;
				}
			} catch (Exception e) {
				String msg = e instanceof InvocationTargetException ? ((InvocationTargetException)e).getCause().toString() : e.toString();
				IDEUtil.openInfoDialog(Messages.AddRepoDialogUrlPingFailedTitle, NLS.bind(Messages.AddRepoDialogUrlPingFailedError, urlPage.getTemplateSourceUrl(), msg));
				urlPage.setErrorMessage(Messages.AddRepoDialogUrlPingFailedMsg);
				return urlPage;
			}
			
			// If no authentication required, return the details page
			authPageIncluded = false;
			detailsPage.updatePage(urlPage.getTemplateSourceUrl(), null);
			return detailsPage;
		} else if (page == authPage) {
			detailsPage.updatePage(urlPage.getTemplateSourceUrl(), authPage.getAuthInfo());
			return detailsPage;
		}
		return super.getNextPage(page);
	}

	@Override
	public boolean canFinish() {
		return urlPage.canFinish() && (!authPageIncluded || authPage.canFinish()) && detailsPage.canFinish();
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
			return new RepoEntry(url, authPage.getUsername(), authPage.getPassword(), authPage.getToken(), name, description);
		}
		return null;
	}
}
