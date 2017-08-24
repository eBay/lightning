package com.ebay.example;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.ebay.lightning.client.LightningClient;
import com.ebay.lightning.client.LightningClientBuilder;
import com.ebay.lightning.client.caller.LightningResponseCallback;
import com.ebay.lightning.core.LightningCoreMain;
import com.ebay.lightning.core.beans.LightningResponse;
import com.ebay.lightning.core.beans.Task;
import com.ebay.lightning.core.beans.URLTask;

public class QuickRun {

	private static final Logger log = Logger.getLogger(QuickRun.class);
	private static int port = 8989;

	public static void main(String[] args) throws Exception {
		startLightningCore();
		startLightningClient();
		System.exit(0);
	}

	private static void startLightningCore() {
		LightningCoreMain.main("server.port=" + port);
		log.info("Started Lightning core on port " + port);
	}

	private static void startLightningClient() throws Exception {
		LightningClient client = new LightningClientBuilder().addSeed("localhost").setCorePort(port).build();

		int taskCount = 5;
		List<Task> tasks = generateTasks(taskCount);

		LightningResponseCallback callback = new LightningResponseCallback() {
			@Override
			public void onTimeout(LightningResponse response) {
				log.info("Callback listener timed-out.");
				log.info("No of requests completed: " + response.getSuccessCount()
						+ response.getFailedResponses().size());
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
	}

	private static List<Task> generateTasks(int taskCount) {
		List<Task> tasks = new ArrayList<Task>();
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new InputStreamReader(QuickRun.class.getResourceAsStream("/sampleUrls.txt")));
			String line = null;
			System.out.println("Started Reading file");
			while ((line = reader.readLine()) != null && tasks.size() < taskCount) {
				tasks.add(new URLTask(line));
			}
		} catch (Exception e) {
			log.error("Error reading stream", e);
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
					log.error("Error closing stream", e);
				}
			}
		}
		return tasks;
	}

}
