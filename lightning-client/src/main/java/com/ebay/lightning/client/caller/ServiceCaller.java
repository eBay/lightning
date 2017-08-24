package com.ebay.lightning.client.caller;

import java.util.List;

import com.ebay.lightning.core.beans.LightningRequest;
import com.ebay.lightning.core.beans.LightningResponse;
import com.ebay.lightning.core.beans.ReservationReceipt;
import com.ebay.lightning.core.beans.SystemStatus;
import com.ebay.lightning.core.config.SystemConfig;
import com.ebay.lightning.core.store.LightningRequestReport;

/**
 * {@code ServiceCaller} acts as a interface between the client and core (aka seeds). This interface provides the 
 * methods to make reservation, submit tasks, poll the task response, get audit data, view/edit configuration etc.
 * 
 * @author shashukla
 * @see RestAPICaller
 * @see EmbeddedAPICaller
 */
public interface ServiceCaller {

	/**
	 * Attempts to reserve task execution bandwidth on the seed
	 * @param count load size of the request
	 * @param serviceHostIp the seed
	 * @return reservation receipt for the requested bandwidth. The seed might return ACCEPTED, DENIED or BUSY
	 * based on the current work load size of the seed.
	 */
	ReservationReceipt reserve(int count, String serviceHostIp);

	/**
	 * Get the audit report for the request id in compressed format.
	 * @param sessionId the session id submitted through {@link #submit(LightningRequest, String)} method or 
	 * returned by the {@link #reserve(int, String)} method
	 * @param serviceHostIp the seed
	 * @return the audit report for the session id in compressed format. Returns the consolidated report if the
	 * session id is not present in the seed
	 */
	LightningRequestReport getAuditReport(String sessionId, String serviceHostIp);
	
	/**
	 * Get the audit report for the request id in JSON format.
	 * @param sessionId the session id submitted through {@link #submit(LightningRequest, String)} method or 
	 * returned by the {@link #reserve(int, String)} method
	 * @param serviceHostIp the seed
	 * @return the audit report for the session id in JSON format. Returns the consolidated report if the
	 * session id is not present in the seed
	 */
	LightningRequestReport getAuditJsonReport(String sessionId, String serviceHostIp);
	
	/**
	 * Get the audit summary in compressed format.
	 * @param sessionId the session id submitted through {@link #submit(LightningRequest, String)} method or 
	 * returned by the {@link #reserve(int, String)} method
	 * @param serviceHostIp the seed
	 * @return the audit summary in compressed format.
	 */
	List<LightningRequestReport> getAuditSummary(String serviceHostIp, String sessionId);
	
	/**
	 * Get the system statistics of the seed
	 * @param servingHostIp the seed
	 * @return the {@link SystemStatus} that include information related to load, memory, CPU etc
	 */
	SystemStatus getLightningStats(String servingHostIp);

	/**
	 * Submits the request to the specific seed.
	 * @param request the request with the reservation receipt and task list
	 * @param serviceHostIp the seed that executes the request
	 * @return {@code true} if the submit operation is successful
	 */
	boolean submit(LightningRequest request, String serviceHostIp);

	/**
	 * Update the configuration of the seed
	 * @param serviceHostIp the seed
	 * @param sysConfig the configuration changes to be applied
	 * @return the latest {@link SystemConfig} after updating
	 */
	SystemConfig updateSystemConfig(String serviceHostIp, SystemConfig sysConfig);
	
	/**
	 * Get the configuration of the seed
	 * @param serviceHostIp the seed
	 * @return the {@link SystemConfig} that includes the seed capacity, retention policy etc
	 */
	SystemConfig getSystemConfig(String serviceHostIp);

	/**
	 * Polls the results for a given session ID
	 * @param sessionId
	 * @return
	 * @throws Exception 
	 */
	/**
	 * Get the result for the session id.
	 * @param sessionId the session id submitted through {@link #submit(LightningRequest, String)} method or 
	 * returned by the {@link #reserve(int, String)} method
	 * @param serviceHostIp the seed
	 * @param pollDeltaOnly get the complete or only the delta
	 * @return the result for the request corresponding to sessionId
	 */
	LightningResponse pollResults(String sessionId, String serviceHostIp, boolean pollDeltaOnly);	
}
