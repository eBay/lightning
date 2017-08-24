/**
 * 
 */
package com.ebay.lightning.test.config;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

import com.ebay.lightning.core.config.RequestConfig;
import com.ebay.lightning.core.config.SystemConfig;
import com.ebay.lightning.core.constants.LightningCoreConstants.HttpMethod;

public class SystemConfigTest {

	@Before
	public void init() {
	}

	@Test
	public void testUpdateConfigWithFileConfig() {
		SystemConfig updatedFileConfig = new SystemConfig();
		
		Integer taskCapacityStress = 100;
		updatedFileConfig.setTaskCapacityStress(taskCapacityStress);
		Integer workerBatchSize = 20000;
		updatedFileConfig.setWorkerBatchSize(workerBatchSize);
		Integer readWriteTimeOutMillis = 100;
		String proxyServerHost = "localhost";
		boolean isRetryFailedTasks = true;

		RequestConfig defaultRCForHEAD = new RequestConfig();
		defaultRCForHEAD.setReadWriteTimeoutMillis(readWriteTimeOutMillis);
		defaultRCForHEAD.setRetryFailedTasks(isRetryFailedTasks);
		defaultRCForHEAD.setProxyServerHost(proxyServerHost);
		defaultRCForHEAD.setMethod(HttpMethod.HEAD);

		updatedFileConfig.getDefaultRequestConfigMap().put(HttpMethod.HEAD, defaultRCForHEAD);
		updatedFileConfig.setDefaultHTTPMethod(HttpMethod.GET);

		SystemConfig deepCopySystemConfig = new SystemConfig().deepCopyUpdatedConfig(updatedFileConfig);

		Assert.assertEquals(deepCopySystemConfig.getDefaultHTTPMethod(), updatedFileConfig.getDefaultHTTPMethod());
		Assert.assertEquals(deepCopySystemConfig.getDefaultRequestConfigMap().get(HttpMethod.HEAD)
				.getReadWriteTimeoutMillis(), updatedFileConfig.getDefaultRequestConfigMap().get(HttpMethod.HEAD)
				.getReadWriteTimeoutMillis());
		Assert.assertEquals(
				deepCopySystemConfig.getDefaultRequestConfigMap().get(HttpMethod.HEAD).isRetryFailedTasks(),
				updatedFileConfig.getDefaultRequestConfigMap().get(HttpMethod.HEAD).isRetryFailedTasks());
		Assert.assertNull(deepCopySystemConfig.getDefaultRequestConfigMap().get(HttpMethod.HEAD).getProxyServerHost());
	}

}
