package com.ebay.lightning.core.workers;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

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

import junit.framework.Assert;

public class SocketBasedHTTPWorkerTest {

	private SocketBasedHTTPWorker socketBasedHTTPWorker;

	@Test
	public void testExecute() throws Exception {
		final InetSocketAddressCache inetCache = new InetSocketAddressCache(new SystemConfig());
		final ExecutionDataStore store = new ExecutionDataStore(new SystemConfig());
		final RequestConfig config = new RequestConfig();
		final SystemConfig systemConfig = new SystemConfig();
		config.loadDefaultValues(systemConfig);
		config.setConnectTimeoutMillis(1000);
		config.setReadWriteTimeoutMillis(1000);

		this.socketBasedHTTPWorker = new SocketBasedHTTPWorker(inetCache, store, systemConfig, config);

		final List<Task> tasks = new ArrayList<>();
		final BufferedReader reader = null;
		try {

			tasks.add(new URLTask("http://localhost/"));

			final LightningRequest request = new LightningRequest("abcdtest", tasks, new ReservationReceipt(State.ACCEPTED, "abcd", 500));
			store.register(request);
			Assert.assertEquals(true, WorkerState.NEVER_STARTED.equals(this.socketBasedHTTPWorker.getCurrentState()));
			this.socketBasedHTTPWorker.execute("abcdtest");
			Assert.assertEquals(true, WorkerState.IDLE.equals(this.socketBasedHTTPWorker.getCurrentState()));
		} catch(final Exception e) {
			e.printStackTrace();
			// Assert.fail();
		} finally {
			if (reader != null) {
				reader.close();
			}
		}
	}

	public static void main(String[] args) throws IOException {
		final InetSocketAddressCache inetCache = new InetSocketAddressCache(new SystemConfig());
		final ExecutionDataStore store = new ExecutionDataStore(new SystemConfig());
		final RequestConfig config = new RequestConfig();
		config.setConnectTimeoutMillis(1000);
		config.setReadWriteTimeoutMillis(2000);
		final SocketBasedHTTPWorker socketBasedHTTPWorker = new SocketBasedHTTPWorker(inetCache, store, new SystemConfig(), config);
		final List<Task> tasks = new ArrayList<>();
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(
					new FileReader(ClassLoader.getSystemClassLoader().getSystemResource("500sandbox-success-urls.txt").getPath()));
			String line = null;
			System.out.println("Started Reading file");
			while ((line = reader.readLine()) != null && tasks.size() < 10060) {
					tasks.add(new URLTask(line));
			}

			final LightningRequest request = new LightningRequest("abcdtest", tasks, new ReservationReceipt(State.ACCEPTED, "abcd", 500));
			store.register(request);
			Assert.assertEquals(true, WorkerState.IDLE.equals(socketBasedHTTPWorker.getCurrentState()));
			socketBasedHTTPWorker.execute("abcdtest");
			Assert.assertEquals(true, WorkerState.IDLE.equals(socketBasedHTTPWorker.getCurrentState()));
		} catch (final Exception e) {
			Assert.fail();
		} finally {
			if (reader != null) {
				reader.close();
			}
		}
	}

}
