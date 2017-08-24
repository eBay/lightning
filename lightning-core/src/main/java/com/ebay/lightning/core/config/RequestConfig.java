package com.ebay.lightning.core.config;

import java.io.Serializable;
import java.lang.reflect.Field;

import com.ebay.lightning.core.constants.LightningCoreConstants.HttpMethod;


/**
 * The {code @RequestConfig} holds all the configuration required for processing the request.
 * 
 * @author shashukla
 */
public class RequestConfig implements Serializable {
	private static final long serialVersionUID = 1L;

	private Integer connectTimeoutMillis ;
	private Integer readWriteTimeoutMillis ;
	private Integer slowUrlsConnectTimeoutMillis ;
	private Integer slowUrlsReadWriteTimeoutMillis;
	private Integer readAccuracyPercent ;
	private Integer connectAccuracyPercent ;
	private Integer retrySlowUrlsConnectTimeoutMillis ;
	private Integer retrySlowUrlsReadWriteTimeoutMillis;
	private Integer retryReadAccuracyPercent ;
	private Integer retryConnectAccuracyPercent ;
	private Integer proxyServerPort;
	private Boolean retryFailedTasks;
	private String proxyServerHost;
	private HttpMethod method;
	
	public void loadDefaultValues(SystemConfig config) {
		RequestConfig target = this;
		if (target.getMethod() == null) {
			method = config.getDefaultHTTPMethod();
		}
		RequestConfig source = config.getDefaultRequestConfig(method);
		boolean overwriteNullOnly = true;
		copyFields(target, source, overwriteNullOnly);
	}

	/**
	 * Get the connection timeout configuration.
	 * @return the connection timeout
	 */
	public Integer getConnectTimeoutMillis() {
		return connectTimeoutMillis;
	}

	/**
	 * Set the connection timeout configuration.
	 * @param connectTimeoutInMillis the connection timeout
	 */
	public void setConnectTimeoutMillis(Integer connectTimeoutInMillis) {
		this.connectTimeoutMillis = connectTimeoutInMillis;
	}

	/**
	 * Get the read/write timeout configuration.
	 * @return the read/write timeout
	 */
	public Integer getReadWriteTimeoutMillis() {
		return readWriteTimeoutMillis;
	}

	/**
	 * Set the read/write timeout configuration.
	 * @param readWriteTimeoutInMillis the read/write timeout
	 */
	public void setReadWriteTimeoutMillis(Integer readWriteTimeoutInMillis) {
		this.readWriteTimeoutMillis = readWriteTimeoutInMillis;
	}

	/**
	 * Get the connection timeout configuration for slow URLs.
	 * @return the connection timeout for long running URLs
	 */
	public Integer getSlowUrlsConnectTimeoutMillis() {
		return slowUrlsConnectTimeoutMillis;
	}

	/**
	 * Set the connection timeout configuration for slow URLs.
	 * @param longRunningUrlConnectTimeoutMillis the connection timeout for long running URLs
	 */
	public void setSlowUrlsConnectTimeoutMillis(Integer longRunningUrlConnectTimeoutMillis) {
		this.slowUrlsConnectTimeoutMillis = longRunningUrlConnectTimeoutMillis;
	}

	/**
	 * Get the read/write timeout configuration for slow URLs.
	 * @return the read/write timeout for long running URLs
	 */
	public Integer getSlowUrlsReadWriteTimeoutMillis() {
		return slowUrlsReadWriteTimeoutMillis;
	}

	/**
	 * Set the read/write timeout configuration for slow URLs.
	 * @param longRunningUrlReadWriteTimeoutMillis the read/write timeout for long running URLs
	 */
	public void setSlowUrlsReadWriteTimeoutMillis(Integer longRunningUrlReadWriteTimeoutMillis) {
		this.slowUrlsReadWriteTimeoutMillis = longRunningUrlReadWriteTimeoutMillis;
	}

	/**
	 * Check if retry of failed tasks is enabled.
	 * @return {@code true} if retry failed tasks enabled
	 */
	public boolean isRetryFailedTasks() {
		return retryFailedTasks;
	}

	/**
	 * Enable/disable the retry of failed tasks.
	 * @param retryFailedTasks retry failed tasks
	 */
	public void setRetryFailedTasks(boolean retryFailedTasks) {
		this.retryFailedTasks = retryFailedTasks;
	}

	/**
	 * Get the proxy server host.
	 * @return the proxy host
	 */
	public String getProxyServerHost() {
		return proxyServerHost;
	}

	/**
	 * Set the proxy server host.
	 * @param proxyServerHost the proxy host
	 */
	public void setProxyServerHost(String proxyServerHost) {
		this.proxyServerHost = proxyServerHost;
	}

	/**
	 * Get the proxy server port.
	 * @return the proxy host
	 */
	public Integer getProxyServerPort() {
		return proxyServerPort;
	}

	/**
	 * Set the proxy server port.
	 * @param proxyServerPort the proxy host
	 */
	public void setProxyServerPort(Integer proxyServerPort) {
		this.proxyServerPort = proxyServerPort;
	}

	/**
	 * Get the HTTP method for the URL task.
	 * @return the HTTP method
	 */
	public HttpMethod getMethod() {
		return method;
	}

	/**
	 * Set the HTTP method for the URL task.
	 * @param method the HTTP method
	 */
	public void setMethod(HttpMethod method) {
		this.method = method;
	}

	/**
	 * Get the minimum threshold for the successful read to connect percentage.
	 * @return the minimum threshold for the successful read to connect percentage
	 */
	public Integer getReadAccuracyPercent() {
		return readAccuracyPercent;
	}

	/**
	 * Set the minimum threshold for the successful read to connect percentage.
	 * @param readAccuracyPercent the minimum threshold for the successful read to connect percentage
	 */
	public void setReadAccuracyPercent(Integer readAccuracyPercent) {
		this.readAccuracyPercent = readAccuracyPercent;
	}

	/**
	 * Get the minimum threshold for the successful connect to total request percentage.
	 * @return the minimum threshold for the successful connect to total request percentage
	 */
	public Integer getConnectAccuracyPercent() {
		return connectAccuracyPercent;
	}

	/**
	 * Set the minimum threshold for the successful connect to total request percentage.
	 * @param connectAccuracyPercent the minimum threshold for the successful connect to total request percentage
	 */
	public void setConnectAccuracyPercent(Integer connectAccuracyPercent) {
		this.connectAccuracyPercent = connectAccuracyPercent;
	}

	/**
	 * Get the connect timeout configuration for failed URLs.
	 * @return the connect timeout configuration for failed URLs
	 */
	public Integer getRetrySlowUrlsConnectTimeoutMillis() {
		return retrySlowUrlsConnectTimeoutMillis;
	}

	/**
	 * Set the connect timeout configuration for failed URLs.
	 * @param retrySlowUrlsConnectTimeoutMillis the connect timeout configuration for failed URLs
	 */
	public void setRetrySlowUrlsConnectTimeoutMillis(Integer retrySlowUrlsConnectTimeoutMillis) {
		this.retrySlowUrlsConnectTimeoutMillis = retrySlowUrlsConnectTimeoutMillis;
	}

	/**
	 * Get the read/write timeout configuration for failed URLs.
	 * @return the read/write timeout configuration for failed URLs
	 */
	public Integer getRetrySlowUrlsReadWriteTimeoutMillis() {
		return retrySlowUrlsReadWriteTimeoutMillis;
	}

	/**
	 * Set the read/write timeout configuration for failed URLs.
	 * @param retrySlowUrlsReadWriteTimeoutMillis the read/write timeout configuration for failed URLs
	 */
	public void setRetrySlowUrlsReadWriteTimeoutMillis(Integer retrySlowUrlsReadWriteTimeoutMillis) {
		this.retrySlowUrlsReadWriteTimeoutMillis = retrySlowUrlsReadWriteTimeoutMillis;
	}

	/**
	 * Get the minimum threshold for the successful read to connect percentage while retrying failed tasks.
	 * @return the minimum threshold for the successful read to connect percentage while retrying failed tasks
	 */
	public Integer getRetryReadAccuracyPercent() {
		return retryReadAccuracyPercent;
	}

	/**
	 * Set the minimum threshold for the successful read to connect percentage while retrying failed tasks.
	 * @param retryReadAccuracyPercent the minimum threshold for the successful read to connect percentage while retrying failed tasks
	 */
	public void setRetryReadAccuracyPercent(Integer retryReadAccuracyPercent) {
		this.retryReadAccuracyPercent = retryReadAccuracyPercent;
	}

	/**
	 * Get the minimum threshold for the successful connect to total request percentage while retrying failed tasks.
	 * @return the minimum threshold for the successful connect to total request percentage while retrying failed tasks
	 */
	public Integer getRetryConnectAccuracyPercent() {
		return retryConnectAccuracyPercent;
	}

	/**
	 * Set the minimum threshold for the successful connect to total request percentage while retrying failed tasks.
	 * @param retryConnectAccuracyPercent the minimum threshold for the successful connect to total request percentage while retrying failed tasks
	 */
	public void setRetryConnectAccuracyPercent(Integer retryConnectAccuracyPercent) {
		this.retryConnectAccuracyPercent = retryConnectAccuracyPercent;
	}

	/* (non-Javadoc)
	 * @see {@link Object#toString()}
	 */
	@Override
	public String toString() {
		return "RequestConfig [connectTimeoutMillis=" + connectTimeoutMillis + ", readWriteTimeoutMillis=" + readWriteTimeoutMillis
				+ ", slowUrlsConnectTimeoutMillis=" + slowUrlsConnectTimeoutMillis + ", slowUrlsReadWriteTimeoutMillis="
				+ slowUrlsReadWriteTimeoutMillis + ", method=" + method + ", readAccuracyPercent=" + readAccuracyPercent 
				+ ", connectAccuracyPercent=" + connectAccuracyPercent + ", proxyServerPort="
				+ proxyServerPort + ", proxyServerHost=" + proxyServerHost + ", retryFailedTasks=" + retryFailedTasks + "]";
	}

	/**
	 * Copy the data from source to target {@code Object}.
	 * @param target the target {@code Object}
	 * @param source the source {@code Object}
	 * @param overwriteNullOnly overwrites only data with {@code null} value if set to {@code true}
	 */
	private void copyFields(Object target, Object source, boolean overwriteNullOnly ) {
		Field[] declaredFields = source.getClass().getDeclaredFields();
		for (Field f : declaredFields) {
			try {
				boolean isfieldNonFinal = (f.getModifiers() & java.lang.reflect.Modifier.FINAL) != java.lang.reflect.Modifier.FINAL;
				if (isfieldNonFinal) {
					boolean isfieldValueNull = f.get(target) == null;
					if (isfieldValueNull || !overwriteNullOnly) {
						Object object = f.get(source);
						f.set(target, object);
					}
				}
			} catch (IllegalArgumentException | IllegalAccessException e) {
				// ignore as this is best effort copy
			}
		}
	}
}
