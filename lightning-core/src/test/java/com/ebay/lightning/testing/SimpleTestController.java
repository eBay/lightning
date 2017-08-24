package com.ebay.lightning.testing;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.ebay.lightning.core.store.LightningRequestReport;
import com.ebay.lightning.core.utils.ZipUtil;

@RestController
@RequestMapping("test")
public class SimpleTestController {
	private static final Logger log = Logger.getLogger(SimpleTestController.class);
	
	private static AtomicInteger counter = new AtomicInteger(0);
	
	public SimpleTestController() {
		log.info("started SimpleTestController...");
	}

	@RequestMapping(value = "/ecv", method = {RequestMethod.GET,RequestMethod.HEAD}, produces = "application/json")
	public String reserve() {
		return "OK";
	}
	
	@RequestMapping(value = "/audit", method = RequestMethod.GET, produces = "application/zip")
	public byte[] audit() {
		List<LightningRequestReport> reports = new ArrayList<LightningRequestReport>();
		reports.add(new LightningRequestReport());
		reports.add(new LightningRequestReport());
		try {
			byte[] zippedBytes = ZipUtil.zipAsByteArray(reports);
			return zippedBytes;
		} catch (IOException e) {
			log.error("Error in Audit", e);
			throw new RuntimeException(e);
		}
	}
	
	@RequestMapping(value = "/submit", method = RequestMethod.POST)
	public String submit(@RequestBody String request) {
		return "OK POST";
	}
	
	@RequestMapping(value = "/timedEcv", method = {RequestMethod.GET,RequestMethod.HEAD}, produces = "application/json")
	public String timedEcv() {
		if(counter.incrementAndGet()==1){
			try{
				Thread.sleep(2000);
			}catch(InterruptedException e){}
		}
		return "OK";
	}
	
	@RequestMapping(value = "/error", method = {RequestMethod.GET,RequestMethod.HEAD}, produces = "application/json")
	public ResponseEntity<String> error() {
		return new ResponseEntity<String>(HttpStatus.GATEWAY_TIMEOUT);
	}
	
}
