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
	
	private static final int DEFAULT_CONNECT_TIMEOUT_MS = 10000;
	private static final int DEFAULT_READ_TIMEOUT_MS = 10000;
	
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
		return sendRequest("GET", uri, null);
	}

	public static HttpResult get(URI uri, int connectTimeoutMS, int readTimeoutMS) throws IOException {
		return sendRequest("GET", uri, null, connectTimeoutMS, readTimeoutMS);
	}
	
	public static HttpResult post(URI uri, JSONObject payload) throws IOException {
		return sendRequest("POST", uri, payload, DEFAULT_CONNECT_TIMEOUT_MS, DEFAULT_READ_TIMEOUT_MS);
	}

	public static HttpResult post(URI uri, JSONObject payload, int readTimeoutSeconds) throws IOException {
		return sendRequest("POST", uri, payload, DEFAULT_CONNECT_TIMEOUT_MS, readTimeoutSeconds * 1000);
	}
	
	public static HttpResult post(URI uri) throws IOException {
		return sendRequest("POST", uri, null);
	}

	public static HttpResult put(URI uri) throws IOException {
		return sendRequest("PUT", uri, null);
	}
	
	public static HttpResult put(URI uri, JSONObject payload) throws IOException {
		return sendRequest("PUT", uri, payload, DEFAULT_CONNECT_TIMEOUT_MS, DEFAULT_READ_TIMEOUT_MS);
	}
	
	public static HttpResult put(URI uri, JSONObject payload, int readTimoutSeconds) throws IOException {
		return sendRequest("PUT", uri, payload, DEFAULT_CONNECT_TIMEOUT_MS, DEFAULT_READ_TIMEOUT_MS);
	}

	public static HttpResult head(URI uri) throws IOException {
		return sendRequest("HEAD", uri, null);
	}
	
	public static HttpResult delete(URI uri) throws IOException {
		return delete(uri, null);
	}
	
	public static HttpResult delete(URI uri, JSONObject payload) throws IOException {
		return sendRequest("DELETE", uri, payload);
	}
	
	public static HttpResult sendRequest(String method, URI uri, JSONObject payload) throws IOException {
		return sendRequest(method, uri, payload, DEFAULT_CONNECT_TIMEOUT_MS, DEFAULT_READ_TIMEOUT_MS);
	}
	
	public static HttpResult sendRequest(String method, URI uri, JSONObject payload, int connectTimeoutMS, int readTimeoutMS) throws IOException {
		HttpURLConnection connection = null;
		if (payload != null) {
			Logger.log("Making a " + method + " request on " + uri + " with payload: " + payload.toString());
		} else {
			Logger.log("Making a " + method + " request on " + uri);
		}

		try {
			connection = (HttpURLConnection) uri.toURL().openConnection();

			connection.setRequestMethod(method);
			connection.setConnectTimeout(connectTimeoutMS);
			connection.setReadTimeout(readTimeoutMS);
			
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
