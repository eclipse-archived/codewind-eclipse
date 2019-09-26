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

package org.eclipse.codewind.ui.internal.views;

import org.eclipse.codewind.core.internal.connection.CodewindConnection;
import org.eclipse.jface.viewers.ViewerComparator;

public class CodewindNavigatorSorter extends ViewerComparator {

	@Override
	public int category(Object element) {
		if (element instanceof CodewindConnection) {
			if (((CodewindConnection) element).isLocal) {
				return 0;
			} else {
				return 1;
			}
		}
		return super.category(element);
	}

}
