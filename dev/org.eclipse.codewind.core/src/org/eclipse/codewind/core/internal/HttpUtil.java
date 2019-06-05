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

package org.eclipse.codewind.core.internal;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;

/**
 * Static utilities to allow easy HTTP communication, and make diagnosing and handling errors a bit easier.
 */
public class HttpUtil {

	private HttpUtil() {}

	public static class HttpResult {
		public final int responseCode;
		public final boolean isGoodResponse;

		// Can be null
		public final String response;
		// Can be null
		public final String error;
		
		private final Map<String, List<String>> headerFields;

		public HttpResult(HttpURLConnection connection) throws IOException {
			responseCode = connection.getResponseCode();
			isGoodResponse = responseCode > 199 && responseCode < 300;
			
			headerFields = isGoodResponse ? connection.getHeaderFields() : null;

			// Read error first because sometimes if there is an error, connection.getInputStream() throws an exception
			InputStream eis = connection.getErrorStream();
			if (eis != null) {
				error = CoreUtil.readAllFromStream(eis);
			}
			else {
				error = null;
			}

			if (!isGoodResponse) {
				Logger.logError("Received bad response code " + responseCode + " from "
						+ connection.getURL() + " - Error:\n" + error);
				response = null;
			} else {
				InputStream is = connection.getInputStream();
				if (is != null) {
					response = CoreUtil.readAllFromStream(is);
				}
				else {
					response = null;
				}
			}
		}
		
		public String getHeader(String key) {
			if (headerFields == null) {
				return null;
			}
			List<String> list = headerFields.get(key);
			if (list == null || list.isEmpty()) {
				return null;
			}
			return list.get(0);
		}
	}

	public static HttpResult get(URI uri) throws IOException {
		HttpURLConnection connection = null;

		try {
			connection = (HttpURLConnection) uri.toURL().openConnection();

			connection.setRequestMethod("GET");
			connection.setReadTimeout(10000);

			return new HttpResult(connection);
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
	}
	
	public static HttpResult post(URI uri, JSONObject payload) throws IOException {
		HttpURLConnection connection = null;

		Logger.log("POST " + payload.toString() + " TO " + uri);
		try {
			connection = (HttpURLConnection) uri.toURL().openConnection();

			connection.setRequestMethod("POST");
			connection.setReadTimeout(10000);
			
			if (payload != null) {
				connection.setRequestProperty("Content-Type", "application/json");
				connection.setDoOutput(true);
	
				DataOutputStream payloadStream = new DataOutputStream(connection.getOutputStream());
				payloadStream.write(payload.toString().getBytes());
			}

			return new HttpResult(connection);
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
	}
	
	public static HttpResult post(URI uri) throws IOException {
		HttpURLConnection connection = null;

		Logger.log("Empty POST TO " + uri);
		try {
			connection = (HttpURLConnection) uri.toURL().openConnection();
			connection.setRequestMethod("POST");
			connection.setReadTimeout(10000);
			return new HttpResult(connection);
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
	}
	
	public static HttpResult put(URI uri) throws IOException {
		HttpURLConnection connection = null;

		Logger.log("PUT " + uri);
		try {
			connection = (HttpURLConnection) uri.toURL().openConnection();

			connection.setRequestMethod("PUT");
			connection.setReadTimeout(10000);

			return new HttpResult(connection);
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
	}
	
	public static HttpResult head(URI uri) throws IOException {
		HttpURLConnection connection = null;

		Logger.log("HEAD " + uri);
		try {
			connection = (HttpURLConnection) uri.toURL().openConnection();

			connection.setRequestMethod("HEAD");
			connection.setReadTimeout(10000);

			return new HttpResult(connection);
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
	}
	
	public static HttpResult delete(URI uri) throws IOException {
		HttpURLConnection connection = null;

		Logger.log("DELETE " + uri);
		try {
			connection = (HttpURLConnection) uri.toURL().openConnection();

			connection.setRequestMethod("DELETE");
			connection.setReadTimeout(10000);

			return new HttpResult(connection);
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
	}
}
