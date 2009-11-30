package uk.ac.warwick.userlookup.cache;

import uk.ac.warwick.userlookup.cache.ehcache.EhCacheStore;

public class EhCacheUtils {
	public static void setUp() {
		System.setProperty("userlookup.ehcache.config", "/ehcache-userlookup-test.xml");
	}
	
	public static void tearDown() {
		EhCacheStore.shutdownDefaultCacheManager();
	}
}
