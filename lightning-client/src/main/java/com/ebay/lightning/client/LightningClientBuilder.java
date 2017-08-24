package com.ebay.lightning.client;

import java.util.ArrayList;
import java.util.List;

import com.ebay.lightning.client.caller.EmbeddedAPICaller;
import com.ebay.lightning.client.caller.RestAPICaller;
import com.ebay.lightning.client.caller.ServiceCaller;
import com.ebay.lightning.client.config.LightningClientConfig;
import com.ebay.lightning.core.config.SystemConfig;
import com.ebay.lightning.core.manager.TaskExecutionManager;
import com.ebay.lightning.core.services.TaskExecutionService;
import com.ebay.lightning.core.services.TaskExecutionServiceImpl;
import com.ebay.lightning.core.store.ExecutionDataStore;
import com.ebay.lightning.core.utils.InetSocketAddressCache;
import com.ebay.lightning.core.utils.UrlUtils;

/**
 * <p>
 * This is a builder class to create an instance of {@link LightningClient} with
 * all required dependencies.
 * </p>
 *
 * <p>
 * The default implementation of {@link LightningClient} returned submits task
 * to one of the many stand alone instances of lightning core through Rest API.
 * One or many lightning core instances has to be registered with the lightning
 * client via {@link #setSeeds(List)} method before calling the {@link #build()}
 * method.
 * </p>
 *
 * <pre>
 * List&lt;String&gt; seeds = new ArrayList&lt;String&gt;();
 * seeds.add("hostname1");
 * seeds.add("hostname2");
 * LightningClient client = new LightningClientBuilder()..addSeed("hostname1").setCorePort(8989).build();
 * </pre>
 *
 * <p>
 * The lightning core instance can be run in embedded mode by calling
 * {@link #setEmbeddedMode(boolean)} before invoking the {@link #build()}
 * method.
 * </p>
 *
 * <pre>
 * LightningClient client = new LightningClientBuilder().setEmbeddedMode(true).build();
 * </pre>
 *
 * @author shashukla
 * @see LightningClient
 */
public class LightningClientBuilder {

	private UrlUtils urlUtils = new UrlUtils();
	private List<String> seeds;
	private List<String> crossRegionSeeds;

	private String pollApiUrl = "http://{host}:port/l/poll";
	private String reserveApiUrl = "http://{host}:port/l/reserve";
	private String submitApiUrl = "http://{host}:port/l/submit";
	private String auditApiUrl = "http://{host}:port/l/audit";
	private String auditJsonApiUrl = "http://{host}:port/l/audit/json";
	private String auditSummaryUrl = "http://{host}:port/l/auditSummary";
	private String lightningStatsUrl = "http://{host}:port/l/lightningStats";
	private String systemConfigUrl = "http://{host}:port/l/getSystemConfig";
	private String systemConfigUpdateUrl = "http://{host}:port/l/updateSystemConfig";

	private boolean embeddedMode = false;
	private boolean allowCrossRegionInteraction = true;
	private int corePort;

	/**
	 * Creates an instance of {@link LightningClient} with all required dependencies.
	 *
	 * @return the instance of {@link LightningClient} based on the configuration parameters before calling this method.
	 * REST API based {@code LightningClient} is returned if {@code embeddedMode} is set to false.
	 * An embedded {@code LightningClient} is returned if {@code embeddedMode} is set to true
	 */
	public LightningClient build() {
		final LightningClientConfig config = new LightningClientConfig();
		config.setEmbeddedMode(embeddedMode);
		ServiceCaller apiCaller = null;
		if (embeddedMode) {
			final SystemConfig systemConfig = new SystemConfig();
			final ExecutionDataStore dataStore = new ExecutionDataStore(systemConfig);
			final InetSocketAddressCache inetCache = new InetSocketAddressCache(systemConfig);
			final TaskExecutionManager taskExecutionManager = new TaskExecutionManager(systemConfig, dataStore, inetCache);
			taskExecutionManager.start();
			final TaskExecutionService service = new TaskExecutionServiceImpl(taskExecutionManager);
			apiCaller = new EmbeddedAPICaller(service);
			final ArrayList<String> seedList = new ArrayList<>();
			seedList.add("embeddedCoreService");
			config.setSeeds(seedList);
		} else {
			config.setPollApiUrl(pollApiUrl.replace(":port", ":" + corePort));
			config.setReserveApiUrl(reserveApiUrl.replace(":port", ":" + corePort));
			config.setSeeds(seeds);
			config.setSubmitApiUrl(submitApiUrl.replace(":port", ":" + corePort));
			config.setAuditApiUrl(auditApiUrl.replace(":port", ":" + corePort));
			config.setAuditJsonApiUrl(auditJsonApiUrl.replace(":port", ":" + corePort));
			config.setAuditSummaryUrl(auditSummaryUrl.replace(":port", ":" + corePort));
			config.setLightningStatsUrl(lightningStatsUrl.replace(":port", ":" + corePort));
			config.setSystemConfigUrl(systemConfigUrl.replace(":port", ":" + corePort));
			config.setSystemConfigUpdateUrl(systemConfigUpdateUrl.replace(":port", ":" + corePort));
			config.setCrossRegionSeeds(crossRegionSeeds);
			config.setAllowCrossRegionInteraction(allowCrossRegionInteraction);

			apiCaller = new RestAPICaller(config, urlUtils);
		}
		final ServiceHostResolver resolver = new ServiceHostResolver(config, apiCaller);
		return new LightningClient.LightningClientImpl(config, resolver, apiCaller);
	}

	/**
	 * Set the {@code UrlUtils}
	 * @param urlUtils the URL utils object to make http/https calls
	 * @return a reference to this object.
	 */
	public LightningClientBuilder setUrlUtils(UrlUtils urlUtils) {
		this.urlUtils = urlUtils;
		return this;
	}

	/**
	 * Set the API URL template for polling response.
	 *
	 * <p>
	 * Format: http://{hostname}:[port]/[some/poll/url]<br>
	 * Example: http://{host}:{port}/l/poll
	 * </p>
	 *
	 * @param pollApiUrl
	 *            the URL template for polling response
	 * @return a reference to this object.
	 */
	public LightningClientBuilder setPollApiUrlTemplate(String pollApiUrl) {
		this.pollApiUrl = pollApiUrl;
		return this;
	}

	/**
	 * Set the API URL template for Reservation.
	 *
	 * <p>
	 * Format: http://{hostname}:[port]/[some/reservation/url]<br>
	 * Example: http://{host}:{port}/l/reserve
	 * </p>
	 *
	 * @param reserveApiUrl
	 *            the URL template for making reservation
	 * @return a reference to this object.
	 */
	public LightningClientBuilder setReserveApiUrlTemplate(String reserveApiUrl) {
		this.reserveApiUrl = reserveApiUrl;
		return this;
	}

	/**
	 * Set the list of stand alone lightning core instances. The seeds are considered only when
	 * {@code embeddedMode} is set to false.<br>
	 * @param seeds the list of host names running lightning core
	 * @return a reference to this object.
	 */
	public LightningClientBuilder setSeeds(List<String> seeds) {
		this.seeds = seeds;
		return this;
	}

	/**
	 * Adds a single stand alone lightning core instances to the existing Seed
	 * List. The seeds are considered only when {@code embeddedMode} is set to
	 * false.<br>
	 *
	 * @param seed name of running lightning core instance
	 * @return a reference to this object.
	 */
	public LightningClientBuilder addSeed(String seed) {
		if (seeds == null) {
			seeds = new ArrayList<>();
		}
		this.seeds.add(seed);
		return this;
	}

	/**
	 * Set the list of stand alone lightning core instances from different colocations.
	 * The cross region seeds are considered only when {@code allowCrossRegionInteraction} is set to true<br>
	 * @param crossRegionSeeds the list of host names on different colocations running lightning core.
	 * @return a reference to this object.
	 */
	public LightningClientBuilder setCrossRegionSeeds(List<String> crossRegionSeeds) {
		this.crossRegionSeeds = crossRegionSeeds;
		return this;
	}

	/**
	 * Set the API URL template for submitting request.
	 *
	 * <p>
	 * Format: http://{hostname}:[port]/[some/submit/url]<br>
	 * Example: http://{host}:{port}/l/submit
	 * </p>
	 *
	 * @param submitApiUrl
	 *            the URL template for submitting tasks
	 * @return a reference to this object.
	 */
	public LightningClientBuilder setSubmitApiUrlTemplate(String submitApiUrl) {
		this.submitApiUrl = submitApiUrl;
		return this;
	}

	/**
	 * Set the API URL template for audit data.
	 *
	 * <p>
	 * Format: http://{hostname}:[port]/[some/audit/url]<br>
	 * Example: http://{host}:{port}/l/audit
	 * </p>
	 *
	 * @param auditApiUrl
	 *            the URL template to get compressed audit data
	 * @return a reference to this object.
	 */
	public LightningClientBuilder setAuditApiUrlTemplate(String auditApiUrl) {
		this.auditApiUrl = auditApiUrl;
		return this;
	}

	/**
	 * Set the API URL template for audit data in JSON format.
	 *
	 * <p>
	 * Format: http://{hostname}:[port]/[some/audit/url]<br>
	 * Example: http://{host}:{port}/l/audit/json
	 * </p>
	 *
	 * @param auditJsonApiUrl
	 *            the URL template to get audit data in JSON format
	 * @return a reference to this object.
	 */
	public LightningClientBuilder setAuditJsonApiUrlTemplate(String auditJsonApiUrl) {
		this.auditJsonApiUrl = auditJsonApiUrl;
		return this;
	}

	/**
	 * Set the API URL template to get lightning statistics.
	 *
	 * <p>
	 * Format: http://{hostname}:[port]/[lightningStatsUrl]<br>
	 * Example: http://{host}:{port}/l/lightningStats
	 * </p>
	 *
	 * @param lightningStatsUrl
	 *            the URL template to get lightning statistics
	 * @return a reference to this object.
	 */
	public LightningClientBuilder setLightningStatsUrlTemplate(String lightningStatsUrl) {
		this.lightningStatsUrl = lightningStatsUrl;
		return this;
	}

	/**
	 * Set the API URL template for audit summary data.
	 *
	 * <p>
	 * Format: http://{hostname}:[port]/[some/auditSummary/url]<br>
	 * Example: http://{host}:{port}/l/auditSummary
	 * </p>
	 *
	 * @param auditSummaryUrl
	 *            the URL template to get audit summary data
	 * @return a reference to this object.
	 */
	public LightningClientBuilder setAuditSummaryUrlTemplate(String auditSummaryUrl) {
		this.auditSummaryUrl = auditSummaryUrl;
		return this;
	}

	/**
	 * Set the API URL template to get system configuration.
	 *
	 * <p>
	 * Format: http://{hostname}:[port]/[getSystemConfigUrl]<br>
	 * Example: http://{host}:{port}/l/getSystemConfig
	 * </p>
	 *
	 * @param systemConfigUrl
	 *            the URL template to get system configuration
	 * @return a reference to this object.
	 */
	public LightningClientBuilder setSystemConfigUrlTemplate(String systemConfigUrl) {
		this.systemConfigUrl = systemConfigUrl;
		return this;
	}

	/**
	 * Set the API URL template to update system configuration.
	 *
	 * <p>
	 * Format: http://{hostname}:[port]/[updateSystemConfigUrl]<br>
	 * Example: http://{host}:{port}/l/updateSystemConfig
	 * </p>
	 *
	 * @param systemConfigUpdateUrl
	 *            the URL template to update system configuration
	 * @return a reference to this object.
	 */
	public LightningClientBuilder setSystemConfigUpdateUrlTemplate(String systemConfigUpdateUrl) {
		this.systemConfigUpdateUrl = systemConfigUpdateUrl;
		return this;
	}

	/**
	 * Set the lightning core to run in embedded mode.
	 * If set to {@code true}, the lightning core will run in embedded mode.
	 * If set to {@code false}, lightning core will run in standalone mode and has to be registered with client by calling {@link #setSeeds(List)} method.
	 * @param mode to run lightning core in embedded mode or standalone mode
	 * @return a reference to this object.
	 */
	public LightningClientBuilder setEmbeddedMode(boolean mode) {
		this.embeddedMode = mode;
		return this;
	}

	/**
	 * Set the lightning core to run in embedded mode.
	 * If set to {@code false}, tasks will only be submitted to lightning core running on the same colocation.
	 * If set to {@code true}, tasks will be submitted to lightning core running on different colocation,
	 * if all the lightning core instances running in the local colocations are busy.
	 * @param allowCrossRegionInteraction to allow submitting task to lightning core running on different colocation
	 * @return a reference to this object.
	 */
	public LightningClientBuilder setAllowCrossRegionInteraction(boolean allowCrossRegionInteraction) {
		this.allowCrossRegionInteraction = allowCrossRegionInteraction;
		return this;
	}

	/**
	 * Set the lightning core port. Please ensure that lightning core is running
	 * on this port before setting this.
	 *
	 * @param corePort
	 *            port on which lightning core is running
	 * @return a reference to this object.
	 */
	public LightningClientBuilder setCorePort(int corePort) {
		this.corePort = corePort;
		return this;
	}
}
