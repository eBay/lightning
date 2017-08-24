package com.ebay.lightning.client.config;

import java.util.List;

/**
 * @author shashukla
 */
public class LightningClientConfig {

	private String reserveApiUrl;
	private String submitApiUrl;
	private String pollApiUrl;
	private String auditApiUrl;
	private String auditJsonApiUrl;
	private String auditSummaryUrl;
	private String lightningStatsUrl;
	private String systemConfigUrl;
	private String systemConfigUpdateUrl;
	private int maxRetryAttempt = 3;
	private boolean embeddedMode = false;
	private boolean allowCrossRegionInteraction = true;
	private List<String> seeds;
	private List<String> crossRegionSeeds;
	
	/**
	 * Get the API URL template for Reservation.
	 * @return the API URL template for Reservation
	 */
	public String getReserveApiUrl() {
		return reserveApiUrl;
	}

	/**
	 * Set the API URL template for Reservation.
	 *
	 * <p>Format: http://{hostname}:[port]/[some/reservation/url]<br>
     * Example: http://{host}:8989/l/reserve</p>
	 * @param reserveApiUrl the URL template for making reservation
	 */
	public void setReserveApiUrl(String reserveApiUrl) {
		this.reserveApiUrl = reserveApiUrl;
	}

	/**
	 * Get the API URL template for submit.
	 * @return the API URL template for submit
	 */
	public String getSubmitApiUrl() {
		return submitApiUrl;
	}

	/**
	 * Set the API URL template for submitting request.
	 * 
	 * <p>Format: http://{hostname}:[port]/[some/submit/url]<br>
	 * Example: http://{host}:8989/l/submit</p>
	 * @param submitApiUrl the URL template for submitting tasks
	 */
	public void setSubmitApiUrl(String submitApiUrl) {
		this.submitApiUrl = submitApiUrl;
	}

	/**
	 * Get the API URL template for polling response.
	 * @return the API URL template for polling response
	 */
	public String getPollApiUrl() {
		return pollApiUrl;
	}

	/**
	 * Set the API URL template for Polling.
	 * 
	 * <p>Format: http://{hostname}:[port]/[some/poll/url]<br>
	 * Example: http://{host}:8989/l/poll</p>
	 * @param pollApiUrl the URL template for polling response
	 */
	public void setPollApiUrl(String pollApiUrl) {
		this.pollApiUrl = pollApiUrl;
	}

	/**
	 * Get the list of stand alone lightning core instances aka seeds
	 * @return the list of seeds
	 */
	public List<String> getSeeds() {
		return seeds;
	}

	/**
	 * Set the list of stand alone lightning core instances aka seeds. The seeds are considered only when 
	 * {@code embeddedMode} is set to false.<br>
	 * @param seeds the list of host names running lightning core
	 */
	public void setSeeds(List<String> seeds) {
		this.seeds = seeds;
	}
	
	/**
	 * Get the API URL template for audit data.
	 * @return the API URL template for audit data
	 */
	public String getAuditApiUrl() {
		return auditApiUrl;
	}

	/**
	 * Set the API URL template for audit data.
	 * 
	 * <p>Format: http://{hostname}:[port]/[some/audit/url]<br>
	 * Example: http://{host}:8989/l/audit</p>
	 * @param auditApiUrl the URL template to get compressed audit data
	 */
	public void setAuditApiUrl(String auditApiUrl) {
		this.auditApiUrl = auditApiUrl;
	}

	/**
	 * Get the API URL template to fetch lightning statistics.
	 * @return the API URL template to fetch lightning statistics
	 */
	public String getLightningStatsUrl() {
		return lightningStatsUrl;
	}

	/**
	 * Set the API URL template to get lightning statistics.
	 * 
	 * <p>Format: http://{hostname}:[port]/[lightningStatsUrl]<br>
	 * Example: http://{host}:8989/l/lightningStats</p>
	 * @param lightningStatsUrl the URL template to get lightning statistics
	 */
	public void setLightningStatsUrl(String lightningStatsUrl) {
		this.lightningStatsUrl = lightningStatsUrl;
	}

	/**
	 * Get the API URL template for audit summary data.
	 * @return the API URL template for audit summary data
	 */
	public String getAuditSummaryUrl() {
		return auditSummaryUrl;
	}

	/**
	 * Set the API URL template for audit summary data.
	 * 
	 * <p>Format: http://{hostname}:[port]/[some/auditSummary/url]<br>
	 * Example: http://{host}:8989/l/auditSummary</p>
	 * @param auditSummaryUrl the URL template to get audit summary data
	 */
	public void setAuditSummaryUrl(String auditSummaryUrl) {
		this.auditSummaryUrl = auditSummaryUrl;
	}

	/**
	 * Get the maximum retry attempt to make reservation with a seed.
	 * @return the maximum retry attempt to make reservation with a seed
	 */
	public int getMaxRetryAttempt() {
		return maxRetryAttempt;
	}

	/**
	 * Set the maximum retry attempt to make reservation with a seed.
	 * @param maxRetryAttempt the maximum retry attempt
	 */
	public void setMaxRetryAttempt(int maxRetryAttempt) {
		this.maxRetryAttempt = maxRetryAttempt;
	}

	/**
	 * Get the API URL template to get system configuration.
	 * @return the API URL template to get system configuration
	 */
	public String getSystemConfigUrl() {
		return systemConfigUrl;
	}

	/**
	 * Set the API URL template to get system configuration.
	 * 
	 * <p>Format: http://{hostname}:[port]/[getSystemConfigUrl]<br>
	 * Example: http://{host}:8989/l/getSystemConfig</p>
	 * @param systemConfigUrl the URL template to get system configuration
	 */
	public void setSystemConfigUrl(String systemConfigUrl) {
		this.systemConfigUrl = systemConfigUrl;
	}

	/**
	 * Get the API URL template to update system configuration.
	 * @return the API URL template to update system configuration
	 */
	public String getSystemConfigUpdateUrl() {
		return systemConfigUpdateUrl;
	}

	/**
	 * Set the API URL template to update system configuration.
	 * 
	 * <p>Format: http://{hostname}:[port]/[updateSystemConfigUrl]<br>
	 * Example: http://{host}:8989/l/updateSystemConfig</p>
	 * @param systemConfigUpdateUrl the URL template to update system configuration
	 */
	public void setSystemConfigUpdateUrl(String systemConfigUpdateUrl) {
		this.systemConfigUpdateUrl = systemConfigUpdateUrl;
	}

	/**
	 * Get the API URL template for audit data in JSON format.
	 * @return the API URL template for audit data in JSON format
	 */
	public String getAuditJsonApiUrl() {
		return auditJsonApiUrl;
	}

	/**
	 * Set the API URL template for audit data in JSON format.
	 * 
	 * <p>Format: http://{hostname}:[port]/[some/audit/url]<br>
	 * Example: http://{host}:8989/l/audit/json</p>
	 * @param auditJsonApiUrl the URL template to get audit data in JSON format
	 */
	public void setAuditJsonApiUrl(String auditJsonApiUrl) {
		this.auditJsonApiUrl = auditJsonApiUrl;
	}

	/**
	 * Check if lightning core runs in embedded mode.
	 * @return {@code true} if the core runs in embedded mode
	 */
	public boolean isEmbeddedMode() {
		return embeddedMode;
	}

	/**
	 * Set the lightning core to run in embedded mode.
	 * If set to {@code true}, the lightning core will run in embedded mode.
	 * If set to {@code false}, lightning core will run in standalone mode and has to be registered with client by calling {@link #setSeeds(List)} method.
	 * @param embeddedMode to run lightning core in embedded mode or standalone mode
	 */
	public void setEmbeddedMode(boolean embeddedMode) {
		this.embeddedMode = embeddedMode;
	}

	/**
	 * Check if cross region interaction is enabled.\
	 * @return {@code true} if cross region interaction is enabled
	 */
	public boolean isAllowCrossRegionInteraction() {
		return allowCrossRegionInteraction;
	}

	/**
	 * Enable cross region interaction.
	 * If set to {@code false}, tasks will only be submitted to lightning core running on the same colocation.
	 * If set to {@code true}, tasks will be submitted to lightning core running on different colocation, 
	 * if all the lightning core instances running in the local colocations are busy.
	 * @param allowCrossRegionInteraction to allow submitting task to lightning core running on different colocation
	 */
	public void setAllowCrossRegionInteraction(boolean allowCrossRegionInteraction) {
		this.allowCrossRegionInteraction = allowCrossRegionInteraction;
	}

	/**
	 * Get the list of stand alone lightning core instances from different colocations.
	 * @return the cross region seeds
	 */
	public List<String> getCrossRegionSeeds() {
		return crossRegionSeeds;
	}

	/**
	 * Set the list of stand alone lightning core instances from different colocations.
	 * The cross region seeds are considered only when {@code allowCrossRegionInteraction} is set to true<br>
	 * @param crossRegionSeeds the list of host names on different colocations running lightning core.
	 */	
	public void setCrossRegionSeeds(List<String> crossRegionSeeds) {
		this.crossRegionSeeds = crossRegionSeeds;
	}
	
}
