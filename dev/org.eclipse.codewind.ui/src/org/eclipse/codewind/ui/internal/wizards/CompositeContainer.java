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

package org.eclipse.codewind.ui.internal.wizards;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.jface.operation.IRunnableWithProgress;

// Allow the composite to communicate with its container which could be a dialog,
// a wizard page, etc.
public interface CompositeContainer {
	public void setErrorMessage(String msg);
	
	public void setMessage(String msg);
	
	public void validate();
	
	public void run(IRunnableWithProgress runnable) throws InvocationTargetException, InterruptedException;
}
