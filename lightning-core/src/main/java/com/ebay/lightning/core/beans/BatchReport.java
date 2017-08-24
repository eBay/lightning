package com.ebay.lightning.core.beans;

import java.io.Serializable;

import com.ebay.lightning.core.store.LightningRequestReport;

/**
 * The {@code BatchReport} holds audit information for the request executed in batches.
 * 
 * @author shashukla
 * @see LightningRequest
 * @see LightningRequestReport
 */
public class BatchReport implements Serializable{

	private static final long serialVersionUID = 1L;

	private int batchId;
	private long executionTime;
	private int successCount;
	private int batchSize;
	private int connectFailureCount;
	private int readWriteFailureCount;
	private int currentInetCacheSize;
	private long connectTimeInMillis;
	private long readWriteTimeInMillis;
	private long inetSocketAddressCreateTimeInMillis;
	private long cleanupTimeInMillis;

	public BatchReport(int batchSize) {
		this.batchSize = batchSize;
	}

	/**
	 * Get the batch Id.
	 * @return the batch Id
	 */
	public int getBatchId() {
		return batchId;
	}

	/**
	 * Set the batch Id.
	 * @param batchId the batch Id
	 */
	public void setBatchId(int batchId) {
		this.batchId = batchId;
	}

	/**
	 * Get the total execution time of all tasks in the batch.
	 * @return the total execution time of all tasks in the batch
	 */
	public long getExecutionTime() {
		return executionTime;
	}

	/**
	 * Set the total execution time of all tasks in the batch.
	 * @param executionTime the total execution time of all tasks in the batch
	 */
	public void setExecutionTime(long executionTime) {
		this.executionTime = executionTime;
	}

	/**
	 * Get the total count of tasks in the batch that failed to connect.
	 * @return the total count of tasks in the batch that failed to connect
	 */
	public int getConnectFailureCount() {
		return connectFailureCount;
	}

	/**
	 * Set the total count of tasks in the batch that failed to connect.
	 * @param connectFailureCount the total count of tasks in the batch that failed to connect
	 */
	public void setConnectFailureCount(int connectFailureCount) {
		this.connectFailureCount = connectFailureCount;
	}

	/**
	 * Get the total count of tasks in the batch that failed to read/write.
	 * @return the total count of tasks in the batch that failed to read/write
	 */
	public int getReadWriteFailureCount() {
		return readWriteFailureCount;
	}

	/**
	 * Set the total count of tasks in the batch that failed to read/write.
	 * @param readWriteFailureCount the total count of tasks in the batch that failed to read/write
	 */
	public void setReadWriteFailureCount(int readWriteFailureCount) {
		this.readWriteFailureCount = readWriteFailureCount;
	}

	/**
	 * Get the total count of successful tasks in the batch.
	 * @return the total count of successful tasks in the batch
	 */
	public int getSuccessCount() {
		return successCount;
	}

	/**
	 * Set the total count of successful tasks in the batch.
	 * @param successCount the total count of successful tasks in the batch
	 */
	public void setSuccessCount(int successCount) {
		this.successCount = successCount;
	}

	/**
	 * Get the inet cache size after complete execution of the batch.
	 * @return the inet cache size after complete execution of the batch
	 */
	public int getCurrentInetCacheSize() {
		return currentInetCacheSize;
	}

	/**
	 * Set the inet cache size after complete execution of the batch.
	 * @param currentInetCacheSize the inet cache size after complete execution of the batch
	 */
	public void setCurrentInetCacheSize(int currentInetCacheSize) {
		this.currentInetCacheSize = currentInetCacheSize;
	}

	/* (non-Javadoc)
	 * @see {@link Object#toString()}
	 */
	@Override
	public String toString() {
		return "\nBatchReport [Id=" + batchId + ", [totalCount=success+connectFail+readWriteFail]=[" + batchSize + "=" + successCount + "+"
				+ connectFailureCount + "+" + readWriteFailureCount + "], [totalTime=connect+read-write+socketCreateTime+clnup]=[" + executionTime + "="
				+ connectTimeInMillis + "+" + readWriteTimeInMillis + "+" + inetSocketAddressCreateTimeInMillis + "+" + cleanupTimeInMillis +"] ,currentInetCacheSize="
				+ currentInetCacheSize + "]";
	}

	/**
	 * Get the total time taken to connect for all tasks in the batch.
	 * @return the total time taken to connect for all tasks in the batch
	 */
	public long getConnectTimeInMillis() {
		return connectTimeInMillis;
	}

	/**
	 * Set the total time taken to connect for all tasks in the batch.
	 * @param connectTimeInMillis the total time taken to connect for all tasks in the batch
	 */
	public void setConnectTimeInMillis(long connectTimeInMillis) {
		this.connectTimeInMillis = connectTimeInMillis;
	}

	/**
	 * Get the total time taken to read/write for all tasks in the batch.
	 * @return the total time taken to read/write for all tasks in the batch
	 */
	public long getReadWriteTimeInMillis() {
		return readWriteTimeInMillis;
	}

	/**
	 * Set the total time taken to read/write for all tasks in the batch.
	 * @param readWriteTimeInMillis the total time taken to read/write for all tasks in the batch
	 */
	public void setReadWriteTimeInMillis(long readWriteTimeInMillis) {
		this.readWriteTimeInMillis = readWriteTimeInMillis;
	}

	/**
	 * Get the total time taken to build inet cache for the batch.
	 * @return the total time taken to build inet cache for the batch
	 */
	public long getInetSocketAddressCreateTimeInMillis() {
		return inetSocketAddressCreateTimeInMillis;
	}

	/**
	 * Set the total time taken to build inet cache for the batch.
	 * @param inetSocketAddressCreateTimeInMillis the total time taken to build inet cache for the batch
	 */
	public void setInetSocketAddressCreateTimeInMillis(long inetSocketAddressCreateTimeInMillis) {
		this.inetSocketAddressCreateTimeInMillis = inetSocketAddressCreateTimeInMillis;
	}

	/**
	 * Get the size of the batch.
	 * @return the size of the batch
	 */
	public int getBatchSize() {
		return batchSize;
	}

	/**
	 * Set the size of the batch.
	 * @param batchSize the size of the batch
	 */
	public void setBatchSize(int batchSize) {
		this.batchSize = batchSize;
	}

	/**
	 * Get the total time taken to cleanup all tasks after execution in the batch.
	 * @return the total time taken to cleanup all tasks after execution in the batch
	 */
	public long getCleanupTimeInMillis() {
		return cleanupTimeInMillis;
	}

	/**
	 * Set the total time taken to cleanup all tasks after execution in the batch.
	 * @param cleanupTimeInMillis the total time taken to cleanup all tasks after execution in the batch
	 */
	public void setCleanupTimeInMillis(long cleanupTimeInMillis) {
		this.cleanupTimeInMillis = cleanupTimeInMillis;
	}

}
