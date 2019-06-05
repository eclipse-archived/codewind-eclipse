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
import java.io.InputStream;
import java.util.concurrent.TimeoutException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;

public class ProcessHelper {

    public static class ProcessResult {
        private final int exitValue;
        private final String sysOut;
        private final String sysError;

        public ProcessResult(int exitValue, String sysOut, String sysError) {
            this.exitValue = exitValue;
            this.sysOut = sysOut;
            this.sysError = sysError;
        }

        public int getExitValue() {
            return exitValue;
        }

        public String getOutput() {
            return sysOut.toString();
        }
        
        public String getError() {
        	return sysError.toString();
        }
    }

    /**
     * Wait for the given process to finish, and return the exit value and output. Checks for status every
     * <code>pollingDelay</code> ms, and terminates the process if it takes more than <code>timeout</code>
     * seconds. The progress monitor passed in must be <code>null</code> or already begun, and this method
     * will add <code>totalWork</code> to it's progress.
     * 
     * @param p the process to monitor
     * @param pollingDelay the delay between polling the process, in ms
     * @param timeout the process timeout, in seconds
     * @return the exit value
     * @throws IOException if the process fails to exit normally and cannot be terminated
     */
    public static ProcessResult waitForProcess(final Process p, int pollingDelay, int timeout, IProgressMonitor monitor) throws IOException, TimeoutException {
        final int BUFFER_STEP = 1024;
        byte[] buf = new byte[BUFFER_STEP];
        InputStream in = null;
        InputStream err = null;
        StringBuilder inBuilder = new StringBuilder();
        StringBuilder errBuilder = new StringBuilder();
        int iter = timeout * 1000 / pollingDelay;
        int work = 20;
        SubMonitor mon = SubMonitor.convert(monitor, work);
        try {
            in = p.getInputStream();
            err = p.getErrorStream();
            for (int i = 0; i < iter; i++) {
                try {
                    Thread.sleep(pollingDelay);
                } catch (InterruptedException e) {
                    // ignore
                }
                
                mon.worked(1);
                mon.setWorkRemaining(work);

                // read data from the process
                inBuilder.append(readInput(in, buf));
                errBuilder.append(readInput(err, buf));

                try {
                    int exitValue = p.exitValue();

                    // finish reading the data
                    inBuilder.append(readInput(in, buf));
                    errBuilder.append(readInput(err, buf));
                    
                    return new ProcessResult(exitValue, inBuilder.toString(), errBuilder.toString());
                } catch (IllegalThreadStateException e) {
                    // process has not terminated yet
                }
            }
        } finally {
            try {
                if (in != null)
                    in.close();
            } catch (Exception ex) {
                // ignore
            }
        }

        p.destroy();

        throw new TimeoutException("Process did not complete and had to be terminated");
    }
    
    private static String readInput(InputStream stream, byte[] buffer) throws IOException {
    	StringBuilder builder = new StringBuilder();
    	int n = stream.available();
        while (n > 0) {
            int len = stream.read(buffer, 0, Math.min(n, buffer.length));
            builder.append(new String(buffer, 0, len)); 
            n = stream.available();
        }
        return builder.toString();
    }
}