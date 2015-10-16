package uk.ac.warwick.userlookup.cache;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import junit.framework.TestCase;
import uk.ac.warwick.util.cache.*;

/**
 * This class uses behaviour from WarwickUtils-Cache - we keep the test
 * here (after the code was refactored out) to ensure that the same behaviour
 * exists in SSO Client as expected.
 */
public class HashMapCacheStoreTest extends TestCase {
	
	/**
	 * Test the behaviour of creating multiple HashMapCacheStores with the same
	 * name - they will use the same map internally, but can be of different
	 * types. Trying to get a value of a different type will cause a ClassCastException
	 * at runtime. 
	 */
	public void testMultipleStores() {
		HashMapCacheStore<String, String> cache = new HashMapCacheStore<String, String>("MyCache");
		HashMapCacheStore<String, String> cache2 = new HashMapCacheStore<String, String>("MyCache");
		HashMapCacheStore<String, ArrayList> cache3 = new HashMapCacheStore<String, ArrayList>("MyCache");
		
		cache.put(new CacheEntry<String, String>("one", "gamma"), 10, TimeUnit.SECONDS);
		
		assertEquals("gamma", cache2.get("one").getValue());
		
		ArrayList list = new ArrayList(Arrays.asList("blah","blah"));
		cache3.put(new CacheEntry<String, ArrayList>("two", list), 10, TimeUnit.SECONDS);
		
		assertEquals(list, cache3.get("two").getValue());
		
		try {
			ArrayList l = cache3.get("one").getValue();
			fail("should have failed with incompatible value");
		} catch (ClassCastException e) {}
		
		try {
			String s = cache2.get("two").getValue();
			fail("should have failed with incompatible value");
		} catch (ClassCastException e) {}
		
	}
}
