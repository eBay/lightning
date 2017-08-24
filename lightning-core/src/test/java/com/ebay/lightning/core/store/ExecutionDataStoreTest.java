package com.ebay.lightning.core.store;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.ebay.lightning.core.beans.LightningRequest;
import com.ebay.lightning.core.beans.LightningResponse;
import com.ebay.lightning.core.beans.ReservationReceipt;
import com.ebay.lightning.core.beans.Task;
import com.ebay.lightning.core.beans.URLTask;
import com.ebay.lightning.core.config.SystemConfig;
import com.ebay.lightning.core.config.SystemConfig.RetentionPolicy;
import com.ebay.lightning.core.manager.TaskExecutionManager;
import com.ebay.lightning.core.manager.TaskExecutionManagerTest;
import com.ebay.lightning.core.utils.InetSocketAddressCache;
import com.ebay.lightning.core.utils.SmartCache;

public class ExecutionDataStoreTest {
	private static final Logger log = Logger.getLogger(TaskExecutionManagerTest.class);
	private static final int RESERVATION_RESPONSE_EXPIRE_TIME_IN_SEC = 1;
	private static final int AUDIT_CLEANUP_FREQ_TIME_IN_SEC = 1;
	private static final int TASK_RETENTION_TIME_IN_MILLIS = 2000;
	private static final int LIGHTNING_REPORT_RETENTION_TIME_IN_MILLIS = 4000;
	private TaskExecutionManager taskExecutionManager;
	private SystemConfig systemConfig;
	private InetSocketAddress localhost;

	@Before
	public void setup() {
		this.localhost = getLocalHost();
		this.systemConfig = getSystemConfig();
		ExecutionDataStore collector = new ExecutionDataStore(this.systemConfig);
		InetSocketAddressCache inetAddressCache = mock(InetSocketAddressCache.class);
		SmartCache<String, InetSocketAddress> inetCache = (SmartCache<String, InetSocketAddress>) mock((SmartCache.class));
		Mockito.when(inetAddressCache.getInetSocketAddress(any(String.class),any(Integer.class))).thenReturn(localhost);
		Mockito.when(inetCache.size()).thenReturn(1L);
		Mockito.when(inetAddressCache.getInetCache()).thenReturn(inetCache);
		taskExecutionManager = new TaskExecutionManager(systemConfig, collector, inetAddressCache);
		taskExecutionManager.start();
	}
	
	private InetSocketAddress getLocalHost(){
		return new InetSocketAddress("localhost", 8989);
	}
	
	private SystemConfig getSystemConfig(){
		SystemConfig systemConfig = new SystemConfig();
		systemConfig.setMaxTaskCapacity(10);
		systemConfig.setAuditCleanupFrequencyInSec(AUDIT_CLEANUP_FREQ_TIME_IN_SEC);
		systemConfig.setReservationResponseExpireTimeInSec(RESERVATION_RESPONSE_EXPIRE_TIME_IN_SEC);
		systemConfig.setLoadFromFile(false);
		RetentionPolicy retentionPolicy = systemConfig.getRetentionPolicy();
		retentionPolicy.setTaskRetentionTimeInMillis(TASK_RETENTION_TIME_IN_MILLIS);
		retentionPolicy.setLightningReportRetentionTimeInMillis(LIGHTNING_REPORT_RETENTION_TIME_IN_MILLIS);
		retentionPolicy.setMaxLightningReportRetentionCount(1);
		return systemConfig;
	}
	
	@Test
	public void testTimeExpiry() throws Exception {
		int load = 1;
		String sessionId = UUID.randomUUID().toString();
		ReservationReceipt reservationRcpt = taskExecutionManager.reserve(load);
		LightningRequest request = new LightningRequest(sessionId, createTasks(load), reservationRcpt);
		taskExecutionManager.submit(request);
		LightningResponse firstResponse = null;
		while(true){
			firstResponse = taskExecutionManager.pollResults(sessionId, false);
			if(firstResponse.isCompleted()) break;
			Thread.sleep(500);
		}
		assertEquals(load, firstResponse.getFailedResponses().size()+firstResponse.getSuccessResponses().size());
		LightningRequestReport report = taskExecutionManager.getReport(sessionId);
		log.info("First report received...");
		assertNotNull(report);
		List<LightningRequestReport> auditReports = taskExecutionManager.getAuditReports(sessionId);
		Assert.assertTrue(auditReports.size() >= 1);
		waitForTimeExpiry(sessionId);
	}
	
	private void waitForTimeExpiry(String sessionId) throws Exception{
		long taskExpiryTime = TASK_RETENTION_TIME_IN_MILLIS + 1000;
		Thread.sleep(taskExpiryTime);
		log.info("Check for task expiry...");
		LightningResponse taskExpiredResponse = taskExecutionManager.pollResults(sessionId, false);
		assertNotNull(taskExpiredResponse);
		assertEquals(0, taskExpiredResponse.getFailedResponses().size()+taskExpiredResponse.getSuccessResponses().size());
		long reportExpiryTime = LIGHTNING_REPORT_RETENTION_TIME_IN_MILLIS - TASK_RETENTION_TIME_IN_MILLIS;
		if(reportExpiryTime>0) Thread.sleep(reportExpiryTime);
		log.info("Check for report expiry...");
		LightningResponse reportExpiredResponse = taskExecutionManager.pollResults(sessionId, false);
		assertNull(reportExpiredResponse);
	}
	
	@Test
	public void testCountExpiry() throws Exception {
		int load = 1;
		String sessionId = UUID.randomUUID().toString();
		ReservationReceipt reservationRcpt = taskExecutionManager.reserve(load);
		LightningRequest request = new LightningRequest(sessionId, createTasks(load), reservationRcpt);
		taskExecutionManager.submit(request);
		LightningResponse firstResponse = null;
		while(true){
			firstResponse = taskExecutionManager.pollResults(sessionId, false);
			if(firstResponse.isCompleted()) break;
			Thread.sleep(500);
		}
		assertEquals(load, firstResponse.getFailedResponses().size()+firstResponse.getSuccessResponses().size());
		int load2 = 2;
		String sessionId2 = UUID.randomUUID().toString();
		ReservationReceipt reservationRcpt2 = taskExecutionManager.reserve(load2);
		LightningRequest request2 = new LightningRequest(sessionId2, createTasks(load), reservationRcpt2);
		taskExecutionManager.submit(request2);
		Thread.sleep(AUDIT_CLEANUP_FREQ_TIME_IN_SEC*1000 + 1000);
		LightningResponse secondResponse = taskExecutionManager.pollResults(sessionId, false);
		assertNull(secondResponse);
	}
	
	private List<Task> createTasks(int load) throws Exception {
		List<Task> resp = new ArrayList<>();
		for (int i = 0; i < load; i++) {
			resp.add(new URLTask("http://localhost:8989/l/ecv"));
		}
		return resp;
	}
}
