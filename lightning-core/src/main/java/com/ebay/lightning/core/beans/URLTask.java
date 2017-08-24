
package com.ebay.lightning.core.beans;

import java.net.URI;

import com.ebay.lightning.core.utils.InetSocketAddressCache;
import com.google.common.base.Strings;

/**
 * The {@code URLTask} class holds additional properties for the {@link Task} like proxy information. Additionally
 * it holds information on the response of URL execution like status code, http body etc.
 *
 * @author shashukla
 * @see ChainedURLTask
 */
public class URLTask extends Task {
	private static final long serialVersionUID = 1L;
	private static final int defaultHttpPort = 80;

	private URI uri;
	private int statusCode;
	private boolean useProxyServer = false;
	private transient String hostIPAddress;
	private String body = null;

	public URLTask(String url)  {
		try{
			this.uri = new URI(url);
		}
		catch (final Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Set the HTTP status code from the URL response.
	 * @param statusCode the URL response code
	 */
	public void setStatusCode(int statusCode) {
		this.statusCode = statusCode;
	}

	/**
	 * Get the URL response code.
	 * @return the URL response code
	 */
	public int getStatusCode() {
		return this.statusCode;
	}

	/**
	 * Get the port from the URL.
	 * @return the port from the URL
	 */
	public int getPort() {
		int port = this.uri.getPort();
		if(port < 0) {
			port = defaultHttpPort;
		}
		return port;
	}

	/**
	 * Get the host from the URL.
	 * @return the host from the URL
	 */
	public String getHost() {
		return this.uri.getHost();
	}

	/**
	 * Get the full URL path.
	 * @return the full URL path
	 */
	public String getPath() {
		final String query = this.uri.getQuery();
		final String path = this.uri.getRawPath();
		return (Strings.isNullOrEmpty(path)? "/" : path) + (query==null? "" : "?"+query);
	}

	/**
	 * Get the IP address of the host set by {@link InetSocketAddressCache}.
	 * @return the IP address of the host used to make connection. Returns the proxy IP if proxy is enabled
	 * via {@link #setUseProxyServer(boolean)}. Or else host IP is returned.
	 */
	public String getHostIPAddress() {
		return this.hostIPAddress;
	}

	/**
	 * Set the IP address of the host.
	 * @param hostIPAddress the IP address of the host used to make connection
	 */
	public void setHostIPAddress(String hostIPAddress) {
		this.hostIPAddress = hostIPAddress;
	}

	/**
	 * Check if proxy is enabled.
	 * @return true if proxy is enabled
	 */
	public boolean isUseProxyServer() {
		return this.useProxyServer;
	}

	/**
	 * Enable proxy for the request.
	 * @param useProxyServer true if proxy enabled
	 */
	public void setUseProxyServer(boolean useProxyServer) {
		this.useProxyServer = useProxyServer;
	}

	/**
	 * Get the full URL.
	 * @return the full URL
	 */
	public String getCompleteURL() {
		return this.uri.toString();
	}

	/**
	 * Get the response body for the URL request.
	 * @return the response body for the URL request
	 */
	public String getBody() {
		return this.body;
	}

	/**
	 * Set the response body for the URL request.
	 * @param body the response body for the URL request
	 */
	public void setBody(String body) {
		this.body = body;
	}

}