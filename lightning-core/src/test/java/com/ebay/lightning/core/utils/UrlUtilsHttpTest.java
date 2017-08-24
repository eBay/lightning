package com.ebay.lightning.core.utils;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.web.EmbeddedServletContainerAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;

import com.ebay.lightning.core.store.LightningRequestReport;
import com.ebay.lightning.testing.SimpleHttpServer;

public class UrlUtilsHttpTest {

	private static UrlUtils urlUtils;
	private static String baseUrl; 
	
	private static ConfigurableApplicationContext context;
	
	@BeforeClass
	public static void initialize(){
		SpringApplication app = new SpringApplication(new Object[] { SimpleHttpServer.class, EmbeddedServletContainerAutoConfiguration.class });
		String[] arguments = new String[1];
		arguments[0] = "LightningCoreTest";
		context = app.run(arguments);
		urlUtils = new UrlUtils();
		baseUrl = String.format("http://localhost:%d/test/", SimpleHttpServer.serverPort);
	}
	
	@Test
	public void testSimpleGetCall() throws Exception{
		String targetURL = baseUrl + "ecv";
		String response = urlUtils.get(targetURL);
		assertEquals("OK", response);
	}
	
	@Test
	public void testByteArrayGetCall() throws Exception{
		String targetURL = baseUrl + "audit";
		byte[] response = urlUtils.getByteArray(targetURL);
		List<LightningRequestReport> reports = (List<LightningRequestReport>) ZipUtil.unZipByteArray(response);
		assertEquals(2, reports.size());
	}
	
	@Test
	public void testPostCall() throws Exception{
		String targetURL = baseUrl + "submit";
		Map<String,String> headers = new HashMap<String, String>();
		headers.put("test", "test");
		String response = urlUtils.post(targetURL, null, headers, "");
		assertEquals("OK POST\r", response);
	}
	
	@AfterClass
	public static void shutDown(){
		SpringApplication.exit(context);
	}
}
