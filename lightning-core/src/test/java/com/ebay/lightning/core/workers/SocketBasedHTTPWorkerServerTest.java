package com.ebay.lightning.core.workers;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.web.EmbeddedServletContainerAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;

import com.ebay.lightning.core.beans.LightningRequest;
import com.ebay.lightning.core.beans.ReservationReceipt;
import com.ebay.lightning.core.beans.ReservationReceipt.State;
import com.ebay.lightning.core.beans.Task;
import com.ebay.lightning.core.beans.URLTask;
import com.ebay.lightning.core.config.RequestConfig;
import com.ebay.lightning.core.config.SystemConfig;
import com.ebay.lightning.core.constants.LightningCoreConstants.WorkerState;
import com.ebay.lightning.core.store.ExecutionDataStore;
import com.ebay.lightning.core.utils.InetSocketAddressCache;
import com.ebay.lightning.testing.SimpleHttpServer;

public class SocketBasedHTTPWorkerServerTest {
	private static String baseUrl; 
	private SocketBasedHTTPWorker socketBasedHTTPWorker;
	
	private static ConfigurableApplicationContext context;
	
	@BeforeClass
	public static void initialize(){
		SpringApplication app = new SpringApplication(new Object[] { SimpleHttpServer.class, EmbeddedServletContainerAutoConfiguration.class });
		String[] arguments = new String[1];
		arguments[0] = "LightningCoreTest";
		context = app.run(arguments);
		baseUrl = String.format("http://localhost:%d/test/", SimpleHttpServer.serverPort);
	}
	
	@Test
	public void testRetryCall() throws Exception{
		InetSocketAddressCache inetCache = new InetSocketAddressCache(new SystemConfig());
		ExecutionDataStore store = new ExecutionDataStore(new SystemConfig());
		RequestConfig config = new RequestConfig();
		config.setRetryFailedTasks(true);
		SystemConfig systemConfig = new SystemConfig();
		config.loadDefaultValues(systemConfig);
		config.setConnectTimeoutMillis(1000);
		config.setReadWriteTimeoutMillis(1000);
		
		socketBasedHTTPWorker = new SocketBasedHTTPWorker(inetCache, store, systemConfig, config);
		
		List<Task> tasks = new ArrayList<>();
		try {
			tasks.add(new URLTask(baseUrl + "timedEcv"));
			tasks.add(new URLTask("http://1test517:8080/test")); //unresolved host
			tasks.add(new URLTask(baseUrl+"error"));
			LightningRequest request = new LightningRequest("abcdtest", tasks, new ReservationReceipt(State.ACCEPTED, "abcd", 500));
			store.register(request);
			Assert.assertEquals(true, WorkerState.NEVER_STARTED.equals(socketBasedHTTPWorker.getCurrentState()));
			socketBasedHTTPWorker.execute("abcdtest");
			Assert.assertEquals(true, WorkerState.IDLE.equals(socketBasedHTTPWorker.getCurrentState()));
			Thread.sleep(4000);
		} catch(Exception e) {
			Assert.fail();
		}
	}
	
	@Test
	public void testConfiguration() throws Exception{
		InetSocketAddressCache inetCache = new InetSocketAddressCache(new SystemConfig());
		ExecutionDataStore store = new ExecutionDataStore(new SystemConfig());
		RequestConfig config = new RequestConfig();
		SystemConfig systemConfig = new SystemConfig();
		config.loadDefaultValues(systemConfig);
		config.setConnectTimeoutMillis(1000);
		config.setReadWriteTimeoutMillis(1000);
		
		socketBasedHTTPWorker = new SocketBasedHTTPWorker(inetCache, store, systemConfig, config);

		int slowUrlsConnectTimeoutMillis = 1000;
		socketBasedHTTPWorker.setSlowUrlsConnectTimeoutMillis(slowUrlsConnectTimeoutMillis);
		Assert.assertEquals(slowUrlsConnectTimeoutMillis, socketBasedHTTPWorker.getSlowUrlsConnectTimeoutMillis());
		
		int slowUrlsReadWriteTimeoutMillis = 100;
		socketBasedHTTPWorker.setSlowUrlsReadWriteTimeoutMillis(slowUrlsReadWriteTimeoutMillis);
		Assert.assertEquals(slowUrlsReadWriteTimeoutMillis, socketBasedHTTPWorker.getSlowUrlsReadWriteTimeoutMillis());
		
		int readAccuracyPercent = 90;
		socketBasedHTTPWorker.setReadAccuracyPercent(readAccuracyPercent);
		Assert.assertEquals(readAccuracyPercent, socketBasedHTTPWorker.getReadAccuracyPercent());
		
		int connectAccuracyPercent = 95;
		socketBasedHTTPWorker.setConnectAccuracyPercent(connectAccuracyPercent);
		Assert.assertEquals(connectAccuracyPercent, socketBasedHTTPWorker.getConnectAccuracyPercent());
		
	}
	
	@AfterClass
	public static void shutDown(){
		SpringApplication.exit(context);
	}
}
