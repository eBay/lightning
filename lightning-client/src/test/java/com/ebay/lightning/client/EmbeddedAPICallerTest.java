package com.ebay.lightning.client;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.ebay.lightning.client.caller.EmbeddedAPICaller;
import com.ebay.lightning.client.caller.ServiceCaller;
import com.ebay.lightning.core.beans.ReservationReceipt;
import com.ebay.lightning.core.beans.SystemStatus;
import com.ebay.lightning.core.config.SystemConfig;
import com.ebay.lightning.core.services.TaskExecutionService;
import com.ebay.lightning.core.store.LightningRequestReport;

public class EmbeddedAPICallerTest {

	
	private ServiceCaller apiCaller;
	private TaskExecutionService service;
	
	@Before
	public void setup() {
		service = Mockito.mock(TaskExecutionService.class);
		apiCaller = new EmbeddedAPICaller(service);
	}
	
	@Test
	public void testReserve() throws Exception {
		ReservationReceipt successReserveResponse = new ReservationReceipt(ReservationReceipt.State.ACCEPTED, "youGotIt", 1000);
		Mockito.when(service.reserve(1000)).thenReturn(successReserveResponse);
		ReservationReceipt resservationReciept = apiCaller.reserve(1000, "embeddedCoreService");
		Assert.assertEquals("youGotIt", resservationReciept.getId());
		Assert.assertEquals(ReservationReceipt.State.ACCEPTED, resservationReciept.getState());

		ReservationReceipt failedReserveResponse = new ReservationReceipt(ReservationReceipt.State.DENIED, "youGotRejected", 10000000);
		Mockito.when(service.reserve(10000000)).thenReturn(failedReserveResponse);
		ReservationReceipt resp = apiCaller.reserve(10000000, "embeddedCoreService");
		Assert.assertEquals("youGotRejected", resp.getId());
		Assert.assertEquals(ReservationReceipt.State.DENIED, resp.getState());

	}
	
	@Test(expected=RuntimeException.class)
	public void testReserveException() throws Exception {
		Mockito.when(service.reserve(10000000)).thenThrow(new RuntimeException("Error Calling service: "));
		apiCaller.reserve(10000000, "embeddedCoreService");
	}
	
	@Test
	public void getAuditReport() throws Exception {
		List<LightningRequestReport> expectedReqs = new ArrayList<LightningRequestReport>();
		LightningRequestReport expectedReq = new LightningRequestReport();
		expectedReqs.add(expectedReq);
		Mockito.when(service.getAuditReports("1000")).thenReturn(expectedReqs);
		List<LightningRequestReport> actualReqs = apiCaller.getAuditSummary("embeddedCoreService","1000");
		Assert.assertNotNull(actualReqs);
		Assert.assertEquals(1,actualReqs.size());
	}
	
	@Test(expected=RuntimeException.class)
	public void getAuditReportException() throws Exception {
		Mockito.when(service.getAuditReports("1000")).thenThrow(new RuntimeException("Error Calling service: "));
		apiCaller.getAuditSummary("embeddedCoreService", "1000");
	}
	
	@Test
	public void getSystemConfig() throws Exception {
		SystemConfig expectedConfig = new SystemConfig();
		SystemConfig resultConfig = new SystemConfig();
		Mockito.when(service.getSystemConfig()).thenReturn(expectedConfig);
		resultConfig = apiCaller.getSystemConfig("embeddedCoreService");
		Assert.assertNotNull(resultConfig);
	}
	
	@Test(expected=RuntimeException.class)
	public void getSystemConfigException() throws Exception {
		Mockito.when(service.getSystemConfig()).thenThrow(new RuntimeException("Error Calling service: "));
		apiCaller.getSystemConfig("embeddedCoreService");
	}
	
	@Test
	public void testgetLightningStats() throws Exception {
		SystemStatus resultStats;
		SystemStatus expectedStats = new SystemStatus();
		Mockito.when(service.getLightningStats()).thenReturn(expectedStats);
		resultStats = apiCaller.getLightningStats("embeddedCoreService");
		Assert.assertNotNull(resultStats);
	}
}
