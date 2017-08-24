package com.ebay.lightning.controller;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import junit.framework.Assert;

import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.util.NestedServletException;

import com.ebay.lightning.core.beans.LightningRequest;
import com.ebay.lightning.core.beans.LightningResponse;
import com.ebay.lightning.core.beans.ReservationReceipt;
import com.ebay.lightning.core.beans.ReservationReceipt.State;
import com.ebay.lightning.core.beans.SystemStatus;
import com.ebay.lightning.core.beans.Task;
import com.ebay.lightning.core.beans.URLTask;
import com.ebay.lightning.core.config.MVCConfig;
import com.ebay.lightning.core.config.SystemConfig;
import com.ebay.lightning.core.constants.LightningCoreConstants.WorkStatus;
import com.ebay.lightning.core.controllers.LightningController;
import com.ebay.lightning.core.services.TaskExecutionService;
import com.ebay.lightning.core.store.LightningRequestReport;
import com.ebay.lightning.core.utils.ZipUtil;
import com.google.gson.Gson;
import com.google.gson.JsonParser;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class LightningControllerTest {
	
	private static final Logger log = Logger.getLogger(LightningControllerTest.class);
	private static MockMvc mockMvc;
	private static LightningController lightningController;
	
	private TaskExecutionService taskExecutionService;
	
	@BeforeClass
	public static void setupClass(){
		lightningController = new LightningController();
		mockMvc = standaloneSetup(lightningController).build();
	}
	
	@Before
	public void setup(){
		taskExecutionService = mock(TaskExecutionService.class);
		lightningController.setTaskExecutionService(taskExecutionService);
	}
	
	@Test
	public void ecvCheck() throws Exception{
		mockMvc.perform(get("/l/ecv")).andExpect(content().string("OK"));
	}
	
	@Test
	public void testReserve() throws Exception{
		int load = 10;
		ReservationReceipt receipt = new ReservationReceipt(State.ACCEPTED, UUID.randomUUID().toString(), load);
		when(taskExecutionService.reserve(load)).thenReturn(receipt);
		MvcResult result = mockMvc.perform(get("/l/reserve/"+load).accept(MediaType.APPLICATION_JSON)).andReturn();
		String content = result.getResponse().getContentAsString();
		ReservationReceipt reservationReciept = new Gson().fromJson(content, ReservationReceipt.class);
		Assert.assertEquals(load, reservationReciept.getLoad());
		Assert.assertEquals(ReservationReceipt.State.ACCEPTED, reservationReciept.getState());
	}

	@Test
	public void testReserveError() throws Exception{
		when(taskExecutionService.reserve(anyInt())).thenThrow(new NullPointerException());
		try{
			mockMvc.perform(get("/l/reserve/10").accept(MediaType.APPLICATION_JSON)).andReturn();
			Assert.assertFalse(true);  //The above call should throw an exception
		}catch(NestedServletException e){
			Assert.assertTrue(e.getRootCause() instanceof RuntimeException);
		}
	}
	
	@Test
	public void testSubmit() throws Exception{
		int load = 10;
		LightningRequest request = new LightningRequest("sessionId", generateTasks(load), new ReservationReceipt(ReservationReceipt.State.ACCEPTED,"1", load));
		MvcResult result = mockMvc.perform(post("/l/submit").content(ZipUtil.zipAsByteArray(request)).accept(MediaType.APPLICATION_JSON)).andReturn();
		String content = result.getResponse().getContentAsString();
		Assert.assertEquals("submitted", new JsonParser().parse(content).getAsJsonObject().get("status").getAsString());
	}
	
	@Test
	public void testSubmitError() throws Exception{
		int load = 10;
		LightningRequest request = new LightningRequest("sessionId", generateTasks(load), new ReservationReceipt(ReservationReceipt.State.ACCEPTED,"1", load));
		try{
			mockMvc.perform(post("/l/submit").content(request.toString()).accept(MediaType.APPLICATION_JSON)).andReturn();
			Assert.assertFalse(true);  //The above call should throw an exception
		}catch(NestedServletException e){
			Assert.assertTrue(e.getRootCause() instanceof RuntimeException);
		}
	}
	
	@Test
	public void testPollJson() throws Exception{
		String sessionId = "session1";
		boolean pollDeltaOnly = false;
		LightningResponse response = new LightningResponse(sessionId, WorkStatus.DONE);
		when(taskExecutionService.pollResponse(sessionId, pollDeltaOnly)).thenReturn(response);
		MvcResult result = mockMvc.perform(get(String.format("/l/poll/json/%s/%s",sessionId, pollDeltaOnly)).accept(MediaType.APPLICATION_JSON)).andReturn();
		String content = result.getResponse().getContentAsString();
		Assert.assertEquals("DONE", new JsonParser().parse(content).getAsJsonObject().get("status").getAsString());
	}
	
	@Test
	public void testPollJsonError() throws Exception{
		String sessionId = "session1";
		boolean pollDeltaOnly = false;
		when(taskExecutionService.pollResponse(sessionId, pollDeltaOnly)).thenThrow(new RuntimeException());
		try{
			mockMvc.perform(get(String.format("/l/poll/json/%s/%s",sessionId, pollDeltaOnly)).accept(MediaType.APPLICATION_JSON)).andReturn();
			Assert.assertFalse(true);  //The above call should throw an exception
		}catch(NestedServletException e){
			Assert.assertTrue(e.getRootCause() instanceof RuntimeException);
		}
	}
	
	@Test
	public void testPoll() throws Exception{
		String sessionId = "session1";
		boolean pollDeltaOnly = false;
		LightningResponse response = new LightningResponse(sessionId, WorkStatus.DONE);
		when(taskExecutionService.pollResponse(sessionId, pollDeltaOnly)).thenReturn(response);
		MvcResult result = mockMvc.perform(get(String.format("/l/poll/%s/%s",sessionId, pollDeltaOnly))).andReturn();
		byte[] content = result.getResponse().getContentAsByteArray();
		LightningResponse unzipResponse = (LightningResponse) ZipUtil.unZipByteArray(content, LightningResponse.class);
		Assert.assertEquals(response.getSessionId(), unzipResponse.getSessionId());
	}
	
	@Test
	public void testAuditJson() throws Exception{
		String sessionId = "session1";
		LightningRequestReport report = new LightningRequestReport();
		report.setStatus(WorkStatus.DONE);
		when(taskExecutionService.getReport(sessionId)).thenReturn(report);
		MvcResult result = mockMvc.perform(get(String.format("/l/audit/json/%s",sessionId)).accept(MediaType.APPLICATION_JSON)).andReturn();
		String content = result.getResponse().getContentAsString();
		Assert.assertEquals(WorkStatus.DONE.toString(), new JsonParser().parse(content).getAsJsonObject().get("status").getAsString());
	}
	
	@Test
	public void testAuditJsonError() throws Exception{
		String sessionId = "session1";
		when(taskExecutionService.getReport(sessionId)).thenThrow(new RuntimeException());
		try{
			mockMvc.perform(get(String.format("/l/audit/json/%s",sessionId)).accept(MediaType.APPLICATION_JSON)).andReturn();
			Assert.assertFalse(true);  //The above call should throw an exception
		}catch(NestedServletException e){
			Assert.assertTrue(e.getRootCause() instanceof RuntimeException);
		}
	}
	
	@Test
	public void testAudit() throws Exception{
		String sessionId = "session1";
		LightningRequestReport report = new LightningRequestReport();
		report.setStatus(WorkStatus.DONE);
		when(taskExecutionService.getReport(sessionId)).thenReturn(report);
		MvcResult result = mockMvc.perform(get(String.format("/l/audit/%s",sessionId))).andReturn();
		byte[] content = result.getResponse().getContentAsByteArray();
		LightningRequestReport unzipResponse = (LightningRequestReport) ZipUtil.unZipByteArray(content, LightningRequestReport.class);
		Assert.assertEquals(WorkStatus.DONE, unzipResponse.getStatus());
	}
	
	@Test
	public void testAuditSummary() throws Exception{
		String sessionId = "session1";
		LightningRequestReport report = new LightningRequestReport();
		report.setStatus(WorkStatus.DONE);
		List<LightningRequestReport> reports = new ArrayList<LightningRequestReport>();
		reports.add(report);
		when(taskExecutionService.getAuditReports(sessionId)).thenReturn(reports);
		MvcResult result = mockMvc.perform(get(String.format("/l/auditSummary?sessionId=%s",sessionId))).andReturn();
		byte[] content = result.getResponse().getContentAsByteArray();
		List unzipResponseList = (List) ZipUtil.unZipByteArray(content, List.class);
		LightningRequestReport unzipResponse = (LightningRequestReport) unzipResponseList.get(0);
		Assert.assertEquals(WorkStatus.DONE, unzipResponse.getStatus());
	}
	
	@Test
	public void testLightningStats() throws Exception{
		long freeMem = 100l;
		SystemStatus status = new SystemStatus();
		status.setFreeMemory(freeMem);
		when(taskExecutionService.getLightningStats()).thenReturn(status);
		MvcResult result = mockMvc.perform(get("/l/lightningStats")).andReturn();
		byte[] content = result.getResponse().getContentAsByteArray();
		SystemStatus unzipStatus = (SystemStatus) ZipUtil.unZipByteArray(content, SystemStatus.class);
		Assert.assertEquals(freeMem, unzipStatus.getFreeMemory());
	}

	@Test
	public void testGetSystemConfig() throws Exception{
		int maxLimit = 150;
		SystemConfig config = new SystemConfig();
		config.setMaxInetCacheSize(maxLimit);
		when(taskExecutionService.getSystemConfig()).thenReturn(config);
		MvcResult result = mockMvc.perform(get("/l/getSystemConfig").accept(MediaType.APPLICATION_JSON)).andReturn();
		String content = result.getResponse().getContentAsString();
		SystemConfig unzipConfig = new Gson().fromJson(content, SystemConfig.class);;
		Assert.assertEquals(maxLimit, unzipConfig.getMaxInetCacheSize());
	}
	
	@Test
	public void testGetSystemConfigError() throws Exception{
		when(taskExecutionService.getSystemConfig()).thenThrow(new RuntimeException());
		try{
			mockMvc.perform(get("/l/getSystemConfig")).andReturn();
			Assert.assertFalse(true);  //The above call should throw an exception
		}catch(NestedServletException e){
			Assert.assertTrue(e.getRootCause() instanceof RuntimeException);
		}
	}
	
	@Test
	public void testUpdateSystemConfig() throws Exception{
		int maxLimit = 155;
		SystemConfig config = new SystemConfig();
		config.setMaxInetCacheSize(maxLimit);
		String configStr = new Gson().toJson(config).toString();
		when(taskExecutionService.updateSystemConfig(any(SystemConfig.class))).thenReturn(config);
		MvcResult result = mockMvc.perform(post("/l/updateSystemConfig").content(configStr).accept(MediaType.APPLICATION_JSON)).andReturn();
		String content = result.getResponse().getContentAsString();
		SystemConfig unzipConfig = new Gson().fromJson(content, SystemConfig.class);;
		Assert.assertEquals(maxLimit, unzipConfig.getMaxInetCacheSize());
	}
	
	@Test
	public void testUpdateSystemConfigError() throws Exception{
		SystemConfig config = new SystemConfig();
		String configStr = new Gson().toJson(config).toString();
		when(taskExecutionService.updateSystemConfig(any(SystemConfig.class))).thenThrow(new RuntimeException());
		try{
			mockMvc.perform(post("/l/updateSystemConfig").content(configStr).accept(MediaType.APPLICATION_JSON)).andReturn();
			Assert.assertFalse(true);  //The above call should throw an exception
		}catch(NestedServletException e){
			Assert.assertTrue(e.getRootCause() instanceof RuntimeException);
		}
	}
	
	private List<Task> generateTasks(int taskCount) throws Exception {
		List<Task> tasks = new ArrayList<>();
		for (int j = 0; j < taskCount; j++)
			tasks.add(new URLTask("http://localhost:8080"));
		return tasks;
	}
	
	@Configuration
	@ComponentScan(basePackages = {"com.ebay.lightning.core"}, excludeFilters = @ComponentScan.Filter(value = MVCConfig.class, type = FilterType.ASSIGNABLE_TYPE))
	public static class SpringConfig{
	}
}
