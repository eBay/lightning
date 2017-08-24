package com.ebay.lightning.core.beans;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.ebay.lightning.core.beans.ReservationReceipt.State;

import static junit.framework.Assert.assertEquals;

public class SystemStatusTest {

	private SystemStatus systemStatus;
	
	@Before
	public void setup(){
		systemStatus = new SystemStatus();
	}
	
	@Test
	public void testSysStatus(){
		String region = "sjc";
		systemStatus.setRegion(region);
		assertEquals(region, systemStatus.getRegion());
		
		String errMsg = "empty string";
		systemStatus.setSystemStatusErrorMsg(errMsg);
		assertEquals(errMsg, systemStatus.getSystemStatusErrorMsg());
		
		boolean systemHealth = true; 
		systemStatus.setSystemHealth(systemHealth);
		assertEquals(systemHealth, systemStatus.isSystemHealth());
		
		String host = "localhost";
		systemStatus.setHostName(host);
		assertEquals(host, systemStatus.getHostName());
		
		String processCPUload = "90";
		systemStatus.setProcessCPULoad(processCPUload);
		assertEquals(processCPUload, systemStatus.getProcessCPULoad());
		
		String maxFileDescCount = "10";
		systemStatus.setMaxFileDescCount(maxFileDescCount);
		assertEquals(maxFileDescCount, systemStatus.getMaxFileDescCount());
		
		String openFileDesCount = "8";
		systemStatus.setOpenFileDesCount(openFileDesCount);
		assertEquals(openFileDesCount, systemStatus.getOpenFileDesCount());
		
		long totPhyMemSize = 9009;
		systemStatus.setTotPhyMemSize(totPhyMemSize);
		assertEquals(totPhyMemSize, systemStatus.getTotPhyMemSize());
		
		long processCPUTime = 99;
		systemStatus.setProcessCPUTime(processCPUTime);
		assertEquals(processCPUTime, systemStatus.getProcessCPUTime());
		
		String freeSwapSpaceSize = "4gb";
		systemStatus.setFreeSwapSpaceSize(freeSwapSpaceSize);
		assertEquals(freeSwapSpaceSize, systemStatus.getFreeSwapSpaceSize());
		
		String totSwapSpaceSize = "2gb";
		systemStatus.setTotSwapSpaceSize(totSwapSpaceSize);
		assertEquals(totSwapSpaceSize, systemStatus.getTotSwapSpaceSize());
		
		long usedMemory = 100;
		systemStatus.setUsedMemory(usedMemory);
		assertEquals(usedMemory, systemStatus.getUsedMemory());
		
		long virtualMemSize = 16000;
		systemStatus.setVirtualMemSize(virtualMemSize);
		assertEquals(virtualMemSize, systemStatus.getVirtualMemSize());
		
		long allocatedMemory = 8000;
		systemStatus.setAllocatedMemory(allocatedMemory);
		assertEquals(allocatedMemory, systemStatus.getAllocatedMemory());
		
		State state = State.BUSY;
		systemStatus.setWorkerState(state);
		assertEquals(state, systemStatus.getWorkerState());
		
		Map<String, String> cpuUsageMap = new HashMap<String, String>();
		cpuUsageMap.put("cpu1", "90");
		systemStatus.setCpuUsageMap(cpuUsageMap);
		assertEquals(cpuUsageMap, systemStatus.getCpuUsageMap());
		
		int reservationLogSize = 5000;
		systemStatus.setReservationLogSize(reservationLogSize);
		assertEquals(reservationLogSize, systemStatus.getReservationLogSize());
	}
	
}
