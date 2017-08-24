package com.ebay.lightning.test.config;

import javax.servlet.ServletContext;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.servlet.config.annotation.DefaultServletHandlerConfigurer;

@Configuration
@ComponentScan(basePackages = { "com.ebay.lightning.core", "com.ebay.lightning.core.config", "com.ebay.lightning.core.workers", "com.ebay.lightning.core.utils" }, 
		excludeFilters = { @Filter(type = FilterType.ANNOTATION, value = Configuration.class) })
public class TestConfig {
	@Bean
	public ServletContext getServletContext() {
		return new MockServletContext();
	}

	@Bean
	public DefaultServletHandlerConfigurer getdefaultServletHandlerMapping() {
		return new DefaultServletHandlerConfigurer(getServletContext());
	}

}