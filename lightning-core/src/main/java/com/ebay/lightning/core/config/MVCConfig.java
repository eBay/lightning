package com.ebay.lightning.core.config;

import org.apache.catalina.connector.Connector;
import org.apache.coyote.http11.AbstractHttp11Protocol;
import org.springframework.boot.autoconfigure.web.DispatcherServletAutoConfiguration;
import org.springframework.boot.context.embedded.ConfigurableEmbeddedServletContainer;
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizer;
import org.springframework.boot.context.embedded.ServletRegistrationBean;
import org.springframework.boot.context.embedded.tomcat.TomcatConnectorCustomizer;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.config.annotation.DefaultServletHandlerConfigurer;
import org.springframework.web.servlet.config.annotation.DelegatingWebMvcConfiguration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@Configuration
// Marks this class as configuration
// Specifies which package to scan
@ComponentScan({ "com.ebay.lightning.core" })
// Enables Spring's annotations
@EnableWebMvc
public class MVCConfig extends DelegatingWebMvcConfiguration {
	public static final String _ARG_SERVER_PORT = "server.port";
	
	@Override
	protected void configureDefaultServletHandling(DefaultServletHandlerConfigurer configurer) {
		configurer.enable();
	}

	/**
	 * Configure the embedded tomcat server to compress the response when the response size is greater than 256.
	 * @return embedded server instance
	 */
	@Bean
	public EmbeddedServletContainerCustomizer servletContainerCustomizer() {
	    return new EmbeddedServletContainerCustomizer() {
	        @Override
	        public void customize(ConfigurableEmbeddedServletContainer servletContainer) {
	            ((TomcatEmbeddedServletContainerFactory) servletContainer).addConnectorCustomizers(
	                    new TomcatConnectorCustomizer() {
	                        @Override
	                        public void customize(Connector connector) {
	                            AbstractHttp11Protocol httpProtocol = (AbstractHttp11Protocol) connector.getProtocolHandler();
	                            httpProtocol.setCompression("on");
	                            httpProtocol.setCompressionMinSize(256);
	                            String mimeTypes = httpProtocol.getCompressableMimeTypes();
	                            String mimeTypesWithJson = mimeTypes + "," + MediaType.APPLICATION_JSON_VALUE;
	                            httpProtocol.setCompressableMimeTypes(mimeTypesWithJson);
	                            try {
									if(System.getProperty(_ARG_SERVER_PORT) != null){
										connector.setPort(Integer.valueOf(System.getProperty(_ARG_SERVER_PORT)));
									}
								} catch (Exception e) {
									// Will default to port 8080. No action required. 
								}
	                        }
	                    }
	            );
	        }
	    };
	}
	
	@Bean
	public ServletRegistrationBean dispatcherRegistration() {
		ServletRegistrationBean registration = new ServletRegistrationBean(dispatcherServlet());
		registration.addUrlMappings("/");
		return registration;
	}

	@Bean(name = DispatcherServletAutoConfiguration.DEFAULT_DISPATCHER_SERVLET_BEAN_NAME)
	public DispatcherServlet dispatcherServlet() {
		return new DispatcherServlet();
	}

}
