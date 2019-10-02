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
import org.eclipse.codewind.core.internal.CodewindManager;
import org.eclipse.codewind.core.internal.InstallStatus;
import org.eclipse.codewind.core.internal.InstallUtil;
import org.eclipse.codewind.core.internal.connection.CodewindConnection;
import org.eclipse.codewind.core.internal.connection.LocalConnection;
import org.eclipse.codewind.core.internal.connection.RemoteConnection;
import org.eclipse.codewind.core.internal.constants.AppStatus;
import org.eclipse.codewind.core.internal.constants.BuildStatus;
import org.eclipse.codewind.core.internal.constants.ProjectLanguage;
import org.eclipse.codewind.core.internal.constants.ProjectType;
import org.eclipse.codewind.ui.CodewindUIPlugin;
import org.eclipse.codewind.ui.internal.messages.Messages;
import org.eclipse.jface.preference.JFacePreferences;
import org.eclipse.jface.resource.FontDescriptor;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.StyledString.Styler;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.TextStyle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.navigator.IDescriptionProvider;

/**
 * Label provider for the Codewind view.
 */
public class CodewindNavigatorLabelProvider extends LabelProvider implements IStyledLabelProvider, IDescriptionProvider {

	static final Styler BOLD_FONT_STYLER = new BoldFontStyler();
	
	public static final Styler ERROR_STYLER = StyledString.createColorRegistryStyler(
			JFacePreferences.ERROR_COLOR, null);
	
	public static Styler LINK_STYLER = new Styler() {
		@Override
		public void applyStyles(TextStyle textStyle) {
			textStyle.underline = true;
		}
	};
	
	@Override
	public String getText(Object element) {
		if (element instanceof CodewindManager) {
			return Messages.CodewindLabel;
		} else if (element instanceof LocalConnection) {
			LocalConnection connection = (LocalConnection)element;
			String name = connection.getName();
			CodewindManager manager = CodewindManager.getManager();
			if (manager.getInstallerStatus() != null) {
				switch(manager.getInstallerStatus()) {
				case INSTALLING:
					return name + "[" + Messages.CodewindInstallingQualifier + "]";
				case UNINSTALLING:
					return name + "[" + Messages.CodewindUninstallingQualifier + "]";
				case STARTING:
					return name + "[" + Messages.CodewindStartingQualifier + "]";
				case STOPPING:
					return name + "[" + Messages.CodewindStoppingQualifier + "]";
				}
			} else {
				InstallStatus status = manager.getInstallStatus();
				if (status.isStarted()) {
					String text = name + " [" + Messages.CodewindRunningQualifier + "]";
					if (connection.getApps().size() == 0) {
						text = text + " (" + Messages.CodewindConnectionNoProjects + ")";
					}
					return text;
				} else if (status.isInstalled()) {
					if (status.hasStartedVersions()) {
						// An older version is running
						return name + "[" + NLS.bind(Messages.CodewindWrongVersionQualifier, status.getStartedVersions()) + "] (" +
								NLS.bind(Messages.CodewindWrongVersionMsg, InstallUtil.getVersion());
					}
					return name + " [" + Messages.CodewindNotStartedQualifier + "] (" + Messages.CodewindNotStartedMsg + ")";
				} else if (status.hasInstalledVersions()) {
					// An older version is installed
					return name + "[" + NLS.bind(Messages.CodewindWrongVersionQualifier, status.getInstalledVersions()) + "] (" +
							NLS.bind(Messages.CodewindWrongVersionMsg, InstallUtil.getVersion());
				} else if (status.isUnknown()) {
					return name + " [" + Messages.CodewindErrorQualifier + "] (" + Messages.CodewindErrorMsg + ")";
				} else {
					return name + " [" + Messages.CodewindErrorQualifier + "] (" + Messages.CodewindErrorMsg + ")";
				}
			}
		} else if (element instanceof RemoteConnection) {
			RemoteConnection connection = (RemoteConnection) element;
			String text = connection.getName();
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
				AppStatus appStatus = app.getAppStatus();
				String displayString = appStatus.getDisplayString(app.getStartMode());
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
		if (element instanceof CodewindManager) {
			return new StyledString(Messages.CodewindLabel);
		} else if (element instanceof LocalConnection) {
			LocalConnection connection = (LocalConnection)element;
			CodewindManager manager = CodewindManager.getManager();
			styledString = new StyledString(connection.getName());
			if (manager.getInstallerStatus() != null) {
				switch(manager.getInstallerStatus()) {
				case INSTALLING:
					styledString.append(" [" + Messages.CodewindInstallingQualifier + "]", StyledString.DECORATIONS_STYLER);
					break;
				case UNINSTALLING:
					styledString.append(" [" + Messages.CodewindUninstallingQualifier + "]", StyledString.DECORATIONS_STYLER);
					break;
				case STARTING:
					styledString.append(" [" + Messages.CodewindStartingQualifier + "]", StyledString.DECORATIONS_STYLER);
					break;
				case STOPPING:
					styledString.append(" [" + Messages.CodewindStoppingQualifier + "]", StyledString.DECORATIONS_STYLER);
					break;
				}
			} else {
				InstallStatus status = manager.getInstallStatus();
				if (status.isStarted()) {
					styledString.append(" [" + Messages.CodewindRunningQualifier + "]", StyledString.DECORATIONS_STYLER);
					if (connection.getApps().size() == 0) {
						styledString.append(" (" + Messages.CodewindConnectionNoProjects + ")", StyledString.DECORATIONS_STYLER);
					}
				} else if (status.isInstalled()) {
					if (status.hasStartedVersions()) {
						// An older version is running
						styledString.append(" [" + NLS.bind(Messages.CodewindWrongVersionQualifier, status.getStartedVersions()) + "]", StyledString.DECORATIONS_STYLER);
						styledString.append(" (" + NLS.bind(Messages.CodewindWrongVersionMsg, InstallUtil.getVersion()) + ")", StyledString.QUALIFIER_STYLER);
					} else {
						styledString.append(" [" + Messages.CodewindNotStartedQualifier + "]", StyledString.DECORATIONS_STYLER);
						styledString.append(" (" + Messages.CodewindNotStartedMsg + ")", StyledString.QUALIFIER_STYLER);
					}
				} else if (status.hasInstalledVersions()) {
					// An older version is installed
					styledString.append(" [" + NLS.bind(Messages.CodewindWrongVersionQualifier, status.getInstalledVersions()) + "]", StyledString.DECORATIONS_STYLER);
					styledString.append(" (" + NLS.bind(Messages.CodewindWrongVersionMsg, InstallUtil.getVersion()) + ")", StyledString.QUALIFIER_STYLER);
				} else if (status.isUnknown()) {
					styledString.append(" [" + Messages.CodewindErrorQualifier + "]", StyledString.DECORATIONS_STYLER);
					styledString.append(" (" + Messages.CodewindErrorMsg + ")", ERROR_STYLER);
				} else {
					styledString.append(" [" + Messages.CodewindNotInstalledQualifier + "]", StyledString.DECORATIONS_STYLER);
					styledString.append(" (" + Messages.CodewindNotInstalledMsg + ")", StyledString.QUALIFIER_STYLER);
				}
			}
		} else if (element instanceof RemoteConnection) {
			RemoteConnection connection = (RemoteConnection) element;
			styledString = new StyledString(connection.getName());
			if (connection.isConnected()) {
				styledString.append(" [" + Messages.CodewindConnected + "]", StyledString.DECORATIONS_STYLER);
				if (connection.getApps().size() == 0) {
					styledString.append(" (" + Messages.CodewindConnectionNoProjects + ")", StyledString.DECORATIONS_STYLER);
				}
			} else {
				styledString.append(" [" + Messages.CodewindDisconnected + "]", StyledString.DECORATIONS_STYLER);
				String errorMsg = connection.getConnectionErrorMsg();
				if (errorMsg == null) {
					styledString.append(" (" + Messages.CodewindDisconnectedDetails + ")", StyledString.QUALIFIER_STYLER);
				} else {
					styledString.append(" (" + errorMsg + ")", ERROR_STYLER);
				}
			}
		} else if (element instanceof CodewindApplication) {
			CodewindApplication app = (CodewindApplication)element;
			styledString = new StyledString(app.name);
			
			if (app.isEnabled()) {
				AppStatus appStatus = app.getAppStatus();
				String displayString = appStatus.getDisplayString(app.getStartMode());
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
		if (element instanceof CodewindManager) {
			return CodewindUIPlugin.getImage(CodewindUIPlugin.CODEWIND_ICON);
		} else if (element instanceof CodewindConnection) {
			return CodewindUIPlugin.getImage(CodewindUIPlugin.PROJECTS_ICON);
		} else if (element instanceof CodewindApplication) {
			ProjectType type = ((CodewindApplication)element).projectType;
			if (type == ProjectType.TYPE_LIBERTY) {
					return CodewindUIPlugin.getImage(CodewindUIPlugin.MICROPROFILE_ICON);
			} else if (type == ProjectType.TYPE_NODEJS) {
					return CodewindUIPlugin.getImage(CodewindUIPlugin.NODE_ICON);
			} else if (type == ProjectType.TYPE_SPRING) {
					return CodewindUIPlugin.getImage(CodewindUIPlugin.SPRING_ICON);
			} else if (type == ProjectType.TYPE_SWIFT) {
					return CodewindUIPlugin.getImage(CodewindUIPlugin.SWIFT_ICON);
			} else {
					ProjectLanguage lang = ((CodewindApplication)element).projectLanguage;
					switch (lang) {
						case LANGUAGE_GO:
							return CodewindUIPlugin.getImage(CodewindUIPlugin.GO_ICON);
						case LANGUAGE_JAVA:
							return CodewindUIPlugin.getImage(CodewindUIPlugin.JAVA_ICON);
						case LANGUAGE_NODEJS:
							return CodewindUIPlugin.getImage(CodewindUIPlugin.NODE_ICON);
						case LANGUAGE_PYTHON:
							return CodewindUIPlugin.getImage(CodewindUIPlugin.PYTHON_ICON);
						default:
							return CodewindUIPlugin.getImage(CodewindUIPlugin.CLOUD_ICON);
					}
			}
		}
		return null;
	}
	
    @Override
    public String getDescription(Object element) {
    	if (element instanceof RemoteConnection) {
    		RemoteConnection connection = (RemoteConnection) element;
    		if (connection.getBaseURI() != null) {
    			return connection.getBaseURI().toString();
    		}
    	} else if (element instanceof CodewindApplication) {
			CodewindApplication app = (CodewindApplication)element;
			if (app.getAppStatusDetails() != null) {
				return app.getAppStatusDetails();
			} else if (app.getRootUrl() != null && (app.getAppStatus() == AppStatus.STARTING || app.getAppStatus() == AppStatus.STARTED)) {
				return NLS.bind(Messages.CodewindDescriptionContextRoot, app.getRootUrl());
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
