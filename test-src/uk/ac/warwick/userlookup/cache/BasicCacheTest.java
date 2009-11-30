package uk.ac.warwick.userlookup.cache;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import junit.framework.TestCase;
import uk.ac.warwick.userlookup.UserLookup;
import uk.ac.warwick.userlookup.cache.ehcache.EhCacheStore;

public class BasicCacheTest extends TestCase {

	BasicCache<String, String> cache;
	BasicCache<String, String> slowCache;
	private BrokenEntryFactory slowFactory;
	
	public void testGetMissingValue() throws Exception {
		assertEquals("Value for dog", cache.get("dog"));
		assertEquals("Value for cat", cache.get("cat"));
		
		// getting the same key twice will return the actual same object,
		// not just equal objects
		assertSame(cache.get("frog"), cache.get("frog"));
	}
	
	public void testSlowConcurrentLookups() throws Exception {
		assertFactoryCount(0);
		
		Runnable getDog = new Runnable() {
			public void run() {
				try {
					slowCache.get("dog");
				} catch (EntryUpdateException e) {
					throw e.getRuntimeException();
				}
			}
		};
		
		Thread t1 = new Thread(getDog);
		Thread t2 = new Thread(getDog);
		
		t1.start();
		t2.start();
		
		// Both threads will try to get the value, because we don't
		// have write locking.
		Thread.sleep(100);
		
		// check that the lock for dog doesn't block other lookups like frog
		slowFactory.addFastRequest("frog"); //simulate a lookup that doesn't block forever
		assertEquals("Value for frog", slowCache.get("frog"));
		
		// let the dog requests go through
		slowFactory.stopBlocking();
		t1.join();
		t2.join();
		
		// check that the factory was only called once.
		List<String> requests = slowFactory.getObjectsCreated();
		assertEquals(3, requests.size());
		assertEquals("frog", requests.get(0));
		assertEquals("dog", requests.get(1));
	}
	
	public void testAsynchronousUpdates() throws Exception {
		slowCache = Caches.newCache(UserLookup.USER_CACHE_NAME, slowFactory, 1);
		slowCache.setAsynchronousUpdateEnabled(true);
		slowFactory.addFastRequest("one");
		
		assertFactoryCount(0);
		String result1 = slowCache.get("one");
		String result2 = slowCache.get("one");
		Thread.sleep(1100);
		assertFactoryCount(1);
		String result3 = slowCache.get("one");
		assertFactoryCount(1);
		Thread.sleep(50);
		assertFactoryCount(2);
		String result4 = slowCache.get("one");
		assertFactoryCount(2);
		
		assertSame("Should have got cached value the second time", result1, result2);
		assertSame("Should have got stale value", result2, result3);
		assertNotSame("Should have returned async-updated result", result3, result4);
		
		slowCache.shutdown();
	}
	
	private void assertFactoryCount(final int number) {
		assertEquals(number, slowFactory.getObjectsCreated().size());
	}
	
	public void testSizeRestriction() throws Exception {	
		int cacheSize = 4;
		cache.setMaxSize(cacheSize);
		
		assertEquals("Should start empty", 0, cache.getStatistics().getCacheSize());
		
		cache.get("one");
		cache.get("two");
		cache.get("three");
		cache.get("four");
		assertEquals(4, cache.getStatistics().getCacheSize());
		cache.get("five");
		cache.get("six");
		assertEquals(4, cache.getStatistics().getCacheSize());
		
		assertEquals("Shouldn't exceed maximum size", cacheSize, cache.getStatistics().getCacheSize());
		assertFalse("Oldest entry should be evicted", cache.contains("one"));
	}
	
	public void testExpiry() throws Exception {
		slowCache = Caches.newCache(UserLookup.USER_CACHE_NAME, slowFactory, 1);
		slowFactory.addFastRequest("one");
		
		String result1 = slowCache.get("one");
		System.err.println("Got first item");
		String result2 = slowCache.get("one");
		Thread.sleep(1100);
		String result3 = slowCache.get("one");
		
		assertSame(result1, result2);
		assertNotSame(result1, result3);
		
		slowCache.shutdown();
	}
	
	protected void setUp() throws Exception {
		EhCacheUtils.setUp();
		cache = Caches.newCache(UserLookup.USER_CACHE_NAME, new SingularEntryFactory<String, String>() {
			private Random r = new Random();
			public String create(String key) {
				return new String("Value for " + key);
			}
			public boolean shouldBeCached(String val) {
				return true;
			}
		}, 100);
		
		slowFactory = new BrokenEntryFactory();
		slowCache = Caches.newCache(UserLookup.USER_CACHE_NAME, slowFactory, 100);
	}
	
	@Override
	protected void tearDown() throws Exception {
		System.out.println("Tearing down");
		cache.shutdown();
		EhCacheUtils.tearDown();
	}

	/**
	 * Mock entry factory that by default will block on a lookup until
	 * stopBlocking() is called. If you add some entries with {@link #addFastRequest(String)}
	 * then these will always return immediately, so you can test lookups
	 * while others are still processing.
	 */
	class BrokenEntryFactory implements EntryFactory<String, String> {
		private volatile boolean blocking = true;
		
		private List<String> requests = Collections.synchronizedList(new ArrayList<String>());
		
		// if a key is in here it'll return straight away.
		private Set<String> fastRequests = new HashSet<String>();
		
		public synchronized String create(String key) {
			if (!fastRequests.contains(key)) {
				while (blocking) {
					try {
						wait();
					} catch (InterruptedException e) {
						//keep on waitin'
					}
				}
			}
			requests.add(key);
			return new String("Value for " + key);
		}
		
		public synchronized void stopBlocking() {
			blocking = false;
			notifyAll();
		}

		public List<String> getObjectsCreated() {
			return requests;
		}
		
		public void addFastRequest(String s) {
			fastRequests.add(s);
		}

		/**
		 * Most implementations wouldn't bother implementing this if it
		 * was just going to look up each individually. Pretend as though
		 * this were a batch lookup.
		 */
		public Map<String, String> create(List<String> keys)
				throws EntryUpdateException {
			Map<String,String> response = new HashMap<String, String>();
			for (String key : keys) {
				response.put(key, create(key));
			}
			return response;
		}

		public boolean isSupportsMultiLookups() {
			return true;
		}

		public boolean shouldBeCached(String val) {
			return true;
		}
	}

}
