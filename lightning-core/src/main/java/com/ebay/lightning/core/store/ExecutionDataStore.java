package com.ebay.lightning.core.store;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.ebay.lightning.core.async.Callback;
import com.ebay.lightning.core.async.Reminder;
import com.ebay.lightning.core.beans.LightningRequest;
import com.ebay.lightning.core.beans.LightningResponse;
import com.ebay.lightning.core.config.SystemConfig;
import com.ebay.lightning.core.config.SystemConfig.RetentionPolicy;
import com.ebay.lightning.core.utils.LightningCoreUtil;

/**
 * The {@code ExecutionDataStore} holds the report for the requests executed. The report gets cleaned based on
 * {@link SystemConfig#getAuditCleanupFrequencyInSec()} interval and {@link RetentionPolicy}.
 * 
 * @author shashukla
 */
@Component
public class ExecutionDataStore {

	private static final Logger log = Logger.getLogger(ExecutionDataStore.class);
	private final Map<String, LightningRequestReport> requestReportStore = new ConcurrentHashMap<>();
	private final SystemConfig config;
	private Reminder executionDataStoreCleanupReminder;

	/**
	 * Callback method for cleaning up the report store.
	 * @param sysConfig the configurations
	 */
	@Autowired
	public ExecutionDataStore(SystemConfig sysConfig) {
		this.config = sysConfig;
		executionDataStoreCleanupReminder = new Reminder("ExecutionDataStoreCleanupReminder", new Callback<String>() {
			@Override
			public void notify(String t) {
				try {
					RetentionPolicy retentionPolicy = config.getRetentionPolicy();
					TreeMap<Long, String> chronologicallyOrderedSessionIds = new TreeMap<Long, String>();

					//Time Based Cleanup
					for (Iterator<Entry<String, LightningRequestReport>> iterator = requestReportStore.entrySet().iterator(); iterator.hasNext();) {
						Entry<String, LightningRequestReport> e = iterator.next();
						LightningRequestReport report = e.getValue();
						long executionEndTime = report.getWorkDequeueTime() + report.getTotalExecutionTimeInMillis();
						long reportLifeTimeInMillis = System.currentTimeMillis() - executionEndTime;
						if (reportLifeTimeInMillis > retentionPolicy.getLightningReportRetentionTimeInMillis()) {
							//Clean Lightning Report
							log.info("Time Based Cleanup, actualLife/allowed=" + reportLifeTimeInMillis + "/"
									+ retentionPolicy.getLightningReportRetentionTimeInMillis() + ", Removing: " + e.getKey());
							iterator.remove();
						} else {
							if (reportLifeTimeInMillis > retentionPolicy.getTaskRetentionTimeInMillis()) {
								//Clean Task Details
								log.info("Time Based Cleanup, actualLife/allowed=" + reportLifeTimeInMillis + "/"
										+ retentionPolicy.getTaskRetentionTimeInMillis() + ", Removing tasks for: " + e.getKey());
								report.retainAudiDataOnly();
							}
							chronologicallyOrderedSessionIds.put(reportLifeTimeInMillis, e.getKey());
						}
					}

					//Count Based cleanup
					int index = 0;
					if (chronologicallyOrderedSessionIds.size() > retentionPolicy.getMaxLightningReportRetentionCount()) {
						for (Entry<Long, String> e : chronologicallyOrderedSessionIds.entrySet()) {
							if (++index > retentionPolicy.getMaxLightningReportRetentionCount()) {
								log.info("Count Based Cleanup, total/allowed=" + chronologicallyOrderedSessionIds.size() + "/"
										+ retentionPolicy.getMaxLightningReportRetentionCount() + " Removing: " + e.getValue());
								requestReportStore.remove(e.getValue());
							}
						}
					}
				} catch (Exception e) {
					log.fatal("Execution data store cleanup failed", e);
				}
			}

		}, (long) config.getAuditCleanupFrequencyInSec(), true);
	}

	/**
	 * Poll the response for the request.
	 * @param sessionId the request id
	 * @param pollDeltaOnly poll the full or latest response
	 * @return the response for the request
	 */
	public LightningResponse pollResults(String sessionId, boolean pollDeltaOnly) {
		LightningRequestReport report = requestReportStore.get(sessionId);
		if (report != null) {
			return report.generateResposne(pollDeltaOnly);
		} else {
			return null;
		}
	}

	/**
	 * Get the detailed response for the request.
	 * @param sessionId the request id
	 * @return the detailed response for the request
	 */
	public LightningRequestReport getReport(String sessionId) {
		return requestReportStore.get(sessionId);
	}

	/**
	 * Get the audit data for the request.
	 * @param sessionId the session id
	 * @return the audit data
	 */
	public List<LightningRequestReport> getAuditReports(String sessionId) {
		return LightningCoreUtil.getAuditReports(requestReportStore, sessionId);
	}

	/**
	 * Register the request with the store.
	 * @param request the request to register
	 * @return the audit data for the request
	 */
	public LightningRequestReport register(LightningRequest request) {
		LightningRequestReport report = new LightningRequestReport(request);
		requestReportStore.put(request.getSessionId(), report);
		return report;
	}
}