package uk.ac.warwick.userlookup.cache;

import uk.ac.warwick.userlookup.UserLookup;
import uk.ac.warwick.userlookup.cache.CacheStore;
import uk.ac.warwick.userlookup.cache.Caches;
import uk.ac.warwick.userlookup.cache.ehcache.EhCacheStore;
import junit.framework.TestCase;

public class CachesTest extends TestCase {
	@Override
	protected void setUp() throws Exception {
		EhCacheUtils.setUp();
	}	
	
	@Override
	protected void tearDown() throws Exception {
		EhCacheUtils.tearDown();
	}
	
	/**
	 * Should return false as we haven't specified a disk cache location in a system property.
	 */
	public void testGetAvailableCache() throws Exception {
		CacheStore<String,String> store = Caches.<String,String>newCacheStore(UserLookup.USER_CACHE_NAME);
		assertFalse(store instanceof EhCacheStore<?,?>);
	}
}
