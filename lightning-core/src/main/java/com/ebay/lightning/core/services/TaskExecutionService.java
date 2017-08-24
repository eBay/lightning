package com.ebay.lightning.core.services;

import java.util.List;

import com.ebay.lightning.core.beans.LightningRequest;
import com.ebay.lightning.core.beans.LightningResponse;
import com.ebay.lightning.core.beans.ReservationReceipt;
import com.ebay.lightning.core.beans.SystemStatus;
import com.ebay.lightning.core.config.SystemConfig;
import com.ebay.lightning.core.manager.TaskExecutionManager;
import com.ebay.lightning.core.store.LightningRequestReport;

/**
 * The {@code TaskExecutionService} interface defines the operations for processing the request and getting the response.
 * 
 * @author shashukla
 * @see TaskExecutionServiceImpl
 * @see TaskExecutionManager
 */
public interface TaskExecutionService {
	
	/**
	 * Submit a request to get executed asynchronously at high speed.
	 * 
	 * The request should contain the {@code ReservationReceipt} returned by the {@link #reserve(int)} call.
	 * @param request contains the list of {@code Task} and configuration to execute
	 */
	public void submit(LightningRequest request);

	/**
	 * Poll the current execution state for the request submitted by #{@link #submit(LightningRequest)} method.
	 * 
	 * This method id helpful to check if the request has completed.
	 * @param sessionId the session id of the {@code LightningRequest}
	 * @param pollDeltaOnly get full if {@code true} and delta response if {@code false}
	 * @return {@code LightningResponse} that has the current state of task execution
	 */
	public LightningResponse pollResponse(String sessionId, boolean pollDeltaOnly);

	/**
	 * Get the detailed execution report for the request.
	 * 
	 * This method is usually called after checking if the request is completed by calling
	 * {@link #pollResponse(String, boolean)}
	 * @param sessionId the session id of the {@code LightningRequest}
	 * @return {@code LightningRequestReport} that has the detailed execution report of the request 
	 */
	public LightningRequestReport getReport(String sessionId);
	
	/**
	 * Check for bandwidth to make reservation for the specified load.
	 * 
	 * @param load the load to check for reservation
	 * @return the reservation receipt containing either of the state ACCEPTED, DENIED, BUSY
	 */
	public ReservationReceipt reserve(int load);
	
	/**
	 * Get the detailed execution reports.
	 * 
	 * @param sessionId the session id of the request
	 * @return the list of {@code LightningRequestReport} available in the store 
	 */
	public List<LightningRequestReport> getAuditReports(String sessionId);
	
	/**
	 * Get the system metrics including CPU, physical and JVM memory.
	 * @return the current system status
	 */
	public SystemStatus getLightningStats();
	
	/**
	 * Update the system configuration including capacity, retention policy etc
	 * @param sysConfig the system configuration changes to make
	 * @return the system configuration after update
	 * @throws Exception when the update fails
	 */
	public SystemConfig updateSystemConfig(SystemConfig sysConfig) throws Exception;
	
	/**
	 * Get the system configuration including capacity, retention policy etc
	 * @return the current system configuration
	 */
	public SystemConfig getSystemConfig();
}
