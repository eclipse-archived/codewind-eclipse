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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.codewind.ui.CodewindUIPlugin;
import org.eclipse.codewind.ui.internal.messages.Messages;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Control;
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
    
    public static void setBold(StyledText text) {
		StyleRange range = new StyleRange();
		range.start = 0;
		range.length = text.getText().length();
		range.fontStyle = SWT.BOLD;
		text.setStyleRange(range);
	}
    
    public static void normalizeBackground(Control control, Control parent) {
    	control.setBackground(parent.getBackground());
    	control.setForeground(parent.getForeground());
    }
    
    public static MultiStatus getMultiStatus(String msg, Exception e) {
    	List<Status> statusList = new ArrayList<Status>();
    	StackTraceElement[] elems = e.getStackTrace();
    	for (StackTraceElement elem : elems) {
    		statusList.add(new Status(IStatus.ERROR, CodewindUIPlugin.PLUGIN_ID, elem.toString()));
    	}
    	return new MultiStatus(CodewindUIPlugin.PLUGIN_ID, IStatus.ERROR,
    			statusList.toArray(new Status[statusList.size()]), e.toString(), e);
    }

}
