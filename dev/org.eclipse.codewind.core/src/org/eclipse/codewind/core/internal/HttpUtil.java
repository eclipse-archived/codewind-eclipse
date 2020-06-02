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

package org.eclipse.codewind.core.internal;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.security.SecureRandom;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

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
	
	public static final X509TrustManager trustManager;
	public static final SSLContext sslContext;
	public static final HostnameVerifier hostnameVerifier;
	
	static {
		trustManager = getTrustAllCertsManager();
		sslContext = getTrustAllCertsContext(trustManager);
		hostnameVerifier = getHostnameVerifier();
	}
	
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
		return get(uri, null);
	}
	
	public static HttpResult get(URI uri, IAuthInfo auth) throws IOException {
		return sendRequest("GET", uri, auth, null);
	}
	
	public static HttpResult get(URI uri, IAuthInfo auth, int connectTimeoutMS, int readTimeoutMS) throws IOException {
		return sendRequest("GET", uri, auth, null, connectTimeoutMS, readTimeoutMS);
	}
	
	public static HttpResult post(URI uri, IAuthInfo auth, JSONObject payload) throws IOException {
		return sendRequest("POST", uri, auth, payload, DEFAULT_CONNECT_TIMEOUT_MS, DEFAULT_READ_TIMEOUT_MS);
	}

	public static HttpResult post(URI uri, IAuthInfo auth, JSONObject payload, int readTimeoutSeconds) throws IOException {
		return sendRequest("POST", uri, auth, payload, DEFAULT_CONNECT_TIMEOUT_MS, readTimeoutSeconds * 1000);
	}
	
	public static HttpResult post(URI uri, IAuthInfo auth) throws IOException {
		return sendRequest("POST", uri, auth, null);
	}

	public static HttpResult put(URI uri, IAuthInfo auth) throws IOException {
		return sendRequest("PUT", uri, auth, null);
	}
	
	public static HttpResult put(URI uri, IAuthInfo auth, JSONObject payload) throws IOException {
		return sendRequest("PUT", uri, auth, payload, DEFAULT_CONNECT_TIMEOUT_MS, DEFAULT_READ_TIMEOUT_MS);
	}
	
	public static HttpResult put(URI uri, IAuthInfo auth, JSONObject payload, int readTimoutSeconds) throws IOException {
		return sendRequest("PUT", uri, auth, payload, DEFAULT_CONNECT_TIMEOUT_MS, DEFAULT_READ_TIMEOUT_MS);
	}

	public static HttpResult head(URI uri, IAuthInfo auth) throws IOException {
		return sendRequest("HEAD", uri, auth, null);
	}
	
	public static HttpResult delete(URI uri, IAuthInfo auth) throws IOException {
		return delete(uri, auth, null);
	}
	
	public static HttpResult delete(URI uri, IAuthInfo auth, JSONObject payload) throws IOException {
		return sendRequest("DELETE", uri, auth, payload);
	}

	public static HttpResult sendRequest(String method, URI uri, IAuthInfo auth, JSONObject payload) throws IOException {
		return sendRequest(method, uri, auth, payload, DEFAULT_CONNECT_TIMEOUT_MS, DEFAULT_READ_TIMEOUT_MS);
	}

	public static HttpResult sendRequest(String method, URI uri, IAuthInfo auth, JSONObject payload, int connectTimeoutMS, int readTimeoutMS) throws IOException {
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
			addAuthorization(connection, auth);
			
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
	
	private static void addAuthorization(HttpURLConnection connection, IAuthInfo auth) {
		if (sslContext == null || auth == null || !auth.isValid() || !(connection instanceof HttpsURLConnection)) {
			return;
		}
		connection.setRequestProperty("Authorization", auth.getHttpAuthorization());
		((HttpsURLConnection)connection).setSSLSocketFactory(sslContext.getSocketFactory());
		((HttpsURLConnection)connection).setHostnameVerifier(hostnameVerifier);
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
	
	private static X509TrustManager getTrustAllCertsManager() {
		return new X509TrustManager() {
			@Override
			public java.security.cert.X509Certificate[] getAcceptedIssuers() {
				return new java.security.cert.X509Certificate[0];
			}
			@Override
			public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) throws java.security.cert.CertificateException {
				// TODO Auto-generated method stub
			}
			@Override
			public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) throws java.security.cert.CertificateException {
				// TODO Auto-generated method stub
			}
		};
	}

	private static SSLContext getTrustAllCertsContext(X509TrustManager manager) {
		try {
			SSLContext context = SSLContext.getInstance("TLSv1.2");
			context.init(new KeyManager[0], new TrustManager[] { manager }, new SecureRandom());
			return context;
		} catch (Exception e) {
			Logger.logError("An error occurred creating a trust all certs context", e);
		}
		return null;
	}
	
	private static HostnameVerifier getHostnameVerifier() {
		return new HostnameVerifier() {
			@Override
			public boolean verify(String hostname, javax.net.ssl.SSLSession sslSession) {
				return true;
			}
		};
	}

}
