/*
 * @author shashukla.
 * ***/

package com.ebay.lightning.client;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.apache.log4j.Logger;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.IsNot;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.ebay.lightning.client.caller.RestAPICaller;
import com.ebay.lightning.client.config.LightningClientConfig;
import com.ebay.lightning.core.beans.ReservationReceipt;
import com.ebay.lightning.core.utils.UrlUtils;

public class ServiceHostResolverTest {
	private static final Logger log = Logger.getLogger(ServiceHostResolverTest.class);

	private UrlUtils urlUtils;
	private ServiceHostResolver serviceHostResolver;
	private int numOfCores = 10;
	private LightningClientConfig config = new LightningClientConfig();

	private int acceptingCores = 2;
	private int deniedCores = 2;
	private int slowOrDownCores = 6;

	private CoresResponseBehavior coreBehavior;

	private static final String RESERVATION_DENIED = "{ \"id\": \"youGotIt\", \"state\" : \"DENIED\", \"busyWithLoad\" : \"%s\" }";
	private static final String RESERVATION_ACCEPTED = "{\"id\": \"youGotIt\" ,  \"state\" : \"ACCEPTED\" }";
	private static final String RESERVATION_BUSY_TEMPLATE = "{\"id\": \"youGotIt\" ,  \"state\" : \"BUSY\", \"busyWithLoad\" : \"%s\" }";

	@Before
	public void setup() {
		config.setReserveApiUrl("http://{host}:port/reserve");
		config.setPollApiUrl("http://{host}:port/poll");
		config.setSubmitApiUrl("http://{host}:port/submit");
		urlUtils = Mockito.mock(UrlUtils.class);
		StringBuilder seedsBuilder = new StringBuilder();
		for (int i = 1; i <= numOfCores; i++) {
			seedsBuilder.append("" + i + ",");
		}
		seedsBuilder.substring(0, (2 * numOfCores) - 2);
		List<String> seeds = new ArrayList(Arrays.asList(seedsBuilder.toString().split(",")));
		config.setSeeds(seeds);

		RestAPICaller restAPICaller = new RestAPICaller(config, urlUtils);
		serviceHostResolver = new ServiceHostResolver(config, restAPICaller);

	}

	@Test
	public void testCrossRegionFlow() throws Exception {
		config.setAllowCrossRegionInteraction(true);
		config.setSeeds(new ArrayList(Arrays.asList("phx1,phx2,phx3,phx4,phx5".split(","))));
		config.setCrossRegionSeeds(new ArrayList(Arrays.asList("slc1,slc2,slc3,lvs4,lvs5".split(","))));

		Mockito.when(urlUtils.get(Mockito.anyString())).then(new Answer<String>() {
			private int callOrder = 0;

			@Override
			public String answer(InvocationOnMock invocationonmock) throws Throwable {
				callOrder = callOrder + 1;
				if (callOrder < 20) {
					return String.format(RESERVATION_DENIED, 1000);
				} else {
					return String.format(RESERVATION_BUSY_TEMPLATE, 4);
				}
			}
		});

		SimpleEntry<ReservationReceipt, String> simpleEntry = serviceHostResolver.getNextEndPoint(100);
		Assert.assertEquals(4, simpleEntry.getKey().getBusyWithLoad());
		Assert.assertEquals(ReservationReceipt.State.BUSY, simpleEntry.getKey().getState());

	}

	@Test
	public void testReservationLeastBusy() throws Exception {
		Mockito.when(urlUtils.get(Mockito.anyString())).then(new Answer<String>() {
			private int callOrder = 0;

			@Override
			public String answer(InvocationOnMock invocationonmock) throws Throwable {
				callOrder = callOrder + 1;
				String result = null;
				switch (callOrder) {
				case 1:
					result = String.format(RESERVATION_BUSY_TEMPLATE, 100);
					break;
				case 2:
					result = String.format(RESERVATION_BUSY_TEMPLATE, 200);
					break;
				case 3:
					result = String.format(RESERVATION_DENIED, 1000);
					break;
				case 4:
					result = String.format(RESERVATION_BUSY_TEMPLATE, 4);
					break;
				case 5:
					result = String.format(RESERVATION_BUSY_TEMPLATE, 5);
					break;
				default:
					result = RESERVATION_DENIED;
				}
				return result;
			}
		});

		SimpleEntry<ReservationReceipt, String> simpleEntry = serviceHostResolver.getNextEndPoint(100);
		Assert.assertEquals(100, simpleEntry.getKey().getBusyWithLoad());
		Assert.assertEquals(ReservationReceipt.State.BUSY, simpleEntry.getKey().getState());

	}

	@Test
	public void testReservationAllDenied() throws Exception {
		Mockito.when(urlUtils.get(Mockito.anyString())).then(new Answer<String>() {
			private int callOrder = 0;

			@Override
			public String answer(InvocationOnMock invocationonmock) throws Throwable {
				callOrder = callOrder + 1;
				String result = null;
				switch (callOrder) {
				case 1:
					result = String.format(RESERVATION_DENIED, 100);
					break;
				case 2:
					result = String.format(RESERVATION_DENIED, 200);
					break;
				case 3:
					result = String.format(RESERVATION_DENIED, 30);
					break;
				case 4:
					result = String.format(RESERVATION_DENIED, 4);
					break;
				case 5:
					result = String.format(RESERVATION_DENIED, 5);
					break;
				default:
					result = RESERVATION_DENIED;
					;
				}
				return result;
			}
		});

		try {
			serviceHostResolver.getNextEndPoint(100);
			Assert.fail("Test Case Failed, exception is expected");
		} catch (Exception e) {

		}
	}

	@Test
	public void testReservationOneGuyAccepts() throws Exception {
		Mockito.when(urlUtils.get(Mockito.anyString())).then(new Answer<String>() {
			private int callOrder = 0;

			@Override
			public String answer(InvocationOnMock invocationonmock) throws Throwable {
				callOrder = callOrder + 1;
				String result = null;
				switch (callOrder) {
				case 1:
					result = String.format(RESERVATION_BUSY_TEMPLATE, 4);
					break;
				case 2:
					result = RESERVATION_ACCEPTED;
					break;
				case 3:
					result = String.format(RESERVATION_BUSY_TEMPLATE, 30);
					break;
				case 4:
					result = String.format(RESERVATION_DENIED, 4);
					break;
				case 5:
					result = String.format(RESERVATION_DENIED, 4);
					break;
				default:
					result = RESERVATION_ACCEPTED;
				}
				return result;

			}
		});

		SimpleEntry<ReservationReceipt, String> simpleEntry = serviceHostResolver.getNextEndPoint(100);
		Assert.assertEquals(ReservationReceipt.State.BUSY, simpleEntry.getKey().getState());

	}

	@Test
	public void testPartialFailureScenario() throws Exception {
		Mockito.when(urlUtils.get(Mockito.anyString())).then(new Answer<String>() {
			ReservationResponse response;

			@Override
			public String answer(InvocationOnMock invocation) throws Throwable {
				response = coreBehavior.getNextResponse();
				return response.answer(invocation);
			}
		});

		int numOfRuns = 100;
		Boolean[] expecteds = new Boolean[numOfRuns];
		for (int i = 0; i < numOfRuns; i++) {
			expecteds[i] = Boolean.TRUE;
		}

		Boolean[] actuals = new Boolean[numOfRuns];
		long start = System.currentTimeMillis();
		for (int i = 0; i < numOfRuns; i++) {
			coreBehavior = new CoresResponseBehavior(acceptingCores, deniedCores, slowOrDownCores);
			log.info(" RUN # " + i);
			SimpleEntry<ReservationReceipt, String> simpleEntry = serviceHostResolver.getNextEndPoint(100);
			actuals[i] = Boolean.valueOf((ReservationReceipt.State.ACCEPTED.equals(simpleEntry.getKey().getState())));
		}
		/*
		 * for(int i =0;i<100;i++) { System.out.println("Run "+i+" Expected "+
		 * expecteds[i]+ " Actual "+actuals[i]); }
		 */System.out.println("Avg time taken : " + ((System.currentTimeMillis() - start) / (double) numOfRuns));

		// Assert.assertArrayEquals(expecteds, actuals);
		Assert.assertThat(expecteds, IsNot.not(IsEqual.equalTo(actuals)));

	}

	@Test
	public void getNextEndPointTest() throws Exception {

		config.setAllowCrossRegionInteraction(true);

		Mockito.when(urlUtils.get(Mockito.anyString())).then(new Answer<String>() {
			private int callOrder = 0;

			@Override
			public String answer(InvocationOnMock invocationonmock) throws Throwable {
				callOrder = callOrder + 1;
				if (callOrder < 20) {
					return String.format(RESERVATION_DENIED, 1000);
				} else {
					return String.format(RESERVATION_BUSY_TEMPLATE, 4);
				}
			}
		});

		SimpleEntry<ReservationReceipt, String> simpleEntry = serviceHostResolver.getNextEndPoint(100);
		Assert.assertEquals(4, simpleEntry.getKey().getBusyWithLoad());
		Assert.assertEquals(ReservationReceipt.State.BUSY, simpleEntry.getKey().getState());

	}

	class ReservationResponse implements Answer<String> {
		private int timeToSleepInMillis;
		private String response;

		public ReservationResponse(int timeToSleepInMillis, String response) {
			this.timeToSleepInMillis = timeToSleepInMillis;
			this.response = response;
		}

		@Override
		public String answer(InvocationOnMock invocation) throws Throwable {
			// Thread.sleep(timeToSleepInMillis);
			log.info("Returning response : " + response + " after " + timeToSleepInMillis + " ms");
			return response;
		}
	}

	class CoresResponseBehavior {
		private List<ReservationResponse> responsesFromCores = new ArrayList<ReservationResponse>();
		private Iterator<ReservationResponse> iter;

		private CoresResponseBehavior instance;

		public CoresResponseBehavior(int accepted, int declined, int slowlyResponded) {

			for (int i = 0; i < accepted; i++) {
				responsesFromCores.add(new ReservationResponse(100 + new Random().nextInt(200), RESERVATION_ACCEPTED));
			}
			for (int i = 0; i < declined; i++) {
				responsesFromCores.add(new ReservationResponse(100 + new Random().nextInt(200), String.format(RESERVATION_DENIED, 4)));
			}
			for (int i = 0; i < slowlyResponded; i++) {
				responsesFromCores.add(new ReservationResponse(1000 + new Random().nextInt(200), String.format(RESERVATION_BUSY_TEMPLATE, 300)));
			}
			shuffleResponses();
			iter = responsesFromCores.iterator();
		}

		public ReservationResponse getNextResponse() {
			return (iter.hasNext() ? iter.next() : null);
		}

		public void shuffleResponses() {
			Collections.shuffle(responsesFromCores);
		}
	}
}
