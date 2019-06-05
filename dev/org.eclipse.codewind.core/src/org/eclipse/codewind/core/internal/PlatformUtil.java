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

package org.eclipse.codewind.core.internal;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.SocketException;
import java.util.Enumeration;

/**
 * Platform utility class. Provides utilities such as determining the
 * operating system.
 */
public class PlatformUtil {

    public enum OperatingSystem {
        LINUX,
        MAC,
        WINDOWS
    }
    
    public static OperatingSystem getOS() {
    	String osName = System.getProperty("os.name");
    	return getOS(osName);
    }

    public static OperatingSystem getOS(String osName) {
        if (osName == null || osName.isEmpty()) {
            Logger.logError("The operating system name is null or empty, defaulting to Linux.", null);
            return OperatingSystem.LINUX;
        }

        String name = osName.toLowerCase();
        if (name.contains("win"))
            return OperatingSystem.WINDOWS;
        if (name.contains("mac"))
            return OperatingSystem.MAC;
        if (name.contains("linux"))
            return OperatingSystem.LINUX;

        Logger.logError("The operating system name is not valid: " + osName + ", defaulting to Linux.", null);
        return OperatingSystem.LINUX;
    }
    
    public static int findFreePort() {
        ServerSocket socket = null;
        try {
            socket = new ServerSocket(0);
            return socket.getLocalPort();
        } catch (IOException e) {
            // ignore
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
        return -1;
    }
    
    public static String getHostName() throws SocketException {
    	Enumeration<NetworkInterface> e = NetworkInterface.getNetworkInterfaces();
		while(e.hasMoreElements())
		{
		    NetworkInterface n = (NetworkInterface) e.nextElement();
		    Enumeration<InetAddress> ee = n.getInetAddresses();
		    while (ee.hasMoreElements())
		    {
		        InetAddress i = (InetAddress) ee.nextElement();
		        if (!i.isSiteLocalAddress() && !i.isLinkLocalAddress() && !i.isLoopbackAddress() && !i.isAnyLocalAddress() && !i.isMulticastAddress()) {
		        	return i.getHostName();
		        }
		    }
		}
		throw new SocketException("Failed to get the IP address for the local host");
    }

}