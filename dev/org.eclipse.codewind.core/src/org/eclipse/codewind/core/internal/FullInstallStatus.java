package org.eclipse.codewind.core.internal;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class FullInstallStatus {
	
//	public static final String KEY_STATUS = "status";
	public static final String KEY_INSTALLED_VERSIONS = "installed-versions";
	public static final String KEY_URL = "url";
	public static final String KEY_STARTED_VERSIONS = "started";
	
	public final List<String> installedVersions;
	public final List<String> startedVersions;			
	public final URI url;					// null if codewind is not started (ie, startedVersions is empty)
	
	public final InstallUtil.InstallStatus simpleStatus;
		
	public FullInstallStatus(JSONObject rawStatus) throws URISyntaxException {
		List<String> installedVersions = new ArrayList<String>(0);
		try {
			JSONArray installedVersionsJson = rawStatus.getJSONArray(KEY_INSTALLED_VERSIONS);
			installedVersions = CoreUtil.jsonArrayToStringArray(installedVersionsJson);
		} catch (JSONException e) { }

		this.installedVersions = installedVersions;

		List<String> startedVersions = new ArrayList<String>(0);
		try {
			JSONArray startedVersionsJson = rawStatus.getJSONArray(KEY_STARTED_VERSIONS);
			startedVersions = CoreUtil.jsonArrayToStringArray(startedVersionsJson);
		} catch (JSONException e) { }

		this.startedVersions = startedVersions;
		
		String url = null;
		try {
			url = rawStatus.getString("url");
			if (!url.endsWith("/")) {
				url = url + "/";
			}
		} catch (JSONException e) { }	
		
		if (url != null) {
			this.url = new URI(url);
		}
		else {
			this.url = null;
		}
		
		this.simpleStatus = getSimpleStatus();
		
		Logger.log(String.format(
			"InstallStatus: Installed: %s Started: %s URL: %s", 
			this.installedVersions, this.startedVersions, this.url)
		);
	}
	
	private InstallUtil.InstallStatus getSimpleStatus() {
		if (this.url != null) {
			return InstallUtil.InstallStatus.RUNNING;
		}
		else if (!this.installedVersions.isEmpty()) {
			return InstallUtil.InstallStatus.STOPPED;
		}
		else {
			return InstallUtil.InstallStatus.UNINSTALLED;
		}
	}
	
	public boolean isRunning() {
		return this.simpleStatus ==  InstallUtil.InstallStatus.RUNNING;
	}
		
	public boolean isCorrectVersionInstalled() {
		return this.installedVersions.stream()
				.anyMatch((String version) -> version.equals(InstallUtil.getRequiredVersion()));
	}
}
