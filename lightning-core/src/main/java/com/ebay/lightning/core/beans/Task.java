package com.ebay.lightning.core.beans;

import java.io.Serializable;

import com.ebay.lightning.core.constants.LightningCoreConstants.TaskStatus;

/**
 * The {@code Task} class holds task information like URL to be executed and the status of execution
 * of the task.
 * 
 * @author shashukla
 * @see URLTask
 * @see ChainedURLTask
 */

public class Task implements Serializable {
	private static final long serialVersionUID = 1L;

	private String url;
	private String errorMsg;
	private TaskStatus status;
	private transient Long lastTaskStatusUpdateTime = null;

	protected static final String DEFAULT_PROTOCOL = "http";
	protected static final int DEFAULT_PORT = 80;

	public Task() {
		super();
	}

	/**
	 * Get the current execution state of the task.
	 * 
	 * <p>Refer {@link TaskStatus} on the possible states for the task.</p>
	 * @return the current execution state of the task
	 */
	public TaskStatus getStatus() {
		return status;
	}

	/**
	 * Set the current execution state of the task.
	 * 
	 * <p>Refer {@link TaskStatus} on the possible states for the task.</p>
	 * @param status the current execution state of the task
	 */
	public void setStatus(TaskStatus status) {
		this.status = status;
		if(status!=null && status.ordinal()>3) {
			this.lastTaskStatusUpdateTime = System.currentTimeMillis();
		}
	}

	/**
	 * Get the error encountered during task execution.
	 * 
	 * @return the error if any encountered during task execution
	 */
	public String getErrorMsg() {
		return errorMsg;
	}

	/**
	 * Set the error encountered during task execution.
	 * 
	 * @param errorMsg the error if any encountered during task execution
	 */
	public void setErrorMsg(String errorMsg) {
		this.errorMsg = errorMsg;
	}

	/**
	 * Get the last update time when there is a change in task status.
	 * 
	 * @return the last update time when there is a change in task status 
	 */
	public Long getLastTaskStatusUpdateTime() {
		return lastTaskStatusUpdateTime;
	}

	/**
	 * Set the last update time when there is a change in task status.
	 * 
	 * @param lastTaskStatusUpdateTime the last update time when there is a change in task status 
	 */
	public void setLastTaskStatusUpdateTime(Long lastTaskStatusUpdateTime) {
		this.lastTaskStatusUpdateTime = lastTaskStatusUpdateTime;
	}

	/**
	 * Get the URL to be executed.
	 * 
	 * @return the URL to be executed 
	 */
	public String getUrl() {
		return url;
	}

	/**
	 * Set the URL to be executed.
	 * 
	 * @param url the URL to be executed 
	 */
	public void setUrl(String url) {
		this.url = url;
	}

	/**
	 * Check if the task failed.
	 * 
	 * @return true if the task failed
	 */
	public boolean hasFailed() {
		return (TaskStatus.CONNECT_FAILED.equals(status) ||
				TaskStatus.READ_WRITE_FAILED.equals(status) ||
				TaskStatus.FAILED.equals(status) ||
				TaskStatus.TIMEDOUT.equals(status));
	}
}
