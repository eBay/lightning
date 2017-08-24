/**
 * 
 */
package com.ebay.lightning.test.config;

import org.junit.Before;
import org.junit.Test;

import com.ebay.lightning.core.config.RequestConfig;
import com.ebay.lightning.core.config.SystemConfig;
import com.ebay.lightning.core.constants.LightningCoreConstants.HttpMethod;

import junit.framework.Assert;

/**
 * @author shashukla
 *
 */
public class RequestConfigTest {

	private SystemConfig config;

	@Before
	public void init() {
		config = new SystemConfig();
	}

	@Test
	public void testDefaultHTTPMethodSetting() {
		RequestConfig target = new RequestConfig();
		Assert.assertNull(target.getMethod());
		target.loadDefaultValues(config);
		Assert.assertEquals(target.getMethod(), config.getDefaultHTTPMethod());
	}

	@Test
	public void testDefaultConfigLoading() {
		RequestConfig target = new RequestConfig();
		target.loadDefaultValues(config);
		RequestConfig defaultRequestConfig = config.getDefaultRequestConfig(target.getMethod());
		Assert.assertEquals(target.getReadAccuracyPercent(), defaultRequestConfig.getReadAccuracyPercent());
		Assert.assertEquals(target.getConnectAccuracyPercent(), defaultRequestConfig.getConnectAccuracyPercent());
		Assert.assertEquals(target.getConnectTimeoutMillis(), defaultRequestConfig.getConnectTimeoutMillis());
		Assert.assertEquals(target.getProxyServerHost(), defaultRequestConfig.getProxyServerHost());
		Assert.assertEquals(target.getProxyServerPort(), defaultRequestConfig.getProxyServerPort());
		Assert.assertEquals(target.getReadWriteTimeoutMillis(), defaultRequestConfig.getReadWriteTimeoutMillis());
		Assert.assertEquals(target.isRetryFailedTasks(), defaultRequestConfig.isRetryFailedTasks());
		Assert.assertEquals(target.getSlowUrlsConnectTimeoutMillis(), defaultRequestConfig.getSlowUrlsConnectTimeoutMillis());
		Assert.assertEquals(target.getSlowUrlsReadWriteTimeoutMillis(), defaultRequestConfig.getSlowUrlsReadWriteTimeoutMillis());
	}

	@Test
	public void testDefaultConfigLoadingWithPrePopulatedValues() {
		RequestConfig target = new RequestConfig();
		HttpMethod preloadedMethod = HttpMethod.GET;

		target.setMethod(preloadedMethod);
		Integer readAccuracyPercent = 50;
		Integer connectAccuracyPercent = 50;
		target.setReadAccuracyPercent(readAccuracyPercent);
		target.setConnectAccuracyPercent(connectAccuracyPercent);
		Integer connectTimeoutInMillis = 60;
		target.setConnectTimeoutMillis(connectTimeoutInMillis);
		String proxyServerHost = "tempHost";
		target.setProxyServerHost(proxyServerHost);
		Integer proxyServerPort = 007;
		target.setProxyServerPort(proxyServerPort);
		Integer readWriteTimeoutInMillis = 90;
		target.setReadWriteTimeoutMillis(readWriteTimeoutInMillis);
		boolean retryFailedTasks = true;
		target.setRetryFailedTasks(retryFailedTasks);
		Integer longRunningUrlConnectTimeoutMillis = 101;
		target.setSlowUrlsConnectTimeoutMillis(longRunningUrlConnectTimeoutMillis);
		Integer longRunningUrlReadWriteTimeoutMillis = 111;
		target.setSlowUrlsReadWriteTimeoutMillis(longRunningUrlReadWriteTimeoutMillis);

		target.loadDefaultValues(config);
		Assert.assertEquals(target.getMethod(), preloadedMethod);
		Assert.assertEquals(target.getReadAccuracyPercent(), readAccuracyPercent);
		Assert.assertEquals(target.getConnectAccuracyPercent(), connectAccuracyPercent);
		Assert.assertEquals(target.getConnectTimeoutMillis(), connectTimeoutInMillis);
		Assert.assertEquals(target.getProxyServerHost(), proxyServerHost);
		Assert.assertEquals(target.getProxyServerPort(), proxyServerPort);
		Assert.assertEquals(target.getReadWriteTimeoutMillis(), readWriteTimeoutInMillis);

		Assert.assertEquals(target.isRetryFailedTasks(), retryFailedTasks);
		Assert.assertEquals(target.getSlowUrlsConnectTimeoutMillis(), longRunningUrlConnectTimeoutMillis);
		Assert.assertEquals(target.getSlowUrlsReadWriteTimeoutMillis(), longRunningUrlReadWriteTimeoutMillis);
	}

}
