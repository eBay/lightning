package com.ebay.lightning.client;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.ebay.lightning.client.caller.RestAPICaller;
import com.ebay.lightning.client.config.LightningClientConfig;
import com.ebay.lightning.core.beans.LightningRequest;
import com.ebay.lightning.core.beans.LightningResponse;
import com.ebay.lightning.core.beans.ReservationReceipt;
import com.ebay.lightning.core.beans.SystemStatus;
import com.ebay.lightning.core.beans.Task;
import com.ebay.lightning.core.beans.URLTask;
import com.ebay.lightning.core.config.SystemConfig;
import com.ebay.lightning.core.store.LightningRequestReport;
import com.ebay.lightning.core.utils.UrlUtils;
import com.ebay.lightning.core.utils.UrlUtils.ContentType;
import com.ebay.lightning.core.utils.ZipUtil;
import com.google.gson.Gson;

/**
 * @author shashukla
 *
 */
public class RestApiCallerTest {

	private static final String RESERVATION_DENIED = "{ \"id\": \"youGotIt\", \"state\" : \"DENIED\"  }";
	private static final String RESERVATION_ACCEPTED = "{\"id\": \"youGotIt\" ,  \"state\" : \"ACCEPTED\" }";

	private RestAPICaller restAPICaller;
	private UrlUtils urlUtils;

	@Before
	public void setup() {
		LightningClientConfig config = new LightningClientConfig();
		config.setReserveApiUrl("http://{host}:port/reserve");
		config.setPollApiUrl("http://{host}:port/poll");
		config.setSubmitApiUrl("http://{host}:port/submit");
		config.setAuditApiUrl("http://{host}:port/audit");
		config.setAuditJsonApiUrl("http://{host}:port/audit/json");
		config.setAuditSummaryUrl("http://{host}:port/auditSummary");
		config.setSystemConfigUrl("http://{host}:port/getSystemConfig");
		config.setLightningStatsUrl("http://{host}:port/lightningStats");
		config.setSystemConfigUpdateUrl("http://{host}:port/updateSystemConfig");
		urlUtils = Mockito.mock(UrlUtils.class);
		restAPICaller = new RestAPICaller(config, urlUtils);
	}

	@Test
	public void testReserve() throws Exception {

		Mockito.when(urlUtils.get("http://localhost:port/reserve/1000")).thenReturn(RESERVATION_ACCEPTED);
		ReservationReceipt resservationReciept = restAPICaller.reserve(1000, "localhost");
		Assert.assertEquals("youGotIt", resservationReciept.getId());
		Assert.assertEquals(ReservationReceipt.State.ACCEPTED, resservationReciept.getState());

		Mockito.when(urlUtils.get("http://localhost:port/reserve/10000000")).thenReturn(RESERVATION_DENIED);
		ReservationReceipt resp = restAPICaller.reserve(10000000, "localhost");
		Assert.assertEquals("youGotIt", resp.getId());
		Assert.assertEquals(ReservationReceipt.State.DENIED, resp.getState());

	}

	@Test
	public void testSubmit() throws Exception {
		LightningRequest request = new LightningRequest("sessionId", generateTasks(1000), new ReservationReceipt(ReservationReceipt.State.ACCEPTED,
				"1", 1000));
		String payload = ZipUtil.zip(request);
		Mockito.when(urlUtils.post("http://localhost:port/submit", ContentType.APPLICATION_JSON, null, payload)).thenReturn(
				"{\"status\": \"submitted\"}");
		boolean resp = restAPICaller.submit(request, "localhost");
		Assert.assertTrue(resp);
		try {
			Mockito.when(urlUtils.post("http://localhost:port/submit", ContentType.APPLICATION_JSON, null, payload)).thenReturn("{}");
			resp = restAPICaller.submit(request, "localhost");
		} catch (Exception e) {
			Assert.assertNotNull(e);
		}
	}

	@Test
	public void testPoll() throws Exception {
		LightningResponse resultResponse = null;
		LightningResponse expectedResponse = new LightningResponse(null, null);
		byte[] zippedResponse = ZipUtil.zipAsByteArray(expectedResponse);
		Mockito.when(urlUtils.getByteArray("http://localhost:port/poll/1000/false")).thenReturn(zippedResponse);
		resultResponse = restAPICaller.pollResults("1000", "localhost", false);
		Assert.assertNotNull(resultResponse);
	}

	@Test
	public void testPollThrows() throws Exception {
		Mockito.when(urlUtils.getByteArray("http://localhost:port/pollTEST/1000/false")).thenThrow(Exception.class);
	}

	@Test
	public void testupdateSystemConfig() throws Exception {
		SystemConfig expectedConfig = new SystemConfig();
		expectedConfig.setTaskCapacityStress(500);
		SystemConfig resultConfig;
		String testPayload = new Gson().toJson(expectedConfig).toString();
		Mockito.when(urlUtils.post("http://localhost:port/updateSystemConfig", ContentType.APPLICATION_JSON, null, testPayload)).thenReturn(
				testPayload);
		resultConfig = restAPICaller.updateSystemConfig("localhost", expectedConfig);
		Assert.assertEquals(resultConfig.getTaskCapacityStress(), expectedConfig.getTaskCapacityStress());
		;

		Assert.assertNotSame(expectedConfig, resultConfig);

	}

	@Test
	public void testupdateSystemThrows() throws Exception {
		SystemConfig expectedConfig = new SystemConfig();
		expectedConfig.setTaskCapacityStress(500);
		SystemConfig resultConfig;
		String testPayload = new Gson().toJson(expectedConfig).toString();
		Mockito.when(urlUtils.post("http://localhost:port/updateSystemConfig", ContentType.APPLICATION_JSON, null, testPayload)).thenThrow(
				Exception.class);
		resultConfig = restAPICaller.updateSystemConfig("localhost", expectedConfig);
		Assert.assertNull(resultConfig);

	}

	@Test
	public void testgetAuditReport() throws Exception {
		LightningRequestReport resultReq = new LightningRequestReport();
		LightningRequestReport expectedReq = new LightningRequestReport();
		byte[] zippedResponse = ZipUtil.zipAsByteArray(expectedReq);
		Mockito.when(urlUtils.getByteArray("http://localhost:port/audit/1000")).thenReturn(zippedResponse);
		resultReq = restAPICaller.getAuditReport("1000", "localhost");
		Assert.assertNotNull(resultReq);

		Mockito.when(urlUtils.getByteArray("http://localhost:port/audit/1000")).thenThrow(Exception.class);
		resultReq = restAPICaller.getAuditReport("1000", "localhost");
		Assert.assertNull(resultReq);

	}

	@Test
	public void testgetAuditJsonReport() throws Exception {
		LightningRequestReport resultReq;
		LightningRequestReport expectedReq = new LightningRequestReport(new LightningRequest("1000"));
		String testResp = new Gson().toJson(expectedReq);

		Mockito.when(urlUtils.get("http://localhost:port/audit/json/1000")).thenReturn(testResp);
		resultReq = restAPICaller.getAuditJsonReport("1000", "localhost");
		Assert.assertNotNull(resultReq);

		Mockito.when(urlUtils.get("http://localhost:port/audit/json/1000")).thenThrow(Exception.class);
		resultReq = restAPICaller.getAuditJsonReport("1000", "localhost");
		Assert.assertNull(resultReq);
	}

	@Test
	public void testgetAuditSummary() throws Exception {
		List<LightningRequestReport> expectedResponse = new ArrayList<>();
		byte[] byteArray = ZipUtil.zipAsByteArray(expectedResponse);
		List<LightningRequestReport> resultResponse;

		Mockito.when(urlUtils.getByteArray("http://localhost:port/auditSummary?sessionId=1000")).thenReturn(byteArray);
		resultResponse = restAPICaller.getAuditSummary("localhost", "1000");
		Assert.assertNotNull(resultResponse);

		Mockito.when(urlUtils.getByteArray("http://localhost:port/auditSummary")).thenReturn(byteArray);
		resultResponse = restAPICaller.getAuditSummary("localhost", "");
		Assert.assertNotNull(resultResponse);

		Mockito.when(urlUtils.getByteArray("http://localhost:port/auditSummary")).thenThrow(Exception.class);
		resultResponse = restAPICaller.getAuditSummary("localhost", "");
		Assert.assertNull(resultResponse);

	}

	@Test
	public void testfillHostIP() throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException,
			InvocationTargetException {
		Method method = RestAPICaller.class.getDeclaredMethod("fillHostIP", new Class[] { String.class, String.class });
		method.setAccessible(true);
		Object obj = method.invoke(restAPICaller, new Object[] { "{host}", "localhost" });
		Assert.assertEquals("localhost", (String) obj);

		obj = method.invoke(restAPICaller, new Object[] { "{ho=st}", "localhost" });
		Assert.assertNotNull((String) obj);

	}

	@Test
	public void testgetSystemConfig() throws Exception {
		SystemConfig expectedConfig = new SystemConfig();
		;
		SystemConfig resultConfig = new SystemConfig();
		String testResp = new Gson().toJson(expectedConfig);

		Mockito.when(urlUtils.get("http://localhost:port/getSystemConfig")).thenReturn(testResp);
		resultConfig = restAPICaller.getSystemConfig("localhost");
		Assert.assertNotNull(resultConfig);

		Mockito.when(urlUtils.get("http://localhost:port/getSystemConfig")).thenThrow(Exception.class);
		resultConfig = restAPICaller.getSystemConfig("localhost");
		Assert.assertNull(resultConfig);
	}

	@Test
	public void testgetLightningStats() throws Exception {
		byte[] zipResp;
		SystemStatus resultStats;
		SystemStatus expectedStats = new SystemStatus();
		zipResp = ZipUtil.zipAsByteArray(expectedStats);
		Mockito.when(urlUtils.getByteArray("http://localhost/port/lightningStats")).thenReturn(zipResp);
		resultStats = restAPICaller.getLightningStats("localhost");
		Assert.assertNotNull(resultStats);

		Mockito.when(urlUtils.getByteArray("http://localhost/port/lightningStats")).thenThrow(Exception.class);
		resultStats = restAPICaller.getLightningStats("localhost");
		Assert.assertNotSame(expectedStats, resultStats);

	}

	private static List<Task> generateTasks(int taskCount) throws Exception {
		List<Task> tasks = new ArrayList<>();

		for (int j = 0; j < taskCount; j++) {
			tasks.add(new URLTask("http://localhost:8080"));
		}

		return tasks;
	}

}
