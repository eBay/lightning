package com.ebay.lightning.client;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

import com.ebay.lightning.client.caller.LightningResponseCallback;
import com.ebay.lightning.core.LightningCoreMain;
import com.ebay.lightning.core.beans.LightningResponse;
import com.ebay.lightning.core.beans.Task;
import com.ebay.lightning.core.beans.URLTask;

public class RestApiLightningTester {

	private static final Logger log = Logger.getLogger(RestApiLightningTester.class);
	
	private final int port = 8989;
	private volatile LightningResponse response = null;
	
	@Test
	public void testRestApiLightning(){
		try{
			startLightningCore();
			startLightningClient();
		}catch(Exception e){
			log.error(e);
		}
	}
	
	private void startLightningCore(){
		LightningCoreMain.main("server.port="+port);
		log.info("Started Lightning core on port "+ port);
	}

	
	private void startLightningClient() throws Exception{
		LightningClient client = new LightningClientBuilder().addSeed("localhost").setCorePort(port).build();

		List<Task> tasks = generateTasks();

		LightningResponseCallback callback = new LightningResponseCallback() {
			
			@Override
			public void onTimeout(LightningResponse response) {
				log.info("Callback listener timed-out.");
				log.info("Requests completed: "+ response.getSuccessCount()+response.getFailedResponses().size());
			}
			
			@Override
			public void onComplete(LightningResponse response) {
				log.info("Request execution complete.");
			    int failureCount = response.getFailedResponses().size();
			    if (failureCount > 0) {
			      log.info("One or more requests failed.");
			    } else {
			    	log.info("All results are successful with HTTP 200.");
			    }
			    log.info(response.prettyPrint());
			}
		};
		
		client.submitWithCallback(tasks, callback, 2000);
		Thread.sleep(3000);
		Assert.assertNotNull(response);
		Assert.assertEquals(2, response.getTotalCount());
		
	}

	private static List<Task> generateTasks()throws URISyntaxException{
		List<Task> tasks = new ArrayList<Task>();
		tasks.add(new URLTask("http://www.ebay.com"));
		tasks.add(new URLTask("http://www.google.com"));
		return tasks;
	}

}
