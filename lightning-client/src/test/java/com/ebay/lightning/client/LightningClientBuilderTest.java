package com.ebay.lightning.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.ebay.lightning.client.caller.ServiceCaller;
import com.ebay.lightning.core.beans.LightningRequest;
import com.ebay.lightning.core.beans.LightningResponse;
import com.ebay.lightning.core.beans.Task;
import com.ebay.lightning.core.beans.URLTask;
import com.ebay.lightning.core.config.SystemConfig;
import com.ebay.lightning.core.manager.TaskExecutionManager;
import com.ebay.lightning.core.store.LightningRequestReport;
import com.ebay.lightning.core.utils.UrlUtils;
import com.ebay.lightning.core.utils.UrlUtils.ContentType;
import com.ebay.lightning.core.utils.ZipUtil;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;

/**
 * The class <code>LightningClientBuilderTest</code> contains tests for the
 * class <code>{@link LightningClientBuilder}</code>.
 *
 * @author shashukla
 */
public class LightningClientBuilderTest {

	private static final String RESERVATION_DENIED = "{ \"id\": \"youGotIt\", \"state\" : \"DENIED\"  }";
	private static final String RESERVATION_ACCEPTED = "{\"id\": \"youGotIt\" ,  \"state\" : \"ACCEPTED\" }";

	private LightningClientBuilder testBuilder;
	private UrlUtils urlUtils;
	private String payload;
	private List<Task> tasks;
	private ServiceCaller testCaller;

	private static final Logger log = Logger.getLogger(LightningClientBuilderTest.class);

	@Before
	public void setUp() throws Exception {
		urlUtils = Mockito.mock(UrlUtils.class);
		testCaller = Mockito.mock(ServiceCaller.class);
		tasks = generateTasks(1000);
		testBuilder = new LightningClientBuilder();
		testBuilder.setUrlUtils(urlUtils);
	}

	@After
	public void tearDown() {
		log.info("LightningClinetBuilderTest - Done!");
	}

	@Test
	public void testBuild_WithSeeds() throws Exception {
		final String reserveApiUrl = "http://{host}:0/reserve";
		final String pollApiUrl = "http://{host}:0/poll";
		final String submitApiUrl = "http://{host}:0/submit";
		final String auditApiUrl = "http://{host}:0/audit";
		final String auditJsonApiUrl = "http://{host}:0/audit/json";
		final String auditSummaryApiUrl = "http://{host}:0/auditSummary";
		final String lightningStatsApiUrl = "http://{host}:0/lightningStats";
		final String systemConfigApiUrl = "http://{host}:0/getSystemConfig";
		final String systemConfigUpdateApiUrl = "http://{host}:0/updateSystemConfig";
		testBuilder.setPollApiUrlTemplate(pollApiUrl).setReserveApiUrlTemplate(reserveApiUrl).setAuditApiUrlTemplate(auditApiUrl)
		.setSubmitApiUrlTemplate(submitApiUrl).setAuditJsonApiUrlTemplate(auditJsonApiUrl).setAuditSummaryUrlTemplate(auditSummaryApiUrl)
		.setLightningStatsUrlTemplate(lightningStatsApiUrl).setSystemConfigUpdateUrlTemplate(systemConfigUpdateApiUrl)
		.setSystemConfigUrlTemplate(systemConfigApiUrl);

		Mockito.when(urlUtils.get("http://localhost:0/reserve/1000")).thenReturn(RESERVATION_ACCEPTED);
		final List<String> seeds = new ArrayList<>();
		final String servingIp = "localhost";
		seeds.add(servingIp);
		testBuilder.setSeeds(seeds);
		LightningClient clientWithSeeds = testBuilder.build();
		Mockito.when(urlUtils.post(anyString(), any(UrlUtils.ContentType.class), any(HashMap.class), anyString()))
		.thenReturn("{\"status\": \"submitted\"}");
		LightningRequest req = clientWithSeeds.submit(tasks);
		Mockito.when(urlUtils.get("http://localhost:0/poll/" + req.getSessionId() + "/false")).thenReturn("{\"response\": \"response\"}");
		assertEquals(servingIp, req.getServingHostIp());
		assertNotNull(req);

		final LightningResponse expectedResponse = new LightningResponse(null, null);
		final byte[] zippedResponse = ZipUtil.zipAsByteArray(expectedResponse);
		Mockito.when(urlUtils.getByteArray("http://localhost:0/poll/" + req.getSessionId() + "/false")).thenReturn(zippedResponse);

		final LightningResponse pollResponse = clientWithSeeds.pollResponse(req, false);// Giving
		// Null
		assertNotNull(pollResponse);
		payload = ZipUtil.zip(req);
		Mockito.when(urlUtils.get("http://localhost:0/reserve/1000")).thenReturn(RESERVATION_ACCEPTED);
		Mockito.when(urlUtils.post("http://localhost:0/submit", ContentType.APPLICATION_JSON, null, payload))
		.thenReturn("{\"status\": \"submitted\"}");
		clientWithSeeds = testBuilder.build();

		req = clientWithSeeds.submit(tasks);
		Mockito.when(urlUtils.get("http://localhost:0/poll/" + req.getSessionId() + "/false")).thenReturn("{\"response\": \"response\"}");

		assertEquals(servingIp, req.getServingHostIp());
		assertNotNull(req);

	}

	@Test
	public void testBuild_NoSeeds() throws Exception {
		final LightningClient client = testBuilder.build();
		Exception actual = null;
		try {

			Mockito.when(urlUtils.get("http://localhost:0/reserve/1000")).thenReturn(RESERVATION_ACCEPTED);
			Mockito.when(urlUtils.post("http://localhost:0/submit", ContentType.APPLICATION_JSON, null, payload))
			.thenReturn("{\"status\": \"submitted\"}");
			Mockito.when(urlUtils.get("http://localhost:0/poll/1000")).thenReturn("{\"response\": \"response\"}");

			client.submit(tasks);
		} catch (final Exception e) {
			actual = e;
		}
		assertNotNull("Submit without any seeds should result in exception", actual);
	}

	@Test
	public void testEmbeddedMode() throws Exception {
		Mockito.when(urlUtils.get("http://localhost:0/reserve/1000")).thenReturn(RESERVATION_ACCEPTED);
		final LightningClient testClient = testBuilder.setEmbeddedMode(true).build();
		Mockito.when(urlUtils.post(anyString(), any(UrlUtils.ContentType.class), any(HashMap.class), anyString()))
		.thenReturn("{\"status\": \"submitted\"}");
		final LightningRequest testRequest = testClient.submit(tasks);
		Mockito.when(urlUtils.get("http://localhost:0/poll/" + testRequest.getSessionId() + "/false")).thenReturn("{\"response\": \"response\"}");
		assertNotNull(testRequest);

		LightningRequestReport testReport = testClient.getAuditJsonReport(testRequest.getSessionId(), testRequest.getServingHostIp());
		assertNotNull(testReport);

		testReport = testClient.getAuditReport(testRequest);
		assertNotNull(testReport);

		testReport = testClient.getAuditReport(testRequest.getSessionId(), testRequest.getServingHostIp());
		assertNotNull(testReport);

		final SystemConfig testConfig = testClient.updateSystemConfig(testRequest.getServingHostIp(), new SystemConfig());
		assertNotNull(testConfig);

		final TaskExecutionManager testManager = Mockito.mock(TaskExecutionManager.class);
		final LightningResponse testResponse = Mockito.mock(LightningResponse.class);
		Mockito.when(testManager.pollResults("1000", false)).thenReturn(testResponse);

		final LightningResponse resultResponse = testClient.pollResponse(testRequest, false);
		assertNotNull(resultResponse);

	}

	private static List<Task> generateTasks(int taskCount) throws Exception {
		final List<Task> tasks = new ArrayList<>();

		for (int j = 0; j < taskCount; j++) {
			tasks.add(new URLTask("http://localhost:8080"));
		}

		return tasks;
	}

}