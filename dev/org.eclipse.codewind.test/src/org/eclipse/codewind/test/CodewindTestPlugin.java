/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.codewind.test;

import java.io.IOException;
import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Plugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

public class CodewindTestPlugin extends Plugin {

    private static CodewindTestPlugin plugin;
    private IPath installLocationPath;
    
	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext bundleContext) throws Exception {
		super.start(bundleContext);
		plugin = this;
	}

	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext bundleContext) throws Exception {
		plugin = null;
		super.stop(bundleContext);
	}
	
    public static CodewindTestPlugin getDefault() {
        return plugin;
    }
    
    public static IPath getInstallLocation() {
        CodewindTestPlugin plugin = getDefault();
        if (plugin.installLocationPath == null) {
            String installLocation = getBundleFullLocationPath(plugin.getBundle());
            plugin.installLocationPath = new Path(installLocation);
        }
        return plugin.installLocationPath;
    }
    
    public static String getBundleFullLocationPath(Bundle bundle) {
        if (bundle == null) {
            return null;
        }
        URL installURL = bundle.getEntry("/");
        String installLocation = null;
        try {
            URL realURL = FileLocator.resolve(installURL);
            installLocation = realURL.getFile();

            // Drop the beginning and end /
            if (installLocation != null && installLocation.startsWith("/") && installLocation.indexOf(":") > 0) {
                installLocation = installLocation.substring(1);
                // Make sure the path ends with a '/'		
                if (!installLocation.endsWith("/")) {
                    installLocation = installLocation + "/";
                }
            }
        } catch (IOException e) {
            System.out.println("Could not get the Plugin Full location Path:" + " getPluginFullLocationPath()");
        }
        return installLocation;
    }

}
