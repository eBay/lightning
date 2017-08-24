package com.ebay.lightning.core.services;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import com.ebay.lightning.core.beans.LightningRequest;
import com.ebay.lightning.core.beans.LightningResponse;
import com.ebay.lightning.core.beans.ReservationReceipt;
import com.ebay.lightning.core.beans.ReservationReceipt.State;
import com.ebay.lightning.core.beans.SystemStatus;
import com.ebay.lightning.core.config.SystemConfig;
import com.ebay.lightning.core.constants.LightningCoreConstants.WorkStatus;
import com.ebay.lightning.core.exception.ManagerQueueFullException;
import com.ebay.lightning.core.exception.WorkQueueCapacityReachedException;
import com.ebay.lightning.core.manager.TaskExecutionManager;
import com.ebay.lightning.core.store.LightningRequestReport;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;

public class TaskExecutionServiceTest {

	private TaskExecutionService taskExecutionService;
	
	private TaskExecutionManager taskExecutionManager;
	
	private String sessionId = "sessionId1";
	
	@Before
	public void setup(){
		taskExecutionManager = mock(TaskExecutionManager.class);
		taskExecutionService = new TaskExecutionServiceImpl(taskExecutionManager);
	}
	
	@Test
	public void testSubmit(){
		LightningRequest request = new LightningRequest(sessionId);
		taskExecutionService.submit(request);
		assertTrue(true);
	}
	
	@Test
	public void testSubmitError1(){
		LightningRequest request = new LightningRequest(sessionId);
		doThrow(new WorkQueueCapacityReachedException()).when(taskExecutionManager).submit(request);
		try{
			taskExecutionService.submit(request);
			assertFalse(true);
		}catch(WorkQueueCapacityReachedException e){
			assertTrue(true);
		}
	}
	
	@Test
	public void testSubmitError2(){
		LightningRequest request = new LightningRequest(sessionId);
		doThrow(new ManagerQueueFullException()).when(taskExecutionManager).submit(request);
		try{
			taskExecutionService.submit(request);
			assertFalse(true);
		}catch(ManagerQueueFullException e){
			assertTrue(true);
		}
	}
	
	@Test
	public void testPollResponse(){
		boolean pollDeltaOnly = false;
		LightningResponse expected = new LightningResponse(sessionId, WorkStatus.IN_QUEUE);
		when(taskExecutionManager.pollResults(sessionId, pollDeltaOnly)).thenReturn(expected);
		LightningResponse response = taskExecutionService.pollResponse(sessionId, pollDeltaOnly);
		assertEquals(sessionId, response.getSessionId());
		assertEquals(expected.getStatus(), response.getStatus());
	}
	
	@Test
	public void testReserve(){
		int load = 7;
		String id = UUID.randomUUID().toString();
		ReservationReceipt expected = new ReservationReceipt(State.BUSY, id, load);
		when(taskExecutionManager.reserve(load)).thenReturn(expected);
		ReservationReceipt response = taskExecutionService.reserve(load);
		assertEquals(id, response.getId());
		assertEquals(load, response.getLoad());
		assertEquals(expected.getState(), response.getState());
	}
	
	@Test
	public void testGetReport(){
		long totExecTime = 1001l;
		LightningRequest request = new LightningRequest(sessionId);
		LightningRequestReport expected = new LightningRequestReport(request);
		expected.setTotalExecutionTimeInMillis(totExecTime);
		when(taskExecutionManager.getReport(sessionId)).thenReturn(expected);
		LightningRequestReport response = taskExecutionService.getReport(sessionId);
		assertEquals(sessionId, response.getRequest().getSessionId());
		assertEquals(totExecTime, response.getTotalExecutionTimeInMillis().longValue());
	}
	
	@Test
	public void testGetAuditReports(){
		long totExecTime = 1003l;
		LightningRequest request = new LightningRequest(sessionId);
		LightningRequestReport report = new LightningRequestReport(request);
		report.setTotalExecutionTimeInMillis(totExecTime);
		List<LightningRequestReport> expected = new ArrayList<LightningRequestReport>(); 
		expected.add(report);
		
		when(taskExecutionManager.getAuditReports(sessionId)).thenReturn(expected);
		List<LightningRequestReport> responses = taskExecutionService.getAuditReports(sessionId);
		assertEquals(1, responses.size());
		LightningRequestReport response = responses.get(0);
		assertEquals(sessionId, response.getRequest().getSessionId());
		assertEquals(totExecTime, response.getTotalExecutionTimeInMillis().longValue());
	}
	
	@Test
	public void testGetLightningStats(){
		long freeMem = 8989l;
		SystemStatus expected = new SystemStatus();
		expected.setFreeMemory(freeMem);
		when(taskExecutionManager.getLightningStats()).thenReturn(expected);
		SystemStatus response = taskExecutionService.getLightningStats();
		assertEquals(freeMem, response.getFreeMemory());
	}
	
	@Test
	public void testSystemConfig()throws Exception{
		int maxTaskCapacity = 250;
		SystemConfig expected = new SystemConfig();
		expected.setMaxTaskCapacity(maxTaskCapacity);
		
		when(taskExecutionManager.getSystemConfig()).thenReturn(expected);
		SystemConfig response = taskExecutionService.getSystemConfig();
		assertEquals(maxTaskCapacity, response.getMaxTaskCapacity());
		
		int maxInetCacheSize=101;
		expected.setMaxInetCacheSize(maxInetCacheSize);
		SystemConfig updatedValue = new SystemConfig();
		when(taskExecutionManager.updateSystemConfig(updatedValue)).thenReturn(expected);
		SystemConfig updateResponse = taskExecutionService.updateSystemConfig(updatedValue);
		assertEquals(maxTaskCapacity, updateResponse.getMaxTaskCapacity());
		assertEquals(maxInetCacheSize, updateResponse.getMaxInetCacheSize());
	}
	
}
