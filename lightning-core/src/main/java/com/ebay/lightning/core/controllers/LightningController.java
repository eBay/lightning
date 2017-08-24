/**
 * 
 */
package com.ebay.lightning.core.controllers;

import java.io.IOException;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ebay.lightning.core.beans.LightningRequest;
import com.ebay.lightning.core.beans.LightningResponse;
import com.ebay.lightning.core.beans.ReservationReceipt;
import com.ebay.lightning.core.beans.SystemStatus;
import com.ebay.lightning.core.config.SystemConfig;
import com.ebay.lightning.core.services.TaskExecutionService;
import com.ebay.lightning.core.store.LightningRequestReport;
import com.ebay.lightning.core.utils.ZipUtil;
import com.google.common.base.Preconditions;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

/**
 * REST based controller for the client to communicate with the seed. 
 * 
 * @author shashukla
 */
@RestController
@RequestMapping("l")
public class LightningController {

	private static final Logger log = Logger.getLogger(LightningController.class);

	public LightningController() {
		log.info("started");
	}

	/**
	 * ECV check URL.
	 * @return the ECV response
	 */
	@RequestMapping(value = "/ecv", method = {RequestMethod.GET,RequestMethod.HEAD}, produces = "application/json")
	public String reserve() {
		return "OK";
	}

	@Autowired
	TaskExecutionService taskExecutionService;
	
	public void setTaskExecutionService(TaskExecutionService taskExecutionService){
		this.taskExecutionService = taskExecutionService;
	}

	/**
	 * Submit a request.
	 * @param request request to submit
	 * @return the submit response
	 */
	@RequestMapping(value = "/submit", method = RequestMethod.POST)
	public String submit(@RequestBody String request) {
		LightningRequest fromReq = null;
		JsonObject resp = new JsonObject();
		try {
			fromReq = (LightningRequest) ZipUtil.unZip(request, LightningRequest.class);
			taskExecutionService.submit(fromReq);
			resp.addProperty("status", "submitted");
			log.info("Submitted Request" + (fromReq == null ? "null" : fromReq.getSessionId()));
			return resp.toString();
		} catch (Exception e) {
			log.error("Error doing submit", e);
			resp.addProperty("status", "failed");
			resp.addProperty("msg", e.getMessage());
			throw new RuntimeException(e);
		}
	}

	/**
	 * Get reservation for the load.
	 * @param load the load
	 * @return the reservation receipt
	 */
	@RequestMapping(value = "/reserve/{load}", method = RequestMethod.GET, produces = "application/json")
	public ReservationReceipt reserve(@PathVariable(value = "load") Integer load) {
		ReservationReceipt reserve = null;
		try {
			Preconditions.checkNotNull(load, "Load cannot be null");
			reserve = taskExecutionService.reserve(load);
			log.info("Reservation generated:" + reserve);
			return reserve;
		} catch (Exception e) {
			log.error("Error in reservation", e);
			throw new RuntimeException(e);
		}
	}

	/**
	 * Poll the current status of the request in JSON format.
	 * @param sessionId the session id for the request
	 * @param pollDeltaOnly get full or changes only
	 * @return the status of request.
	 */
	@RequestMapping(value = "/poll/json/{sessionId}/{pollDeltaOnly}", method = RequestMethod.GET, produces = "application/json")
	public LightningResponse pollJson(@PathVariable(value = "sessionId") String sessionId,
			@PathVariable(value = "pollDeltaOnly") boolean pollDeltaOnly) {
		LightningResponse pollResults = null;
		try {
			pollResults = taskExecutionService.pollResponse(sessionId, pollDeltaOnly);
			return pollResults;
		} catch (Exception e) {
			log.error("Error in pollJson", e);
			throw new RuntimeException(e);
		}
	}

	/**
	 * Poll the current status of the request in compressed format.
	 * @param sessionId the session id for the request
	 * @param pollDeltaOnly get full or changes only
	 * @return the status of request.
	 */
	@RequestMapping(value = "/poll/{sessionId}/{pollDeltaOnly}", method = RequestMethod.GET, produces = "application/zip")
	public byte[] poll(@PathVariable(value = "sessionId") String sessionId, @PathVariable(value = "pollDeltaOnly") boolean pollDeltaOnly) {
		LightningResponse pollResults = null;
		
		try {
			pollResults = taskExecutionService.pollResponse(sessionId, pollDeltaOnly);
			byte[] zippedBytes = ZipUtil.zipAsByteArray(pollResults);
			return zippedBytes;
		} catch (IOException e) {
			log.error("Error in poll", e);
			throw new RuntimeException(e);
		}
	}

	/**
	 * Get the detailed report for the request in compressed format.
	 * @param sessionId the session id for the request
	 * @return the detailed report for the request
	 */
	@RequestMapping(value = "/audit/{sessionId}", method = RequestMethod.GET, produces = "application/zip")
	public byte[] audit(@PathVariable(value = "sessionId") String sessionId) {
		LightningRequestReport report = null;
		
		try {
			report = taskExecutionService.getReport(sessionId);
			byte[] zippedBytes = ZipUtil.zipAsByteArray(report);
			return zippedBytes;
		} catch (IOException e) {
			log.error("Error in Audit", e);
			throw new RuntimeException(e);
		}
	}

	/**
	 * Get the detailed report for the request in JSON format.
	 * @param sessionId the session id for the request
	 * @return the detailed report for the request
	 */
	@RequestMapping(value = "/audit/json/{sessionId}", method = RequestMethod.GET, produces = "application/json")
	public LightningRequestReport auditJson(@PathVariable(value = "sessionId") String sessionId) {
		LightningRequestReport report = null;
		
		try {
			report = taskExecutionService.getReport(sessionId);
			return report;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Get the list of detailed reports in store.
	 * @param sessionId the session id
	 * @return the list of detailed reports
	 */
	@RequestMapping(value = "/auditSummary", method = RequestMethod.GET, produces = "application/zip")
	public byte[] auditSummary(@RequestParam(value = "sessionId", required = false, defaultValue = "") String sessionId) {
		List<LightningRequestReport> reports = null;

		try {
			reports = taskExecutionService.getAuditReports(sessionId);
			byte[] zippedBytes = ZipUtil.zipAsByteArray(reports);
			return zippedBytes;
		} catch (IOException e) {
			log.error("Error in Audit", e);
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Get the system metrics including CPU, physical and JVM memory.
	 * @return the lightning stats
	 */
	@RequestMapping(value = "/lightningStats", method = RequestMethod.GET, produces = "application/zip")
	public byte[] getLightningStats() {
		SystemStatus report = null;

		try {
			report = taskExecutionService.getLightningStats();
			byte[] zippedBytes = ZipUtil.zipAsByteArray(report);
			return zippedBytes;
		} catch (IOException e) {
			log.error("Error in Audit", e);
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Update the system configuration including capacity, retention policy etc
	 * @param payload the {@code SystemConfig} in JSON format
	 * @return the system configuration after update
	 */
	@RequestMapping(value = "/updateSystemConfig", method = RequestMethod.POST, produces = "application/json")
	public SystemConfig updateSystemConfig(@RequestBody String payload) {

		SystemConfig config = null;
		try {
			config = taskExecutionService.updateSystemConfig(new Gson().fromJson(payload, SystemConfig.class));
			return config;
		} catch (Exception e) {
			log.error("Error in Update SystemConfig");
			throw new RuntimeException(e.getMessage());
		}
	}

	/**
	 * Get the system configuration including capacity, retention policy etc
	 * @return the current system configuration
	 */
	@RequestMapping(value = "/getSystemConfig", method = RequestMethod.GET, produces = "application/json")
	public String getSystemConfig() {

		SystemConfig config = null;
		try {
			config = taskExecutionService.getSystemConfig();
			return new Gson().toJson(config).toString();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}