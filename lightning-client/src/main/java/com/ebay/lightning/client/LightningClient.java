/**
 *
 */
package com.ebay.lightning.client;

import java.util.AbstractMap.SimpleEntry;
import java.util.List;
import java.util.UUID;

import com.ebay.lightning.client.caller.LightningResponseCallback;
import com.ebay.lightning.client.caller.ServiceCaller;
import com.ebay.lightning.client.config.LightningClientConfig;
import com.ebay.lightning.core.beans.LightningRequest;
import com.ebay.lightning.core.beans.LightningResponse;
import com.ebay.lightning.core.beans.ReservationReceipt;
import com.ebay.lightning.core.beans.SystemStatus;
import com.ebay.lightning.core.beans.Task;
import com.ebay.lightning.core.config.RequestConfig;
import com.ebay.lightning.core.config.SystemConfig;
import com.ebay.lightning.core.store.LightningRequestReport;

/**
 * {@code LightningClient} provides the interface for users to submit requests, poll response, see audit data etc to/from lightning core.
 * Additionally, the interface provides functionality to read and update configuration at runtime.
 *
 * @author shashukla
 * @see LightningClientImpl
 */
public interface LightningClient {

	/**
	 * Submit a list of tasks to be executed asynchronously at high speed.
	 * @param tasks list of {@code Task} to be executed asynchronously
	 * @return {@code LightningRequest} that contain {@code sessionId}, {@code ReservationReceipt} etc
	 */
	LightningRequest submit(List<Task> tasks);

	/**
	 * Submit a list of tasks to be executed asynchronously and configuration parameters.
	 * @param tasks list of {@code Task} to be executed asynchronously
	 * @param requestconfig configuration parameters to execute the task
	 * @return {@code LightningRequest} that contain {@code sessionId}, {@code ReservationReceipt} etc
	 */
	LightningRequest submit(List<Task> tasks, RequestConfig requestconfig);

	/**
	 * Submit a list of tasks to be executed asynchronously at high speed.
	 * @param tasks list of {@code Task} to be executed asynchronously
	 * @param callback {@code LightningResponseCallback} to invoke on completion of request or timeout.
	 * @param timeoutInMillis timeout for callback
	 */
	void submitWithCallback(List<Task> tasks, LightningResponseCallback callback, final long timeoutInMillis);

	/**
	 * Submit a list of tasks to be executed asynchronously and configuration parameters.
	 * @param tasks list of {@code Task} to be executed asynchronously
	 * @param requestconfig configuration parameters to execute the task
	 * @param callback {@code LightningResponseCallback} to invoke on completion of request or timeout.
	 * @param timeoutInMillis timeout for callback
	 */
	void submitWithCallback(List<Task> tasks, RequestConfig requestconfig, LightningResponseCallback callback,
			final long timeoutInMillis);

	/**
	 * Poll the current result for request.
	 *
	 * The method returns the complete or partial response based on the current state of the request.
	 * @param req the {@code LightningRequest} object returned by {@link #submit(List)} method
	 * @param pollDeltaOnly get full or delta response
	 * @return {@code LightningResponse} that has the current state of task execution
	 */
	LightningResponse pollResponse(LightningRequest req, boolean pollDeltaOnly);

	/**
	 * Get the detailed execution report for the request in compressed format.
	 *
	 * This method is usually called after the complete execution of the request. The execution state of
	 * the method can be polled with {@link #pollResponse(LightningRequest, boolean)} method.
	 * @param req the {@code LightningRequest} object returned by {@link #submit(List)} method
	 * @return {@code LightningRequestReport} that has the detailed execution report
	 */
	LightningRequestReport getAuditReport(LightningRequest req);

	/**
	 * Get the detailed execution report for the request in compressed format.
	 *
	 * This method is usually called after the complete execution of the request. The execution state of
	 * the method can be polled with {@link #pollResponse(LightningRequest, boolean)} method.
	 * @param sessionId the {@code sessionId} from {@code LightningRequest}
	 * @param servingHostIp the lightning core host that executed the task with the specific {@code sessionId}
	 * @return {@code LightningRequestReport} that has the detailed execution report in compressed format
	 */
	LightningRequestReport getAuditReport(String sessionId, String servingHostIp);

	/**
	 * Get the detailed execution report for the request in JSON format.
	 *
	 * This method is usually called after the complete execution of the request. The execution state of
	 * the method can be polled with {@link #pollResponse(LightningRequest, boolean)} method.
	 * @param sessionId the {@code sessionId} from {@code LightningRequest}
	 * @param servingHostIp the lightning core host that executed the task with the specific {@code sessionId}
	 * @return {@code LightningRequestReport} that has the detailed execution report in JSON format
	 */
	LightningRequestReport getAuditJsonReport(String sessionId, String servingHostIp);

	/**
	 * Get all detailed execution report in compressed format.
	 * @param sessionId the {@code sessionId} from {@code LightningRequest}
	 * @param servingHostIp the lightning core host for which audit summary need to be fetched
	 * @return list of {@code LightningRequestReport} that has the all detailed execution report in compressed format
	 */
	List<LightningRequestReport> getAuditSummary(String servingHostIp, String sessionId);

	/**
	 * Update system configuration parameters.
	 * @param servingHostIp the lightning core instance that has to be updated
	 * @param config the new {@code SystemConfig} parameters
	 * @return the {@code SystemConfig} instance after update
	 */
	SystemConfig updateSystemConfig(String servingHostIp, SystemConfig config);

	/**
	 * Get the system configuration parameters.
	 * @param servingHostIp the lightning core instance for which the parameters has to be fetched
	 * @return the current {@code SystemConfig} instance of the host represented by {@code servingHostIp}
	 */
	SystemConfig getSystemConfig(String servingHostIp);

	/**
	 * Get the lighting statistics.
	 * @param servingHostIp the lightning core instance for which lightning statistics has to be fetched
	 * @return the current {@code SystemStatus} instance of the host represented by {@code servingHostIp}
	 */
	SystemStatus getLightningStats(String servingHostIp);

	/**
	 * Get the lighting client configuration.
	 * @return the lightning client configuration {@code LightningClientConfig}
	 */
	LightningClientConfig getConfig();

	/**
	 * {@code LightningClientImpl} is the one and only implementation of {@link LightningClient} as of version 1.0
	 * This class is package protected to discourage direct instantiation. LightningClientBuild should be used to instantiate LightningClient
	 *
	 * @author shashukla
	 *
	 */
	static class LightningClientImpl implements LightningClient {

		private final ServiceCaller caller;
		private final ServiceHostResolver resolver;
		private final LightningClientConfig config;

		public LightningClientImpl(LightningClientConfig config, ServiceHostResolver resolver, ServiceCaller caller) {
			this.caller = caller;
			this.resolver = resolver;
			this.config = config;
		}

		/* (non-Javadoc)
		 * @see com.ebay.lightning.client.LightningClient#submit(java.util.List)
		 */
		@Override
		public LightningRequest submit(List<Task> tasks) {
			return submit(tasks, null);
		}

		/* (non-Javadoc)
		 * @see com.ebay.lightning.client.LightningClient#submit(java.util.List, com.ebay.lightning.core.config.RequestConfig)
		 */
		@Override
		public LightningRequest submit(List<Task> tasks, RequestConfig requestconfig) {
			final SimpleEntry<ReservationReceipt, String> resvIdEndpointPair = resolver.getNextEndPoint(tasks.size());
			final ReservationReceipt reservationReciept = resvIdEndpointPair.getKey();
			final String endPoint = resvIdEndpointPair.getValue();

			final LightningRequest req = new LightningRequest(UUID.randomUUID().toString(), tasks, reservationReciept);
			req.setServingHostIp(endPoint);
			req.setRequestconfig(requestconfig);
			caller.submit(req, endPoint);
			return req;
		}

		/* (non-Javadoc)
		 * @see com.ebay.lightning.client.LightningClient#pollResponse(com.ebay.lightning.core.beans.LightningRequest, boolean)
		 */
		@Override
		public LightningResponse pollResponse(LightningRequest req, boolean pollDeltaOnly) {
			return caller.pollResults(req.getSessionId(), req.getServingHostIp(), pollDeltaOnly);
		}

		/* (non-Javadoc)
		 * @see LightningClient#submitWithCallback(List, LightningResponseCallback, long)
		 */
		@Override
		public void submitWithCallback(List<Task> tasks, LightningResponseCallback callback, final long timeoutInMillis){
			submitWithCallback(tasks, null, callback, timeoutInMillis);
		}

		/* (non-Javadoc)
		 * @see LightningClient#submitWithCallback(List, RequestConfig, LightningResponseCallback, long)
		 */
		@Override
		public void submitWithCallback(List<Task> tasks, RequestConfig requestconfig, LightningResponseCallback callback,
				final long timeoutInMillis){
			final LightningRequest request = submit(tasks, requestconfig);
			addResponseCallback(request, callback, timeoutInMillis);
		}

		/**
		 * Register callback for request.
		 *
		 * Call this method to register on-complete and on-timeout callback.
		 * @param request the {@code LightningRequest} object returned by {@link #submit(List)} method
		 * @param callback {@code LightningResponseCallback} to invoke on completion of request or timeout.
		 * @param timeoutInMillis timeout for callback
		 */
		protected void addResponseCallback(final LightningRequest request, final LightningResponseCallback callback, final long timeoutInMillis) {
			new Thread(){
				@Override
				public void run() {
					boolean notTimedOut = true;
					final long startTime = System.currentTimeMillis();
					while (notTimedOut) {
						notTimedOut = (System.currentTimeMillis() - startTime) < timeoutInMillis;
						final LightningResponse pollResults = caller.pollResults(request.getSessionId(), request.getServingHostIp(), false);
						if (pollResults.isCompleted()) {
							callback.onComplete(caller.pollResults(request.getSessionId(), request.getServingHostIp(), false));
							return;
						}
					}
					callback.onTimeout(caller.pollResults(request.getSessionId(), request.getServingHostIp(), false));
				}
			}.start();
		}

		/* (non-Javadoc)
		 * @see com.ebay.lightning.client.LightningClient#getAuditReport(com.ebay.lightning.core.beans.LightningRequest)
		 */
		@Override
		public LightningRequestReport getAuditReport(LightningRequest req) {
			return caller.getAuditReport(req.getSessionId(), req.getServingHostIp());
		}

		/* (non-Javadoc)
		 * @see com.ebay.lightning.client.LightningClient#getAuditReport(java.lang.String, java.lang.String)
		 */
		@Override
		public LightningRequestReport getAuditReport(String sessionId, String servingHostIp) {
			return caller.getAuditReport(sessionId, servingHostIp);
		}

		/* (non-Javadoc)
		 * @see com.ebay.lightning.client.LightningClient#getAuditJsonReport(java.lang.String, java.lang.String)
		 */
		@Override
		public LightningRequestReport getAuditJsonReport(String sessionId, String servingHostIp) {
			return caller.getAuditJsonReport(sessionId, servingHostIp);
		}

		/* (non-Javadoc)
		 * @see com.ebay.lightning.client.LightningClient#getAuditSummary(java.lang.String, java.lang.String)
		 */
		@Override
		public List<LightningRequestReport> getAuditSummary(String servingHostIp, String sessionId) {
			return caller.getAuditSummary(servingHostIp, sessionId);
		}

		/* (non-Javadoc)
		 * @see com.ebay.lightning.client.LightningClient#updateSystemConfig(java.lang.String, com.ebay.lightning.core.config.SystemConfig)
		 */
		@Override
		public SystemConfig updateSystemConfig(String servingHostIp, SystemConfig config) {
			return caller.updateSystemConfig(servingHostIp, config);
		}

		/* (non-Javadoc)
		 * @see com.ebay.lightning.client.LightningClient#getSystemConfig(java.lang.String)
		 */
		@Override
		public SystemConfig getSystemConfig(String servingHostIp) {
			return caller.getSystemConfig(servingHostIp);
		}

		/* (non-Javadoc)
		 * @see com.ebay.lightning.client.LightningClient#getLightningStats(java.lang.String)
		 */
		@Override
		public SystemStatus getLightningStats(String servingHostIp) {
			return caller.getLightningStats(servingHostIp);
		}

		/* (non-Javadoc)
		 * @see com.ebay.lightning.client.LightningClient#getConfig()
		 */
		@Override
		public LightningClientConfig getConfig() {
			return config;
		}

	}

}