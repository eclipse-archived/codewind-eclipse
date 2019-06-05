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

package org.eclipse.codewind.ui.internal.views;

import org.eclipse.codewind.core.internal.CodewindApplication;
import org.eclipse.codewind.core.internal.connection.CodewindConnection;
import org.eclipse.codewind.core.internal.constants.AppState;
import org.eclipse.codewind.core.internal.constants.BuildStatus;
import org.eclipse.codewind.core.internal.constants.ProjectType;
import org.eclipse.codewind.ui.CodewindUIPlugin;
import org.eclipse.codewind.ui.internal.messages.Messages;
import org.eclipse.jface.preference.JFacePreferences;
import org.eclipse.jface.resource.FontDescriptor;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.StyledString.Styler;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.TextStyle;
import org.eclipse.swt.widgets.Display;

/**
 * Label provider for the Codewind view.
 */
public class CodewindNavigatorLabelProvider extends LabelProvider implements IStyledLabelProvider {

	static final Styler BOLD_FONT_STYLER = new BoldFontStyler();
	
	public static final Styler ERROR_STYLER = StyledString.createColorRegistryStyler(
			JFacePreferences.ERROR_COLOR, null);
	
	@Override
	public String getText(Object element) {
		if (element instanceof CodewindConnection) {
			CodewindConnection connection = (CodewindConnection)element;
			String text = Messages.CodewindConnectionLabel + " " + connection.baseUrl;
			if (!connection.isConnected()) {
				String errorMsg = connection.getConnectionErrorMsg();
				if (errorMsg == null) {
					errorMsg = Messages.CodewindDisconnected;
				}
				text = text + " (" + errorMsg + ")";
			} else if (connection.getApps().size() == 0) {
				text = text + " (" + Messages.CodewindConnectionNoProjects + ")";
			}
			return text;
		} else if (element instanceof CodewindApplication) {
			CodewindApplication app = (CodewindApplication)element;
			StringBuilder builder = new StringBuilder(app.name);
			
			if (app.isEnabled()) {
				AppState appState = app.getAppState();
				String displayString = appState.getDisplayString(app.getStartMode());
				builder.append(" [" + displayString + "]");
				
				BuildStatus buildStatus = app.getBuildStatus();
				String buildDetails = app.getBuildDetails();
				if (buildDetails != null && !buildDetails.isEmpty()) {
					builder.append(" [" + buildStatus.getDisplayString() + ": " + buildDetails + "]");
				} else {
					builder.append(" [" + buildStatus.getDisplayString() + "]");
				}
			} else {
				builder.append(" [" + Messages.CodewindProjectDisabled + "]");
			}
			return builder.toString();
		}
		return super.getText(element);
	}

	@Override
	public StyledString getStyledText(Object element) {
		StyledString styledString;
		if (element instanceof CodewindConnection) {
			CodewindConnection connection = (CodewindConnection)element;
			styledString = new StyledString(Messages.CodewindConnectionLabel + " " );
			styledString.append(connection.baseUrl.toString(), StyledString.QUALIFIER_STYLER);
			if (!connection.isConnected()) {
				String errorMsg = connection.getConnectionErrorMsg();
				if (errorMsg == null) {
					errorMsg = Messages.CodewindDisconnected;
				}
				styledString.append(" (" + errorMsg + ")", ERROR_STYLER);
			} else if (connection.getApps().size() == 0) {
				styledString.append(" (" + Messages.CodewindConnectionNoProjects + ")", StyledString.DECORATIONS_STYLER);
			}
		} else if (element instanceof CodewindApplication) {
			CodewindApplication app = (CodewindApplication)element;
			styledString = new StyledString(app.name);
			
			if (app.isEnabled()) {
				AppState appState = app.getAppState();
				String displayString = appState.getDisplayString(app.getStartMode());
				styledString.append(" [" + displayString + "]", StyledString.DECORATIONS_STYLER);
				
				BuildStatus buildStatus = app.getBuildStatus();
				String buildDetails = app.getBuildDetails();
				if (buildDetails != null) {
					styledString.append(" [" + buildStatus.getDisplayString() + ": ", StyledString.DECORATIONS_STYLER);
					styledString.append(buildDetails, StyledString.QUALIFIER_STYLER);
					styledString.append("]", StyledString.DECORATIONS_STYLER);
				} else {
					styledString.append(" [" + buildStatus.getDisplayString() + "]", StyledString.DECORATIONS_STYLER);
				}
			} else {
				styledString.append(" [" + Messages.CodewindProjectDisabled + "]", StyledString.DECORATIONS_STYLER);
			}
		} else {
			styledString = new StyledString(getText(element));
		}
		return styledString;
	}

	@Override
	public Image getImage(Object element) {
		if (element instanceof CodewindConnection) {
			return CodewindUIPlugin.getImage(CodewindUIPlugin.CODEWIND_ICON);
		} else if (element instanceof CodewindApplication) {
			ProjectType type = ((CodewindApplication)element).projectType;
			if (type.isLanguage(ProjectType.LANGUAGE_JAVA)) {
				return CodewindUIPlugin.getImage(CodewindUIPlugin.JAVA_ICON);
			}
			if (type.isLanguage(ProjectType.LANGUAGE_NODEJS)) {
				return CodewindUIPlugin.getImage(CodewindUIPlugin.NODE_ICON);
			}
			if (type.isLanguage(ProjectType.LANGUAGE_SWIFT)) {
				return CodewindUIPlugin.getImage(CodewindUIPlugin.SWIFT_ICON);
			}
			if (type.isLanguage(ProjectType.LANGUAGE_GO)) {
				return CodewindUIPlugin.getImage(CodewindUIPlugin.GO_ICON);
			}
			if (type.isLanguage(ProjectType.LANGUAGE_PYTHON)) {
				return CodewindUIPlugin.getImage(CodewindUIPlugin.PYTHON_ICON);
			}
		}
		return null;
	}

	static class BoldFontStyler extends Styler {
	    @Override
	    public void applyStyles(final TextStyle textStyle)
	    {
	        FontDescriptor boldDescriptor = FontDescriptor.createFrom(new FontData()).setStyle(SWT.BOLD);
	        Font boldFont = boldDescriptor.createFont(Display.getCurrent());
	        textStyle.font = boldFont;
	    }
	}

}
