package com.ebay.lightning.core.utils;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.ebay.lightning.core.beans.SystemStatus;
import com.ebay.lightning.core.config.SystemConfig;
import com.ebay.lightning.core.constants.LightningCoreConstants;
import com.ebay.lightning.core.store.LightningRequestReport;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

/**
 * Helper class containing utility functions specific to lightning.
 * 
 * @author shashukla
 */
public class LightningCoreUtil {
	
	private static final Logger LOGGER = Logger.getLogger(LightningCoreUtil.class);
	
	private static final int mb = 1024 * 1024;
	
	/**
	 * Get the audit report from store.
	 * @param requestReportStore the store
	 * @param sessionId the request Id
	 * @return the list of audit report
	 */
	public static List<LightningRequestReport> getAuditReports(
			final Map<String, LightningRequestReport> requestReportStore, final String sessionId) {

		List<LightningRequestReport> reports = new ArrayList<>();

		if (requestReportStore != null) {
			if (StringUtils.isNotEmpty(sessionId) && requestReportStore.get(sessionId) != null) {
				reports.add(LightningRequestReport.getAuditReport(requestReportStore.get(sessionId)));
			}
			if (StringUtils.isEmpty(sessionId)) { // audit summary
				for (Map.Entry<String, LightningRequestReport> entry : requestReportStore.entrySet()) {
					if (entry.getValue() != null) {
						reports.add(LightningRequestReport.getAuditReport(entry.getValue()));
					}
				}
			}
		}

		return reports;
	}
	
	/**
	 * Update CPU and physical memory statistics to the report.
	 * @param report the report to be updated
	 */
	public static void getCPUUsage(SystemStatus report) {
		
		OperatingSystemMXBean operatingSystemMXBean = ManagementFactory.getOperatingSystemMXBean();

		for (Method method : operatingSystemMXBean.getClass().getDeclaredMethods()) {
			method.setAccessible(true);

			if (method.getName().startsWith("get") && Modifier.isPublic(method.getModifiers())) {
				Object value;
				String key;

				try {
					value = method.invoke(operatingSystemMXBean);
					key = method.getName().substring(3);

					if (StringUtils.isNotEmpty(key) && value != null) {
						if ("OpenFileDescriptorCount".equalsIgnoreCase(key)) {
							report.setOpenFileDesCount(value.toString());
						}
						if ("ProcessCpuLoad".equalsIgnoreCase(key)) {
							report.setProcessCPULoad(value.toString());
						}
						if ("CommittedVirtualMemorySize".equalsIgnoreCase(key)) {
							report.setVirtualMemSize(bytesToMB(value));
						}
						if ("ProcessCpuTime".equalsIgnoreCase(key)) {
							report.setProcessCPUTime(convertNStoMs(value));
						}
						if ("FreePhysicalMemorySize".equalsIgnoreCase(key)) {
							report.setFreePhyMemSize(bytesToMB(value));
						}
						if ("TotalPhysicalMemorySize".equalsIgnoreCase(key)) {
							report.setTotPhyMemSize(bytesToMB(value));
						}
						if ("MaxFileDescriptorCount".equalsIgnoreCase(key)) {
							report.setMaxFileDescCount(value.toString());
						}
						if ("SystemCpuLoad".equalsIgnoreCase(key)) {
							report.setSysCPULoad(value.toString());
						}
					}
				} catch (Exception e) {
					LOGGER.error("Error fetching System info for ### " + method.getName() + " ###");
				}
			}
		}
	}

	/**
	 * Update JVM memory statistics to the report.
	 * @param report the report to be updated
	 */
	public static void getJVMMemory(SystemStatus report) {
		Runtime runtime = Runtime.getRuntime();
		report.setUsedMemory(((runtime.totalMemory() - runtime.freeMemory()) / mb));
		report.setMaxMemory(runtime.maxMemory() / mb);
		report.setAllocatedMemory(runtime.totalMemory() / mb);
		report.setFreeMemory(runtime.freeMemory() / mb);
	}
	
	/**
	 * Convert byte size to mega byte size.
	 * @param obj contains the byte size
	 * @return the mega byte size
	 */
	private static long bytesToMB(Object obj){
		long val = (Long) obj / mb;
		
		return val;
	}
	
	/**
	 * Convert milli seconds to nano seconds.
	 * @param obj contains milli seconds
	 * @return nano seconds
	 */
	private static long convertNStoMs(Object obj){
		long durationInMs = TimeUnit.MILLISECONDS.convert((Long)obj, TimeUnit.NANOSECONDS);
		
		return durationInMs;
	}
	
	/**
	 * Read the systemConfig stored in the fileSystem
	 * @param fileLocation configuration file location
	 * @return the configuration parameters
	 */
	public static SystemConfig readSystemConfigFromFileSystem(String fileLocation) {

		SystemConfig config = null;
		
		try {
			JsonElement obj = new JsonParser().parse(new FileReader(fileLocation));
			config = new Gson().fromJson(obj, SystemConfig.class);
		} catch (Exception e) {
			LOGGER.error("Error reading config from file system " + e.getMessage());
		}

		return config;
	}

	/**
	 * Write the systemConfig to fileSystem.
	 * 
	 * When the system gets reboot systemConfig is retrieved from the file system
	 * this will retain the previous configuration setup by the user
	 * @param config the config data to be written
	 * @throws Exception when the write operation fails
	 */
	public static void writeSystemConfigToFileSystem(SystemConfig config) throws Exception {

		try (FileWriter file = new FileWriter(LightningCoreConstants.DEFAULT_SYSTEM_CONFIG_FILE_LOCATION)) {
			file.write(new Gson().toJson(config).toString());
		} catch (IOException e) {
			throw new Exception("\nError writing systemConfig to file: " + e.getMessage());
		}
	}
	
	/**
	 * Convert Object to json format
	 * @param obj the Object to convert
	 * @return the object in JSON format
	 */
	public static String convertObjectToJsonString(Object obj) {
		return new Gson().toJson(obj);
	}
}
