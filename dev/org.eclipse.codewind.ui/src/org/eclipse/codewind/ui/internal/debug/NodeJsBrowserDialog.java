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

package org.eclipse.codewind.ui.internal.debug;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import org.eclipse.codewind.core.internal.Logger;
import org.eclipse.codewind.ui.internal.messages.Messages;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.internal.browser.BrowserManager;
import org.eclipse.ui.internal.browser.ExternalBrowserInstance;
import org.eclipse.ui.internal.browser.IBrowserDescriptor;

@SuppressWarnings("restriction")
public class NodeJsBrowserDialog extends MessageDialog {
	
final String url;
	
	final String browserName;
	
	public NodeJsBrowserDialog(Shell parentShell, String dialogTitle,
			Image dialogTitleImage, String dialogMessage, int dialogImageType,
			String[] dialogButtonLabels, int defaultIndex, String url, String browserName) {
		
		super(parentShell, dialogTitle, dialogTitleImage, dialogMessage,
				dialogImageType, dialogButtonLabels, defaultIndex);

		this.url = url;
		this.browserName = browserName;
		
		setShellStyle(getShellStyle() | SWT.RESIZE);		
	}
	
	@Override
	protected Control createDialogArea(Composite PARENT) {
		Composite composite = (Composite) super.createDialogArea(PARENT);
		composite.setLayout(new GridLayout(3, false));
		
		// Instruction text
		Text l = new Text(composite, SWT.READ_ONLY | SWT.WRAP);
		l.setText(Messages.NodeJsBrowserDialogPasteMessage);
		l.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
		l.setBackground(composite.getBackground());
		l.setForeground(composite.getForeground());
		
		// Text for the URL
		Text l2 = new Text(composite, SWT.BORDER);
		GridData gridData = new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1);
		gridData.minimumWidth = 500;
		gridData.widthHint = 500;
		l2.setLayoutData(gridData);
		l2.setText(url);
		
		// Force the buttons to the right
		Label emptyLabel = new Label(composite, SWT.NONE);
		emptyLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
		
		// Copy to clipboard button
		Button button = new Button(composite, SWT.PUSH);
		button.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		button.setText(Messages.NodeJsBrowserDialogCopyToClipboardButton);
		
		button.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Clipboard clipboard = new Clipboard(Display.getDefault());
		        String textData = url;
		        TextTransfer textTransfer = TextTransfer.getInstance();
		        clipboard.setContents(new Object[] { textData}, new Transfer[] { textTransfer} );
		        clipboard.dispose();
			}
		});

		// Open browser button
		button = new Button(composite, SWT.PUSH);
		button.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		button.setText(Messages.NodeJsBrowserDialogOpenChromeButton);
		
		button.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				launchWebBrowser(browserName, "https://microclimate-dev2ops.github.io/");
			}
		});
		
		// Add space before the OK button
		emptyLabel = new Label(composite, SWT.NONE);
		emptyLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true, 3, 1));
		
		return composite;
	}

	@Override
	protected boolean isResizable() {
		return true;
	}

	protected void launchWebBrowser(String browserName, String urlStr) {
		Logger.log("Launch web browser " + browserName + " with url: " + url); //$NON-NLS-1$ //$NON-NLS-2$
		
		URL url = null;
		try {
			url = new URL(urlStr);
		} catch (MalformedURLException e1) {
			Logger.logError("Could not create a url for the string: " + urlStr, e1);
			return;
		}

		BrowserManager bm = BrowserManager.getInstance();
		List<IBrowserDescriptor> browserList = bm.getWebBrowsers();

		IBrowserDescriptor foundBrowser = null;

		int size = browserList.size();
		for (int i = 0; i < size; i++) {
			IBrowserDescriptor browserDescriptor = browserList.get(i);
			if (browserDescriptor != null) {
				String name = browserDescriptor.getName();
				if (name != null && name.equals(browserName)) {
					foundBrowser = browserDescriptor;
					break;
				}
			}
		}

		if (foundBrowser != null) {
			ExternalBrowserInstance ebi = new ExternalBrowserInstance("Node.js Debugger", foundBrowser); //$NON-NLS-1$
			try {
				ebi.openURL(url);
			} catch (Exception e) {
				Logger.logError("Could not launch the browser for Node.js debugging", e); //$NON-NLS-1$
			}
		}
	}

}
