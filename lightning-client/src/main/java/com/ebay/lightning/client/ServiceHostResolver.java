package com.ebay.lightning.client;

import java.util.AbstractMap;
import java.util.AbstractMap.SimpleEntry;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.log4j.Logger;

import com.ebay.lightning.client.caller.RestAPICaller;
import com.ebay.lightning.client.caller.ServiceCaller;
import com.ebay.lightning.client.config.LightningClientConfig;
import com.ebay.lightning.core.beans.ReservationReceipt;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

/**
 * {@code ServiceHostResolver} manages the seeds aka lightning core instances. The list of seeds to be managed 
 * are passed to the constructor via {link  LightningClientConfig}. The {@link LightningClient} routes the 
 * tasks to the seed returned by {@code ServiceHostResolver}.
 * 
 * @author shashukla
 * @see LightningClientConfig
 * @see RestAPICaller
 */
public class ServiceHostResolver {

	private static final Logger log = Logger.getLogger(ServiceHostResolver.class);

	private ServiceCaller apiCaller;
	private LightningClientConfig config;
	private ExecutorService executor;
	private int minimumSize = 1;

    /**
     * Constructs a new {@code ServiceHostResolver} object with seed management configuration
     *
     * @param config {@link LightningClientConfig} contains the seeds and configuration for seed management
     * @param apiCaller {@code ServiceCaller} makes calls to seed to check if it can process a request of specific load
     */
	public ServiceHostResolver(LightningClientConfig config, ServiceCaller apiCaller) {
		this.config = config;
		this.apiCaller = apiCaller;
		int seedSize = config.getSeeds() != null ? config.getSeeds().size() : 0;
		int threadPoolSize = seedSize > minimumSize ? seedSize : minimumSize;
		executor = Executors.newFixedThreadPool(threadPoolSize, new ThreadFactoryBuilder().setNameFormat("ServiceHostResolver-%d").build());
	}

	/**
	 * Get the next available seed that can handle the task.
	 * @param forLoad the load of the task to be executed by the seed
	 * @return the reservation receipt and the seed that accepted the pay load
	 * throws RuntimeException if none of the seeds accepted the request
	 */
	public SimpleEntry<ReservationReceipt, String> getNextEndPoint(final int forLoad) {

		List<String> seeds = getAllSeed(true);
		String errMsg = null;
		SimpleEntry<ReservationReceipt, String> nextEndPoint = null;
		try {
			nextEndPoint = getNextEndPoint(forLoad, seeds);
		} catch (Exception e) {
			errMsg = "Error getting Endpoint in my region: " + e.getMessage();
		}

		if (nextEndPoint == null && config.isAllowCrossRegionInteraction() && config.getCrossRegionSeeds() != null) {
			try {
				seeds = config.getCrossRegionSeeds();
				log.info(errMsg +", Now trying cross region seeds "+ seeds);
				nextEndPoint = getNextEndPoint(forLoad, seeds);
			} catch (Exception e) {
				errMsg = errMsg + ", Error getting Endpoint in cross-region: " + e.getMessage();
			}
		}

		if (nextEndPoint == null && errMsg != null) {
			throw new RuntimeException(errMsg);
		}

		return nextEndPoint;
	}

	/**
	 * Get the next available seed that can handle the task.
	 * @param forLoad the load of the task to be executed by the seed
	 * @param seeds	the list of seeds to check sequentially until one accepts
	 * @return the reservation receipt and the seed that accepted the pay load
	 * throws RuntimeException if none of the seeds accepted the load request
	 */
	private SimpleEntry<ReservationReceipt, String> getNextEndPoint(final int forLoad, List<String> seeds) {
		int retryAttempt = 0;

		if (seeds != null && !seeds.isEmpty()) {
			while (retryAttempt++ < config.getMaxRetryAttempt()) {

				for (int i = 0; i < seeds.size(); i++) {
					try {
						final String seedToCall = seeds.get(i);
						ReservationReceipt reservationRcptTemp = getReservationRcpt(forLoad, seedToCall);
						boolean denied = ReservationReceipt.State.DENIED.equals(reservationRcptTemp.getState());
						if (!denied) {
							return new AbstractMap.SimpleEntry<ReservationReceipt, String>(reservationRcptTemp, seedToCall);
						}
					} catch (Exception e) {
						log.error("Unable to get reservation on" + seeds.get(i), e);
					}
				}

				//if none of the seeds accept the reservation then get the first randomized seed
				log.info(String.format("None of the seeds [%s] have accepted my reservation for load [%d], retrying attmept: %d", seeds, forLoad,
						retryAttempt));

				//All core machines are busy, lets wait for some time before trying
				sleepFor(2000);
			}

			throw new RuntimeException(String.format("None of the seeds [%s] accepted  reservation for load [%d] even after %d retries", seeds,
					forLoad, config.getMaxRetryAttempt()));

		} else {
			throw new RuntimeException("No Seeds found ");
		}
	}

	/**
	 * Check if the seed can accept the request
	 * @param forLoad the load of the task to be executed by the seed
	 * @param seedToCall the seed to check
	 * @return the reservation receipt returned by the seed. Returns {@code null} if the seed
	 * do not respond within a second.
	 * throws RuntimeException if none of the seeds accepted the load request
	 */
	private ReservationReceipt getReservationRcpt(final int forLoad, final String seedToCall)
			throws InterruptedException, ExecutionException, TimeoutException {
		Future<ReservationReceipt> future = executor.submit(new Callable<ReservationReceipt>() {
			@Override
			public ReservationReceipt call() throws Exception {
				return apiCaller.reserve(forLoad, seedToCall);
			}
		});

		ReservationReceipt reservationRcptTemp = future.get(1000, TimeUnit.MILLISECONDS);
		return reservationRcptTemp;
	}

	private void sleepFor(int sleepTime) {
		try {
			Thread.sleep(sleepTime);
		} catch (InterruptedException e) {
			//ignore
		}
	}

	/**
	 * Gets the list of seeds from configuration and shuffles if required
	 * @return the list of seeds
	 */
	private List<String> getAllSeed(boolean shuffleSeeds) {
		List<String> result = config.getSeeds();
		if (shuffleSeeds) {
			java.util.Collections.shuffle(result);
		}
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#finalize()
	 */
	@Override
	protected void finalize() throws Throwable {
		super.finalize();
		executor.shutdownNow();
	}
}
