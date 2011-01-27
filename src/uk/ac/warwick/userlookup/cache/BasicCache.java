package uk.ac.warwick.userlookup.cache;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import uk.ac.warwick.userlookup.UserLookup;


/**
 * Cache which implements the following features
 * 
 * <ul>
 *  <li>Self-population (updates are done during get() via the EntryFactory)</li>
 * 	<li>Asynchronous background updates for expired entries</li>
 * </ul>
 * <p>
 * The backing CacheStore determines how elements are evicted when we grow
 * past the maximum cache size.
 */
public final class BasicCache<K extends Serializable, V extends Serializable> implements Cache<K,V> {
	private static final String CACHE_SIZE_PROPERTY = "userlookup.groupcachesize";
	
	private static final long MILLISECS_IN_SECS = 1000;
	
	private final EntryFactory<K,V> _entryFactory;
	
	private final List<CacheListener<K,V>> listeners = new ArrayList<CacheListener<K,V>>();
	
	// Uses an unbounded queue, so when all threads are busy it will queue up
	// waiting jobs and do them next
	// SSO-830 static - all caches together will use one threadpool.
	private static final ExecutorService threadPool = Executors.newFixedThreadPool(
			Integer.parseInt(UserLookup.getConfigProperty("ssoclient.cache.threadpool.size")),
			new DaemonThreadFactory()
	);
	
	private static class DaemonThreadFactory implements ThreadFactory {
		private static final ThreadFactory def = Executors.defaultThreadFactory();
		public Thread newThread(Runnable r) {
			Thread t = def.newThread(r);
			t.setDaemon(true);
			return t;
		}
	}
	
	private final CacheStore<K,V> store;
	
	private long _timeOutMillis;
	
	private boolean asynchronousUpdateEnabled = false;
	
	
	private ExpiryStrategy<K, V> expiryStrategy = new ExpiryStrategy<K, V>() {
		public boolean isExpired(Entry<K,V> entry) {
			final long expires = entry.getTimestamp() + _timeOutMillis;
			final long now = System.currentTimeMillis();
			return expires <= now;
		};
	};
	
	public BasicCache(CacheStore<K,V> cacheStore, long timeoutSeconds) {
		this(cacheStore, null, timeoutSeconds);
	}

	public BasicCache(CacheStore<K,V> cacheStore, EntryFactory<K,V> factory, long timeoutSeconds) {
		this._entryFactory = factory;
		this._timeOutMillis = timeoutSeconds * MILLISECS_IN_SECS;

		/*
		 * This creates an Ehcache based store if Ehcache is available. Otherwise
		 * it uses a synchronized HashMap.
		 */
		this.store = cacheStore;
		
		if (UserLookup.getConfigProperty(CACHE_SIZE_PROPERTY) != null) {
			this.store.setMaxSize(Integer.parseInt(UserLookup.getConfigProperty(CACHE_SIZE_PROPERTY)));
		}
	}
	
	/**
	 * @param timeoutSeconds The number of seconds for entries to expire. This is ignored
	 * 		if you subsequently override the ExpiryStrategy.
	 */
	public BasicCache(String storeName, EntryFactory<K,V> factory, long timeoutSeconds) {
		this(Caches.<K,V>newCacheStore(storeName), factory, timeoutSeconds);
	}
	
	public void setMaxSize(final int cacheSize) {
		this.store.setMaxSize(cacheSize);
	}
	
	/**
	 * Set the cache entry timeout in seconds.
	 * This has no effect if you have overridden the ExpiryStrategy using 
	 */
	public void setTimeout(final int seconds) {
		this._timeOutMillis = seconds * MILLISECS_IN_SECS;
	}

	
	/**
	 * Gets the value for the given key. A read lock is initially applied.
	 * If the cache value is present and valid then we unlock and return the
	 * value. If it's not present or it's expired, it will get a new value from
	 * the EntryFactory and place it in, and that is returned.
	 * <p>
	 * 
	 * IF you aren't using an EntryFactory, then null is returned if an entry
	 * is missing or expired. 
	 */
	public V get(final K key, final Object data) throws EntryUpdateException {
		Entry<K,V> entry = store.get(key);
		boolean expired = ( entry != null && isExpired(entry) );
		if (entry != null && !expired) {
			broadcastHit(key, entry);
		} else if (_entryFactory != null) {
			if (entry == null || !asynchronousUpdateEnabled) {
				entry = updateEntry(new KeyEntry<K,V>(key, entry, data));
			} else {
				// Entry is just expired. Return the stale version
				// now and update in the background
				threadPool.execute(UpdateEntryTask.task(this, new KeyEntry<K,V>(key, entry, data)));
			}
		} else {
			return null;
		}
		return entry.getValue();
	}
	
	

	public V get(K key) throws EntryUpdateException {
		return get(key, null);
	}
	
	/**
	 * If the EntryFactory supports it, looks up a collection of keys in one go.
	 * 
	 * First it finds which of the keys are currently in the cache and valid. It
	 * gathers up which ones are missing and which are expired. If none are missing
	 * but some are expired, the stale values are used and updates run asynchronously
	 * (when asynchronous updates are enabled).
	 * 
	 * If any entries are missing then ALL lookups are done syncronously, including
	 * any expired entries. If the client has to wait for one lookup, we may as well
	 * do it all now and get fresh data for every key.
	 */
	public Map<K,V> get(List<K> keys) throws EntryUpdateException {
		if (_entryFactory == null) {
			throw new UnsupportedOperationException("Batch lookups only supported when a suitable EntryFactory is used");
		}
		if (!_entryFactory.isSupportsMultiLookups()) {
			throw new UnsupportedOperationException("The given EntryFactory does not support batch lookups");
		}
		
		Map<K,V> results = new HashMap<K, V>();
		List<KeyEntry<K,V>> missing = new ArrayList<KeyEntry<K,V>>();
		List<KeyEntry<K,V>> expired = new ArrayList<KeyEntry<K,V>>();
		for (K key : keys) {
			Entry<K,V> entry = store.get(key);
			if (entry == null || (!asynchronousUpdateEnabled && isExpired(entry))) {
				missing.add(new KeyEntry<K,V>(key, entry, null));
			} else {
				results.put(key, entry.getValue());
				if (isExpired(entry)) {
					expired.add(new KeyEntry<K,V>(key, entry, null));
				}
			}
		}
		
		if (!missing.isEmpty()) {
			missing.addAll(expired);
			Map<K, Entry<K, V>> updated = updateEntries(missing);
			for (Map.Entry<K,Entry<K,V>> entry : updated.entrySet()) {
				results.put(entry.getKey(), entry.getValue().getValue());
			}
		} else if (!expired.isEmpty()) {
			threadPool.execute(UpdateEntryTask.task(this, expired));
		}
		
		return results;
	}

	private void broadcastMiss(final K key, final Entry<K,V> newEntry) {
		for (CacheListener<K,V> listener : listeners) {
			listener.cacheMiss(key, newEntry);
		}
	}

	private void broadcastHit(final K key, final Entry<K,V> entry) {
		for (CacheListener<K,V> listener : listeners) {
			listener.cacheHit(key, entry);
		}
	}

	/**
	 * Updates the given key with a value from the factory and places it in the cache.
	 * @param data 
	 * 
	 * @param key Key to lookup from
	 * @param existingEntry Entry currently in the map. May be null if it doesn't exist
	 * @return
	 */
	Entry<K,V> updateEntry(final KeyEntry<K,V> kEntry) throws EntryUpdateException {
		final K key = kEntry.key;
		final Entry<K,V> foundEntry = kEntry.entry;
		
		Entry<K,V> entry = store.get(key);
		
		// if entry is null, we definitely need to go update it.
		// if entry is not currently updating, update it UNLESS the version we
		// got outside the lock is a different object - meaning another thread just updated it
		if (entry == null || (entry.equals(foundEntry) && !entry.isUpdating())) {
			if (entry != null) {
				entry.setUpdating(true);
			}
			try {
				V newValue = _entryFactory.create(key, kEntry.data);
				entry = newEntry(key, newValue);
				if (_entryFactory.shouldBeCached(newValue)) {
					store.put(entry);
				}
				broadcastMiss(key, entry);
			} finally {
				if (entry != null) {
					entry.setUpdating(false);
				}
			}
		} else {
			// entry in map changed since we got the write lock, meaning another thread
			// just replaced it - so we've essentially hit the cache and can return
			// this value.
			broadcastHit(key, entry);
		}
		
		return entry;
	}
	
	/**
	 * Updates all the given key/value pairs and puts them in the map.
	 * 
	 * Doesn't obtain a write lock, because this is too complicated. This doesn't
	 * affect thread-safety but it can result in multiple threads updating the
	 * same cache keys.
	 */
	Map<K, Entry<K,V>> updateEntries(final Collection<KeyEntry<K,V>> kentries) throws EntryUpdateException {
		Map<K, Entry<K,V>> result = new HashMap<K, Entry<K,V>>();
		List<K> keys = new ArrayList<K>();
		for (KeyEntry<K,V> kentry : kentries) {
			final Entry<K, V> foundEntry = kentry.entry;
			if (foundEntry != null) {
				foundEntry.setUpdating(true);
			}
			keys.add(kentry.key);
		}
		
		Map<K,V> createdMap = _entryFactory.create(keys);
		for (Map.Entry<K, V> created : createdMap.entrySet()) {
			final K key = created.getKey();
			final V value = created.getValue();
			final Entry<K, V> entry = new Entry<K, V>(key, value);
			if (_entryFactory.shouldBeCached(value)) {
				store.put(entry);
			}
			result.put(key, entry);
			broadcastMiss(key, entry);
		}
		return result;
	}

	public void put(Entry<K, V> entry) {
		store.put(entry);
	}

	public boolean remove(final K key) {
		return store.remove(key);
	}
	
	private Entry<K,V> newEntry(K key, V newValue) {
		return new Entry<K,V>(key, newValue);
	}
	
	private boolean isExpired(final Entry<K,V> entry) {
		return expiryStrategy.isExpired(entry);
	}

	public void addCacheListener(CacheListener<K,V> listener) {
		listeners.add(listener);
	}

	public CacheStatistics getStatistics() {
		return store.getStatistics();
	}

	public boolean isAsynchronousUpdateEnabled() {
		return asynchronousUpdateEnabled;
	}

	public void setAsynchronousUpdateEnabled(boolean asynchronousUpdateEnabled) {
		this.asynchronousUpdateEnabled = asynchronousUpdateEnabled;
	}

	public boolean clear() {
		return this.store.clear();
	}
	
	public boolean contains(K key) {
		return this.store.contains(key);
	}

	public void setExpiryStrategy(ExpiryStrategy<K, V> expiryStrategy) {
		this.expiryStrategy = expiryStrategy;
	}

	/**
	 * Holds a key and an entry. Even though Entry has getKey(), entry can
	 * be null so we still need this internally.
	 */
	static class KeyEntry<K extends Serializable,V extends Serializable> {
		public final K key;
		public final Object data;
		public final Entry<K,V> entry;
		public KeyEntry(K k, Entry<K,V> e, Object d) {
			key = k;
			entry = e;
			data = d;
		}
	}

	public void shutdown() {
		store.shutdown();
	}

}
