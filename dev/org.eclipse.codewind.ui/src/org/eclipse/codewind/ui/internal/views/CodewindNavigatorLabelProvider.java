/*******************************************************************************
 * Copyright (c) 2018, 2020 IBM Corporation and others.
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
import org.eclipse.codewind.core.internal.CoreUtil;
import org.eclipse.codewind.core.internal.cli.InstallStatus;
import org.eclipse.codewind.core.internal.cli.InstallUtil;
import org.eclipse.codewind.core.internal.connection.LocalConnection;
import org.eclipse.codewind.core.internal.connection.RemoteConnection;
import org.eclipse.codewind.core.internal.constants.AppStatus;
import org.eclipse.codewind.core.internal.constants.BuildStatus;
import org.eclipse.codewind.core.internal.constants.DetailedAppStatus;
import org.eclipse.codewind.core.internal.constants.DetailedAppStatus.Severity;
import org.eclipse.codewind.core.internal.constants.ProjectLanguage;
import org.eclipse.codewind.core.internal.constants.ProjectType;
import org.eclipse.codewind.ui.CodewindUIPlugin;
import org.eclipse.codewind.ui.internal.messages.Messages;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.preference.JFacePreferences;
import org.eclipse.jface.resource.FontDescriptor;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.IToolTipProvider;
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
public class CodewindNavigatorLabelProvider extends LabelProvider implements IStyledLabelProvider, IDescriptionProvider, IToolTipProvider {

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
					if (connection.isConnected()) {
						String text = name + " [" + Messages.CodewindRunningQualifier + "]";
						if (connection.getApps().size() == 0) {
							text = text + " (" + Messages.CodewindConnectionNoProjects + ")";
						}
						return text;
					}
					return name + " [" + Messages.CodewindErrorQualifier + "] (" + Messages.CodewindErrorMsg + ")";
				} else if (status.isInstalled()) {
					if (status.hasStartedVersions()) {
						// An older version is running
						return name + "[" + NLS.bind(Messages.CodewindWrongVersionQualifier, status.getStartedVersionsString()) + "] (" +
								NLS.bind(Messages.CodewindWrongVersionMsg, InstallUtil.getVersion());
					}
					return name + " [" + Messages.CodewindNotStartedQualifier + "] (" + Messages.CodewindNotStartedMsg + ")";
				} else if (status.hasInstalledVersions()) {
					// An older version is installed
					return name + "[" + NLS.bind(Messages.CodewindWrongVersionQualifier, status.getInstalledVersionsString()) + "] (" +
							NLS.bind(Messages.CodewindWrongVersionMsg, InstallUtil.getVersion());
				} else if (status.isUnknown()) {
					String errorMsg = manager.getInstallerErrorMsg() != null ? Messages.CodewindErrorMsgWithDetails : Messages.CodewindErrorMsg;
					return name + " [" + Messages.CodewindErrorQualifier + "] (" + errorMsg + ")";
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
			IProject project = CoreUtil.getEclipseProject(app);
			if (project != null && !project.getName().equals(app.name)) {
				builder.append("(" + project.getName() + ")");
			}
			
			if (app.isEnabled()) {
				AppStatus appStatus = app.getAppStatus();
				BuildStatus buildStatus = app.getBuildStatus();
				if (appStatus == AppStatus.UNKNOWN && buildStatus == BuildStatus.UNKOWN) {
					builder.append(" [" + Messages.CodewindProjectNoStatus + "]");
				} else {
					if (appStatus != AppStatus.UNKNOWN) {
						DetailedAppStatus details = app.getAppStatusDetails();
						if (details != null && details.getMessage() != null) {
						    builder.append(" [" + appStatus.getDisplayString(app.getStartMode()) + ": ");
							if (details.getSeverity() != null) {
								builder.append("(" + details.getSeverity().displayString + ") ");
							}
							builder.append(Messages.CodewindHoverForDetails);
							builder.append("]");
						} else {
							builder.append(" [" + appStatus.getDisplayString(app.getStartMode()) + "]");
						}
					}
					
					if (buildStatus != BuildStatus.UNKOWN) {
						String buildDetails = app.getBuildDetails();
						if (buildDetails != null && !buildDetails.isEmpty()) {
							builder.append(" [" + buildStatus.getDisplayString() + ": " + buildDetails + "]");
						} else {
							builder.append(" [" + buildStatus.getDisplayString() + "]");
						}
					}
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
					if (connection.isConnected()) {
						styledString.append(" [" + Messages.CodewindRunningQualifier + "]", StyledString.DECORATIONS_STYLER);
						if (connection.getApps().size() == 0) {
							styledString.append(" (" + Messages.CodewindConnectionNoProjects + ")", StyledString.DECORATIONS_STYLER);
						}
					} else {
						styledString.append(" [" + Messages.CodewindErrorQualifier + "]", StyledString.DECORATIONS_STYLER);
						styledString.append(" [" + Messages.CodewindErrorMsg + "]", ERROR_STYLER);
					}
				} else if (status.isInstalled()) {
					if (status.hasStartedVersions()) {
						// An older version is running
						styledString.append(" [" + NLS.bind(Messages.CodewindWrongVersionQualifier, status.getStartedVersionsString()) + "]", StyledString.DECORATIONS_STYLER);
						styledString.append(" (" + NLS.bind(Messages.CodewindWrongVersionMsg, InstallUtil.getVersion()) + ")", StyledString.QUALIFIER_STYLER);
					} else {
						styledString.append(" [" + Messages.CodewindNotStartedQualifier + "]", StyledString.DECORATIONS_STYLER);
						styledString.append(" (" + Messages.CodewindNotStartedMsg + ")", StyledString.QUALIFIER_STYLER);
					}
				} else if (status.hasInstalledVersions()) {
					// An older version is installed
					styledString.append(" [" + NLS.bind(Messages.CodewindWrongVersionQualifier, status.getInstalledVersionsString()) + "]", StyledString.DECORATIONS_STYLER);
					styledString.append(" (" + NLS.bind(Messages.CodewindWrongVersionMsg, InstallUtil.getVersion()) + ")", StyledString.QUALIFIER_STYLER);
				} else if (status.isUnknown()) {
					styledString.append(" [" + Messages.CodewindErrorQualifier + "]", StyledString.DECORATIONS_STYLER);
					String errorMsg = manager.getInstallerErrorMsg() != null ? Messages.CodewindErrorMsgWithDetails : Messages.CodewindErrorMsg;
					styledString.append(" (" + errorMsg + ")", ERROR_STYLER);
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
			IProject project = CoreUtil.getEclipseProject(app);
			if (project != null && !project.getName().equals(app.name)) {
				styledString.append("(" + project.getName() + ")");
			}
			
			if (app.isEnabled()) {
				AppStatus appStatus = app.getAppStatus();
				BuildStatus buildStatus = app.getBuildStatus();
				if (appStatus == AppStatus.UNKNOWN && buildStatus == BuildStatus.UNKOWN) {
					styledString.append(" [" + Messages.CodewindProjectNoStatus + "]", StyledString.DECORATIONS_STYLER);
				} else {
					if (appStatus != AppStatus.UNKNOWN) {
						DetailedAppStatus details = app.getAppStatusDetails();
						if (details != null && details.getMessage() != null) {
							styledString.append(" [" + appStatus.getDisplayString(app.getStartMode()) + ": ", StyledString.DECORATIONS_STYLER);
							Styler styler = details.getSeverity() != null && details.getSeverity() == Severity.ERROR ? ERROR_STYLER : StyledString.QUALIFIER_STYLER;
							if (details.getSeverity() != null) {
								styledString.append("(" + details.getSeverity().displayString + ") ", styler);
							}
							styledString.append(Messages.CodewindHoverForDetails, styler);
							styledString.append("]", StyledString.DECORATIONS_STYLER);
						} else {
							styledString.append(" [" + appStatus.getDisplayString(app.getStartMode()) + "]", StyledString.DECORATIONS_STYLER);
						}
						
					}
					
					if (buildStatus != BuildStatus.UNKOWN) {
						String buildDetails = app.getBuildDetails();
						if (buildDetails != null) {
							styledString.append(" [" + buildStatus.getDisplayString() + ": ", StyledString.DECORATIONS_STYLER);
							styledString.append(buildDetails, StyledString.QUALIFIER_STYLER);
							styledString.append("]", StyledString.DECORATIONS_STYLER);
						} else {
							styledString.append(" [" + buildStatus.getDisplayString() + "]", StyledString.DECORATIONS_STYLER);
						}
					}
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
		return getCodewindImage(element);
	}

	public static Image getCodewindImage(Object element) {
		if (element instanceof CodewindManager) {
			return CodewindUIPlugin.getImage(CodewindUIPlugin.CODEWIND_ICON);
		} else if (element instanceof LocalConnection) {
			return CodewindUIPlugin.getImage(((LocalConnection)element).isConnected() ? CodewindUIPlugin.LOCAL_ACTIVE_ICON : CodewindUIPlugin.LOCAL_INACTIVE_ICON);
		} else if (element instanceof RemoteConnection) {
			return CodewindUIPlugin.getImage(((RemoteConnection)element).isConnected() ? CodewindUIPlugin.REMOTE_CONNECTED_ICON : CodewindUIPlugin.REMOTE_DISCONNECTED_ICON);
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
				if (lang.isGo()) {
					return CodewindUIPlugin.getImage(CodewindUIPlugin.GO_ICON);
				} else if (lang.isJava()) {
					return CodewindUIPlugin.getImage(CodewindUIPlugin.JAVA_ICON);
				} else if (lang.isJavaScript()) {
					return CodewindUIPlugin.getImage(CodewindUIPlugin.NODE_ICON);
				} else if (lang.isPython()) {
					return CodewindUIPlugin.getImage(CodewindUIPlugin.PYTHON_ICON);
				} else if (lang.isSwift()) {
					return CodewindUIPlugin.getImage(CodewindUIPlugin.SWIFT_ICON);
				}
				return CodewindUIPlugin.getImage(CodewindUIPlugin.GENERIC_PROJECT_ICON);
			}
		}
		return null;
	}
	
    @Override
    public String getDescription(Object element) {
    	if (element instanceof LocalConnection) {
    		if (CodewindManager.getManager().getInstallStatus() == InstallStatus.UNKNOWN &&
    				CodewindManager.getManager().getInstallerErrorMsg() != null) {
    			return CodewindManager.getManager().getInstallerErrorMsg();
    		}
    	} else if (element instanceof RemoteConnection) { 
    		RemoteConnection connection = (RemoteConnection) element;
    		if (connection.getBaseURI() != null) {
    			return connection.getBaseURI().toString();
    		}
    	} else if (element instanceof CodewindApplication) {
			CodewindApplication app = (CodewindApplication)element;
			if (app.getAppStatusDetails() != null && app.getAppStatusDetails().getMessage() != null) {
				return app.getAppStatusDetails().getMessage();
			} else if (app.getRootUrl() != null && (app.getAppStatus() == AppStatus.STARTING || app.getAppStatus() == AppStatus.STARTED)) {
				return NLS.bind(Messages.CodewindDescriptionContextRoot, app.getRootUrl());
			}
    	}
    	return null;
    }

	@Override
	public String getToolTipText(Object element) {
		if (element instanceof CodewindApplication) {
			DetailedAppStatus details = ((CodewindApplication)element).getAppStatusDetails();
			if (details != null && details.getMessage() != null) {
				return (details.getSeverity() == null ? "" : details.getSeverity().displayString + ": ") + details.getMessage();
			}
		}
		return getDescription(element);
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
