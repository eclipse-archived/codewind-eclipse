/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.codewind.ui.internal.prefs;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.eclipse.codewind.core.CodewindCorePlugin;
import org.eclipse.codewind.core.internal.Logger;
import org.eclipse.codewind.core.internal.cli.TemplateUtil;
import org.eclipse.codewind.core.internal.connection.CodewindConnection;
import org.eclipse.codewind.core.internal.connection.RepositoryInfo;
import org.eclipse.codewind.ui.CodewindUIPlugin;
import org.eclipse.codewind.ui.internal.IDEUtil;
import org.eclipse.codewind.ui.internal.UIConstants;
import org.eclipse.codewind.ui.internal.messages.Messages;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;

public class RepositoryManagementComposite extends Composite {
	
	private final CodewindConnection connection;
	private final List<RepositoryInfo> repoList; // Original set of repos
	private List<RepoEntry> repoEntries; // Current set of repos (content of the table)
	private CheckboxTableViewer repoViewer;
	private Button editButton, removeButton;
	private ScrolledComposite detailsScroll;
	private Font boldFont;
	private Label descLabel, styleLabel, linkLabel, secureLabel;
	private Text descText, styleText, secureText;
	private Link urlLink;
	
	public RepositoryManagementComposite(Composite parent, CodewindConnection connection, List<RepositoryInfo> repoList) {
		super(parent, SWT.NONE);
		this.connection = connection;
		this.repoList = repoList;
		repoEntries = getRepoEntries(repoList);
		createControl();
	}
	
	protected void createControl() {
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginHeight = 8;
		layout.marginWidth = 8;
		layout.horizontalSpacing = 7;
		layout.verticalSpacing = 5;
		setLayout(layout);
		
		Text description = new Text(this, SWT.READ_ONLY | SWT.WRAP | SWT.MULTI);
		description.setText("");
		description.setBackground(this.getBackground());
		description.setForeground(this.getForeground());
		description.setLayoutData(new GridData(GridData.FILL, GridData.END, true, false, 1, 1));
		
		Link learnMoreLink = new Link(this, SWT.NONE);
		learnMoreLink.setText("<a>" + Messages.RepoMgmtLearnMoreLink + "</a>");
		learnMoreLink.setLayoutData(new GridData(GridData.END, GridData.END, false, false, 1, 1));
		
		learnMoreLink.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				try {
					IWorkbenchBrowserSupport browserSupport = PlatformUI.getWorkbench().getBrowserSupport();
					IWebBrowser browser = browserSupport.getExternalBrowser();
					URL url = new URL(UIConstants.TEMPLATES_INFO_URL);
					browser.openURL(url);
				} catch (Exception e) {
					Logger.logError("An error occurred trying to open an external browser at: " + UIConstants.TEMPLATES_INFO_URL, e); //$NON-NLS-1$
				}
			}
		});
		
		new Label(this, SWT.NONE).setLayoutData(new GridData(GridData.FILL, GridData.FILL, false, false, 2, 1));
		
		repoViewer = CheckboxTableViewer.newCheckList(this, SWT.BORDER | SWT.V_SCROLL | SWT.MULTI);
		repoViewer.setContentProvider(new RepoContentProvider());
		repoViewer.setLabelProvider(new RepoLabelProvider());
		repoViewer.setInput(repoEntries.toArray(new RepoEntry[repoEntries.size()]));
		GridData tableData = new GridData(GridData.FILL, GridData.FILL, true, true, 1, 3);
		tableData.horizontalIndent = 1;
		repoViewer.getTable().setLayoutData(tableData);
		
		Button addButton = new Button(this, SWT.PUSH);
		addButton.setText(Messages.RepoMgmtAddButton);
		addButton.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, false, false));
		
		editButton = new Button(this, SWT.PUSH);
		editButton.setText(Messages.RepoMgmtEditButton);
		editButton.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, false, false));
		
		removeButton = new Button(this, SWT.PUSH);
		removeButton.setText(Messages.RepoMgmtRemoveButton);
		removeButton.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, false, false));
		
		repoViewer.getTable().addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) {
				updateDetails();
				updateButtons();
			}
		});
		
		addButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				AddTemplateSourceWizard wizard = new AddTemplateSourceWizard();
				WizardDialog dialog = new WizardDialog(Display.getDefault().getActiveShell(), wizard);
				dialog.create();
				if (dialog.open() == IStatus.OK) {
					RepoEntry repoEntry = wizard.getRepoEntry();
					if (repoEntry != null) {
						repoEntries.add(repoEntry);
						repoViewer.refresh();
						repoViewer.setChecked(repoEntry, true);
					}
				}
			}
		});
		
		editButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				TableItem[] items = repoViewer.getTable().getSelection();
				RepoEntry repoEntry = (RepoEntry) items[0].getData();
				EditTemplateSourceWizard wizard = new EditTemplateSourceWizard(repoEntry);
				WizardDialog dialog = new WizardDialog(Display.getDefault().getActiveShell(), wizard);
				dialog.create();
				if (dialog.open() == IStatus.OK) {
					RepoEntry newRepoEntry = wizard.getRepoEntry();
					if (newRepoEntry != null) {
						int index = repoEntries.indexOf(repoEntry);
						newRepoEntry.enabled = repoEntry.enabled;
						repoEntries.set(index, newRepoEntry);
						repoViewer.refresh();
						repoViewer.setChecked(newRepoEntry, newRepoEntry.enabled);
					}
				}
			}
		});
		
		removeButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				Arrays.stream(repoViewer.getTable().getSelection()).forEach(item -> { repoEntries.remove(item.getData()); });
				repoViewer.refresh();
			}
		});
		
		detailsScroll = new ScrolledComposite(this, SWT.V_SCROLL);
		GridData data = new GridData(GridData.FILL, GridData.FILL, true, true, 2, 1);
		data.widthHint = 300;
		detailsScroll.setLayoutData(data);
		
		Composite detailsComp = new Composite(detailsScroll, SWT.NONE);
		final GridLayout detailsLayout = new GridLayout();
		detailsLayout.numColumns = 1;
		detailsComp.setLayout(detailsLayout);
		detailsComp.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true));
		
		boldFont = IDEUtil.newFont(getShell(), getFont(), SWT.BOLD);
		
		descLabel = new Label(detailsComp, SWT.NONE);
		descLabel.setFont(boldFont);
		descLabel.setText(Messages.RepoMgmtDescriptionLabel);
		descText = new Text(detailsComp, SWT.READ_ONLY | SWT.MULTI | SWT.WRAP);
		descText.setText("");
		GridData descData = new GridData(GridData.BEGINNING, GridData.FILL, false, false);
		descText.setLayoutData(descData);
		IDEUtil.normalizeBackground(descText, detailsComp);

		styleLabel = new Label(detailsComp, SWT.NONE);
		styleLabel.setFont(boldFont);
		styleLabel.setText(Messages.RepoMgmtStylesLabel);
		styleText = new Text(detailsComp, SWT.READ_ONLY | SWT.MULTI | SWT.WRAP);
		styleText.setText("");
		GridData styleData = new GridData(GridData.BEGINNING, GridData.FILL, false, false);
		styleText.setLayoutData(styleData);
		IDEUtil.normalizeBackground(styleText, detailsComp);
		
		linkLabel = new Label(detailsComp, SWT.NONE);
		linkLabel.setFont(boldFont);
		linkLabel.setText(Messages.RepoMgmtUrlLabel);
		urlLink = new Link(detailsComp, SWT.WRAP);
		urlLink.setText("");
		GridData linkData = new GridData(GridData.BEGINNING, GridData.FILL, false, false);
		urlLink.setLayoutData(linkData);
		
		urlLink.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {				
				RepoEntry entry = (RepoEntry)urlLink.getData();
				if (entry.url != null) {
					try {
						URL url = new URL(entry.url);
						IWorkbenchBrowserSupport browserSupport = PlatformUI.getWorkbench().getBrowserSupport();
						IWebBrowser browser = browserSupport.getExternalBrowser();
						if (browser == null) {
							browser = browserSupport
								.createBrowser(IWorkbenchBrowserSupport.NAVIGATION_BAR | IWorkbenchBrowserSupport.LOCATION_BAR,
										entry.url, null, null);
						}
						browser.openURL(url);
					} catch (Exception e) {
						Logger.logError("Failed to open the browser for url: " + entry.url, e); //$NON-NLS-1$
					}
				}
			}
		});	
		
		secureLabel = new Label(detailsComp, SWT.NONE);
		secureLabel.setFont(boldFont);
		secureLabel.setText(Messages.RepoMgmtSecureLabel);
		secureText = new Text(detailsComp, SWT.READ_ONLY | SWT.MULTI | SWT.WRAP);
		secureText.setText("");
		GridData secureData = new GridData(GridData.BEGINNING, GridData.FILL, false, false);
		secureText.setLayoutData(secureData);
		IDEUtil.normalizeBackground(secureText, detailsComp);
		
		detailsScroll.addListener(SWT.Resize, (event) -> {
			int width = detailsScroll.getClientArea().width;
			descData.widthHint = width - detailsLayout.marginWidth;
			styleData.widthHint = width - detailsLayout.marginWidth;
			linkData.widthHint = width - detailsLayout.marginWidth;
			secureData.widthHint = width - detailsLayout.marginWidth;
			Point size = detailsComp.computeSize(SWT.DEFAULT, SWT.DEFAULT);
			detailsScroll.setMinSize(size);
		});
		
		detailsScroll.setContent(detailsComp);
		detailsScroll.setExpandHorizontal(true);
		detailsScroll.setExpandVertical(true);
		detailsScroll.setMinSize(detailsScroll.getContent().computeSize(SWT.DEFAULT, SWT.DEFAULT));
		
		description.setText(Messages.RepoMgmtDescription);
		
		for (TableItem item : repoViewer.getTable().getItems()) {
			RepoEntry entry = (RepoEntry)item.getData();
			item.setChecked(entry.enabled);
		}
		repoViewer.addCheckStateListener(new ICheckStateListener() {
			@Override
			public void checkStateChanged(CheckStateChangedEvent event) {
				((RepoEntry)event.getElement()).enabled = event.getChecked();
			}
		});
		
		// Add Context Sensitive Help
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, CodewindUIPlugin.MAIN_CONTEXTID);
		
		if (repoViewer.getTable().getItemCount() > 0) {
			repoViewer.getTable().setSelection(0);
		}
		updateDetails();
		updateButtons();
		repoViewer.getTable().setFocus();
	}
	
	@Override
	public boolean setFocus() {
		return repoViewer.getTable().setFocus();
	}

	@Override
	public void dispose() {
		if (boldFont != null) {
			boldFont.dispose();
		}
		super.dispose();
	}

	private void updateDetails() {
		TableItem[] items = repoViewer.getTable().getSelection();
		String desc = "";
		String styles = "";
		String url = "";
		boolean secure = false;
		RepoEntry entry = null;
		boolean enabled = false;
		if (items.length == 1) {
			enabled = true;
			entry = (RepoEntry)items[0].getData();
			desc = entry.description;
			styles = entry.getStyles();
			url = entry.url;
			secure = entry.requiresAuthentication;
		}
		descLabel.setEnabled(enabled);
		descText.setText(desc);
		styleLabel.setEnabled(enabled);
		styleText.setText(styles);
		linkLabel.setEnabled(enabled);
		urlLink.setText("<a href=\"" + url + "\">" + url + "</a>");
		if (entry != null) {
			urlLink.setData(entry);
		}
		secureLabel.setEnabled(enabled);
		secureText.setText(secure ? Messages.RepoMgmtYesValue : Messages.RepoMgmtNoValue);
		
		resizeEntry(descText);
		resizeEntry(styleText);
		resizeEntry(urlLink);
		resizeEntry(secureText);
		
		detailsScroll.requestLayout();
	}
	
	private void resizeEntry(Control control) {
		// resize label to make scroll bars appear
		int width = control.getParent().getClientArea().width;
		control.setSize(width, control.computeSize(width, SWT.DEFAULT).y);
		
		// resize again if scroll bar added or removed
		int newWidth = control.getParent().getClientArea().width;
		if (newWidth != width) {
			control.setSize(newWidth, control.computeSize(newWidth, SWT.DEFAULT).y);
		}
	}
	

	private void updateButtons() {
		boolean enabled = true;
		TableItem[] items = repoViewer.getTable().getSelection();
		if (items.length >= 1) {
			for (TableItem item : items) {
				if (((RepoEntry)item.getData()).isProtected()) {
					enabled = false;
					break;
				}
			}
		} else {
			enabled = false;
		}
		removeButton.setEnabled(enabled);
		
		editButton.setEnabled(items.length == 1 && enabled);
	}
	
	private class RepoContentProvider implements IStructuredContentProvider {
		@Override
		public Object[] getElements(Object arg0) {
			return repoEntries.toArray(new RepoEntry[repoEntries.size()]);
		}
	}
	
	private static class RepoLabelProvider extends LabelProvider {
		@Override
		public String getText(Object element) {
			String name = ((RepoEntry)element).name;
			if (name == null || name.isEmpty()) {
				name = ((RepoEntry)element).description;
			}
			return name;
		}
	}
	
	private List<RepoEntry> getRepoEntries(List<RepositoryInfo> infos) {
		List<RepoEntry> entries = new ArrayList<RepoEntry>(infos.size());
		for (RepositoryInfo info : infos) {
			entries.add(new RepoEntry(info));
		}
		return entries;
	}

	// This should only be called once the user has made all of their changes
	// and indicated they want to update (clicked OK or Apply rather than Cancel).
	// Callers should wrap in a job and show progress.
	public IStatus updateRepos(IProgressMonitor monitor) {
		SubMonitor mon = SubMonitor.convert(monitor, Messages.RepoUpdateTask, 100);
		MultiStatus multiStatus = new MultiStatus(CodewindCorePlugin.PLUGIN_ID, IStatus.ERROR, Messages.RepoMgmtUpdateError, null);
		
		// Check for the differences between the original repo set and the new set
		for (RepositoryInfo info : repoList) {
			Optional<RepoEntry> entry = getRepoEntry(info);
			if (!entry.isPresent()) {
				// The new set does not contain the original repo so remove it
				try {
					TemplateUtil.removeTemplateSource(info.getURL(), connection.getConid(), mon.split(25));
				} catch (Exception e) {
					Logger.logError("Failed to remove repository: " + info.getURL(), e); //$NON-NLS-1$
					multiStatus.add(new Status(IStatus.ERROR, CodewindCorePlugin.PLUGIN_ID, NLS.bind(Messages.RepoMgmtRemoveFailed, info.getURL()), e));
				}
			} else if (info.getEnabled() != entry.get().enabled) {
				// The new set contains the original repo but the enablement does not match so update it
				try {
					TemplateUtil.enableTemplateSource(entry.get().enabled, info.getURL(), connection.getConid(), mon.split(25));
				} catch (Exception e) {
					Logger.logError("Failed to update repository: " + info.getURL(), e); //$NON-NLS-1$
					multiStatus.add(new Status(IStatus.ERROR, CodewindCorePlugin.PLUGIN_ID, NLS.bind(Messages.RepoMgmtUpdateFailed, info.getURL()), e));
				}
			}
			if (mon.isCanceled()) {
				return Status.CANCEL_STATUS;
			}
			mon.setWorkRemaining(100);
		}
		// Check for new entries (RepositoryInfo is null) and add them
		for (RepoEntry entry : repoEntries) {
			if (entry.info == null) {
				// Add the repository
				try {
					TemplateUtil.addTemplateSource(entry.url, entry.username, entry.password, entry.accessToken, entry.name, entry.description, connection.getConid(), mon.split(25));
				} catch (Exception e) {
					Logger.logError("Failed to add repository: " + entry.url, e); //$NON-NLS-1$
					multiStatus.add(new Status(IStatus.ERROR, CodewindCorePlugin.PLUGIN_ID, NLS.bind(Messages.RepoMgmtAddFailed, entry.url), e));
				}
			}
			if (mon.isCanceled()) {
				return Status.CANCEL_STATUS;
			}
			mon.setWorkRemaining(100);
		}
		if (multiStatus.getChildren().length > 0) {
			return multiStatus;
		}
		return Status.OK_STATUS;
	}
	
	public boolean hasChanges() {
		// For all pre-existing repos, if not in the current list then they
		// need to be removed. If in the current list but the enablement is
		// different then they need to be updated.
		for (RepositoryInfo info : repoList) {
			Optional<RepoEntry> entry = getRepoEntry(info);
			if (!entry.isPresent()) {
				return true;
			} else if (info.getEnabled() != entry.get().enabled) {
				return true;
			}
		}
		// For all entries that are not pre-existing (RepositoryInfo is null),
		// they need to be added.
		for (RepoEntry entry : repoEntries) {
			if (entry.info == null) {
				return true;
			}
		}
		return false;
	}
	
	private Optional<RepoEntry> getRepoEntry(RepositoryInfo info) {
		return repoEntries.stream().filter(entry -> entry.info == info).findFirst();
	}

	public static class RepoEntry {
		public final String url;
		public final boolean requiresAuthentication;
		public final String username;
		public final String password;
		public final String accessToken;
		public final String name;
		public final String description;
		public boolean enabled;
		public RepositoryInfo info;

		public RepoEntry(String url, boolean requiresAuthentication, String username, String password, String accessToken, String name, String description) {
			this.url = url;
			this.requiresAuthentication = requiresAuthentication;
			this.username = username;
			this.password = password;
			this.accessToken = accessToken;
			this.name = name;
			this.description = description;
			this.enabled = true;
		}
		
		public RepoEntry(RepositoryInfo info) {
			this.url = info.getURL();
			this.requiresAuthentication = info.hasAuthentication();
			this.username = info.getUsername();
			this.password = null;
			this.accessToken = null;
			this.name = info.getName();
			this.description = info.getDescription();
			this.enabled = info.getEnabled();
			this.info = info;
		}
		
		public boolean isLogonMethod() {
			return !requiresAuthentication || username != null;
		}
		
		public boolean isProtected() {
			if (info != null) {
				return info.isProtected();
			}
			return false;
		}
		
		public String getStyles() {
			if (info != null) {
				List<String> styles = info.getStyles();
				if (styles == null || styles.isEmpty()) {
					return Messages.GenericNotAvailable;
				}
				StringBuilder builder = new StringBuilder();
				boolean start = true;
				for (String style : styles) {
					if (!start) {
						builder.append(", ");
					} else {
						start = false;
					}
					builder.append(style);
					return builder.toString();
				}
			}
			return Messages.GenericNotAvailable;
		}
	}
}
