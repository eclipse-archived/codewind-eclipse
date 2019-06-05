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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.codewind.ui.internal.messages.Messages;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.internal.browser.BrowserManager;
import org.eclipse.ui.internal.browser.IBrowserDescriptor;

@SuppressWarnings("restriction") //$NON-NLS-1$
public class WebBrowserSelectionDialog extends MessageDialog {

	protected Link manageBrowserButton;
	protected boolean isNodejsDefaultBrowser = false;
	
	protected String browserName = null;
	protected Combo webBrowserCombo;
	protected List<IBrowserDescriptor> validBrowsers;
	protected Button isNodejsDefaultBrowserBtn;
	
	
	public WebBrowserSelectionDialog(Shell parentShell, String dialogTitle,
			Image dialogTitleImage, String dialogMessage, int dialogImageType,
			String[] dialogButtonLabels, int defaultIndex) {
		super(parentShell, dialogTitle, dialogTitleImage, dialogMessage,
				dialogImageType, dialogButtonLabels, defaultIndex);

		setShellStyle(getShellStyle() | SWT.RESIZE);		
	}
	
	protected Control createCustomArea(Composite parent) 
	{
        final Composite composite = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout(3, false);
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        composite.setLayout(layout);
        GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
        composite.setLayoutData(data);
        
        Text label = new Text(composite, SWT.READ_ONLY | SWT.SINGLE );
        label.setText(Messages.BrowserSelectionListLabel);
        label.setBackground(composite.getBackground());
        label.setForeground(composite.getForeground());
        
        updateWebBrowserCombo(composite);
        
		manageBrowserButton = new Link(composite, SWT.NONE);
		manageBrowserButton.setText("<a>" + Messages.BrowserSelectionManageButtonText+ "</a>"); //$NON-NLS-1$ //$NON-NLS-2$
		manageBrowserButton.addSelectionListener(new SelectionAdapter() {			
			public void widgetSelected(SelectionEvent e) {
				String id = "org.eclipse.ui.browser.preferencePage"; //$NON-NLS-1$
				String id2 = "org.eclipse.ui.browser.preferencePage"; //$NON-NLS-1$
				final PreferenceDialog dialog = 
						PreferencesUtil.createPreferenceDialogOn(getShell(), id2, new String[] { id, id2 }, null);
				if (dialog.open() == org.eclipse.jface.window.Window.OK){
					// Refresh the combo box after selection has been completed
					updateWebBrowserCombo(composite);
				}
			}
		});

        // Empty line
	    Label curLabel = new Label(composite, SWT.NONE);
	    GridData data1 = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
	    data1.horizontalSpan = 2;	    
	    curLabel.setLayoutData(data1);		

		isNodejsDefaultBrowserBtn = new Button(composite, SWT.CHECK);
	    isNodejsDefaultBrowserBtn.setText(Messages.BrowserSelectionAlwaysUseMsg);
	    isNodejsDefaultBrowserBtn.setLayoutData(data1);
	    isNodejsDefaultBrowserBtn.addSelectionListener(new SelectionListener(){
	      public void widgetDefaultSelected(SelectionEvent e) {
	    	  // Do nothing
	      }

	      public void widgetSelected(SelectionEvent e) {
	    	  // Only deal with the default preference if a valid browser is selected.
	    	  // Otherwise, return false
	    	  if (validBrowsers != null && validBrowsers.size() > 0){
	    		  isNodejsDefaultBrowser = isNodejsDefaultBrowserBtn.getSelection();
	    	  }
	      }
	    });
	    
        return composite;
	}
	
	protected void updateWebBrowserCombo(Composite composite){		
		if (webBrowserCombo == null){
			webBrowserCombo = new Combo(composite, SWT.BORDER | SWT.READ_ONLY);
		}
		else {
			webBrowserCombo.removeAll();
		}
        
        BrowserManager bm = BrowserManager.getInstance();
        if (bm != null){
	        List<IBrowserDescriptor> browserList = bm.getWebBrowsers();
	        if (browserList != null){
		        validBrowsers = new ArrayList<IBrowserDescriptor>();
		        
		        int len = browserList.size();
		        
		        for (int i=0;i<len;i++){
		        	IBrowserDescriptor tempBrowser = browserList.get(i);
		        	if (tempBrowser != null && tempBrowser.getLocation() != null && 
		        			!tempBrowser.getLocation().trim().equals("")){ //$NON-NLS-1$
		        		validBrowsers.add(tempBrowser);
		        	}
		        }
		
		        len = validBrowsers.size();
		        int selectedIndex = -1;
		        
		        for (int i=0;i<len;i++){
		        	IBrowserDescriptor tempBrowser = validBrowsers.get(i);
		        	if (tempBrowser != null){
			    		webBrowserCombo.add(tempBrowser.getName());
			    		String location = tempBrowser.getLocation();
		    			// Try to select the first Chrome browser that can be found (use location to determine if it's Chrome) 
		    		   if (location != null && location.toLowerCase().indexOf("chrome") >= 0){ //$NON-NLS-1$
		    			   selectedIndex = i;
		    		   }
		        	}
		        }
		        
		        // Only enable the OK button if something is selected
				Button okButton = this.getButton(IDialogConstants.OK_ID);
				if (okButton != null) {
					okButton.setEnabled(selectedIndex >= 0);
				}
		        
		        if (selectedIndex >= 0){
			        webBrowserCombo.select(selectedIndex);
			        browserName = validBrowsers.get(selectedIndex).getName();
		        }
		        
				webBrowserCombo.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent e) {
						boolean valid = false;
						if (validBrowsers != null){
							if (webBrowserCombo.getSelectionIndex() >= 0){
								browserName = validBrowsers.get(webBrowserCombo.getSelectionIndex()).getName();
								valid = true;
							}
						}
						Button okButton = getButton(IDialogConstants.OK_ID);
						if (okButton != null) {
							okButton.setEnabled(valid);
						}
					}
				});
	        }
        }
		webBrowserCombo.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
	}

	public boolean isNodejsDefaultBrowserSet(){
		return isNodejsDefaultBrowser;
	}
	
	public String getBrowserName(){
		return browserName;
	}
	
	public int open(){
		// Make sure the OK button is only enabled if there is a selection
		Button okButton = this.getButton(IDialogConstants.OK_ID);		
		if (okButton != null) {
			okButton.setEnabled(webBrowserCombo != null && webBrowserCombo.getSelectionIndex() >= 0);
		}
		
		return super.open();
	}
}