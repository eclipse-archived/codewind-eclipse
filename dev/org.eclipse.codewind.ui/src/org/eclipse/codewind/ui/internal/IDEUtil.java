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

package org.eclipse.codewind.ui.internal;

import org.eclipse.codewind.ui.CodewindUIPlugin;
import org.eclipse.codewind.ui.internal.messages.Messages;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

public class IDEUtil {
	
    /*
     * Dialog which asks the user a question and they can select Yes, No
     * or Cancel.
     * Returns:
     *  0 - user selected Yes
     *  1 - user selected No
     *  2 - user selected Cancel
     */
    public static int openQuestionCancelDialog(String title, String msg) {
    	final int[] result = new int[1];
		Display.getDefault().syncExec(new Runnable() {
			@Override
			public void run() {
				Shell shell = Display.getDefault().getActiveShell();
				String[] buttonLabels = new String[] {Messages.DialogYesButton, Messages.DialogNoButton, Messages.DialogCancelButton};
				MessageDialog dialog = new MessageDialog(shell, title, CodewindUIPlugin.getImage(CodewindUIPlugin.CODEWIND_ICON),
						msg, MessageDialog.QUESTION, buttonLabels, 0);
				result[0] = dialog.open();
			}
		});
		
		return result[0];
	}
    
    public static boolean openConfirmDialog(String title, String msg) {
    	final boolean[] result = new boolean[1];
		Display.getDefault().syncExec(new Runnable() {
			@Override
			public void run() {
				Shell shell = Display.getDefault().getActiveShell();
				result[0] = MessageDialog.openConfirm(shell, title, msg);
			}
		});
		
		return result[0];
	}

}
