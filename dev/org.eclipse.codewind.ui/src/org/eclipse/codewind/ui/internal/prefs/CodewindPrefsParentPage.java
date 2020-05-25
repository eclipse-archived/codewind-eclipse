/*******************************************************************************
 * Copyright (c) 2018, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.codewind.ui.internal.prefs;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.codewind.core.CodewindCorePlugin;
import org.eclipse.codewind.core.internal.Logger;
import org.eclipse.codewind.core.internal.cli.InstallUtil;
import org.eclipse.codewind.ui.CodewindUIPlugin;
import org.eclipse.codewind.ui.internal.actions.CodewindInstall;
import org.eclipse.codewind.ui.internal.debug.NodeJSDebugLauncher;
import org.eclipse.codewind.ui.internal.messages.Messages;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.internal.browser.BrowserManager;
import org.eclipse.ui.internal.browser.IBrowserDescriptor;

/**
 * Top level Codewind preference page
 */
@SuppressWarnings("restriction")
public class CodewindPrefsParentPage extends PreferencePage implements IWorkbenchPreferencePage {

	public static final String ID = "CodewindParentPage"; //$NON-NLS-1$
	
	private static IPreferenceStore prefs;

	private Button autoOpenOverviewButton, supportFeaturesButton;
	private Text installTimeoutText, uninstallTimeoutText, startTimeoutText, stopTimeoutText;
	private Text debugTimeoutText;
	private Label browserSelectionLabel;
	private Combo webBrowserCombo;
	private Link addBrowserButton;
	private Button[] stopAppsButtons = new Button[3];
	private Button useBuiltinDebugButton;
	private Composite selectWebBrowserComposite;
		
	private String browserName = null;

	@Override
	public void init(IWorkbench arg0) {
		// setDescription("Expand this preferences category to set specific Codewind preferences.");
		setImageDescriptor(CodewindUIPlugin.getDefaultIcon());

		prefs = CodewindCorePlugin.getDefault().getPreferenceStore();
	}

	@Override
	protected Control createContents(Composite parent) {
		final Composite composite = new Composite(parent, SWT.NONE);
	    GridLayout layout = new GridLayout();
		layout.horizontalSpacing = convertHorizontalDLUsToPixels(4);
		layout.verticalSpacing = convertVerticalDLUsToPixels(3);
		layout.marginWidth = 0;
		layout.marginHeight = 0;
	    layout.numColumns = 1;
	    composite.setLayout(layout);
	    composite.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.VERTICAL_ALIGN_FILL));

	    if (CodewindInstall.ENABLE_STOP_APPS_OPTION) {
		    Text stopAppContainersLabel = new Text(composite, SWT.READ_ONLY | SWT.SINGLE);
		    stopAppContainersLabel.setText(Messages.PrefsParentPage_StopAppsLabel);
		    stopAppContainersLabel.setLayoutData(new GridData(GridData.BEGINNING, GridData.FILL, false, false, 2, 1));
		    stopAppContainersLabel.setBackground(composite.getBackground());
		    stopAppContainersLabel.setForeground(composite.getForeground());
	
		    Composite stopAppsComposite = new Composite(composite, SWT.NONE);
		    layout = new GridLayout();
		    layout.horizontalSpacing = convertHorizontalDLUsToPixels(8);
		    layout.verticalSpacing = convertVerticalDLUsToPixels(3);
		    layout.marginWidth = 20;
		    layout.marginHeight = 2;
		    layout.numColumns = 3;
		    stopAppsComposite.setLayout(layout);
		    stopAppsComposite.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false, 2, 1));
	
		    stopAppsButtons[0] = new Button(stopAppsComposite, SWT.RADIO);
		    stopAppsButtons[0].setText(Messages.PrefsParentPage_StopAppsAlways);
		    stopAppsButtons[0].setData(InstallUtil.STOP_APP_CONTAINERS_ALWAYS);
	
		    stopAppsButtons[1] = new Button(stopAppsComposite, SWT.RADIO);
		    stopAppsButtons[1].setText(Messages.PrefsParentPage_StopAppsNever);
		    stopAppsButtons[1].setData(InstallUtil.STOP_APP_CONTAINERS_NEVER);
	
		    stopAppsButtons[2] = new Button(stopAppsComposite, SWT.RADIO);
		    stopAppsButtons[2].setText(Messages.PrefsParentPage_StopAppsPrompt);
		    stopAppsButtons[2].setData(InstallUtil.STOP_APP_CONTAINERS_PROMPT);
	
		    setStopAppsSelection(prefs.getString(InstallUtil.STOP_APP_CONTAINERS_PREFSKEY));
	
		    new Label(composite, SWT.HORIZONTAL).setLayoutData(new GridData(GridData.FILL_HORIZONTAL, GridData.CENTER, true, false, 2, 1));
	    }
	    
	    Group generalGroup = new Group(composite, SWT.NONE);
	    generalGroup.setText(Messages.PrefsParentPage_GeneralGroup);
	    layout = new GridLayout();
	    layout.horizontalSpacing = convertHorizontalDLUsToPixels(7);
	    layout.verticalSpacing = convertVerticalDLUsToPixels(5);
	    layout.marginWidth = 3;
	    layout.marginHeight = 10;
	    layout.numColumns = 1;
	    generalGroup.setLayout(layout);
	    generalGroup.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.VERTICAL_ALIGN_FILL));
	    
	    autoOpenOverviewButton = new Button(generalGroup, SWT.CHECK);
	    autoOpenOverviewButton.setText(Messages.PrefsParentPage_AutoOpenOverviewButton);
	    autoOpenOverviewButton.setLayoutData(new GridData(GridData.BEGINNING, GridData.FILL, false, false));
	    autoOpenOverviewButton.setSelection(prefs.getBoolean(CodewindCorePlugin.AUTO_OPEN_OVERVIEW_PAGE));
	    
	    supportFeaturesButton = new Button(generalGroup, SWT.CHECK);
	    supportFeaturesButton.setText(Messages.PrefsParentPage_EnableSupportFeatures);
	    supportFeaturesButton.setLayoutData(new GridData(GridData.BEGINNING, GridData.FILL, false, false));
	    supportFeaturesButton.setSelection(prefs.getBoolean(CodewindCorePlugin.ENABLE_SUPPORT_FEATURES));
	    
	    Group installGroup = new Group(composite, SWT.NONE);
	    installGroup.setText(Messages.PrefsParentPage_StartupShutdownGroup);
	    layout = new GridLayout();
	    layout.horizontalSpacing = convertHorizontalDLUsToPixels(7);
	    layout.verticalSpacing = convertVerticalDLUsToPixels(5);
	    layout.marginWidth = 3;
	    layout.marginHeight = 10;
	    layout.numColumns = 4;
	    installGroup.setLayout(layout);
	    installGroup.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.VERTICAL_ALIGN_FILL));
	    
	    installTimeoutText = createCWTimeoutEntry(installGroup, Messages.PrefsParentPage_InstallTimeout, CodewindCorePlugin.CW_INSTALL_TIMEOUT);
	    uninstallTimeoutText = createCWTimeoutEntry(installGroup, Messages.PrefsParentPage_UninstallTimeout, CodewindCorePlugin.CW_UNINSTALL_TIMEOUT);
	    startTimeoutText = createCWTimeoutEntry(installGroup, Messages.PrefsParentPage_StartTimeout, CodewindCorePlugin.CW_START_TIMEOUT);
	    stopTimeoutText = createCWTimeoutEntry(installGroup, Messages.PrefsParentPage_StopTimeout, CodewindCorePlugin.CW_STOP_TIMEOUT);
	    
	    Group debugGroup = new Group(composite, SWT.NONE);
	    debugGroup.setText(Messages.PrefsParentPage_DebugGroup);
	    layout = new GridLayout();
	    layout.horizontalSpacing = convertHorizontalDLUsToPixels(7);
	    layout.verticalSpacing = convertVerticalDLUsToPixels(5);
	    layout.marginWidth = 3;
	    layout.marginHeight = 10;
	    layout.numColumns = 2;
	    debugGroup.setLayout(layout);
	    debugGroup.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.VERTICAL_ALIGN_FILL));

		Label debugTimeoutLabel = new Label(debugGroup, SWT.READ_ONLY);
		debugTimeoutLabel.setText(" " + Messages.PrefsParentPage_DebugTimeoutLabel); //$NON-NLS-1$
		debugTimeoutLabel.setLayoutData(new GridData(GridData.BEGINNING, GridData.FILL, false, false));

		debugTimeoutText = new Text(debugGroup, SWT.BORDER);
		debugTimeoutText.setTextLimit(3);
		debugTimeoutText.setText("" + 	//$NON-NLS-1$
				prefs.getInt(CodewindCorePlugin.DEBUG_CONNECT_TIMEOUT_PREFSKEY));

		GridData debugTextData = new GridData(GridData.BEGINNING, GridData.FILL, false, false);
		debugTextData.widthHint = 50;
		debugTimeoutText.setLayoutData(debugTextData);

		debugTimeoutText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent arg0) {
				validate();
			}
		});
		
	    if (NodeJSDebugLauncher.hasBuiltinDebugger()) {
	    	useBuiltinDebugButton = new Button(debugGroup, SWT.CHECK);
	    	useBuiltinDebugButton.setText("Use Eclipse builtin node.js debugger");
	    	useBuiltinDebugButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 2, 1));
	    	useBuiltinDebugButton.setSelection(prefs.getBoolean(CodewindCorePlugin.USE_BUILTIN_NODEJS_DEBUG_PREFSKEY));
	    }
		 	    
	    selectWebBrowserComposite = new Composite(debugGroup, SWT.NONE);
	    layout = new GridLayout();
		layout.horizontalSpacing = convertHorizontalDLUsToPixels(4);
		layout.verticalSpacing = convertVerticalDLUsToPixels(3);
		layout.marginWidth = 15;
		layout.marginHeight = 0;
	    layout.numColumns = 2;
	    selectWebBrowserComposite.setLayout(layout);
	    selectWebBrowserComposite.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false, 2, 1));
	    
	    browserSelectionLabel = new Label(selectWebBrowserComposite, SWT.NONE);
	    browserSelectionLabel.setText(Messages.BrowserSelectionLabel);
	    browserSelectionLabel.setLayoutData(new GridData(GridData.BEGINNING, GridData.FILL, false, false, 2, 1));
        
        webBrowserCombo = new Combo(selectWebBrowserComposite, SWT.BORDER | SWT.READ_ONLY);
        
	    refreshPreferencesPage();
	    	    
		addBrowserButton = new Link(selectWebBrowserComposite, SWT.NONE);
		addBrowserButton.setText("<a href=\"org.eclipse.ui.browser.preferencePage\">"+Messages.BrowserSelectionManageButtonText+"</a>"); //$NON-NLS-1$ //$NON-NLS-2$
		addBrowserButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {				
				PreferencesUtil.createPreferenceDialogOn(composite
						.getShell(), e.text, null, null);
				composite.layout();				
			}
		});
		
		if (useBuiltinDebugButton != null) {
			useBuiltinDebugButton.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					enableBrowserControls();
				}
			});
		}
		
		Label endSpacer = new Label(composite, SWT.NONE);
		endSpacer.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true, 2, 1));
		// this last label just moves the Default and Apply buttons over
		new Label(composite, SWT.NONE);

		enableBrowserControls();
		return composite;
	}
	
	private void enableBrowserControls() {
		boolean enable = useBuiltinDebugButton == null || !useBuiltinDebugButton.getSelection();
		browserSelectionLabel.setEnabled(enable);
		webBrowserCombo.setEnabled(enable);
		addBrowserButton.setEnabled(enable);
	}
	
	private Text createCWTimeoutEntry(Composite comp, String label, String prefKey) {
	    Label timeoutLabel = new Label(comp, SWT.NONE);
	    timeoutLabel.setText(label);
	    timeoutLabel.setLayoutData(new GridData(GridData.BEGINNING, GridData.FILL, false, false));
	    
	    Text text = new Text(comp, SWT.BORDER);
	    text.setText(Integer.toString(prefs.getInt(prefKey)));
	    GridData data = new GridData(GridData.BEGINNING, GridData.FILL, false, false);
		data.widthHint = 50;
		text.setLayoutData(data);
		
		text.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent arg0) {
				validate();
			}
		});
		
	    return text;
	}
	
	private void setStopAppsSelection(String selection) {
		for (Button button : stopAppsButtons) {
			if (button.getData().equals(selection)) {
				button.setSelection(true);
				break;
			}
		}
	}

	private void validate() {
		if (!validateTimeout(installTimeoutText.getText().trim()) ||
				!validateTimeout(uninstallTimeoutText.getText().trim()) ||
				!validateTimeout(startTimeoutText.getText().trim()) ||
				!validateTimeout(stopTimeoutText.getText().trim()) ||
				!validateTimeout(debugTimeoutText.getText().trim())) {
			return;
		}
		
		setErrorMessage(null);
		setValid(true);
	}
	
	private boolean validateTimeout(String timeoutStr) {
		boolean isValid = true;
		try {
			int timeout = Integer.parseInt(timeoutStr);
			if (timeout <= 0) {
				isValid = false;
			}
		} catch(NumberFormatException e) {
			isValid = false;
		}
		if (!isValid) {
			setErrorMessage(NLS.bind(Messages.PrefsParentPage_ErrInvalidTimeout, timeoutStr));
			setValid(false);
		}
		return isValid;
	}

	@Override
	public boolean performOk() {
		if (!isValid()) {
			return false;
		}
		
		if (CodewindInstall.ENABLE_STOP_APPS_OPTION) {
			for (Button button : stopAppsButtons) {
				if (button.getSelection()) {
					prefs.setValue(InstallUtil.STOP_APP_CONTAINERS_PREFSKEY, (String)button.getData());
					break;
				}
			}
		}
		
		prefs.setValue(CodewindCorePlugin.AUTO_OPEN_OVERVIEW_PAGE, autoOpenOverviewButton.getSelection());
		prefs.setValue(CodewindCorePlugin.ENABLE_SUPPORT_FEATURES, supportFeaturesButton.getSelection());
		
		prefs.setValue(CodewindCorePlugin.CW_INSTALL_TIMEOUT, Integer.parseInt(installTimeoutText.getText().trim()));
		prefs.setValue(CodewindCorePlugin.CW_UNINSTALL_TIMEOUT, Integer.parseInt(uninstallTimeoutText.getText().trim()));
		prefs.setValue(CodewindCorePlugin.CW_START_TIMEOUT, Integer.parseInt(startTimeoutText.getText().trim()));
		prefs.setValue(CodewindCorePlugin.CW_STOP_TIMEOUT, Integer.parseInt(stopTimeoutText.getText().trim()));
		
		// validate in validate() that this is a good integer
		int debugTimeout = Integer.parseInt(debugTimeoutText.getText().trim());
		prefs.setValue(CodewindCorePlugin.DEBUG_CONNECT_TIMEOUT_PREFSKEY, debugTimeout);

		// removes any trimmed space
		debugTimeoutText.setText("" + debugTimeout); //$NON-NLS-1$
		
		prefs.setValue(CodewindCorePlugin.USE_BUILTIN_NODEJS_DEBUG_PREFSKEY, useBuiltinDebugButton.getSelection());
		
		if (this.webBrowserCombo != null) {
			// The first option in the webBrowserCombo is to not use the default browser.
			// As a result, if the first option is selected, then remove the preference
			if (webBrowserCombo.getSelectionIndex() > 0) {
				// If it is selected, then save the preference. Do not add if it's the first item, since the option
				// for the first entry is "No browser selected"
				if (browserName != null) {
					prefs.setValue(CodewindCorePlugin.NODEJS_DEBUG_BROWSER_PREFSKEY, browserName);
				}
			} else {
				prefs.setToDefault(CodewindCorePlugin.NODEJS_DEBUG_BROWSER_PREFSKEY);
			}
		}

		return true;
	}
	
	@Override
	public void performDefaults() {
		if (CodewindInstall.ENABLE_STOP_APPS_OPTION) {
			setStopAppsSelection(prefs.getDefaultString(InstallUtil.STOP_APP_CONTAINERS_PREFSKEY));
		}
		
		autoOpenOverviewButton.setSelection(prefs.getDefaultBoolean(CodewindCorePlugin.AUTO_OPEN_OVERVIEW_PAGE));
		supportFeaturesButton.setSelection(prefs.getDefaultBoolean(CodewindCorePlugin.ENABLE_SUPPORT_FEATURES));
		
		installTimeoutText.setText(Integer.toString(prefs.getDefaultInt(CodewindCorePlugin.CW_INSTALL_TIMEOUT)));
		uninstallTimeoutText.setText(Integer.toString(prefs.getDefaultInt(CodewindCorePlugin.CW_UNINSTALL_TIMEOUT)));
		startTimeoutText.setText(Integer.toString(prefs.getDefaultInt(CodewindCorePlugin.CW_START_TIMEOUT)));
		stopTimeoutText.setText(Integer.toString(prefs.getDefaultInt(CodewindCorePlugin.CW_STOP_TIMEOUT)));
		
		debugTimeoutText.setText("" + 	//$NON-NLS-1$
				prefs.getDefaultInt(CodewindCorePlugin.DEBUG_CONNECT_TIMEOUT_PREFSKEY));
		
		if (useBuiltinDebugButton != null) {
			useBuiltinDebugButton.setSelection(prefs.getDefaultBoolean(CodewindCorePlugin.USE_BUILTIN_NODEJS_DEBUG_PREFSKEY));
			webBrowserCombo.setEnabled(!useBuiltinDebugButton.getSelection());
			addBrowserButton.setEnabled(!useBuiltinDebugButton.getSelection());
		}
		webBrowserCombo.select(0);
		enableBrowserControls();
	}
	
	// Refreshes all changes
	protected void refreshPreferencesPage() {
		if (webBrowserCombo == null)
			return;
		
		// Remove all the browsers, since a browser may have been removed through
		// the Web Browser preference page
		webBrowserCombo.removeAll();
		webBrowserCombo.add(Messages.BrowserSelectionNoBrowserSelected);
		webBrowserCombo.select(0);
        
        BrowserManager bm = BrowserManager.getInstance();
    	List<IBrowserDescriptor> validBrowsers;
        
        if (bm != null) {
	        List<IBrowserDescriptor> browserList = bm.getWebBrowsers();
	        if (browserList != null) {
		        validBrowsers = new ArrayList<IBrowserDescriptor>();		        
		        int len = browserList.size();
		        
		        // Search for valid browers only. If the location is null, that means
		        // it is the default system browser. Since the browser will be launched
		        // as an external browser, a location must exist or there will be an error
		        for (int i=0; i<len; i++) {
		        	IBrowserDescriptor tempBrowser = browserList.get(i);
		        	if (tempBrowser != null && tempBrowser.getLocation() != null && 
		        			!tempBrowser.getLocation().trim().equals("")) { //$NON-NLS-1$
		        		validBrowsers.add(tempBrowser);
		        	}
		        }
		
		        len = validBrowsers.size();		        
				Logger.log("Refresh preference page valid browser length: " + len);  //$NON-NLS-1$
		        		        		        
				// When it loads, check if preference exists
				String foundDefaultBrowser = prefs.getString(CodewindCorePlugin.NODEJS_DEBUG_BROWSER_PREFSKEY);
		        boolean foundDefaultBrowserName = false;
		        
		        for (int i=0; i<len; i++) {
		        	IBrowserDescriptor tempBrowser = validBrowsers.get(i);
		        	if (tempBrowser != null) {
		        		String browserName = tempBrowser.getName();
			    		webBrowserCombo.add(browserName);

		    		   if (browserName != null && foundDefaultBrowser != null && browserName.equalsIgnoreCase(foundDefaultBrowser)) {
		    			   // Need to add 1 because of the first option of no browser selected
		    			   webBrowserCombo.select(i + 1);
		    			   foundDefaultBrowserName = true;
		    		   }
		        	}
		        }
		        
				Logger.log("Refresh preference page found default browser: " + foundDefaultBrowser);  //$NON-NLS-1$
		        
				if (foundDefaultBrowser == null || !foundDefaultBrowserName){
					// Could not find browser from preference or browser got removed
					Logger.log("Refresh preference page: could not find preferences or browser was removed");  //$NON-NLS-1$
					prefs.setToDefault(CodewindCorePlugin.NODEJS_DEBUG_BROWSER_PREFSKEY);
				}
				
				webBrowserCombo.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent e) {
						if (webBrowserCombo.getSelectionIndex() >= 0){
							browserName = webBrowserCombo.getText();
						}
					}
				});
	        }
        }
	
		webBrowserCombo.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
	}

	@Override
	public void setVisible(boolean visible){
		// Override the setVisible for when the user updates the web browser page
		// and then returns back to this preference page. The preference page will
		// not get redrawn, so update the combo box in this method
		if (webBrowserCombo != null){
			refreshPreferencesPage();
		}
		super.setVisible(visible);
	}
}
