/**
 * 
 */
package com.ebay.lightning.core.manager;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.ebay.lightning.core.async.Callback;
import com.ebay.lightning.core.async.Reminder;
import com.ebay.lightning.core.beans.LightningRequest;
import com.ebay.lightning.core.beans.LightningResponse;
import com.ebay.lightning.core.beans.ReservationReceipt;
import com.ebay.lightning.core.beans.SystemStatus;
import com.ebay.lightning.core.config.RequestConfig;
import com.ebay.lightning.core.config.SystemConfig;
import com.ebay.lightning.core.constants.LightningCoreConstants.WorkStatus;
import com.ebay.lightning.core.exception.ManagerQueueFullException;
import com.ebay.lightning.core.exception.WorkQueueCapacityReachedException;
import com.ebay.lightning.core.store.ExecutionDataStore;
import com.ebay.lightning.core.store.LightningRequestReport;
import com.ebay.lightning.core.utils.InetSocketAddressCache;
import com.ebay.lightning.core.utils.LightningCoreUtil;
import com.ebay.lightning.core.workers.SocketBasedHTTPWorker;
import com.google.common.base.Preconditions;

/**
 * The {@code TaskExecutionManager} provides the actual implementation of request processing, reporting and storage.
 * 
 * The requests are queued up and gets processed in FIFO order.
 * THe class delegates report storage to {@link ExecutionDataStore}
 * 
 * @author shashukla
 *
 */
@Component
public class TaskExecutionManager {
	private static final String TASK_EXECUTION_MANAGER_THREAD = "TaskExecutionManagerThread";
	private static final Logger log = Logger.getLogger(TaskExecutionManager.class);

	private final Object initLock = new Object();
	private Boolean started = Boolean.FALSE;
	private final SystemConfig systemConfig;
	private BlockingQueue<LightningRequest> workQueue;
	private final Map<ReservationReceipt, Long> reservationResponseLog = new ConcurrentHashMap<>();

	private ExecutionDataStore dataStore;
	
	@SuppressWarnings("unused")
	private Reminder reservationCleanupReminder = null;
	private Thread queueReader;
	private final InetSocketAddressCache inetcache;
	
	@Autowired
	public TaskExecutionManager(SystemConfig systemConfig, ExecutionDataStore dataStore, InetSocketAddressCache inetCache) {
		this.systemConfig = !systemConfig.isLoadFromFile() ? systemConfig : systemConfig.loadFromFile();
		this.dataStore = dataStore;
		this.inetcache = inetCache;
		workQueue = new LinkedBlockingQueue<LightningRequest>(systemConfig.getMaxTaskCapacity());
	}

	/**
	 * Initialized the {@code TaskExecutionManager} with reservation cleanup {@link Reminder} and request processing thread.
	 */
	@PostConstruct
	public void start() {
		synchronized (initLock) {
			if (started) {
				return;
			} else {
				started = true;
			}
		}
		reservationCleanupReminder = new Reminder("ReservationCleanupReminder", new Callback<String>() {
			@Override
			public void notify(String arg) {
				for (java.util.Map.Entry<ReservationReceipt, Long> entry : reservationResponseLog.entrySet()) {
					if (entry.getValue() < System.currentTimeMillis()) {
						reservationResponseLog.remove(entry.getKey());
					}
				}
			}
		}, systemConfig.getOldReservationCleanupReminderTimeInMillis() / 1000L, true);

		queueReader = new Thread(new Runnable() {
			@Override
			public void run() {
				while (true) {
					try {
						processEntireWorkQueue();
						sleepFor(systemConfig.getTimeToSleepBetweenEachQueueProcessInMillis());
					} catch (Exception e) {
						log.error("Error in executing qd requests", e);
					}
				}
			}

			private void processEntireWorkQueue() {
				while (!workQueue.isEmpty()) {
					LightningRequest request = null;
					try {
						//Just peak, it will be polled only after successful completion
						request = workQueue.peek();
						LightningRequestReport report = dataStore.getReport(request.getSessionId());
						report.setWorkDequeueTime(System.currentTimeMillis());
						long startTime = System.currentTimeMillis();
						report.setProcessStartTime(startTime);
						loadDefaultsInRequestConfig(request);
						SocketBasedHTTPWorker worker = new SocketBasedHTTPWorker(inetcache, dataStore, systemConfig, request.getRequestconfig());
						worker.execute(request.getSessionId());
						report.setTotalExecutionTimeInMillis(System.currentTimeMillis() - startTime);
						log.info("\n\n" + report);

						//all batches are executed, now remove the request from Q
						workQueue.poll();
					} catch (Exception e) {
						log.fatal("Error processing " + request, e);
					}
				}
			}

			private void loadDefaultsInRequestConfig(LightningRequest request) {
				RequestConfig requestConfig = request.getRequestconfig() != null ? request.getRequestconfig() : new RequestConfig();
				requestConfig.loadDefaultValues(systemConfig);
				request.setRequestconfig(requestConfig);
			}

		}, TASK_EXECUTION_MANAGER_THREAD);

		queueReader.start();
	}

	/* (non-Javadoc)
	 * @see {@link TaskExecutionService#reserve(int)}
	 */
	public synchronized ReservationReceipt reserve(int load) {
		
		ReservationReceipt reserveResponse = null;
		int busyWithLoad = getSubmittedLoad() + getReservedLoad();
		int leftoverCapacity = systemConfig.getMaxTaskCapacity() - busyWithLoad;

		if (leftoverCapacity >= load) {
			if (busyWithLoad > 0) {
				reserveResponse = new ReservationReceipt(ReservationReceipt.State.BUSY, UUID.randomUUID().toString(), load);
			} else {
				reserveResponse = new ReservationReceipt(ReservationReceipt.State.ACCEPTED, UUID.randomUUID().toString(), load);
			}
		} else {
			reserveResponse = new ReservationReceipt(ReservationReceipt.State.DENIED, UUID.randomUUID().toString(), load);
		}

		long expiryTime = (System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(systemConfig.getReservationResponseExpireTimeInSec()));
		reservationResponseLog.put(reserveResponse, expiryTime);
		reserveResponse.setBusyWithLoad(busyWithLoad);
		return reserveResponse;
	}

	/* (non-Javadoc)
	 * @see {@link TaskExecutionService#submit(LightningRequest)}
	 */
	public void submit(LightningRequest request) {
		Preconditions.checkState((request != null), "Request Cannot be null");
		Preconditions.checkState((request.getReservationReciept() != null), "Reservation Reciept Cannot be null");
		Preconditions.checkState(reservationResponseLog.containsKey(request.getReservationReciept()),
				"Unknown Reservation Reciept. Reservation is either expired or never created. %s", request.getReservationReciept());
		Preconditions.checkState(!ReservationReceipt.State.DENIED.equals(request.getReservationReciept().getState()),
				"Request submission is attemped on a Denied Reservation.");
		try {
			if (request.getTasks() != null) {
				LightningRequestReport report = dataStore.register(request);
				workQueue.add(request);
				reservationResponseLog.remove(request.getReservationReciept());
				report.setWorkEnqueueTime(System.currentTimeMillis());
				report.setStatus(WorkStatus.IN_QUEUE);
			} else {
				throw new WorkQueueCapacityReachedException();
			}
		} catch (IllegalStateException e) {
			throw new ManagerQueueFullException();
		}
	}

	/**
	 * Sleep for the specified amount of time.
	 */
	private void sleepFor(long sleepMillis) {
		try {
			Thread.sleep(sleepMillis);
		} catch (InterruptedException e) {
			log.error("Unable to sleep...", e);
		}
	}

	/**
	 * Get the work load to be processed.
	 * @return the work load to be processed
	 */
	private int getSubmittedLoad() {
		int load = 0;
		for (LightningRequest e : workQueue) {
			load += e.getRequestSize();
		}
	
		return load;
	}

	/**
	 * Get the total load of reservation made.
	 * @return the total load of reservation made
	 */
	private int getReservedLoad() {
		int load = 0;
		for (Entry<ReservationReceipt, Long> e : reservationResponseLog.entrySet()) {
			if (!ReservationReceipt.State.DENIED.equals(e.getKey().getState())) {
				load += e.getKey().getLoad();
			}
		}
		
		return load;
	}

	/* (non-Javadoc)
	 * @see {@link TaskExecutionService#pollResponse(String, boolean)}
	 */
	public LightningResponse pollResults(String sessionId, boolean pollDeltaOnly) {
		return dataStore.pollResults(sessionId, pollDeltaOnly);
	}

	/* (non-Javadoc)
	 * @see {@link TaskExecutionService#getReport(String)}
	 */
	public LightningRequestReport getReport(String sessionId) {
		LightningRequestReport report = dataStore.getReport(sessionId);
		return report;
	}
	
	/* (non-Javadoc)
	 * @see {@link TaskExecutionService#getAuditReports(String)}
	 */
	public List<LightningRequestReport> getAuditReports(String sessionId) {
		return dataStore.getAuditReports(sessionId);
	}
	
	/* (non-Javadoc)
	 * @see {@link TaskExecutionService#getLightningStats()}
	 */
	public SystemStatus getLightningStats(){
		SystemStatus status = new SystemStatus();
		
		status.setWorkQueueSize(workQueue.size());
		status.setReservationLogSize(reservationResponseLog.size());
		status.setLastReservationCleanup(systemConfig.getOldReservationCleanupReminderTimeInMillis());
		LightningCoreUtil.getJVMMemory(status);
		LightningCoreUtil.getCPUUsage(status);
		status.setUpTime(System.currentTimeMillis() - status.getSystemStartTime());
		status.setQueueLoadSize(getSubmittedLoad());
		status.setReservationLoadSize(getReservedLoad());
		status.setAvailableTaskCapacity(systemConfig.getMaxTaskCapacity() - (status.getQueueLoadSize() + status.getReservationLoadSize()));
		
		return status;
	}
	
	/* (non-Javadoc)
	 * @see {@link TaskExecutionService#updateSystemConfig(SystemConfig)}
	 */
	public SystemConfig updateSystemConfig(SystemConfig sysConfig) throws Exception {
		try {
			this.systemConfig.updateSystemConfig(sysConfig);
			LightningCoreUtil.writeSystemConfigToFileSystem(sysConfig);
			log.info("\n SystemConfig updated & persisted to file system: " + this.systemConfig.toString());
		} catch (Exception e) {
			log.error(e.getMessage());
			throw new Exception(e.getMessage());
		}
		return this.systemConfig;
	}

	/**
	 * Get the request to be processed queue.
	 * @return to process queue
	 */
	public BlockingQueue<LightningRequest> getWorkQueue() {
		return workQueue;
	}

	/**
	 * Set the request to be processed queue.
	 * @param workQueue to process queue
	 */
	public void setWorkQueue(BlockingQueue<LightningRequest> workQueue) {
		this.workQueue = workQueue;
	}

	/* (non-Javadoc)
	 * @see {@link TaskExecutionService#getSystemConfig()}
	 */
	public SystemConfig getSystemConfig() {
		return this.systemConfig;
	}

	/**
	 * Get the current reservation list.
	 * @return the current reservation list
	 */
	public Map<ReservationReceipt, Long> getReservationResponseLog() {
		return reservationResponseLog;
	}
}
