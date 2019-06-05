/*******************************************************************************
 * Copyright (c) 2018, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.codewind.ui;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.codewind.core.CodewindCorePlugin;
import org.eclipse.codewind.core.internal.Logger;
import org.eclipse.codewind.core.internal.constants.ProjectType;
import org.eclipse.codewind.ui.internal.debug.NodeJSDebugLauncher;
import org.eclipse.codewind.ui.internal.views.UpdateHandler;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class CodewindUIPlugin extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "org.eclipse.codewind.ui"; //$NON-NLS-1$

	private static URL ICON_BASE_URL;
	protected Map<String, ImageDescriptor> imageDescriptors = new HashMap<String, ImageDescriptor>();
	private UpdateHandler updateHandler;
	
	public static final String
			ICON_BASE_PATH = "icons/",
			CODEWIND_ICON = "codewind.ico",
			CODEWIND_BANNER = "codewindBanner.png",
			ERROR_ICON_PATH = "error.gif",
			OPEN_BROWSER_ICON = "elcl16/internal_browser.gif",
			BUILD_ICON = "elcl16/build_exec.png",
			IMPORT_ICON = "elcl16/import_wiz.png",
			LAUNCH_DEBUG_ICON = "elcl16/launch_debug.gif",
			LAUNCH_RUN_ICON = "elcl16/launch_run.gif",
			REFRESH_ICON = "elcl16/refresh.png",
			JAVA_ICON = "obj16/java.png",
			NODE_ICON = "obj16/node.png",
			SWIFT_ICON = "obj16/swift.png",
			GO_ICON = "obj16/go.png",
			PYTHON_ICON = "obj16/python.png";

	// The shared instance
	private static CodewindUIPlugin plugin;

	/**
	 * The constructor
	 */
	public CodewindUIPlugin() {
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		updateHandler = new UpdateHandler();
		CodewindCorePlugin.setUpdateHandler(updateHandler);
		CodewindCorePlugin.addDebugLauncher(ProjectType.LANGUAGE_NODEJS, new NodeJSDebugLauncher());
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	@Override
	public void stop(BundleContext context) throws Exception {
		updateHandler = null;
		CodewindCorePlugin.setUpdateHandler(null);
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static CodewindUIPlugin getDefault() {
		return plugin;
	}

	public static ImageDescriptor getImageDescriptor(String path) {
		final URL url = CodewindUIPlugin.getDefault().getBundle().getEntry(ICON_BASE_PATH + path);
		return ImageDescriptor.createFromURL(url);
	}

	public static ImageDescriptor getDefaultIcon() {
		return getImageDescriptor(CODEWIND_ICON);
	}
	
    public static Image getImage(String key) {
        return plugin.getImageRegistry().get(key);
    }
	
    @Override
    protected ImageRegistry createImageRegistry() {
        ImageRegistry registry = new ImageRegistry();
        if (ICON_BASE_URL == null)
            ICON_BASE_URL = plugin.getBundle().getEntry(ICON_BASE_PATH);

        registerImage(registry, CODEWIND_ICON, ICON_BASE_URL + CODEWIND_ICON);
        registerImage(registry, OPEN_BROWSER_ICON, ICON_BASE_URL + OPEN_BROWSER_ICON);
        registerImage(registry, BUILD_ICON, ICON_BASE_URL + BUILD_ICON);
        registerImage(registry, IMPORT_ICON, ICON_BASE_URL + IMPORT_ICON);
        registerImage(registry, LAUNCH_DEBUG_ICON, ICON_BASE_URL + LAUNCH_DEBUG_ICON);
        registerImage(registry, LAUNCH_RUN_ICON, ICON_BASE_URL + LAUNCH_RUN_ICON);
        registerImage(registry, REFRESH_ICON, ICON_BASE_URL + REFRESH_ICON);
        registerImage(registry, JAVA_ICON, ICON_BASE_URL + JAVA_ICON);
        registerImage(registry, NODE_ICON, ICON_BASE_URL + NODE_ICON);
        registerImage(registry, SWIFT_ICON, ICON_BASE_URL + SWIFT_ICON);
        registerImage(registry, GO_ICON, ICON_BASE_URL + GO_ICON);
        registerImage(registry, PYTHON_ICON, ICON_BASE_URL + PYTHON_ICON);

        return registry;
    }

    private void registerImage(ImageRegistry registry, String key, String partialURL) {
        try {
            ImageDescriptor id = ImageDescriptor.createFromURL(new URL(ICON_BASE_URL, partialURL));
            registry.put(key, id);
            imageDescriptors.put(key, id);
        } catch (Exception e) {
            Logger.logError("Error registering image", e);
        }
    }

	@Override
	/**
	 * @return The core plugin's preference store - everything should be stored there to prevent confusion.
	 */
	public IPreferenceStore getPreferenceStore() {
		return CodewindCorePlugin.getDefault().getPreferenceStore();
	}
	
	public static UpdateHandler getUpdateHandler() {
		return getDefault().updateHandler;
	}

}
