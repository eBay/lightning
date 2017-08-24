package com.ebay.lightning.core.beans;

import java.io.Serializable;
import java.util.Map;

import com.ebay.lightning.core.beans.ReservationReceipt.State;
import com.ebay.lightning.core.utils.LightningCoreUtil;

public class SystemStatus implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	private static final Long DEFAULT_SYSTEM_START_TIME = System.currentTimeMillis();
	
	private String hostName;
	private String region;
	private int workQueueSize; 
	private int reservationLogSize;
	private int lastReservationCleanup;
	private State workerState;
	private boolean systemHealth;
	private String systemStatusErrorMsg;
	private Long systemStartTime;
	private Long upTime;
	private int availableTaskCapacity;
	private int queueLoadSize;
	private int reservationLoadSize;
	
	private long freeMemory;
	private long allocatedMemory;
	private long maxMemory;
	private long usedMemory;
	
	private long virtualMemSize;
	private long freePhyMemSize;
	private long totPhyMemSize;
	private String totSwapSpaceSize;
	private String freeSwapSpaceSize;
	private long processCPUTime;
	private String openFileDesCount;
	private String maxFileDescCount;
	private String sysCPULoad;
	private String processCPULoad;
	
	private Map<String, String> cpuUsageMap;
	
	public SystemStatus() {
		setSystemStartTime(DEFAULT_SYSTEM_START_TIME);
	}
	
	public int getWorkQueueSize() {
		return workQueueSize;
	}
	public void setWorkQueueSize(int workQueueSize) {
		this.workQueueSize = workQueueSize;
	}
	public int getReservationLogSize() {
		return reservationLogSize;
	}
	public void setReservationLogSize(int reservationLogSize) {
		this.reservationLogSize = reservationLogSize;
	}
	public int getLastReservationCleanup() {
		return lastReservationCleanup;
	}
	public void setLastReservationCleanup(int lastReservationCleanup) {
		this.lastReservationCleanup = lastReservationCleanup;
	}
	public State getWorkerState() {
		return workerState;
	}
	public void setWorkerState(State workerState) {
		this.workerState = workerState;
	}
	
	public Map<String, String> getCpuUsageMap() {
		return cpuUsageMap;
	}
	public void setCpuUsageMap(Map<String, String> cpuUsageMap) {
		this.cpuUsageMap = cpuUsageMap;
	}
	public long getFreeMemory() {
		return freeMemory;
	}
	public void setFreeMemory(long freeMemory) {
		this.freeMemory = freeMemory;
	}
	public long getAllocatedMemory() {
		return allocatedMemory;
	}
	public void setAllocatedMemory(long allocatedMemory) {
		this.allocatedMemory = allocatedMemory;
	}
	public long getMaxMemory() {
		return maxMemory;
	}
	public void setMaxMemory(long maxMemory) {
		this.maxMemory = maxMemory;
	}
	public long getUsedMemory() {
		return usedMemory;
	}
	public void setUsedMemory(long usedMemory) {
		this.usedMemory = usedMemory;
	}
	public long getVirtualMemSize() {
		return virtualMemSize;
	}
	public void setVirtualMemSize(long virtualMemSize) {
		this.virtualMemSize = virtualMemSize;
	}
	public String getTotSwapSpaceSize() {
		return totSwapSpaceSize;
	}
	public void setTotSwapSpaceSize(String totSwapSpaceSize) {
		this.totSwapSpaceSize = totSwapSpaceSize;
	}
	public String getFreeSwapSpaceSize() {
		return freeSwapSpaceSize;
	}
	public void setFreeSwapSpaceSize(String freeSwapSpaceSize) {
		this.freeSwapSpaceSize = freeSwapSpaceSize;
	}
	public long getProcessCPUTime() {
		return processCPUTime;
	}
	public void setProcessCPUTime(long processCPUTime) {
		this.processCPUTime = processCPUTime;
	}
	public long getFreePhyMemSize() {
		return freePhyMemSize;
	}
	public void setFreePhyMemSize(long freePhyMemSize) {
		this.freePhyMemSize = freePhyMemSize;
	}
	public long getTotPhyMemSize() {
		return totPhyMemSize;
	}
	public void setTotPhyMemSize(long totPhyMemSize) {
		this.totPhyMemSize = totPhyMemSize;
	}
	public String getOpenFileDesCount() {
		return openFileDesCount;
	}
	public void setOpenFileDesCount(String openFileDesCount) {
		this.openFileDesCount = openFileDesCount;
	}
	public String getMaxFileDescCount() {
		return maxFileDescCount;
	}
	public void setMaxFileDescCount(String maxFileDescCount) {
		this.maxFileDescCount = maxFileDescCount;
	}
	public String getSysCPULoad() {
		return sysCPULoad;
	}
	public void setSysCPULoad(String sysCPULoad) {
		this.sysCPULoad = sysCPULoad;
	}
	public String getProcessCPULoad() {
		return processCPULoad;
	}
	public void setProcessCPULoad(String processCPULoad) {
		this.processCPULoad = processCPULoad;
	}
	public String getHostName() {
		return hostName;
	}
	public void setHostName(String hostName) {
		this.hostName = hostName;
	}
	public boolean isSystemHealth() {
		return systemHealth;
	}
	public void setSystemHealth(boolean systemHealth) {
		this.systemHealth = systemHealth;
	}
	
	public String getSystemStatusErrorMsg() {
		return systemStatusErrorMsg;
	}
	public void setSystemStatusErrorMsg(String systemStatusErrorMsg) {
		this.systemStatusErrorMsg = systemStatusErrorMsg;
	}
	public Long getSystemStartTime() {
		return systemStartTime;
	}
	public void setSystemStartTime(Long systemStartTime) {
		this.systemStartTime = systemStartTime;
	}
	public Long getUpTime() {
		return upTime;
	}
	public void setUpTime(Long upTime) {
		this.upTime = upTime;
	}
	public int getAvailableTaskCapacity() {
		return availableTaskCapacity;
	}

	public void setAvailableTaskCapacity(int availableTaskCapacity) {
		this.availableTaskCapacity = availableTaskCapacity;
	}

	public int getQueueLoadSize() {
		return queueLoadSize;
	}

	public void setQueueLoadSize(int queueLoadSize) {
		this.queueLoadSize = queueLoadSize;
	}

	public int getReservationLoadSize() {
		return reservationLoadSize;
	}

	public void setReservationLoadSize(int reservationLoadSize) {
		this.reservationLoadSize = reservationLoadSize;
	}

	public String getRegion() {
		return region;
	}

	public void setRegion(String region) {
		this.region = region;
	}

	@Override
	public String toString() { 
		return LightningCoreUtil.convertObjectToJsonString(this);
	}
}