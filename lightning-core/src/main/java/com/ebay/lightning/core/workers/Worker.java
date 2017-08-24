package com.ebay.lightning.core.workers;

import com.ebay.lightning.core.beans.Task;
import com.ebay.lightning.core.constants.LightningCoreConstants.WorkerState;

/**
 * Defines the interface for the actual execution of the {@link Task} list.
 * 
 * @author shashukla
 * @see SocketBasedHTTPWorker
 *
 */

public interface Worker {
	
	/**
	 * Execute the request by fetching the request based on sessionId.
	 * @param sessionId the session id of the request to be executed
	 * @return the state of the working thread after the execution
	 */
	public WorkerState execute(String sessionId);

}
