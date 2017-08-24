package com.ebay.lightning.core.services;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.ebay.lightning.core.beans.LightningRequest;
import com.ebay.lightning.core.beans.LightningResponse;
import com.ebay.lightning.core.beans.ReservationReceipt;
import com.ebay.lightning.core.beans.SystemStatus;
import com.ebay.lightning.core.config.SystemConfig;
import com.ebay.lightning.core.manager.TaskExecutionManager;
import com.ebay.lightning.core.store.LightningRequestReport;


/**
 * The {@code TaskExecutionServiceImpl} provides the implementation for {@link TaskExecutionService} that defines
 *  the operations for processing the request and getting the response.
 *  
 *  Most of the operation are delegated to {@link TaskExecutionManager}.
 * 
 * @author shashukla
 * @see TaskExecutionServiceImpl
 * @see TaskExecutionManager
 */
@Component
@Scope(value = "singleton")
public class TaskExecutionServiceImpl implements TaskExecutionService {

	TaskExecutionManager taskExecutionManager;

	@Autowired
	public TaskExecutionServiceImpl(TaskExecutionManager taskExecutionManager) {
		this.taskExecutionManager = taskExecutionManager;
	}

	/* (non-Javadoc)
	 * @see {@link TaskExecutionService#submit(LightningRequest)}
	 */
	@Override
	public void submit(LightningRequest request) {
		taskExecutionManager.submit(request);
	}
	
	/* (non-Javadoc)
	 * @see {@link TaskExecutionService#pollResponse(String, boolean)}
	 */
	@Override
	public LightningResponse pollResponse(String sessionId, boolean pollDeltaOnly) {
		return taskExecutionManager.pollResults(sessionId, pollDeltaOnly);
	}

	/* (non-Javadoc)
	 * @see {@link TaskExecutionService#reserve(int)}
	 */
	@Override
	public ReservationReceipt reserve(int load) {
		return taskExecutionManager.reserve(load);
	}

	/* (non-Javadoc)
	 * @see {@link TaskExecutionService#getReport(String)}
	 */
	@Override
	public LightningRequestReport getReport(String sessionId) {
		return taskExecutionManager.getReport(sessionId);
	}

	/* (non-Javadoc)
	 * @see {@link TaskExecutionService#getLightningStats()}
	 */
	@Override
	public SystemStatus getLightningStats() {
		return taskExecutionManager.getLightningStats();
	}

	/* (non-Javadoc)
	 * @see {@link TaskExecutionService#updateSystemConfig(SystemConfig)}
	 */
	@Override
	public SystemConfig updateSystemConfig(SystemConfig sysConfig) throws Exception {
		return taskExecutionManager.updateSystemConfig(sysConfig);
	}

	/* (non-Javadoc)
	 * @see {@link TaskExecutionService#getSystemConfig()}
	 */
	@Override
	public SystemConfig getSystemConfig() {
		return taskExecutionManager.getSystemConfig();
	}

	/* (non-Javadoc)
	 * @see {@link TaskExecutionService#getAuditReports(String)}
	 */
	@Override
	public List<LightningRequestReport> getAuditReports(String sessionId) {
		return taskExecutionManager.getAuditReports(sessionId);
	}
}
