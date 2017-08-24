package com.ebay.lightning.core.config;

import java.lang.reflect.Field;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.ebay.lightning.core.beans.ReservationReceipt;
import com.ebay.lightning.core.constants.LightningCoreConstants;
import com.ebay.lightning.core.constants.LightningCoreConstants.HttpMethod;
import com.ebay.lightning.core.utils.LightningCoreUtil;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * The {code SystemConfig} holds all the configuration for the task executor, data store etc.
 * 
 * @author shashukla
 */
@Component
@Scope(value = "singleton")
public class SystemConfig {

	public static final int DEFAULT_THREAD_POOL_SIZE = 300;
	private int taskCapacityStress = 1;
	private int workerBatchSize = 10000;
	private int maxTaskCapacity = 400000;

	private int maxInetCacheSize = 100000;

	private int auditCleanupFrequencyInSec = 60;
	private int reservationResponseExpireTimeInSec = 20;
	private int timeToSleepBetweenEachQueueProcessInMillis = 100;
	private int oldReservationCleanupReminderTimeInMillis = 2000;

	private RetentionPolicy retentionPolicy;
	private int executorThreadPoolSize = DEFAULT_THREAD_POOL_SIZE;

	private String updateSystemConfigStatusMsg;
	private HttpMethod defaultHTTPMethod = HttpMethod.HEAD;
	private boolean isLoadFromFile = true;

	private Map<HttpMethod, RequestConfig> defaultRequestConfigMap = new HashMap<>();

	public SystemConfig() {
		retentionPolicy = new RetentionPolicy();
		//load Default Request Configs 
		RequestConfig defaultRCForGET = new RequestConfig();
		defaultRCForGET.setReadAccuracyPercent(100);
		defaultRCForGET.setConnectAccuracyPercent(95);
		defaultRCForGET.setConnectTimeoutMillis(1000);
		defaultRCForGET.setReadWriteTimeoutMillis(1000);
		defaultRCForGET.setRetryFailedTasks(false);
		defaultRCForGET.setSlowUrlsConnectTimeoutMillis(3000);
		defaultRCForGET.setSlowUrlsReadWriteTimeoutMillis(5000);
		defaultRCForGET.setRetryConnectAccuracyPercent(100);
		defaultRCForGET.setRetryReadAccuracyPercent(100);
		defaultRCForGET.setRetrySlowUrlsConnectTimeoutMillis(5000);
		defaultRCForGET.setRetrySlowUrlsReadWriteTimeoutMillis(7000);
		defaultRCForGET.setMethod(HttpMethod.GET);
		getDefaultRequestConfigMap().put(HttpMethod.GET, defaultRCForGET);

		RequestConfig defaultRCForHEAD = new RequestConfig();
		defaultRCForHEAD.setReadAccuracyPercent(95);
		defaultRCForHEAD.setConnectAccuracyPercent(95);
		defaultRCForHEAD.setConnectTimeoutMillis(1000);
		defaultRCForHEAD.setReadWriteTimeoutMillis(1000);
		defaultRCForHEAD.setRetryFailedTasks(false);
		defaultRCForHEAD.setSlowUrlsConnectTimeoutMillis(1500);
		defaultRCForHEAD.setSlowUrlsReadWriteTimeoutMillis(1500);
		defaultRCForHEAD.setRetryConnectAccuracyPercent(100);
		defaultRCForHEAD.setRetryReadAccuracyPercent(100);
		defaultRCForHEAD.setRetrySlowUrlsConnectTimeoutMillis(2000);
		defaultRCForHEAD.setRetrySlowUrlsReadWriteTimeoutMillis(2000);
		defaultRCForHEAD.setMethod(HttpMethod.HEAD);
		getDefaultRequestConfigMap().put(HttpMethod.HEAD, defaultRCForHEAD);
	}

	/**
	 * Initializes the {@code SystemConfig} from the file.
	 * @return the SystemConfig
	 */
	public SystemConfig loadFromFile() {
		SystemConfig config = LightningCoreUtil
				.readSystemConfigFromFileSystem(LightningCoreConstants.DEFAULT_SYSTEM_CONFIG_FILE_LOCATION);
		if (config != null) {
			return updateSystemConfig(config);
		} else {
			return this;
		}
	}

	/**
	 * Clone the {@code SystemConfig} .
	 * @param updatedConfig the {@code SystemConfig} to clone
	 * @return the update SystemConfig
	 */
	public SystemConfig updateSystemConfig(SystemConfig updatedConfig) {
		return (SystemConfig) copyFields(this, deepCopyUpdatedConfig(updatedConfig), false);
	}

	/**
	 * Update the newConfig 
	 * @param updatedConfig the new config to update 
	 * @return the updated config
	 */
	public SystemConfig deepCopyUpdatedConfig(SystemConfig updatedConfig) {
		JsonObject newConfig = new Gson().toJsonTree(updatedConfig).getAsJsonObject();
		JsonObject currentConfig = new Gson().toJsonTree(this).getAsJsonObject();

		for (Map.Entry<String, JsonElement> newConfigEntry : newConfig.entrySet()) {
			if (currentConfig.has(newConfigEntry.getKey())) {
				deepCopy(newConfigEntry, currentConfig);
			}
		}

		return new Gson().fromJson(currentConfig, SystemConfig.class);
	}

	/**
	 * This method does deepCopy of source to target; In case of JsonArray process is repeated recursively
	 * @param newConfigEntry the new config
	 * @param currentConfig current config
	 */
	private void deepCopy(Map.Entry<String, JsonElement> newConfig, JsonObject currentConfig) {
		if (newConfig.getValue().isJsonObject()) {
			for (Map.Entry<String, JsonElement> nestEntry : newConfig.getValue().getAsJsonObject().entrySet()) {
				if (currentConfig.has(newConfig.getKey())) {
					deepCopy(nestEntry, currentConfig.getAsJsonObject(newConfig.getKey()));
				}
			}
		} else if (currentConfig.has(newConfig.getKey())) {
			currentConfig.add(newConfig.getKey(), newConfig.getValue());
		}
	}
	
	/**
	 * This method does copy fields from source to target object using reflection
	 * @param target the target Object
	 * @param source the source Object
	 * @param overwriteNullOnly overwrite only null values if set to {@code true}
	 * @return the updated target Object
	 */
	private Object copyFields(Object target, Object source, boolean overwriteNullOnly) {
		Field[] declaredFields = source.getClass().getDeclaredFields();
		for (Field f : declaredFields) {
			try {
				boolean isfieldNonFinal = (f.getModifiers() & java.lang.reflect.Modifier.FINAL) != java.lang.reflect.Modifier.FINAL;
				if (isfieldNonFinal) {
					boolean isfieldValueNull = f.get(target) == null;
					if (isfieldValueNull || !overwriteNullOnly) {
						Object object = f.get(source);
						f.set(target, object);
					}
				}
			} catch (IllegalArgumentException | IllegalAccessException e) {
				// ignore as this is best effort copy
			}
		}

		return target;
	}
	

	/* (non-Javadoc)
	 * @see {@link Object#toString()}
	 */
	@Override
	public String toString(){
		return LightningCoreUtil.convertObjectToJsonString(this);
	}

	/**
	 * Get the maximum capacity of the seed.
	 * @return the maximum capacity of the seed
	 */
	public int getMaxTaskCapacity() {
		return maxTaskCapacity;
	}


	/**
	 * Set the maximum capacity of the seed.
	 * @param maxTaskCapacity the maximum capacity of the seed
	 */
	public void setMaxTaskCapacity(int maxTaskCapacity) {
		this.maxTaskCapacity = maxTaskCapacity;
	}

	/**
	 * Get the maximum capacity stress percentage.
	 * @return the maximum capacity stress percentage
	 */
	public int getTaskCapacityStress() {
		return taskCapacityStress;
	}

	/**
	 * Set the maximum capacity stress percentage.
	 * @param taskCapacityStress the maximum capacity stress percentage
	 */
	public void setTaskCapacityStress(int taskCapacityStress) {
		this.taskCapacityStress = taskCapacityStress;
	}

	/**
	 * Get the maximum {@link InetAddress} cache size.
	 * @return the maximum InetAddress cache size
	 */
	public int getMaxInetCacheSize() {
		return maxInetCacheSize;
	}

	/**
	 * Set the maximum {@link InetAddress} cache size.
	 * @param maxInetCacheSize the maximum InetAddress cache size
	 */
	public void setMaxInetCacheSize(int maxInetCacheSize) {
		this.maxInetCacheSize = maxInetCacheSize;
	}

	/**
	 * Get the batch size for request processing.
	 * @return the batch size for request processing
	 */
	public int getWorkerBatchSize() {
		return workerBatchSize;
	}

	/**
	 * Set the batch size for request processing.
	 * @param workerBatchSize the batch size for request processing
	 */
	public void setWorkerBatchSize(int workerBatchSize) {
		this.workerBatchSize = workerBatchSize;
	}

	/**
	 * Get the validity time of the {@link ReservationReceipt}.
	 * @return the validity time of the ReservationReceipt
	 */
	public int getReservationResponseExpireTimeInSec() {
		return reservationResponseExpireTimeInSec;
	}

	/**
	 * Set the validity time of the {@link ReservationReceipt}.
	 * @param reservationResponseExpireTimeInSec the validity time of the ReservationReceipt
	 */
	public void setReservationResponseExpireTimeInSec(int reservationResponseExpireTimeInSec) {
		this.reservationResponseExpireTimeInSec = reservationResponseExpireTimeInSec;
	}

	/**
	 * Get the time to wait for polling request from queue for processing.
	 * @return the time to wait for polling request from queue
	 */
	public int getTimeToSleepBetweenEachQueueProcessInMillis() {
		return timeToSleepBetweenEachQueueProcessInMillis;
	}

	/**
	 * Set the time to wait for polling request from queue for processing.
	 * @param timeToSleepBetweenEachQueueProcessInMillis the time to wait for polling request from queue
	 */
	public void setTimeToSleepBetweenEachQueueProcessInMillis(int timeToSleepBetweenEachQueueProcessInMillis) {
		this.timeToSleepBetweenEachQueueProcessInMillis = timeToSleepBetweenEachQueueProcessInMillis;
	}

	/**
	 * Get the reminder frequency for cleaning up {@link ReservationReceipt}.
	 * @return the reminder frequency for cleaning reservations
	 */
	public int getOldReservationCleanupReminderTimeInMillis() {
		return oldReservationCleanupReminderTimeInMillis;
	}

	/**
	 * Set the reminder frequency for cleaning up {@link ReservationReceipt}.
	 * @param oldReservationCleanupReminderTimeInMillis the reminder frequency for cleaning reservations
	 */
	public void setOldReservationCleanupReminderTimeInMillis(int oldReservationCleanupReminderTimeInMillis) {
		this.oldReservationCleanupReminderTimeInMillis = oldReservationCleanupReminderTimeInMillis;
	}

	/**
	 * Get the pool size of the executor service.
	 * @return the pool size of the executor service
	 */
	public int getExecutorThreadPoolSize() {
		return executorThreadPoolSize;
	}

	/**
	 * Set the pool size of the executor service.
	 * @param executorThreadPoolSize the pool size of the executor service
	 */
	public void setExecutorThreadPoolSize(int executorThreadPoolSize) {
		this.executorThreadPoolSize = executorThreadPoolSize;
	}

	/**
	 * Get the reminder cleanup frequency for audit report cleanup.
	 * @return the reminder cleanup frequency for audit report cleanup
	 */
	public int getAuditCleanupFrequencyInSec() {
		return auditCleanupFrequencyInSec;
	}

	/**
	 * Set the reminder cleanup frequency for audit report cleanup.
	 * @param auditCleanupFrequencyInSec the reminder cleanup frequency for audit report cleanup
	 */
	public void setAuditCleanupFrequencyInSec(int auditCleanupFrequencyInSec) {
		this.auditCleanupFrequencyInSec = auditCleanupFrequencyInSec;
	}

	/**
	 * Get the retention policy for audit reports.
	 * @return the retention policy for audit reports
	 */
	public RetentionPolicy getRetentionPolicy() {
		return retentionPolicy;
	}

	/**
	 * Set the retention policy for audit reports.
	 * @param retentionPolicy the retention policy for audit reports
	 */
	public void setRetentionPolicy(RetentionPolicy retentionPolicy) {
		this.retentionPolicy = retentionPolicy;
	}
	
	
	public String getUpdateSystemConfigStatusMsg() {
		return updateSystemConfigStatusMsg;
	}

	public void setUpdateSystemConfigStatusMsg(String updateSystemConfigStatusMsg) {
		this.updateSystemConfigStatusMsg = updateSystemConfigStatusMsg;
	}
	
	/**
	 * Check to load system configuration from file.
	 * @return true if load system configuration from file is enabled
	 */
	public boolean isLoadFromFile() {
		return isLoadFromFile;
	}

	/**
	 * Set to load system configuration from file.
	 * @param isLoadFromFile load system configuration from file
	 */
	public void setLoadFromFile(boolean isLoadFromFile) {
		this.isLoadFromFile = isLoadFromFile;
	}

	/**
	 * Get the default HTTP for processing URL tasks.
	 * @return the default HTTP for processing URL tasks
	 */
	public HttpMethod getDefaultHTTPMethod() {
		return defaultHTTPMethod;
	}

	/**
	 * Set the default HTTP for processing URL tasks.
	 * @param defaultHTTPMethod the default HTTP for processing URL tasks
	 */
	public void setDefaultHTTPMethod(HttpMethod defaultHTTPMethod) {
		this.defaultHTTPMethod = defaultHTTPMethod;
	}

	/**
	 * Get the default {code @RequestConfig} for the HTTP method.
	 * @param method the HTTP method
	 * @return {code @RequestConfig} for the HTTP method
	 */
	public RequestConfig getDefaultRequestConfig(HttpMethod method) {
		return getDefaultRequestConfigMap().get(method);
	}

	public static class RetentionPolicy {
		//Max time for which lightning report will be retained
		private long lightningReportRetentionTimeInMillis = TimeUnit.DAYS.toMillis(7);

		//Max time for which tasks inside lightning report will be retained
		private long taskRetentionTimeInMillis = TimeUnit.MINUTES.toMillis(10);

		//Max number of audit report that can exist in the store
		private long maxLightningReportRetentionCount = 50000;

		/**
		 * Get the retention time for request tasks.
		 * @return the retention time for request tasks
		 */
		public long getTaskRetentionTimeInMillis() {
			return taskRetentionTimeInMillis;
		}

		/**
		 * Set the retention time for request tasks.
		 * @param taskRetentionTimeInMillis the retention time for request tasks
		 */
		public void setTaskRetentionTimeInMillis(long taskRetentionTimeInMillis) {
			this.taskRetentionTimeInMillis = taskRetentionTimeInMillis;
		}

		/**
		 * Get the maximum count of audit reports.
		 * @return the maximum count of audit reports
		 */
		public long getMaxLightningReportRetentionCount() {
			return maxLightningReportRetentionCount;
		}

		/**
		 * Set the maximum count of audit reports.
		 * @param maxLightningReportRetentionCount the maximum count of audit reports
		 */
		public void setMaxLightningReportRetentionCount(long maxLightningReportRetentionCount) {
			this.maxLightningReportRetentionCount = maxLightningReportRetentionCount;
		}

		/**
		 * Get the retention time for audit reports.
		 * @return the retention time for audit reports
		 */
		public long getLightningReportRetentionTimeInMillis() {
			return lightningReportRetentionTimeInMillis;
		}

		/**
		 * Set the retention time for audit reports.
		 * @param lightningReportRetentionTimeInMillis the retention time for audit reports
		 */
		public void setLightningReportRetentionTimeInMillis(long lightningReportRetentionTimeInMillis) {
			this.lightningReportRetentionTimeInMillis = lightningReportRetentionTimeInMillis;
		}
	}

	/**
	 * Get the default {code @RequestConfig} map.
	 * @return {code @RequestConfig} map
	 */
	public Map<HttpMethod, RequestConfig> getDefaultRequestConfigMap() {
		return defaultRequestConfigMap;
	}

	/**
	 * Set the default {code @RequestConfig} map.
	 * @param defaultRequestConfigMap the {code @RequestConfig} map
	 */
	public void setDefaultRequestConfigMap(Map<HttpMethod, RequestConfig> defaultRequestConfigMap) {
		this.defaultRequestConfigMap = defaultRequestConfigMap;
	}
}
