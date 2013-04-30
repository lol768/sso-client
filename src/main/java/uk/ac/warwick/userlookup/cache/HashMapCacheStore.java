package uk.ac.warwick.userlookup.cache;

import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Cache store that uses a LinkedHashMap to evict the oldest items.
 * 
 * If you create two stores with the same name, they will use the same backing Map.
 * This normally doesn't cause a problem but you will probably find that only the
 * first created HashMapCacheStore will be able to set the size of the cache.
 * 
 * The Map is not typed simply because sometimes it's useful to have multiple
 * caches of different types sharing a cache. 
 * 
 * @author cusebr
 */
@SuppressWarnings("unchecked")
public final class HashMapCacheStore<K extends Serializable,V extends Serializable> implements CacheStore<K,V> {

	private static final int DEFAULT_CACHE_SIZE = 10000;
	
	private int maximumCacheSize = DEFAULT_CACHE_SIZE;
	
	private static HashMap<String, WeakReference<Map>> maps = new HashMap<String, WeakReference<Map>>();
	
	/**
	 * Synchronized means we're pretty thread safe but you still need to manually synchronize over
	 * loops. We don't do any loops at the moment.
	 */
	private final Map map;
	private final String name;
	
	public HashMapCacheStore(String name) {
		this.name = name;
		Map existingMap = get(name);
		if (existingMap == null) {
			map = Collections.synchronizedMap(new LinkedHashMap() {
				private static final long serialVersionUID = 1L;
				protected boolean removeEldestEntry(final Map.Entry eldest) {
			        return size() > maximumCacheSize;
			    }
			});
			maps.put(name, new WeakReference(map));
		} else {
			map = existingMap;
		}
	}

	public String getName() {
		return this.name;
	}

	private Map get(final String cacheName) {
		WeakReference<Map> weakReference = maps.get(cacheName);
		if (weakReference != null) {
			return weakReference.get();
		}
		return null;
	}
	
	/**
	 * If you try to get a key for a value that is of a different type,
	 * you will get a ClassCastException when retrieving the value.
	 */
	public Entry<K, V> get(K key) {
		return (Entry<K, V>) map.get(key);
	}

	public void put(Entry entry) {
		map.put(entry.getKey(), entry);
	}

	public boolean remove(K key) {
		map.remove(key);
		return true;
	}

	public CacheStatistics getStatistics() {
		return new CacheStatistics(
			map.size()	
		);
	}

	public void setMaxSize(int max) {
		maximumCacheSize = max;
	}

	public boolean clear() {
		map.clear();
		return true;
	}

	public boolean contains(K key) {
		return map.containsKey(key);
	}

	public void shutdown() {
		clear();
		maps.remove(name);
	}
	
	/** Empty all hashmap cache stores. For debugging. */
	public static void clearAll() {
		for (WeakReference<Map> ref : maps.values()) {
			Map m = ref.get();
			if (m != null) {
				m.clear();
			}
		}
	}

}
