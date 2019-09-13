/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
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
import java.util.List;

import org.eclipse.codewind.core.CodewindCorePlugin;
import org.eclipse.codewind.core.internal.Logger;
import org.eclipse.codewind.core.internal.connection.CodewindConnection;
import org.eclipse.codewind.core.internal.connection.RepositoryInfo;
import org.eclipse.codewind.ui.CodewindUIPlugin;
import org.eclipse.codewind.ui.internal.messages.Messages;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;

public class RepositoryManagementComposite extends Composite {
	
	private final CodewindConnection connection;
	private final List<RepositoryInfo> repoList;
	private List<RepoEntry> repoEntries;
	private CheckboxTableViewer repoViewer;
	private Button removeButton;
	private Font boldFont;
//	private Label descLabel;
//	private Text descText;
	private Label styleLabel;
	private Text styleText;
	private Label linkLabel;
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
		layout.numColumns = 1;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		layout.horizontalSpacing = 5;
		layout.verticalSpacing = 5;
		setLayout(layout);
		
		new Label(this, SWT.NONE);
		
		Text description = new Text(this, SWT.READ_ONLY | SWT.WRAP | SWT.MULTI);
		description.setText("");
		description.setBackground(this.getBackground());
		description.setForeground(this.getForeground());
		description.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false));
		
		new Label(this, SWT.NONE);
		
		Text tableText = new Text(this, SWT.READ_ONLY);
		tableText.setText(Messages.RepoMgmtTableLabel);
		tableText.setBackground(this.getBackground());
		tableText.setForeground(this.getForeground());
 
		Group composite = new Group(this, SWT.NONE);
		layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginHeight = 8;
		layout.marginWidth = 8;
		layout.horizontalSpacing = 7;
		layout.verticalSpacing = 7;
		composite.setLayout(layout);
		GridData data = new GridData(GridData.FILL, GridData.FILL, true, true);
		data.heightHint = 300;
		composite.setLayoutData(data);
		
		repoViewer = CheckboxTableViewer.newCheckList(composite, SWT.BORDER | SWT.V_SCROLL);
		repoViewer.setContentProvider(new RepoContentProvider());
		repoViewer.setLabelProvider(new RepoLabelProvider());
		repoViewer.setInput(repoEntries.toArray(new RepoEntry[repoEntries.size()]));
		repoViewer.getTable().setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true, 1, 3));
		
		Button addButton = new Button(composite, SWT.PUSH);
		addButton.setText(Messages.RepoMgmtAddButton);
		addButton.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, false, false));
		
		removeButton = new Button(composite, SWT.PUSH);
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
				AddDialog dialog = new AddDialog(getShell());
				if (dialog.open() == IStatus.OK) {
					RepoEntry repoEntry = dialog.getRepoEntry();
					if (repoEntry != null) {
						repoEntries.add(repoEntry);
						repoViewer.refresh();
						repoViewer.setChecked(repoEntry, true);
					}
				}
			}
		});
		
		removeButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				TableItem[] items = repoViewer.getTable().getSelection();
				for (TableItem item : items) {
					repoEntries.remove(item.getData());
					repoViewer.refresh();
				}
			}
		});
		
		ScrolledComposite detailsScroll = new ScrolledComposite(composite, SWT.V_SCROLL);
		data = new GridData(GridData.FILL, GridData.FILL, true, true, 2, 1);
		data.widthHint = 300;
		detailsScroll.setLayoutData(data);
		
		Composite detailsComp = new Composite(detailsScroll, SWT.NONE);
		final GridLayout detailsLayout = new GridLayout();
		detailsLayout.numColumns = 1;
		detailsComp.setLayout(detailsLayout);
		detailsComp.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true));

		Font font = detailsComp.getFont();
		if (boldFont == null) {
			Display display = getShell().getDisplay();
			FontData[] fontData = font.getFontData();
			fontData[0].setStyle(SWT.BOLD);
			fontData[0].setHeight(fontData[0].getHeight());
			boldFont = new Font(display, fontData);
		}
		
		detailsComp.addDisposeListener((event) -> {
			if (boldFont != null)
				boldFont.dispose();
		});

//		descLabel = new Label(detailsComp, SWT.NONE);
//		descLabel.setFont(boldFont);
//		descLabel.setText(Messages.RepoMgmtDescriptionLabel);
//		descText = new Text(detailsComp, SWT.READ_ONLY | SWT.MULTI | SWT.WRAP);
//		descText.setText("");
//		GridData descData = new GridData(GridData.FILL, GridData.FILL, false, false);
//		descText.setLayoutData(descData);

		styleLabel = new Label(detailsComp, SWT.NONE);
		styleLabel.setFont(boldFont);
		styleLabel.setText(Messages.RepoMgmtStylesLabel);
		styleText = new Text(detailsComp, SWT.READ_ONLY | SWT.MULTI | SWT.WRAP);
		styleText.setText("");
		GridData styleData = new GridData(GridData.FILL, GridData.FILL, false, false);
		styleText.setLayoutData(styleData);
		
		linkLabel = new Label(detailsComp, SWT.NONE);
		linkLabel.setFont(boldFont);
		linkLabel.setText(Messages.RepoMgmtUrlLabel);
		urlLink = new Link(detailsComp, SWT.WRAP);
		urlLink.setText("");
		GridData linkData = new GridData(GridData.FILL, GridData.FILL, false, false);
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
		
		detailsScroll.addListener(SWT.Resize, (event) -> {
			  int width = detailsScroll.getClientArea().width;
//			  descData.widthHint = width - detailsLayout.marginWidth; // Add padding to right.
			  styleData.widthHint = width - detailsLayout.marginWidth;
			  linkData.widthHint = width - detailsLayout.marginWidth;
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
		
		if (repoViewer.getTable().getItemCount() > 0) {
			repoViewer.getTable().setSelection(0);
		}
		updateDetails();
		updateButtons();
	}	   

	private void updateDetails() {
		TableItem[] items = repoViewer.getTable().getSelection();
//		String description = "";
		String styles = "";
		String url = "";
		RepoEntry entry = null;
		boolean enabled = false;
		if (items.length == 1) {
			enabled = true;
			entry = (RepoEntry)items[0].getData();
//			description = entry.description;
//			if (description == null || description.isEmpty()) {
//				description = Messages.NewProjectPage_DescriptionNone;
//			}
			styles = entry.getStyles();
			url = entry.url;
		}
//		descLabel.setEnabled(enabled);
//		descText.setText(description);
		styleLabel.setEnabled(enabled);
		styleText.setText(styles);
		linkLabel.setEnabled(enabled);
		urlLink.setText("<a href=\"" + url + "\">" + url + "</a>");
		if (entry != null) {
			urlLink.setData(entry);
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
			return ((RepoEntry)element).description;
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
			RepoEntry entry = getRepoEntry(info.getURL());
			if (entry == null) {
				// Remove the repository
				try {
					connection.requestRepoRemove(info.getURL());
				} catch (Exception e) {
					Logger.logError("Failed to remove repository: " + info.getURL(), e); //$NON-NLS-1$
					multiStatus.add(new Status(IStatus.ERROR, CodewindCorePlugin.PLUGIN_ID, NLS.bind(Messages.RepoMgmtRemoveFailed, info.getURL()), e));
				}
			} else if (info.getEnabled() != entry.enabled) {
				try {
					connection.requestRepoEnable(info.getURL(), entry.enabled);
				} catch (Exception e) {
					Logger.logError("Failed to update repository: " + info.getURL(), e); //$NON-NLS-1$
					multiStatus.add(new Status(IStatus.ERROR, CodewindCorePlugin.PLUGIN_ID, NLS.bind(Messages.RepoMgmtUpdateFailed, info.getURL()), e));
				}
			}
			if (mon.isCanceled()) {
				return Status.CANCEL_STATUS;
			}
			mon.worked(25);
			mon.setWorkRemaining(100);
		}
		for (RepoEntry entry : repoEntries) {
			RepositoryInfo info = getRepoInfo(entry.url);
			if (info == null) {
				// Add the repository
				try {
					connection.requestRepoAdd(entry.description, entry.url);
				} catch (Exception e) {
					Logger.logError("Failed to add repository: " + entry.url, e); //$NON-NLS-1$
					multiStatus.add(new Status(IStatus.ERROR, CodewindCorePlugin.PLUGIN_ID, NLS.bind(Messages.RepoMgmtAddFailed, entry.url), e));
				}
			}
			if (mon.isCanceled()) {
				return Status.CANCEL_STATUS;
			}
			mon.worked(25);
			mon.setWorkRemaining(100);
		}
		if (multiStatus.getChildren().length > 0) {
			return multiStatus;
		}
		return Status.OK_STATUS;
	}
	
	public boolean hasChanges() {
		for (RepositoryInfo info : repoList) {
			RepoEntry entry = getRepoEntry(info.getURL());
			if (entry == null) {
				return true;
			} else if (info.getEnabled() != entry.enabled) {
				return true;
			}
		}
		for (RepoEntry entry : repoEntries) {
			RepositoryInfo info = getRepoInfo(entry.url);
			if (info == null) {
				return true;
			}
		}
		return false;
	}
	
	private RepoEntry getRepoEntry(String url) {
		for (RepoEntry entry : repoEntries) {
			if (url.equals(entry.url)) {
				return entry;
			}
		}
		return null;
	}
	
	private RepositoryInfo getRepoInfo(String url) {
		for (RepositoryInfo info : repoList) {
			if (url.equals(info.getURL())) {
				return info;
			}
		}
		return null;
	}
	
	private static class RepoEntry {
		public final String description;
		public final String url;
		public boolean enabled;
		public RepositoryInfo info;
		
		public RepoEntry(String description, String url) {
			this.description = description;
			this.url = url;
			this.enabled = true;
		}
		
		public RepoEntry(RepositoryInfo info) {
			this.description = info.getDescription();
			this.url = info.getURL();
			this.enabled = info.getEnabled();
			this.info = info;
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
	
	private static class AddDialog extends TitleAreaDialog {
		
		private String description;
		private String url;
		
		public AddDialog(Shell parentShell) {
			super(parentShell);
		}
		
		@Override
		protected void configureShell(Shell newShell) {
			super.configureShell(newShell);
			newShell.setText("Add Repositories");
		}
		
		@Override
		protected boolean isResizable() {
			return true;
		}

		@Override
		protected Control createButtonBar(Composite parent) {
			return super.createButtonBar(parent);
		}

		protected Control createDialogArea(Composite parent) {
			setTitleImage(CodewindUIPlugin.getImage(CodewindUIPlugin.CODEWIND_BANNER));
			setTitle(Messages.AddRepoDialogTitle);
			setMessage(Messages.AddRepoDialogMessage);
			
			final Composite composite = new Composite(parent, SWT.NONE);
			GridLayout layout = new GridLayout();
			layout.marginHeight = 11;
			layout.marginWidth = 9;
			layout.horizontalSpacing = 5;
			layout.verticalSpacing = 7;
			layout.numColumns = 2;
			composite.setLayout(layout);
			GridData data = new GridData(GridData.FILL_BOTH);
			data.minimumWidth = 300;
			composite.setLayoutData(data);
			composite.setFont(parent.getFont());
			
			Label label = new Label(composite, SWT.NONE);
			label.setText(Messages.AddRepoDialogDescriptionLabel);
			label.setLayoutData(new GridData(GridData.BEGINNING, GridData.CENTER, false, false));
			
			Text descriptionText = new Text(composite, SWT.BORDER);
			descriptionText.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false));
			
			label = new Label(composite, SWT.NONE);
			label.setText(Messages.AddRepoDialogUrlLabel);
			label.setLayoutData(new GridData(GridData.BEGINNING, GridData.CENTER, false, false));
			
			Text urlText = new Text(composite, SWT.BORDER);
			urlText.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false));
			
			descriptionText.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(ModifyEvent e) {
					description = descriptionText.getText().trim();
					enableOKButton(validate());
				}
			});
			
			urlText.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(ModifyEvent e) {
					url = urlText.getText().trim();
					enableOKButton(validate());
				}
			});
			
			return composite; 
		}
		
		protected void enableOKButton(boolean value) {
			getButton(IDialogConstants.OK_ID).setEnabled(value);
		}
		
		private boolean validate() {
			if (description == null || description.isEmpty()) {
				setErrorMessage(Messages.AddRepoDialogNoDescription);
				return false;
			}
			if (url == null || url.isEmpty()) {
				setErrorMessage(Messages.AddRepoDialogNoUrl);
				return false;
			}
			
			setErrorMessage(null);
			return true;
		}
		
		public RepoEntry getRepoEntry() {
			if (description != null && !description.isEmpty() &&
				url != null && !url.isEmpty()) {
				return new RepoEntry(description, url);
			}
			return null;
		}
	}
	
}
