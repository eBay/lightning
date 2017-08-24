/**
 * 
 */
package com.ebay.lightning.core.utils;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.IOUtils;

/**
 * Helper class to execute HTTP GET, HTTP POST calls.
 * 
 * @author shashukla
 *
 */
public class UrlUtils {

	private static final int TIMEOUT_10_SECS = 10000;

	public static enum ContentType {
		TEXT_PLAIN("text/plain"), XML("text/xml"), APPLICATION_JSON("application/json");
		private String contentType;

		private ContentType(String contentType) {
			this.contentType = contentType;
		}

		public String getContentType() {
			return contentType;
		}

		@Override
		public String toString() {
			return contentType;
		}
	}

	/**
	 * Get the HTTP content for the URL as string.
	 * @param url the URL
	 * @return the HTTP content as string
	 * @throws Exception when the URL fails
	 */
	public String get(String url) throws Exception {
		HttpURLConnection yc = connect(url);
		return IOUtils.toString(yc.getInputStream());
	}
	
	/**
	 * Initiate connection for the URL.
	 * @param url the URL
	 * @return the HTTP URL connection
	 * @throws MalformedURLException when the URL is in incorrect format
	 * @throws IOException when establishing connection fails
	 */
	private HttpURLConnection connect(String url) throws MalformedURLException, IOException {
		URL alertUrl = new URL(url);
		HttpURLConnection yc = (HttpURLConnection) alertUrl.openConnection();
		yc.setConnectTimeout(TIMEOUT_10_SECS);
		yc.setReadTimeout(TIMEOUT_10_SECS);
		return yc;
	}
	
	/**
	 * Get the HTTP content for the URL as byte array.
	 * @param url the URL
	 * @return the HTTP content as byte array
	 * @throws Exception when the URL fails
	 */
	public byte[] getByteArray(String url) throws Exception {
		HttpURLConnection yc = connect(url);
		return IOUtils.toByteArray(yc.getInputStream());
	}

	/**
	 * Get the HTTP content for a POST HTTP URL.
	 * @param targetURL the POST HTTP URL
	 * @param contentType content type of the URL
	 * @param headerParams headers for the call
	 * @param payload post payload informaton
	 * @return the HTTP content from the response
	 * @throws Exception when the URL fails
	 */
	public String post(String targetURL, ContentType contentType, Map<String, String> headerParams, String payload) throws Exception {
		HttpURLConnection connection = null;
		try {
			URL url = new URL(targetURL);
			connection = (HttpURLConnection) url.openConnection();
			connection.setConnectTimeout(TIMEOUT_10_SECS);
			connection.setReadTimeout(TIMEOUT_10_SECS);
			connection.setRequestMethod("POST");
			connection.setRequestProperty("Content-Type", contentType != null ? contentType.toString() : ContentType.TEXT_PLAIN.toString());
			connection.setRequestProperty("Content-Length", "" + Integer.toString(payload.getBytes().length));
			connection.setRequestProperty("Content-Language", "en-US");
			if (headerParams != null && headerParams.size() > 0) {
				for(Entry<String, String> headerParam : headerParams.entrySet()){
					connection.addRequestProperty(headerParam.getKey(), headerParam.getValue());
				}
			}

			connection.setUseCaches(false);
			connection.setDoInput(true);
			connection.setDoOutput(true);

			DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
			wr.writeBytes(payload);
			wr.flush();
			wr.close();

			InputStream is = connection.getInputStream();
			BufferedReader rd = new BufferedReader(new InputStreamReader(is));
			String line;
			StringBuffer response = new StringBuffer();
			while ((line = rd.readLine()) != null) {
				response.append(line);
				response.append('\r');
			}
			rd.close();
			return response.toString();

		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
	}

}
