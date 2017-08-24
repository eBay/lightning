package com.ebay.lightning.core.utils;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.ebay.lightning.core.config.SystemConfig;


/**
 * The {@code InetSocketAddressCache} caches the {@link InetSocketAddress} for the URL tasks to speed up the
 * execution. Caching the {@link InetSocketAddress} reduces the domain address resolution time there by speeding 
 * up the execution time.
 * 
 * @author shashukla
 */
@Component
@Scope(value = "singleton")
public class InetSocketAddressCache {

	private SmartCache<String, InetSocketAddress> inetCache;

	private SmartCache.ValueLoader<String, InetSocketAddress> inetCacheLoader;

	/**
	 * Initialize the {@code InetSocketAddressCache} based on the configuration.
	 * @param systemConfig the configuration parameters for cache
	 */
	@Autowired
	public InetSocketAddressCache(SystemConfig systemConfig) {
		inetCacheLoader = new SmartCache.ValueLoader<String, InetSocketAddress>() {
			@Override
			public InetSocketAddress load(String key) throws IllegalArgumentException {
				InetSocketAddress createInetSocketAddress;
				try {
					createInetSocketAddress = InetSocketAddressCache.createInetSocketAddress(key);
				} catch (Exception e) {
					createInetSocketAddress = null;
				}
				return createInetSocketAddress;
			}
		};
		inetCache = new SmartCache<String, InetSocketAddress>(systemConfig.getMaxInetCacheSize(), 1800, TimeUnit.SECONDS, inetCacheLoader);
	}

	/**
	 * Callback method to get the {@link InetSocketAddress} for host:port.
	 * @param key host and port in "host:port" format
	 * @return the {@code InetSocketAddress} corresponding to host and port
	 */
	public static InetSocketAddress createInetSocketAddress(String key) {
		InetSocketAddress inetAddress = null;
		if (key != null) {
			String[] hostPort = key.split(":");
			if (hostPort.length == 2) {
				inetAddress = new InetSocketAddress(hostPort[0], Integer.valueOf(hostPort[1]));
			}
		}

		return inetAddress;
	}

	/**
	 * Get the {@link InetSocketAddress} for host and port.
	 * @param host the request host
	 * @param port the request port
	 * @return the {@code InetSocketAddress} corresponding to host and port
	 */
	public InetSocketAddress getInetSocketAddress(String host, int port) {
		String key = host + ":" + port;
		InetSocketAddress inetAddress = null;
		try {
			inetAddress = inetCache.get(key);
		} catch (Exception e) {
			// skip if can not be loaded from cache
		}
		

		return inetAddress;
	}

	/**
	 * Remove the key from cache.
	 * @param key the key to be removed
	 */
	public void invalidate(String key) {
		inetCache.remove(key);
	}

	/**
	 * Get the {@code InetSocketAddress} cache.
	 * @return the {@code InetSocketAddress} Cache
	 */
	public SmartCache<String, InetSocketAddress> getInetCache() {
		return inetCache;
	}

	/**
	 * Set the {@code InetSocketAddress} cache.
	 * @param inetCache the inetCache to set
	 */
	public void setInetCache(SmartCache<String, InetSocketAddress> inetCache) {
		this.inetCache = inetCache;
	}
	
}