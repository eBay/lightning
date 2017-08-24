package com.ebay.lightning.core;

import org.apache.commons.lang3.math.NumberUtils;
import org.apache.log4j.Logger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.web.EmbeddedServletContainerAutoConfiguration;
import org.springframework.context.annotation.Configuration;

import com.ebay.lightning.core.config.MVCConfig;

@Configuration
@EnableAutoConfiguration
public class LightningCoreMain{

	private static final Logger log = Logger.getLogger(LightningCoreMain.class);

	private SpringApplication app;

	/**
	 * The entry point for lightning core.
	 * @param args program arguments
	 * Usage: LightningCoreMain server.port=[port as number]
	 */
	public static void main(String... args) {
		try {
			handleProgramArguments(args);
			new LightningCoreMain().bootstrap();
		} catch (Throwable t) {
			log.fatal("Fatal Error in LightningCoreMain", t);
		}
	}

	private static void handleProgramArguments(String... args) {
		if (args.length > 0 && args[0].contains(MVCConfig._ARG_SERVER_PORT)) {
			String port = args[0].substring(MVCConfig._ARG_SERVER_PORT.length() + 1);
			if (NumberUtils.isNumber(port)) {
				//Add to system properties for spring boot to pick up
				System.setProperty(MVCConfig._ARG_SERVER_PORT, port);
			} else {
				throw new IllegalArgumentException("Usage: LightningCoreMain server.port=<port as number>");
			}
		}
	}

	private void bootstrap() {
		app = new SpringApplication(new Object[] { LightningCoreMain.class, MVCConfig.class, EmbeddedServletContainerAutoConfiguration.class });
		String[] arguments = new String[1];
		arguments[0] = "LightningCoreMain";
		app.run(arguments);
	}

	public void run() {
		bootstrap();
	}
}
