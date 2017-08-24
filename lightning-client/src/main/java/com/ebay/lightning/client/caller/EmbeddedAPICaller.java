/**
 * 
 */
package com.ebay.lightning.client.caller;

import java.util.List;

import com.ebay.lightning.core.beans.LightningRequest;
import com.ebay.lightning.core.beans.LightningResponse;
import com.ebay.lightning.core.beans.ReservationReceipt;
import com.ebay.lightning.core.beans.SystemStatus;
import com.ebay.lightning.core.config.SystemConfig;
import com.ebay.lightning.core.services.TaskExecutionService;
import com.ebay.lightning.core.store.LightningRequestReport;

/**
 * Embedded version of {@link ServiceCaller}. This Class calls all the method on embedded service running on
 * the same JVM.
 * 
 * @author shashukla
 * @see RestAPICaller
 */
public class EmbeddedAPICaller implements ServiceCaller {

	private TaskExecutionService service;

	public EmbeddedAPICaller(TaskExecutionService embeddedService) {
		this.service = embeddedService;
	}

	/* (non-Javadoc)
	 * @see {@link ServiceCaller#reserve(int, String)}
	 */
	@Override
	public ReservationReceipt reserve(int forLoad, String serviceHostIp) {
		ReservationReceipt reservationReciept = null;
		try {
			reservationReciept = service.reserve(forLoad);
		} catch (Exception e) {
			throw new RuntimeException("Error Calling service: ", e);
		}
		return reservationReciept;
	}

	/* (non-Javadoc)
	 * @see {@link ServiceCaller#submit(LightningRequest, String)}
	 */
	@Override
	public boolean submit(LightningRequest request, String serviceHostIp) {
		boolean success = false;
		try {
			service.submit(request);
			success = true;
		} catch (Exception e) {
			throw new RuntimeException("Error Calling service: ", e);
		}
		return success;
	}

	/* (non-Javadoc)
	 * @see {@link ServiceCaller#pollResults(String, String, boolean)}
	 */
	@Override
	public LightningResponse pollResults(String sessionId, String serviceHostIp, boolean pollDeltaOnly) {
		try {
			LightningResponse resp = service.pollResponse(sessionId, pollDeltaOnly);
			return resp;
		} catch (Exception e) {
			throw new RuntimeException("Error Calling service: ", e);
		}
	}

	/* (non-Javadoc)
	 * @see {@link ServiceCaller#getAuditReport(String, String)
	 */
	@Override
	public LightningRequestReport getAuditReport(String sessionId, String serviceHostIp) {
		try {
			LightningRequestReport report = service.getReport(sessionId);
			return report;
		} catch (Exception e) {
			throw new RuntimeException("Error Calling service: ", e);
		}
	}

	/* (non-Javadoc)
	 * @see {@link ServiceCaller#getAuditJsonReport(String, String)}
	 */
	@Override
	public LightningRequestReport getAuditJsonReport(String sessionId, String serviceHostIp) {
		try {
			return service.getReport(sessionId);
		} catch (Exception e) {
			throw new RuntimeException("Error Calling service: ", e);
		}
	}

	/* (non-Javadoc)
	 * @see {@link ServiceCaller#getAuditSummary(String, String)}
	 */
	@Override
	public List<LightningRequestReport> getAuditSummary(String serviceHostIp, String sessionId) {
		try {
			return service.getAuditReports(sessionId);
		} catch (Exception e) {
			throw new RuntimeException("Error Calling service: ", e);
		}
	}

	/* (non-Javadoc)
	 * @see {@link ServiceCaller#updateSystemConfig(String, SystemConfig)}
	 */
	@Override
	public SystemConfig updateSystemConfig(String serviceHostIp, SystemConfig sysConfig) {
		try {
			return service.updateSystemConfig(sysConfig);
		} catch (Exception e) {
			throw new RuntimeException("Error Calling service: ", e);
		}
	}

	/* (non-Javadoc)
	 * @see {@link ServiceCaller#getSystemConfig(String)}
	 */
	@Override
	public SystemConfig getSystemConfig(String serviceHostIp) {
		try {
			return service.getSystemConfig();
		} catch (Exception e) {
			throw new RuntimeException("Error Calling service: ", e);
		}
	}

	/* (non-Javadoc)
	 * @see {@link ServiceCaller#getLightningStats(String)}
	 */
	@Override
	public SystemStatus getLightningStats(String serviceHostIp) {
		return service.getLightningStats();
	}
}