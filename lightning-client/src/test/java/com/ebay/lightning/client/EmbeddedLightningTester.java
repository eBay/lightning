/**
 *
 */
package com.ebay.lightning.client;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;

import com.ebay.lightning.client.caller.LightningResponseCallback;
import com.ebay.lightning.core.beans.LightningRequest;
import com.ebay.lightning.core.beans.LightningResponse;
import com.ebay.lightning.core.beans.Task;
import com.ebay.lightning.core.beans.URLTask;
import com.ebay.lightning.core.store.LightningRequestReport;
import com.google.common.base.Stopwatch;

import junit.framework.Assert;

/**
 * @author shashukla
 *
 */
public class EmbeddedLightningTester {

	private static final Logger log = Logger.getLogger(EmbeddedLightningTester.class);

	private int taskCount = 5000;
	private LightningClient client;
	private List<Task> tasks;

	public static void main(String[] args) throws Exception {
		final EmbeddedLightningTester embeddedTester = new EmbeddedLightningTester();
		try {
			embeddedTester.taskCount = Integer.parseInt(args[0]);
		} catch (final Exception e) {
			log.error("Please provide task size as program argument! default is " + embeddedTester.taskCount);
		}

		embeddedTester.setup();
		embeddedTester.testSubmit();
		embeddedTester.testSubmit();
		embeddedTester.testgetAuditSummary();

	}

	@Before
	public void setup(){
		final LightningClientBuilder builder = new LightningClientBuilder();
		final List<String> seeds = new ArrayList<>();
		seeds.add("localhost");
		this.client = builder.setEmbeddedMode(true).build();
		this.tasks = generateTasks(this.taskCount);
	}

	@Test
	public void testMain() throws Exception {
		main(null);
		final String[] a = { "1" };
		main(a);
		org.junit.Assert.assertTrue(true);
	}

	@Test
	public void testSubmit() throws Exception {
		final LightningResponseCallback callback = new LightningResponseCallback() {
			
			@Override
			public void onTimeout(LightningResponse response) {
				log.info("Callback listener timed-out.");
				log.info("Requests completed: "+ response.getSuccessCount()+response.getFailedResponses().size());
			}
			
			@Override
			public void onComplete(LightningResponse response) {
				log.info("Request execution complete.");
			    final int failureCount = response.getFailedResponses().size();
			    if (failureCount > 0) {
			      log.info("One or more requests failed.");
			    } else {
			    	log.info("All results are successful with HTTP 200.");
			    }
			    log.info(response.prettyPrint());
			}
		};
		
		this.client.submitWithCallback(this.tasks, callback, 2000);
		Thread.sleep(4000);
	}


	@Test
	public void testgetAuditSummary() throws Exception {
		final LightningRequest req = this.client.submit(this.tasks);
		final List<LightningRequestReport> response = this.client.getAuditSummary(req.getServingHostIp(), req.getSessionId());
		Assert.assertNotNull(response);
		Assert.assertEquals(true, !response.isEmpty());
	}

	private static List<Task> generateTasks(int taskCount) {
		final Stopwatch stopwatch = Stopwatch.createStarted();
		final List<Task> tasks = new ArrayList<>();
			tasks.add(new URLTask("http://www.ebay.com"));
			tasks.add(new URLTask("http://localhost:8989"));

		log.info("time taken in secods: " + stopwatch.stop().elapsed(TimeUnit.SECONDS));
		return tasks;
	}

}
