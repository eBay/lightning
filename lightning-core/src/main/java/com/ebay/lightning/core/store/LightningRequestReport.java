package com.ebay.lightning.core.store;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.ebay.lightning.core.beans.BatchReport;
import com.ebay.lightning.core.beans.LightningRequest;
import com.ebay.lightning.core.beans.LightningResponse;
import com.ebay.lightning.core.beans.LightningResponse.FailedResponse;
import com.ebay.lightning.core.beans.LightningResponse.SuccessResponse;
import com.ebay.lightning.core.beans.Task;
import com.ebay.lightning.core.beans.URLTask;
import com.ebay.lightning.core.constants.LightningCoreConstants.WorkStatus;
import com.google.common.base.Preconditions;

/**
 * The {@code LightningRequestReport} class holds the execution summary and the detailed output of the request.
 * 
 * @author shashukla
 */
public class LightningRequestReport implements Serializable {

	private static final long serialVersionUID = 1L;

	private LightningRequest request;
	private Map<Integer, BatchReport> batchReport = new ConcurrentHashMap<Integer, BatchReport>();
	private WorkStatus status;
	private long workEnqueueTime;
	private long workDequeueTime;
	private Long totalExecutionTimeInMillis;
	private Long lastReportGenerationTime = 0L;
	private Long processStartTime;
	private long processEndTime;
	private LightningResponse response;
	
	public LightningRequestReport(){
		
	}

	/**
	 * Initialize the report with the submitted request.
	 * @param request the submitted request
	 */
	public LightningRequestReport(LightningRequest request) {
		this.request = request;
	}

	/**
	 * Get the submitted request.
	 * @return the submitted request
	 */
	public LightningRequest getRequest() {
		return request;
	}

	/**
	 * Set the submitted request.
	 * @param request the submitted request
	 */
	public void setRequest(LightningRequest request) {
		this.request = request;
	}

	/**
	 * Get the current work status.
	 * 
	 * The status could be either of the states IN_QUEUE, RUNNING, DONE, STOPPED, CLEANED_UP
	 * @return the current status of the request
	 */
	public WorkStatus getStatus() {
		return status;
	}

	/**
	 * Set the current work status.
	 * @param status the current status of the request
	 */
	public void setStatus(WorkStatus status) {
		this.status = status;
	}

	/**
	 * Get the time request got dequeued.
	 * @return the time request got dequeued
	 */
	public Long getWorkDequeueTime() {
		return workDequeueTime;
	}

	/**
	 * Set the dequeue time for the request.
	 * @param workDequeueTime the request dequeue time
	 */
	public void setWorkDequeueTime(Long workDequeueTime) {
		this.workDequeueTime = workDequeueTime;
	}

	/**
	 * Get the time request got enqueued.
	 * @return the time request got enqueued
	 */
	public Long getWorkEnqueueTime() {
		return workEnqueueTime;
	}

	/**
	 * Set the enqueue time for the request.
	 * @param workEnqueueTime the request enqueue time
	 */
	public void setWorkEnqueueTime(Long workEnqueueTime) {
		this.workEnqueueTime = workEnqueueTime;
	}

	/**
	 * Generate the response for the request.
	 * @param pollDeltaOnly gets only the latest changes if set to {@code true}; gets the complete response if
	 * set to {@code false}
	 * @return the response for the request
	 */
	LightningResponse generateResposne(boolean pollDeltaOnly) {
		Preconditions.checkNotNull(request.getTasks(), "Response cannot be generated because request.getTasks()");
		Long thisReportGenerationTime = System.currentTimeMillis();
		LightningResponse lightningResponse = new LightningResponse(request.getSessionId(), status);
		int index = 0;
		Map<Integer, FailedResponse> failedResponses = new HashMap<>();
		Map<Integer, SuccessResponse> successResponses = new HashMap<>();
		int successCount = 0;
		for (Task task : request.getTasks()) {
			boolean isTaskUpdatedAfterLastRepGen = false;
			boolean isTaskUpdatedbeforeThisRepGenStart = false;
			if (task.getLastTaskStatusUpdateTime() != null) {
				isTaskUpdatedAfterLastRepGen = task.getLastTaskStatusUpdateTime() > lastReportGenerationTime;
				isTaskUpdatedbeforeThisRepGenStart = task.getLastTaskStatusUpdateTime() <= thisReportGenerationTime;
			}
			boolean addDeltaDetails = !pollDeltaOnly || (isTaskUpdatedAfterLastRepGen && isTaskUpdatedbeforeThisRepGenStart);
			if (task.getStatus() != null && addDeltaDetails) {
				switch (task.getStatus()) {
				case CONNECT_FAILED:
				case FAILED:
				case READ_WRITE_FAILED:
					failedResponses.put(index, new LightningResponse.FailedResponse(((URLTask) task).getStatusCode(), task.getErrorMsg()));
					break;
				case TIMEDOUT:
					if(isWorkCompleted(status)) {
						failedResponses.put(index, new LightningResponse.FailedResponse(((URLTask) task).getStatusCode(), task.getErrorMsg()));
					}
					break;
				case SUCCESS:
					successResponses.put(index, new LightningResponse.SuccessResponse(((URLTask) task).getBody()));
					((URLTask) task).setBody("");
					successCount++;
					break;
				default:
					break;
				}
			}
			index++;
		}
		lastReportGenerationTime = thisReportGenerationTime;
		lightningResponse.setFailedResponses(failedResponses);
		lightningResponse.setSuccessResponses(successResponses);
		lightningResponse.setTotalCount(request.getTasks().size());
		lightningResponse.setSuccessCount(successCount);
		lightningResponse.setStatus(status);
		return lightningResponse;
	}

	/* (non-Javadoc)
	 * @see {@link Object#toString()}
	 */
	@Override
	public String toString() {
		return "** RequestReport ** \nsessionId: " + request.getSessionId() + ", QTime:ExecTime(ms): " + (workDequeueTime - workEnqueueTime) + ":"
				+ totalExecutionTimeInMillis + ", \n" + batchReport + ", status=" + status + ", QueueTime(ms)=" + (workDequeueTime - workEnqueueTime)
				+ ", totalExecutionTimeInMillis=" + totalExecutionTimeInMillis + "]";
	}

	/**
	 * Get all the batch reports.
	 * @return the batch reports
	 */
	public Map<Integer, BatchReport> getBatchReport() {
		return batchReport;
	}

	/**
	 * Set all the batch reports.
	 * @param batchReport the list of batch reports
	 */
	public void setBatchReport(Map<Integer, BatchReport> batchReport) {
		this.batchReport = batchReport;
	}

	/**
	 * Get the total execution time for the request.
	 * @return the total execution time for the request
	 */
	public Long getTotalExecutionTimeInMillis() {
		return totalExecutionTimeInMillis;
	}

	/**
	 * Set the total execution time for the request.
	 * @param totalExecutionTimeInMillis the total execution time for the request
	 */
	public void setTotalExecutionTimeInMillis(Long totalExecutionTimeInMillis) {
		this.totalExecutionTimeInMillis = totalExecutionTimeInMillis;
	}
	
	/**
	 * Get the processing start time for the request.
	 * @return the processing start time for the request
	 */
	public Long getProcessStartTime() {
		return processStartTime;
	}
	
	/**
	 * Set the processing start time for the request.
	 * @param processStartTime the processing start time for the request
	 */
	public void setProcessStartTime(Long processStartTime) {
		this.processStartTime = processStartTime;
	}

	/**
	 * Get the processing end time for the request.
	 * @return the processing end time for the request
	 */
	public long getProcessEndTime() {
		return processEndTime;
	}

	/**
	 * Set the processing end time for the request.
	 * @param processEndTime the processing end time for the request
	 */
	public void setProcessEndTime(long processEndTime) {
		this.processEndTime = processEndTime;
	}

	/**
	 * Generate the final response and remove task details.
	 */
	void retainAudiDataOnly() {
		if (!WorkStatus.CLEANED_UP.equals(status)) {
			this.response = generateResposne(false);
			response.getFailedResponses().clear();
			response.getSuccessResponses().clear();
			request.getTasks().clear();
			this.status = WorkStatus.CLEANED_UP;
		}
	}
	
	/**
	 * Clone the audit data from the report.
	 * @param tmpReport the complete report
	 * @return the audit data only
	 */
	public static LightningRequestReport getAuditReport(LightningRequestReport tmpReport) {

		LightningRequestReport report = new LightningRequestReport();

		if (tmpReport != null && tmpReport.getRequest() != null && tmpReport.getRequest().getSessionId() != null) {
			report.setRequest(new LightningRequest(tmpReport.getRequest().getSessionId()));
			if (tmpReport.getStatus() != null) {
				report.setStatus(tmpReport.getStatus());
			}
			if (tmpReport.getWorkEnqueueTime() != null) {
				report.setWorkEnqueueTime(tmpReport.getWorkEnqueueTime());
			}
			if (tmpReport.getWorkDequeueTime() != null) {
				report.setWorkDequeueTime(tmpReport.getWorkDequeueTime());
			}
			if (tmpReport.getTotalExecutionTimeInMillis() != null) {
				report.setTotalExecutionTimeInMillis(tmpReport.getTotalExecutionTimeInMillis());
			}
			if (tmpReport.getBatchReport() != null) {
				report.setBatchReport(tmpReport.getBatchReport());
			}
			if(tmpReport.getWorkDequeueTime() != null && tmpReport.getTotalExecutionTimeInMillis() != null){
				report.setProcessEndTime(tmpReport.getWorkDequeueTime() + tmpReport.getTotalExecutionTimeInMillis());
			}
			if(tmpReport.getProcessStartTime() != null){
				report.setProcessStartTime(tmpReport.getProcessStartTime());
			}
		}

		return report;
	}

	/**
	 * Check if the request is completed.
	 * @return {@code true} if the request is completed; false otherwise
	 */
	private boolean isWorkCompleted(WorkStatus status) {
		return WorkStatus.DONE.equals(status) || WorkStatus.STOPPED.equals(status) || WorkStatus.CLEANED_UP.equals(status); 
	}
}
