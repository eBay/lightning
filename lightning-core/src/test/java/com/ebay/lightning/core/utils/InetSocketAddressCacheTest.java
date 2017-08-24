package com.ebay.lightning.core.utils;

import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;

import com.ebay.lightning.core.config.SystemConfig;

import junit.framework.Assert;

public class InetSocketAddressCacheTest {

	private static final Logger log = Logger.getLogger(InetSocketAddressCacheTest.class);
	
	private SystemConfig systemConfig;
	private InetSocketAddressCache inetSocketAddressCache;
	
	@Before
	public void setup(){
		systemConfig = new SystemConfig();
		inetSocketAddressCache = new InetSocketAddressCache(systemConfig);
	}
	
	@Test
	public void testPreloadCache()throws Exception{
		inetSocketAddressCache.getInetSocketAddress("localhost", 8080);
		Assert.assertEquals(1,inetSocketAddressCache.getInetCache().size());
		inetSocketAddressCache.invalidate("localhost:8080");
		Assert.assertEquals(0,inetSocketAddressCache.getInetCache().size());
	}
	
	@Test
	public void testPreloadCacheError()throws Exception{
		Assert.assertEquals(0,inetSocketAddressCache.getInetCache().size());
	}
}
