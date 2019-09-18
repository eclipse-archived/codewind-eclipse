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

import org.json.JSONArray;
import org.json.JSONObject;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Static utilities to allow easy HTTP communication, and make diagnosing and handling errors a bit easier.
 */
public class HttpUtil {
	
	private static final int DEFAULT_READ_TIMEOUT_S = 10;
	private static final int DEFAULT_READ_TIMEOUT_MS = DEFAULT_READ_TIMEOUT_S * 1000;
	
	public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
	
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
		
		// HttpResult for OkHttp (used for PATCH)
		public HttpResult(URI uri, Response httpResponse) throws IOException {
			responseCode = httpResponse.code();
			isGoodResponse = responseCode > 199 && responseCode < 300;
			
			headerFields = null;

			InputStream stream = httpResponse.body().byteStream();
			String content = null;
			if (stream != null) {
				content = CoreUtil.readAllFromStream(stream);
			}
			if (isGoodResponse) {
				response = content;
				error = null;
			} else {
				error = content;
				response = null;
			}

			if (!isGoodResponse) {
				Logger.logError("Received bad response code " + responseCode + " from "
						+ uri + " - Error:\n" + content);
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
			connection.setReadTimeout(DEFAULT_READ_TIMEOUT_MS);

			return new HttpResult(connection);
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
	}
	
	/**
	 * Post to the given URI passing along the payload.  The default value is used
	 * for the read timeout.
	 */
	public static HttpResult post(URI uri, JSONObject payload) throws IOException {
		return post(uri, payload, DEFAULT_READ_TIMEOUT_S);
	}

	public static HttpResult post(URI uri, JSONObject payload, int readTimoutSeconds) throws IOException {
		return sendPayload("POST", uri, payload, DEFAULT_READ_TIMEOUT_S);
	}
	
	/**
	 * POST/PUT to the given URI passing along the payload.  The readTimeout is in seconds.
	 */
	public static HttpResult sendPayload(String method, URI uri, JSONObject payload, int readTimoutSeconds) throws IOException {
		HttpURLConnection connection = null;

		Logger.log(method + " " + payload.toString() + " TO " + uri);
		try {
			connection = (HttpURLConnection) uri.toURL().openConnection();

			connection.setRequestMethod(method);
			connection.setReadTimeout(readTimoutSeconds * 1000);
			
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
			connection.setReadTimeout(DEFAULT_READ_TIMEOUT_MS);
			return new HttpResult(connection);
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
	}

	/**
	 * Put to the given URI passing along the payload.  The default value is used
	 * for the read timeout.
	 */
	public static HttpResult put(URI uri, JSONObject payload) throws IOException {
		return put(uri, payload, DEFAULT_READ_TIMEOUT_S);
	}

	/**
	 * Put to the given URI passing along the payload.  The readTimeout is in seconds.
	 */
	public static HttpResult put(URI uri, JSONObject payload, int readTimoutSeconds) throws IOException {
		return sendPayload("PUT", uri, payload, DEFAULT_READ_TIMEOUT_S);
	}
	
	public static HttpResult put(URI uri) throws IOException {
		HttpURLConnection connection = null;

		Logger.log("Empty PUT TO " + uri);
		try {
			connection = (HttpURLConnection) uri.toURL().openConnection();

			connection.setRequestMethod("PUT");
			connection.setReadTimeout(DEFAULT_READ_TIMEOUT_MS);

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
			connection.setReadTimeout(DEFAULT_READ_TIMEOUT_MS);

			return new HttpResult(connection);
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
	}
	
	public static HttpResult delete(URI uri) throws IOException {
		return delete(uri, null);
	}
	
	public static HttpResult delete(URI uri, JSONObject payload) throws IOException {
		HttpURLConnection connection = null;

		Logger.log("DELETE " + uri);
		try {
			connection = (HttpURLConnection) uri.toURL().openConnection();

			connection.setRequestMethod("DELETE");
			connection.setReadTimeout(DEFAULT_READ_TIMEOUT_MS);
			
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
	
	public static HttpResult patch(URI uri, JSONArray payload) throws IOException {
		Logger.log("PATCH " + uri);
		
		// No PATCH for HttpURLConnection so use OkHttp
		RequestBody body = RequestBody.create(JSON, payload.toString());
		OkHttpClient client = new OkHttpClient();
		Request request = new Request.Builder().url(uri.toURL()).patch(body).build();
		Response response = client.newCall(request).execute();
		return new HttpResult(uri, response);
	}

}
