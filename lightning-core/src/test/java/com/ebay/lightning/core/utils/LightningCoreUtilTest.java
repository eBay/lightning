package com.ebay.lightning.core.utils;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.junit.Test;

import com.ebay.lightning.core.beans.LightningRequest;
import com.ebay.lightning.core.beans.SystemStatus;
import com.ebay.lightning.core.config.SystemConfig;
import com.ebay.lightning.core.constants.LightningCoreConstants;
import com.ebay.lightning.core.store.LightningRequestReport;

public class LightningCoreUtilTest {
	private static final Logger log = Logger.getLogger(LightningCoreUtilTest.class);
	
	private long procStart1 = 1000;
	private long procStart2 = 1500;
	private long procEnd1 = 3000;
	private long procEnd2 = 2000;
	private String session1 = "session1";
	private String session2 = "session2";
	
	@Test
	public void testCPUUsage()throws Exception{
		SystemStatus status = new SystemStatus();
		assertTrue(status.getFreePhyMemSize() == 0);
		LightningCoreUtil.getCPUUsage(status);
		log.info("SysCPULoad: "+status.getSysCPULoad());
		double cpuLoad = Double.parseDouble(status.getSysCPULoad());
		assertTrue(cpuLoad==-1d || cpuLoad >= 0d);
		log.info("FreePhyMemSize: "+status.getFreePhyMemSize());
		assertTrue(status.getFreePhyMemSize() == -1l || status.getFreePhyMemSize() >= 0l);
		
		assertTrue(status.getFreeMemory() == 0);
		assertTrue(status.getMaxMemory() == 0);
		LightningCoreUtil.getJVMMemory(status);
		assertTrue(status.getFreeMemory() >= 0);
		assertTrue(status.getMaxMemory() >= 0);
		log.info(status);
	}
	
	@Test
	public void testSystemConfig() throws Exception{
		int maxInetCacheSize = 500;
		SystemConfig config = LightningCoreUtil.readSystemConfigFromFileSystem(LightningCoreConstants.DEFAULT_SYSTEM_CONFIG_FILE_LOCATION);
		if(config==null)
			config = new SystemConfig();
		
		config.setMaxInetCacheSize(maxInetCacheSize);
		LightningCoreUtil.writeSystemConfigToFileSystem(config);
		
		SystemConfig newConfig = LightningCoreUtil.readSystemConfigFromFileSystem(LightningCoreConstants.DEFAULT_SYSTEM_CONFIG_FILE_LOCATION);
		assertNotNull(newConfig);
		assertTrue(maxInetCacheSize == newConfig.getMaxInetCacheSize());
		log.info(newConfig);
	}
	
	@Test
	public void testAuditReports(){
		Map<String, LightningRequestReport> requestReportStore = new HashMap<String, LightningRequestReport>();
		requestReportStore.put(session1, createReport(session1, procStart1, procEnd1));
		requestReportStore.put(session2, createReport(session1, procStart2, procEnd2));
		List<LightningRequestReport> reports1 = LightningCoreUtil.getAuditReports(requestReportStore, session1);
		assertNotNull(reports1);
		assertEquals(1,reports1.size());
		LightningRequestReport report = reports1.get(0);
		assertNotNull(report);
		assertEquals(procStart1,report.getProcessStartTime().longValue());
		assertEquals(procStart1+10,report.getWorkDequeueTime().longValue());
		
		List<LightningRequestReport> reports2 = LightningCoreUtil.getAuditReports(requestReportStore, "nonExistingSession");
		assertNotNull(reports2);
		assertEquals(0,reports2.size());
		List<LightningRequestReport> reports3 = LightningCoreUtil.getAuditReports(requestReportStore, null);
		assertNotNull(reports3);
		assertEquals(2,reports3.size());
	}
	
	public static LightningRequestReport createReport(String sessionId, long procStart, long procEnd){
		LightningRequestReport report = new LightningRequestReport();
		report.setRequest(new LightningRequest(sessionId));
		report.setProcessStartTime(procStart);
		report.setWorkEnqueueTime(procStart+1);
		report.setProcessEndTime(procEnd);
		report.setWorkDequeueTime(procStart+10);
		report.setTotalExecutionTimeInMillis(procEnd-procStart);
		return report;
	}
	
}
