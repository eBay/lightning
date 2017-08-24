package com.ebay.lightning.core.utils;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.ebay.lightning.core.async.Callback;
import com.ebay.lightning.core.async.Reminder;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalCause;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

/**
 * The {@code SmartCache} is a wrapper over Google Cache
 * 
 * So what's the difference between this and Google Cache??
 * 1. On getIfPresent, It will always return cached data, even if it is expired. 
 * 2. QUICKER: Auto Updates cache on element expired using a separate thread pool. This will make fetches faster.
 * 3. FAULT TOLERANT: When element expires, it tries to re fetch data automatically and upon failure it re inserts the old element.
 * 
 * @author shashukla
 *
 */
public class SmartCache<K, V> {

	/**
	 * Value Loader used to load missing values
	 * @param <K> the key
	 * @param <V> the value
	 * 
	 * @author shashukla
	 */
	public interface ValueLoader<K, V> {
		public V load(K k) throws Exception;
	}

	//Caches
	private Cache<K, V> cache;
	private ConcurrentHashMap<K, V> concurrentMap = new ConcurrentHashMap<>();
	
	private ValueLoader<K, V> valueLoader;
	
	private Long staleTime;
	private TimeUnit timeUnit;
	private Map<K, Long> lastUsedTimeTracker = new ConcurrentHashMap<>(); 
	
	//Local Value Loading Executor
	private BlockingQueue<Runnable> workQueue = new ArrayBlockingQueue<Runnable>(50000);
	private ThreadPoolExecutor taskExecutor = new ThreadPoolExecutor(50, 50, 1, TimeUnit.MINUTES, workQueue, new ThreadFactoryBuilder()
			.setNameFormat("SmartCacheLoaderThread-%d").build());

	/**
	 * A {@link Reminder} callback to cleanup cache.
	 */
	@SuppressWarnings("unused")
	private Reminder cacheCleanupReminder = new Reminder("SmartCache-cleanup-reminder", new Callback<String>() {
		@Override
		public void notify(String t) {
			//cleanup Cache
			cache.cleanUp();
			//Remove Stale Data
			if (timeUnit != null && staleTime != null) {
				for (Iterator<Entry<K, Long>> iterator = lastUsedTimeTracker.entrySet().iterator(); iterator.hasNext();) {
					Entry<K, Long> e = iterator.next();
					long staleThreshold = System.currentTimeMillis() - timeUnit.toMillis(staleTime);
					if (e.getValue() < staleThreshold) {
						//staleKey, time to evict
						//Log.logInfo(getClass(), "Removing "+ (staleThreshold - e.getValue())+"ms stale entry:" + e.getKey());
						concurrentMap.remove(e.getKey());
						cache.invalidate(e.getKey());
						iterator.remove();
					}
				}
			}
		}
	}, 5L, true);

	/**
	 * If set, cache will automatically remove the keys 
	 * @param staleTime the time after which the entry become stale
	 * @param timeUnit the {@link TimeUnit}
	 */
	public void removeEntryIfNotAccessedFor(Long staleTime, TimeUnit timeUnit) {
		this.staleTime = staleTime;
		this.timeUnit = timeUnit;
	}
	
	/**
	 * Callback for removing a cache entry.
	 */
	private RemovalListener<K, V> listener = new RemovalListener<K, V>() {
		@Override
		public void onRemoval(RemovalNotification<K, V> notification) {
			final K key = notification.getKey();
			final V value = notification.getValue();
			if (RemovalCause.EXPIRED.equals(notification.getCause())) {
				//if key is exipired and removed then reload it...
				taskExecutor.execute(new Runnable() {
					@Override
					public void run() {
						try {
							if (concurrentMap.containsKey(key)) {
								//cache map has this key that means it is time based expiry and needs reloading 
								loadValue(key);
								//Log.logInfo(getClass(), "Re-loading : " + key );
							}
						} catch (Exception e) {
							//insert the evicted value back
							cache.put(key, value);
						}
					}
				});
			} else if(!RemovalCause.REPLACED.equals(notification.getCause())) {
				//if key is evicted because of some other reason then remove from cache map
				concurrentMap.remove(notification.getKey());
				if(RemovalCause.SIZE.equals(notification.getCause())){
					//Log.logError(getClass(), "Cache has hit its limit of "+ cache.size());
				}
			}
		}
	};

	/**
	 * Initialize the {@code SmartCache} with the input configuration.
	 * @param size the maximum size of cache
	 * @param duration expire time of cache entry
	 * @param timeUnit the time unit for duration
	 * @param valueLoader callback for value loader
	 */
	public SmartCache(long size, long duration, TimeUnit timeUnit, ValueLoader<K, V> valueLoader) {
		this.valueLoader = valueLoader;
		this.cache = CacheBuilder.newBuilder().maximumSize(size).removalListener(listener).expireAfterWrite(duration, timeUnit).build();
	}

	/**
	 * Get the Value associated with the Key.
	 * 
	 * Serves Everything from cache and reloads cache using internal thread pool
	 * @param key input Key
	 * @return the value corresponding to the key
	 * @throws Exception when the load operation fails
	 */
	public V get(final K key) throws Exception {
		V value = concurrentMap.get(key);
		
		if (value == null && !concurrentMap.containsKey(key)) {
			//This is a new Key, Load and cache
			value = loadValue(key);
		}
		
		//update the usage data
		if (staleTime != null) {
			lastUsedTimeTracker.put(key, System.currentTimeMillis());
		}
		
		return value;
	}

	/**
	 * Load Value associated with the Key.
	 * 
	 * @param key input Key
	 * @return the value corresponding to the key
	 * @throws Exception when the load operation fails
	 */
	private V loadValue(final K key) throws Exception {
		V value = valueLoader.load(key);
		concurrentMap.put(key, value);
		cache.put(key, value);
		
		return value;
	}

	/**
	 * Latest data is saved in both cache and cache map.
	 * @param key input key
	 * @param value corresponding value
	 */
	public void put(K key, V value) {
		concurrentMap.put(key, value);
		cache.put(key, value);
	}

	/**
	 * Reload data for the key.
	 * @param key the input Key
	 */
	public void reload(final K key) {
		if (concurrentMap.containsKey(key)) {
			taskExecutor.execute(new Runnable() {
				@Override
				public void run() {
					try {
						//cache map has this key that means it is time based expiry and needs reloading 
						loadValue(key);
					} catch (Exception e) {
						//ignore
					}
				}
			});
		}
	}

	/**
	 * Remove data from the cache.
	 * @param key the input Key
	 */
	public void remove(K key) {
		//do not change the order
		concurrentMap.remove(key);
		cache.invalidate(key);
	}

	/**
	 * Perform cleanup operation on the cache.
	 */
	public void cleanup() {
		cache.cleanUp();
	}
	
	/**
	 * Get the cache size.
	 * @return the cache size
	 */
	public long size(){
		return cache.size();
	}
}