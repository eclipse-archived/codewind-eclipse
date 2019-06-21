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

package org.eclipse.codewind.filewatchers.core.internal;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLConnection;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.eclipse.codewind.filewatchers.core.FWLogger;
import org.json.JSONObject;

/**
 * Static utilities to allow easy HTTP communication, and make diagnosing and
 * handling errors a bit easier.
 */
public class HttpUtil {

	private HttpUtil() {
	}

	private static String readAllFromStream(InputStream stream) {
		Scanner s = new Scanner(stream);
		// end-of-stream
		s.useDelimiter("\\A"); //$NON-NLS-1$
		String result = s.hasNext() ? s.next() : ""; //$NON-NLS-1$
		s.close();
		return result;
	}

	private static void logError(String str) {
		FWLogger.getInstance().logError(str);
//		System.err.println(str);
	}

	private static void log(String str) {
		if (FWLogger.getInstance().isDebug()) {
			System.out.println(str);
		}
	}

	/** Stores the result of an HTTP request */
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

			// Read error first because sometimes if there is an error,
			// connection.getInputStream() throws an exception
			InputStream eis = connection.getErrorStream();
			if (eis != null) {
				error = readAllFromStream(eis);
			} else {
				error = null;
			}

			if (!isGoodResponse) {
				logError("Received bad response code " + responseCode + " from " + connection.getURL() + " - Error:\n"
						+ error);
				response = null;
			} else {
				InputStream is = connection.getInputStream();
				if (is != null) {
					response = readAllFromStream(is);
				} else {
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

	public static HttpResult get(URI uri, IHttpConnectionConfig conf) throws IOException {
		HttpURLConnection connection = null;

		try {
			connection = (HttpURLConnection) uri.toURL().openConnection();

			connection.setRequestMethod("GET");

			if (conf != null) {
				conf.setupConnection(connection);
			}

			return new HttpResult(connection);
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
	}

	public static HttpResult put(URI uri, JSONObject payload, IHttpConnectionConfig conf) throws IOException {
		HttpURLConnection connection = null;

		log("PUT " + payload.toString() + " TO " + uri);
		try {
			connection = (HttpURLConnection) uri.toURL().openConnection();

			connection.setRequestMethod("PUT");

			if (conf != null) {
				conf.setupConnection(connection);
			}

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

	public static HttpResult post(URI uri, JSONObject payload, IHttpConnectionConfig conf) throws IOException {
		HttpURLConnection connection = null;

		log("POST " + payload.toString() + " TO " + uri);
		try {
			connection = (HttpURLConnection) uri.toURL().openConnection();

			connection.setRequestMethod("POST");

			if (conf != null) {
				conf.setupConnection(connection);
			}

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

	/** Allow calling methods to configure the connection before it used. */
	public static interface IHttpConnectionConfig {
		public void setupConnection(URLConnection conn);
	}

	public static void allowAllCerts(URLConnection connection) {
		if (connection instanceof HttpsURLConnection) {
			HttpsURLConnection huc = (HttpsURLConnection) connection;

			// Ignore invalid certificates since we're using internal sites
			X509TrustManager tm = new X509TrustManager() {
				public void checkClientTrusted(X509Certificate[] xcs, String str) throws CertificateException {
					// Do nothing
				}

				public void checkServerTrusted(X509Certificate[] xcs, String str) throws CertificateException {
					// Do nothing
				}

				public X509Certificate[] getAcceptedIssuers() {
					return null;
				}
			};

			// SSL setup
			SSLContext ctx;
			try {
				ctx = SSLContext.getInstance("TLS");
				ctx.init(null, new TrustManager[] { tm }, new java.security.SecureRandom());
				huc.setSSLSocketFactory(ctx.getSocketFactory());

			} catch (NoSuchAlgorithmException e) {
				throw new RuntimeException(e);
			} catch (KeyManagementException e) {
				throw new RuntimeException(e);
			}

		}

	}
}
