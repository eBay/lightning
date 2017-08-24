/**
 * 
 */
package com.ebay.lightning.client.caller;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.ebay.lightning.client.config.LightningClientConfig;
import com.ebay.lightning.core.beans.LightningRequest;
import com.ebay.lightning.core.beans.LightningResponse;
import com.ebay.lightning.core.beans.ReservationReceipt;
import com.ebay.lightning.core.beans.SystemStatus;
import com.ebay.lightning.core.config.SystemConfig;
import com.ebay.lightning.core.store.LightningRequestReport;
import com.ebay.lightning.core.utils.UrlUtils;
import com.ebay.lightning.core.utils.ZipUtil;
import com.ebay.lightning.core.utils.UrlUtils.ContentType;
import com.google.common.base.Preconditions;
import com.google.gson.Gson;
import com.google.gson.JsonParser;

/**
 * The REST API based implementation of {@link ServiceCaller} that communicates with the seeds through REST calls.
 * 
 * @author shashukla
 * @see EmbeddedAPICaller
 */

public class RestAPICaller implements ServiceCaller {
	
	private static final Logger log = Logger.getLogger(RestAPICaller.class);

	private static final String HOST_VARIABLE_LITERAL = "{host}";

	private UrlUtils urlUtils;
	private LightningClientConfig config;

	public RestAPICaller(LightningClientConfig config, UrlUtils urlUtils) {
		this.urlUtils = urlUtils;
		this.config = config;
	}


	/* (non-Javadoc)
	 * see {@link ServiceCaller#reserve(int, String)}
	 */
	@Override
	public ReservationReceipt reserve(int forLoad, String serviceHostIp) {
		ReservationReceipt reservationReciept = null;
		String reserveApiUrl = config.getReserveApiUrl();
		try {
			reserveApiUrl = fillHostIP(config.getReserveApiUrl(), serviceHostIp) + "/" + forLoad;
			String response = urlUtils.get(reserveApiUrl);
			reservationReciept = new Gson().fromJson(response, ReservationReceipt.class);
		} catch (Exception e) {
			throw new RuntimeException("Error Calling Lightning Core @ URL: " + reserveApiUrl, e);
		}
		return reservationReciept;
	}

	private String fillHostIP(String reserveApiUrlTemplate, String serviceHostIp) {
		return reserveApiUrlTemplate.replace(HOST_VARIABLE_LITERAL, serviceHostIp);
	}

	/* (non-Javadoc)
	 * see {@link ServiceCaller#submit(LightningRequest, String)}
	 */
	@Override
	public boolean submit(LightningRequest request, String serviceHostIp) {
		boolean success = false;
		String url = fillHostIP(config.getSubmitApiUrl(), serviceHostIp);
		try {
			String payload = ZipUtil.zip(request);
			String response = urlUtils.post(url, ContentType.APPLICATION_JSON, null, payload);
			if ("submitted".equals(new JsonParser().parse(response).getAsJsonObject().get("status").getAsString())) {
				success = true;
			}
		} catch (Exception e) {
			throw new RuntimeException("Error Calling Lightning Core @URL: " + url, e);
		}
		return success;
	}

	/* (non-Javadoc)
	 * see {@link ServiceCaller#pollResults(String, String, boolean)}
	 */
	@Override
	public LightningResponse pollResults(String sessionId, String serviceHostIp, boolean pollDeltaOnly) {
		byte[] response;
		String url = fillHostIP(config.getPollApiUrl(), serviceHostIp) + "/" + sessionId + "/" + pollDeltaOnly;
		try {
			response = urlUtils.getByteArray(url);
			LightningResponse resp = (LightningResponse) ZipUtil.unZipByteArray(response, LightningResponse.class);
			return resp;
		} catch (Exception e) {
			log.error("Error Calling URL" + url, e);
		}
		return null;
	}
	
	/* (non-Javadoc)
	 * @see {@link ServiceCaller#getAuditReport(String, String)
	 */
	@Override
	public LightningRequestReport getAuditReport(String sessionId, String serviceHostIp) {
		byte[] response;
		Preconditions.checkNotNull(config.getAuditApiUrl(), "Audit API is null. Please configure Audit API url");
		String url = fillHostIP(config.getAuditApiUrl(), serviceHostIp) + "/" + sessionId;
		try {
			response = urlUtils.getByteArray(url);
			LightningRequestReport report = (LightningRequestReport) ZipUtil.unZipByteArray(response, LightningRequestReport.class);
			
			return report;
		} catch (Exception e) {
			log.error("Error Calling URL: " + url, e);
		}
		
		return null;
	}
	
	/* (non-Javadoc)
	 * @see {@link ServiceCaller#getAuditJsonReport(String, String)}
	 */
	@Override
	public LightningRequestReport getAuditJsonReport(String sessionId, String serviceHostIp) {
		String response = "";
		Preconditions.checkNotNull(config.getAuditJsonApiUrl(), "Audit Json API is null. Please configure Audit Json API url");
		String url = fillHostIP(config.getAuditJsonApiUrl(), serviceHostIp) + "/" + sessionId;
		try {
			response = urlUtils.get(url);
			return new Gson().fromJson(response, LightningRequestReport.class);
		} catch (Exception e) {
			log.error("Error Calling URL: " + url, e);
		}
		
		return null;
	}
	
	/* (non-Javadoc)
	 * @see {@link ServiceCaller#getAuditSummary(String, String)}
	 */
	@SuppressWarnings("unchecked")
	@Override
	public List<LightningRequestReport> getAuditSummary(String serviceHostIp, String sessionId) {
		byte[] response;
		Preconditions.checkNotNull(config.getAuditSummaryUrl(), "AuditSummary API is null. Please configure Audit Summary API url");
		
		String url = "";
		if(StringUtils.isNotEmpty(sessionId)){
			url = fillHostIP(config.getAuditSummaryUrl(), serviceHostIp) + "?sessionId=" + sessionId;
		} else {
			url = fillHostIP(config.getAuditSummaryUrl(), serviceHostIp);
		}
		
		try {
			response = urlUtils.getByteArray(url);
			List<LightningRequestReport> reports = (List<LightningRequestReport>) ZipUtil.unZipByteArray(response);
			
			return reports;
		} catch (Exception e) {
			log.error("Error Calling URL: " + url, e);
		}
		
		return null;
	}
	
	/* (non-Javadoc)
	 * @see {@link ServiceCaller#updateSystemConfig(String, SystemConfig)}
	 */
	@Override
	public SystemConfig updateSystemConfig(String serviceHostIp, SystemConfig sysConfig) {

		String response = "";
		Preconditions.checkNotNull(serviceHostIp, "Update SystemConfig: hostname is null");
		String url = fillHostIP(config.getSystemConfigUpdateUrl(), serviceHostIp);

		try {
			response = urlUtils.post(url, UrlUtils.ContentType.APPLICATION_JSON, null, new Gson().toJson(sysConfig));
			return new Gson().fromJson(response, SystemConfig.class);
		} catch (Exception e) {
			log.error("Error Calling system config update URL: " + url, e);
		}
		return null;
	}
	
	/* (non-Javadoc)
	 * @see {@link ServiceCaller#getSystemConfig(String)}
	 */
	@Override
	public SystemConfig getSystemConfig(String serviceHostIp) {
		String response = "";
		Preconditions.checkNotNull(serviceHostIp, "Update SystemConfig: hostname is null");
		String url = fillHostIP(config.getSystemConfigUrl(), serviceHostIp);

		try {
			response = urlUtils.get(url);
			return new Gson().fromJson(response, SystemConfig.class);
		} catch (Exception e) {
			log.error("Error Calling system config URL: " + url, e);
		}

		return null;
	}
	
	/* (non-Javadoc)
	 * @see {@link ServiceCaller#getLightningStats(String)}
	 */
	@Override
	public SystemStatus getLightningStats(String serviceHostIp) {
		byte[] response;
		Preconditions.checkNotNull(config.getLightningStatsUrl(),
				"Lightning Stats API is null. Please configure Lightning Stats API url");
		String url = fillHostIP(config.getLightningStatsUrl(), serviceHostIp);
		SystemStatus stats = null;
		try {
			response = urlUtils.getByteArray(url);
			stats = (SystemStatus) ZipUtil.unZipByteArray(response, SystemStatus.class);
			stats.setHostName(serviceHostIp);
			stats.setSystemHealth(true);
		} catch (Exception e) {
			log.error("Error Calling lightning stats URL: " + url, e);
			stats = new SystemStatus();
			stats.setHostName(serviceHostIp);
			stats.setSystemStartTime(Long.getLong("0"));
			stats.setSystemHealth(false);
			stats.setSystemStatusErrorMsg(e.toString());
		}

		return stats;
	}
}