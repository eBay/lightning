package com.ebay.lightning.core.utils;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

/**
 * Helper class to execute tasks asynchronously
 * 
 * @author shashukla
 *
 */
public class ExecutorUtil {

	private ExecutorService es = null;

	public ExecutorUtil(int threadPoolSize) {
		es = Executors.newFixedThreadPool(threadPoolSize, new ThreadFactoryBuilder().setNameFormat("ExecutorUtil-%d").build());
	}
	
	/**
	 * Submit the task to be executed asynchronously.
	 * @param call the method to execute
	 * @param <T> the task parameter
	 * @return the {@link Future} corresponding to the task
	 */
	public <T> Future<T> submit(Callable<T> call) {
		return es.submit(call);
	}

	/**
	 * Shutdown the executor service.
	 */
	public void shutdownNow() {
		es.shutdownNow();
	}

	/* (non-Javadoc)
	 * @see {@link Object#finalize()}
	 */
	@Override
	protected void finalize() throws Throwable {
		super.finalize();
		shutdownNow();
	}
}
