package com.ebay.lightning.core.beans;

import java.io.Serializable;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.ebay.lightning.core.config.RequestConfig;

/**
 * The {@code LightningRequest} class defines the interface for the task agreed by the client and core.
 * 
 * @author shashukla
 * @see LightningResponse
 */
public class LightningRequest implements Serializable{
	private static final long serialVersionUID = 1L;
	
	private String sessionId;
	private List<Task> tasks;
	private String requestType;
	private ReservationReceipt reservationReciept;
	private int requestSize;
	private String servingHostIp;
	private RequestConfig requestconfig = null;
	
	public LightningRequest(String sessionId){
		setSessionId(sessionId);
	}
	
	public LightningRequest(String sessionId, List<Task> tasks, ReservationReceipt reservationReciept) {
		setSessionId(sessionId);
		setTasks(tasks);
		setReservationReciept(reservationReciept);
	}

	/**
	 * Get the sessionId of the request.
	 * @return the sessionId of the request
	 */
	public String getSessionId() {
		return sessionId;
	}

	/**
	 * Set the sessionId of the request.
	 * @param sessionId the sessionId to set
	 */
	public void setSessionId(String sessionId) {
		if (StringUtils.isEmpty(sessionId)) {
            throw new IllegalArgumentException("Invalid session id.  Lightning request should be formed with valid unique sessionId.");
		}
		this.sessionId = sessionId;
	}

	/**
	 * Get the list of task to be executed.
	 * @return the list of task to be executed
	 */
	public List<Task> getTasks() {
		return tasks;
	}

	/**
	 * Set the list of task to be executed.
	 * @param tasks the task to be executed
	 */
	public void setTasks(List<Task> tasks) {
		if (tasks != null && tasks.size() != 0) {
			this.tasks = tasks;
			this.requestSize = tasks.size();
		} else {
            throw new IllegalArgumentException("Invalid set of tasks. Lightning request should be at least with one valid Task.");
		}
	}

	/**
	 * Get the request type.
	 * @return the request type
	 */
	public String getRequestType() {
		return requestType;
	}

	/**
	 * Set the request type.
	 * @param requestType the request type
	 */
	public void setRequestType(String requestType) {
		this.requestType = requestType;
	}

	/**
	 * Get the size of the task list.
	 * @return the task list size
	 */
	public int getRequestSize() {
		return requestSize;
	}
	
	/**
	 * Get the reservation receipt.
	 * @return the reservation receipt
	 */
	public ReservationReceipt getReservationReciept() {
		return reservationReciept;
	}

	/**
	 * Set the reservation receipt.
	 * @param reservationReciept the reservation receipt
	 */
	public void setReservationReciept(ReservationReceipt reservationReciept) {
		if (reservationReciept == null) {
            throw new IllegalArgumentException("Invalid reservation reciept cannot be null.");
		}
		
		this.reservationReciept = reservationReciept;
	}

	/**
	 * Get the host executing the task.
	 * @return the host executing the task
	 */
	public String getServingHostIp() {
		return servingHostIp;
	}

	/**
	 * Set the host executing the task.
	 * @param servingHostIp the host executing the task
	 */
	public void setServingHostIp(String servingHostIp) {
		this.servingHostIp = servingHostIp;
	}

	/**
	 * Get the task execution configuration parameters.
	 * @return the task execution configuration parameters
	 */
	public RequestConfig getRequestconfig() {
		return requestconfig;
	}

	/**
	 * Set the task execution configuration parameters.
	 * @param requestconfig the task execution configuration parameters
	 */
	public void setRequestconfig(RequestConfig requestconfig) {
		this.requestconfig = requestconfig;
	}
	
}
