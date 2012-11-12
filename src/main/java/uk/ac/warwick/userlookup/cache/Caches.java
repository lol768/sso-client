package uk.ac.warwick.userlookup.cache;

import java.io.Serializable;

import org.apache.log4j.Logger;

import uk.ac.warwick.userlookup.cache.ehcache.EhCacheStore;

/**
 * Cache factory methods.
 */
public final class Caches {
	
	private static final Logger LOGGER = Logger.getLogger(Caches.class);
	
	private static boolean ehChecked;
	private static boolean ehAvailable;
	
	private Caches() {}
	
	public static <K extends Serializable,V extends Serializable> BasicCache<K, V> newCache(String name, EntryFactory<K,V> factory, long timeout) {
		return new BasicCache<K, V>(name, factory, timeout);
	}
	
	/**
	 * Creates a new cache. If Ehcache is available, it is used - reflection is used
	 * to avoid attempting to classload any EhCache classes until we know it's actually
	 * available.
	 */
	public static <K extends Serializable,V extends Serializable> CacheStore<K, V> newCacheStore(String name) {
		CacheStore<K,V> cache;
		if (isEhCacheAvailable()) {
			LOGGER.info("Ehcache detected - using EhCacheStore.");
			cache = new EhCacheStore<K, V>(name);
		} else {
			LOGGER.info("Ehcache not found - using built in cache store.");
			cache = new HashMapCacheStore<K, V>(name);
		}
		return cache;
	}

	/**
	 * Attempt to dynamically classload one of the main Ehcache classes, to
	 * see if it's available to the default classloader.
	 */
	public static boolean isEhCacheAvailable() {
		if (!ehChecked) {			
			try {
				Class.forName("net.sf.ehcache.CacheManager");
				String storeDir = System.getProperty("ehcache.disk.store.dir");
				String userLookupDir = System.getProperty("userlookup.disk.store.dir");
				if (storeDir == null && userLookupDir == null) {
					LOGGER.info("Ehcache found but no disk cache location specified with ehcache.disk.store.dir or userlookup.disk.store.dir, so not using Ehcache");
					ehAvailable = false;
				} else {
					ehAvailable = true;
				}
			} catch (ClassNotFoundException e) {
				ehAvailable = false;
			}
			ehChecked = true;
		}
		return ehAvailable;
	}
}
