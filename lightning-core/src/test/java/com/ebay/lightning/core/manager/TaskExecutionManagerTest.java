/**
 * 
 */
package com.ebay.lightning.core.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;
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

import com.ebay.lightning.core.beans.ChainedURLTask;
import com.ebay.lightning.core.beans.LightningRequest;
import com.ebay.lightning.core.beans.LightningResponse;
import com.ebay.lightning.core.beans.ReservationReceipt;
import com.ebay.lightning.core.beans.SystemStatus;
import com.ebay.lightning.core.beans.Task;
import com.ebay.lightning.core.beans.URLTask;
import com.ebay.lightning.core.config.SystemConfig;
import com.ebay.lightning.core.store.ExecutionDataStore;
import com.ebay.lightning.core.store.LightningRequestReport;
import com.ebay.lightning.core.utils.InetSocketAddressCache;
import com.ebay.lightning.core.utils.SmartCache;

/**
 * @author shashukla
 *
 */
public class TaskExecutionManagerTest {
	private static final Logger log = Logger.getLogger(TaskExecutionManagerTest.class);
	private static final int RESERVATION_RESPONSE_EXPIRE_TIME_IN_SEC = 1;
	private static final int MAX_TASK_CAPACITY = 10;
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
		systemConfig.setMaxTaskCapacity(MAX_TASK_CAPACITY);
		systemConfig.setReservationResponseExpireTimeInSec(RESERVATION_RESPONSE_EXPIRE_TIME_IN_SEC);
		systemConfig.setLoadFromFile(false);
		return systemConfig;
	}

	@Test
	public void testSubmitTwice() throws Exception {
		int load = 1;
		ReservationReceipt reservationRcpt = taskExecutionManager.reserve(load);
		LightningRequest request = new LightningRequest("sessionId", createTasks(load), reservationRcpt);
		taskExecutionManager.submit(request);
		Exception expectedException = null;
		try {
			taskExecutionManager.submit(request);
		} catch (Exception e) {
			expectedException = e;
		}
		assertNotNull(expectedException);
	}
	
	@Test
	public void testSubmitAndPoll() throws Exception {
		int load = 10;
		String sessionId = submit(load);
		LightningResponse firstResponse = null;
		while(true){
			firstResponse = taskExecutionManager.pollResults(sessionId, false);
			if(firstResponse.isCompleted()) break;
			Thread.sleep(500);
		}
		assertEquals(load, firstResponse.getFailedResponses().size()+firstResponse.getSuccessResponses().size());
		LightningRequestReport report = taskExecutionManager.getReport(sessionId);
		assertNotNull(report);
		List<LightningRequestReport> auditReports = taskExecutionManager.getAuditReports(sessionId);
		Assert.assertTrue(auditReports.size() >= 1);
	}
	
	@Test
	public void testSubmitChainedTasks() throws Exception {
		int load = 3;
		String sessionId = UUID.randomUUID().toString();
		ReservationReceipt reservationRcpt = taskExecutionManager.reserve(load);
		LightningRequest request = new LightningRequest(sessionId, createChainedTasks(load), reservationRcpt);
		taskExecutionManager.submit(request);
		LightningResponse firstResponse = null;
		Thread.sleep(2000);
		while(true){
			firstResponse = taskExecutionManager.pollResults(sessionId, false);
			if(firstResponse.isCompleted()) break;
			Thread.sleep(500);
		}
		assertEquals(1, firstResponse.getTotalCount());
		LightningRequestReport report = taskExecutionManager.getReport(sessionId);
		assertNotNull(report);
		List<LightningRequestReport> auditReports = taskExecutionManager.getAuditReports(sessionId);
		Assert.assertTrue(auditReports.size() >= 1);
	}

	private List<Task> createTasks(int load) throws Exception {
		List<Task> tasks = new ArrayList<>();
		for (int i = 0; i < load; i++)
			tasks.add(new URLTask("http://localhost:8989/l/ecv"));
		return tasks;
	}
	
	private List<Task> createChainedTasks(int load) throws Exception {
		List<Task> tasks = new ArrayList<>();
		ChainedURLTask chainedTask = new ChainedURLTask();
		for (int i = 0; i < load; i++)
			chainedTask.addUrlTask(new URLTask("http://localhost:8989/l/ecv"));
		tasks.add(chainedTask);
		return tasks;
	}

	@Test
	public void testReserveZeroLoad() {

		//zero load
		int load = 0;
		ReservationReceipt resp = taskExecutionManager.reserve(load);
		assertNotNull(resp);
		assertNotNull(resp.getId());
		assertEquals(load, resp.getLoad());
		assertEquals(ReservationReceipt.State.ACCEPTED, resp.getState());
	}

	@Test
	public void testReserveOptimalLoad() {
		//optimal load
		int load = 10;
		ReservationReceipt resp = taskExecutionManager.reserve(load);
		assertNotNull(resp);
		assertNotNull(resp.getId());
		assertEquals(load, resp.getLoad());
		assertEquals(ReservationReceipt.State.ACCEPTED, resp.getState());
	}

	@Test
	public void testReserveMoreLoad() {
		//more load
		int load = 11;
		ReservationReceipt resp = taskExecutionManager.reserve(load);
		assertNotNull(resp);
		assertNotNull(resp.getId());
		assertEquals(load, resp.getLoad());
		assertEquals(ReservationReceipt.State.DENIED, resp.getState());
	}

	@Test
	public void testReservationExpiry() {
		//Test
		int load = MAX_TASK_CAPACITY;
		ReservationReceipt resp = taskExecutionManager.reserve(load);
		assertNotNull(resp);
		assertNotNull(resp.getId());
		assertEquals(load, resp.getLoad());
		assertEquals(ReservationReceipt.State.ACCEPTED, resp.getState());
		
		waitFor(RESERVATION_RESPONSE_EXPIRE_TIME_IN_SEC+2);
	
		resp = taskExecutionManager.reserve(load);
		assertNotNull(resp);
		assertNotNull(resp.getId());
		assertEquals(load, resp.getLoad());
		assertEquals(ReservationReceipt.State.ACCEPTED, resp.getState());
	}
	
	@Test
	public void testLightningStats() throws Exception{
		submit(2);
		taskExecutionManager.reserve(3);
		SystemStatus status = taskExecutionManager.getLightningStats();
		assertNotNull(status);
		assertTrue(status.getWorkQueueSize() >= 0);
		assertTrue(status.getReservationLoadSize() >= 0);
		assertTrue(status.getQueueLoadSize() >= 0);
		assertTrue(status.getUpTime() > 0);
		assertTrue(status.getAvailableTaskCapacity() > 0);
	}
	
	@Test
	public void testUpdateSystemConfig()throws Exception{
		int cacheLimit = 500;
		SystemConfig config = new SystemConfig();
		config.setMaxInetCacheSize(cacheLimit);
		taskExecutionManager.updateSystemConfig(config);
		SystemConfig newConfig = taskExecutionManager.getSystemConfig();
		assertNotNull(newConfig);
		assertEquals(cacheLimit, newConfig.getMaxInetCacheSize());
		assertNotNull(taskExecutionManager.getWorkQueue());
		assertNotNull(taskExecutionManager.getReservationResponseLog());
	}
	
	private String submit(int load) throws Exception {
		String sessionId = UUID.randomUUID().toString();
		ReservationReceipt reservationRcpt = taskExecutionManager.reserve(load);
		LightningRequest request = new LightningRequest(sessionId, createTasks(load), reservationRcpt);
		taskExecutionManager.submit(request);
		return sessionId;
	}

	private void waitFor(int waitTimeInSec) {
		try {
			Thread.sleep(waitTimeInSec*1000);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}
}
