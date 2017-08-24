package com.ebay.lightning.core.beans;

import java.io.Serializable;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.ebay.lightning.core.constants.LightningCoreConstants.TaskStatus;

/**
 * The {@code ChainedURLTask} class can execute a set of URLs sequentially. The URL execution stops when it
 * encounters an error in the sequential execution process.
 * 
 * @author shashukla
 * @see URLTask
 */
public class ChainedURLTask extends URLTask implements Serializable {
	private static final long serialVersionUID = 6819445337695255767L;
	
	/**
	 * The list of URLs to be executed in sequential order.
	 */
	private List<URLTask> urlTasks = new ArrayList<>();
	private transient Iterator<URLTask> taskIterator = null;

	private URLTask currentUrlTask;

	public ChainedURLTask() throws URISyntaxException {
		super("");
	}

	/**
	 * Add an URL task to the list of URLs.
	 * @param urlTask the URL task to be added to the list
	 */
	public void addUrlTask(URLTask urlTask) {
		urlTasks.add(urlTask);
	}

	/**
	 * Get the state of the current execution task.
	 * 
	 * <p>Refer {@link TaskStatus} on the possible states for the task.</p>
	 * @return the state of the current execution task
	 */
	@Override
	public TaskStatus getStatus() {
		return currentUrlTask.getStatus();
	}
	
	/**
	 * Set the state of the current execution task.
	 * 
	 * <p>Refer {@link TaskStatus} on the possible states for the task.</p>
	 * @param status the state of the current execution task
	 */
	@Override
	public void setStatus(TaskStatus status) {
		currentUrlTask.setStatus(status);
	}
	
	/**
	 * Set the HTTP status code of the current execution task.
	 * 
	 * @param statusCode the HTTP status code of the current execution task
	 */
	@Override
	public void setStatusCode(int statusCode) {
		currentUrlTask.setStatusCode(statusCode);
	}

	/**
	 * Get the HTTP status code of the current execution task.
	 * 
	 * @return the HTTP status code of the current execution task
	 */
	@Override
	public int getStatusCode() {
		return currentUrlTask.getStatusCode();
	}

	/**
	 * Get the port of the current execution task.
	 * 
	 * @return the port of the current execution task
	 */
	@Override
	public int getPort() {
		return currentUrlTask.getPort();
	}

	/**
	 * Get the host of the current execution task.
	 * 
	 * @return the host of the current execution task
	 */
	@Override
	public String getHost() {
		return currentUrlTask.getHost();
	}

	/**
	 * Get the URL path of the current execution task.
	 * 
	 * @return the URL path of the current execution task
	 */
	@Override
	public String getPath() {
		return currentUrlTask.getPath();
	}

	/**
	 * Check if proxy is enabled for the current execution task.
	 * 
	 * @return true if proxy is enabled for the current execution task
	 */
	@Override
	public boolean isUseProxyServer() {
		return currentUrlTask.isUseProxyServer();
	}

	/**
	 * Enable proxy for the current execution task.
	 * 
	 * @param useProxyServer enable proxy for the current execution task
	 */
	@Override
	public void setUseProxyServer(boolean useProxyServer) {
		this.currentUrlTask.setUseProxyServer(useProxyServer);
	}

	/**
	 * Get the host IP of the current execution task. 
	 * 
	 * @return the host IP of the current execution task. If {@link #setUseProxyServer(boolean)} is enabled, the proxy IP
	 * is returned. Or else the host IP is returned.
	 */
	@Override
	public String getHostIPAddress() {
		return currentUrlTask.getHostIPAddress();
	}

	/**
	 * Set the host IP of the current execution task. 
	 * 
	 * @param hostIPAddress the host IP of the current execution task
	 */
	@Override
	public void setHostIPAddress(String hostIPAddress) {
		currentUrlTask.setHostIPAddress(hostIPAddress);
	}

	/**
	 * Get the complete URL of the current execution task.
	 * 
	 * @return the complete URL of the current execution task
	 */
	@Override
	public String getCompleteURL() {
		return currentUrlTask.getCompleteURL();
	}
	
	/**
	 * Get the error message if any for the current execution task.
	 * 
	 * @return the error message if any for the current execution task. If successful, {@code null} is returned
	 */
	@Override
	public String getErrorMsg() {
		return currentUrlTask.getErrorMsg();
	}
	
	/**
	 * Set the error message if any for the current execution task.
	 * 
	 * @param errorMsg the error message if any for the current execution task
	 */
	@Override
	public void setErrorMsg(String errorMsg) {
		currentUrlTask.setErrorMsg(errorMsg);
	}

	/**
	 * Get the last update time when there is a change in task status for the current execution task.
	 * 
	 * @return the last update time when there is a change in task status for the current execution task
	 */
	@Override
	public Long getLastTaskStatusUpdateTime() {
		return currentUrlTask.getLastTaskStatusUpdateTime();
	}
	
	/**
	 * Set the last update time when there is a change in task status for the current execution task.
	 * 
	 * @param lastTaskStatusUpdateTime the last update time when there is a change in task status for the
	 * current execution task
	 */
	@Override
	public void setLastTaskStatusUpdateTime(Long lastTaskStatusUpdateTime) {
		currentUrlTask.setLastTaskStatusUpdateTime(lastTaskStatusUpdateTime);
	}
	
	/**
	 * Check for more task to be executed in the task list.
	 * @return true if there are more tasks to be executed.
	 */
	public synchronized boolean hasNext() {
		if(taskIterator == null) {
			taskIterator = urlTasks.iterator();
		}
		return taskIterator.hasNext();
	}

	/**
	 * Move to the next task to be executed in the task list. 
	 * 
	 * {@link #hasNext()} method has to be called before calling this method.
	 */
	public synchronized void moveToNext() {
		currentUrlTask = taskIterator.next();
	}
}