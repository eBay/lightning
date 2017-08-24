package com.ebay.lightning.core.constants;

import java.nio.charset.Charset;

/**
 * Defines enum and constants.
 * 
 * @author shashukla
 */
public class LightningCoreConstants {

	public static final String HTTP_REQUEST_REQUEST_TEMPLATE = "%s %s HTTP/1.1\nHost: %s\nConnection: keep-alive\n\n";
	public static final Charset DEFAULT_CHARSET = Charset.forName("ISO-8859-1");
	public static final String DEFAULT_SYSTEM_CONFIG_FILE_LOCATION = "./systemConfig.txt";

	public static enum WorkerState {
		NEVER_STARTED, RUNNING, IDLE, CACHE_INITIALIZED, BAD_STATE
	}

	public static enum WorkStatus {
		IN_QUEUE, RUNNING, DONE, STOPPED, CLEANED_UP
	}

	public static enum TaskStatus {
		INIT, CONNECTED, WRITTEN, READ, CONNECT_FAILED, READ_WRITE_FAILED, SUCCESS, FAILED, TIMEDOUT
	}

	public enum HttpMethod {
		GET, HEAD
	};
}
