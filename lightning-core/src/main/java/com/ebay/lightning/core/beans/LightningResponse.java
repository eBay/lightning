package com.ebay.lightning.core.beans;

import java.io.Serializable;
import java.util.Map;
import java.util.Map.Entry;

import com.ebay.lightning.core.constants.LightningCoreConstants.WorkStatus;

/**
 * The {@code LightningResponse} class defines the interface for the task agreed by the client and core.
 * The class also contains {@link SuccessResponse} to define a successful task, which is identified by a
 * HTTP response code of '200' and {@link FailedResponse} to define a failed task.
 *
 * @author shashukla
 * @see LightningRequest
 * @see SuccessResponse
 * @see FailedResponse
 * @see BatchReport
 */
public class LightningResponse implements Serializable {
	private static final long serialVersionUID = 1L;

	private String sessionId;
	private int totalCount;
	private int successCount;
	private WorkStatus status;
	private Map<Integer, FailedResponse> failedResponses;
	private Map<Integer, SuccessResponse> successResponses;

	public LightningResponse(String sessionId, WorkStatus status) {
		this.sessionId = sessionId;
		this.status = status;
	}

	/**
	 * Get the sessionId of the request.
	 * @return the sessionId of the request
	 */
	public String getSessionId() {
		return this.sessionId;
	}

	/**
	 * Set the sessionId of the request.
	 * @param sessionId the sessionId to set
	 */
	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	/**
	 * Get the total count of tasks executed.
	 * @return the total count of tasks executed
	 */
	public int getTotalCount() {
		return this.totalCount;
	}

	/**
	 * Set the total count of tasks executed.
	 * @param totalCount the total count of tasks executed
	 */
	public void setTotalCount(int totalCount) {
		this.totalCount = totalCount;
	}

	/**
	 * Get the successful count of tasks executed.
	 * @return the successful count of tasks executed
	 */
	public int getSuccessCount() {
		return this.successCount;
	}

	/**
	 * Set the successful count of tasks executed.
	 * @param successCount the successful count of tasks executed
	 */
	public void setSuccessCount(int successCount) {
		this.successCount = successCount;
	}

	/**
	 * Get the {@link WorkStatus} of the request.
	 *
	 * The status can be one of (IN_QUEUE, RUNNING, DONE, STOPPED, CLEANED_UP)
	 * @return the {@link WorkStatus} of the request
	 */
	public WorkStatus getStatus() {
		return this.status;
	}

	/**
	 * Set the {@link WorkStatus} of the request.
	 *
	 * The status can be one of (IN_QUEUE, RUNNING, DONE, STOPPED, CLEANED_UP)
	 * @param status the {@link WorkStatus} of the request
	 */
	public void setStatus(WorkStatus status) {
		this.status = status;
	}

	/**
	 * Check of the request is completed.
	 * @return true if the all tasks in the request are completed.
	 */
	public boolean isCompleted() {
		return WorkStatus.DONE.equals(this.status) || WorkStatus.STOPPED.equals(this.status);
	}

	/* (non-Javadoc)
	 * @see {@link Object#toString()}
	 */
	@Override
	public String toString() {
		return "LightningResponse [Id=" + this.sessionId + ", success/total=" + this.successCount + "/" + this.totalCount + ", status=" + this.status + "]";
	}

	public String prettyPrint(){
		final String seperator = System.getProperty("line.separator");
		final StringBuilder builder = new StringBuilder();
		builder.append(toString()).append(seperator);
		if(this.successResponses!=null && this.successResponses.size()>0){
			builder.append("Successful Response").append(seperator).append("********************").append(seperator);
			for(final Entry<Integer, SuccessResponse> response : this.successResponses.entrySet()) {
				builder.append(response.getKey()+ " -> " + response.getValue()).append(seperator);
			}
		}
		if(this.failedResponses!=null && this.failedResponses.size()>0){
			builder.append("Failed Response").append(seperator).append("****************").append(seperator);
			for(final Entry<Integer, FailedResponse> response : this.failedResponses.entrySet()) {
				builder.append(response.getKey()+ " -> " + response.getValue()).append(seperator);
			}
		}
		return builder.toString();
	}

	/**
	 * Get the list of failed response.
	 * @return the list of failed response
	 */
	public Map<Integer, FailedResponse> getFailedResponses() {
		return this.failedResponses;
	}

	/**
	 * Set the list of failed response.
	 * @param failedResponses the list of failed response
	 */
	public void setFailedResponses(Map<Integer, FailedResponse> failedResponses) {
		this.failedResponses = failedResponses;
	}

	/**
	 * Get the list of successful response.
	 * @return the list of successful response
	 */
	public Map<Integer, SuccessResponse> getSuccessResponses() {
		return this.successResponses;
	}

	/**
	 * Set the list of successful response.
	 * @param successResponses the list of successful response
	 */
	public void setSuccessResponses(Map<Integer, SuccessResponse> successResponses) {
		this.successResponses = successResponses;
	}

	public static class FailedResponse implements Serializable {
		private static final long serialVersionUID = 1L;
		private final int statusCode;
		private final String errMsg;

		/**
		 * Initialized a failed response.
		 * @param statusCode the HTTP status code
		 * @param errMsg the error message
		 */
		public FailedResponse(int statusCode, String errMsg) {
			this.statusCode = statusCode;
			this.errMsg = errMsg;
		}

		/**
		 * Get the error message related to the failure.
		 * @return the error message
		 */
		public String getErrMsg() {
			return this.errMsg;
		}

		/**
		 * Get the HTTP status code.
		 * @return the HTTP status code
		 */
		public int getStatusCode() {
			return this.statusCode;
		}

		/* (non-Javadoc)
		 * @see {@link Object#toString()}
		 */
		@Override
		public String toString() {
			return String.format("Status [%d], Error Msg [%s]", this.statusCode, this.errMsg);
		}
	}

	public static class SuccessResponse implements Serializable {
		private static final long serialVersionUID = 1L;
		private static final int maxFormatLength = 10;
		private String body;

		/**
		 * Initializes a successful response.
		 * @param body the response content
		 */
		public SuccessResponse(String body) {
			this.body = body;
		}

		/**
		 * Get the response body content.
		 * @return the response body content
		 */
		public String getBody() {
			return this.body;
		}

		/**
		 * Set the response body content.
		 * @param body the response body content
		 */
		public void setBody(String body) {
			this.body = body;
		}

		/* (non-Javadoc)
		 * @see {@link Object#toString()}
		 */
		@Override
		public String toString() {
			String trimBody = this.body;
			if(trimBody != null && trimBody.length()>maxFormatLength){
				trimBody =  trimBody.substring(0, maxFormatLength) + "...";
			}
			return String.format("Status [200], Body [%s]", trimBody);
		}
	}
}