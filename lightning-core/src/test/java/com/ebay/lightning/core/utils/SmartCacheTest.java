package com.ebay.lightning.core.utils;


import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.ebay.lightning.core.utils.SmartCache.ValueLoader;
import com.google.common.base.Stopwatch;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

/**
 * @author shashukla
 *
 */
public class SmartCacheTest {

	private SmartCache<String, String> quickerCache;
	private ValueLoader<String, String> slowLoader;

	@Before
	public void testSetup() {
		slowLoader =  new ValueLoader<String, String>() {
			@Override
			public String load(String k) throws Exception {
				Thread.sleep(4000);
				return "value";
			}
		};

		quickerCache = new SmartCache<String, String>(10000, 1, TimeUnit.SECONDS, slowLoader);
	}

	@Test
	public void testCacheSpeedTest() throws ExecutionException, InterruptedException {
		Stopwatch stopwatch = Stopwatch.createStarted();
		for (int cycle = 0; cycle <= 10; cycle++) {
			System.out.println("Starting Cycle #" + cycle);
			for (int count = 0; count < 400; count++) {
				final int c = count;
				ThreadingHelper.instance().execute(new Runnable() {
					@Override
					public void run() {
						try {
							quickerCache.get("key" + c);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				});
			}

			Thread.sleep(999);
		}
		long elapsed = stopwatch.elapsed(TimeUnit.SECONDS);
		Assert.assertTrue("Cache hits should complete under 10 seconds, actual: " + elapsed, elapsed <= 11);
	}
	
	@AfterClass
	public static void shutdown(){
		ThreadingHelper.instance().shutdown();
	}
	
	public static class ThreadingHelper {

		private static final Logger log = Logger.getLogger(ThreadingHelper.class);

		private BlockingQueue<Runnable> workQueue;
		private ThreadPoolExecutor taskExecutor;
		private static volatile ThreadingHelper instance;

		public static ThreadingHelper instance() {
			if (instance == null) {
				synchronized (ThreadingHelper.class) {
					if (instance == null) {
						instance = new ThreadingHelper(100, 100, 1, 20000, TimeUnit.MINUTES);
					}
				}
			}
			return instance;
		}

		private ThreadingHelper(int corePoolSize, int maximumPoolSize, int keepAliveTime, int workQueueSize, TimeUnit unit) {
			this.workQueue = new ArrayBlockingQueue<Runnable>(workQueueSize);
			taskExecutor = new ThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue,  new ThreadFactoryBuilder().setNameFormat("ThreadingHelper-%d").build());
		}

		public void execute(Runnable runnable) {
			try {
				 taskExecutor.execute(runnable);
			} catch (RejectedExecutionException re) {
				String msg = "Unable to excute a new thread: Available workQueue Size - " + workQueue.size();
				log.error(msg, re);
			} catch (Throwable re) {
				String msg = "Unable to excute a new thread: Available workQueue Size - " + workQueue.size();
				log.error(msg, re);
			}
		}

		public int getCurrentWorkQueueSize() {
			return taskExecutor.getQueue().size();
		}

		public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
			return taskExecutor.awaitTermination(timeout, unit);
		}

		@Override
		protected void finalize() throws Throwable {
			shutdownNow();
			super.finalize();
		}

		public List<Runnable> shutdownNow() {
			return taskExecutor.shutdownNow();
		}

		public void shutdown() {
			taskExecutor.shutdown();
		}

	}
	
}