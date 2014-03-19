package uk.ac.warwick.userlookup;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import uk.ac.warwick.userlookup.webgroups.GroupServiceAdapter;
import uk.ac.warwick.util.cache.Cache;
import uk.ac.warwick.util.cache.CacheListener;

public abstract class CacheingGroupServiceAdapter<K extends Serializable,V extends Serializable> extends GroupServiceAdapter implements CacheingService {
		
	private Cache<K, V> cache;
	
	protected CacheingGroupServiceAdapter(GroupService theGroupService) {
		this(theGroupService, null);
	}
	
	protected CacheingGroupServiceAdapter(GroupService theGroupService, Cache<K, V> theCache) {
		super(theGroupService);
		this.cache = theCache;
	}
	
	protected final Cache<K, V> getCache() {
		return cache;
	}

    protected final void setCache(Cache<K, V> cache) {
		this.cache = cache;
	}

	public void addCacheListener(final CacheListener<K, V> listener) {
        cache.addCacheListener(listener);
    }

	@Override
	public Map<String, Set<Cache<?, ?>>> getCaches() {
		Map<String, Set<Cache<?, ?>>> caches = new HashMap<String, Set<Cache<?, ?>>>();
		boolean addedCache = false;
		for (Map.Entry<String, Set<Cache<?, ?>>> otherCacheEntry: super.getCaches().entrySet()) {
			if (otherCacheEntry.getKey().equals(cache.getName())) {
				Set<Cache<?, ?>> theseCaches = new HashSet<Cache<?, ?>>();
				theseCaches.addAll(otherCacheEntry.getValue());
				theseCaches.add(cache);
				addedCache = true;
				caches.put(otherCacheEntry.getKey(), Collections.unmodifiableSet(theseCaches));
			} else {
				caches.put(otherCacheEntry.getKey(), otherCacheEntry.getValue());
			}
		}
		if (!addedCache) {
			Set<Cache<?, ?>> thisCache = new HashSet<Cache<?, ?>>();
			thisCache.add(cache);
			caches.put(cache.getName(), Collections.unmodifiableSet(thisCache));
		}
		
		return Collections.unmodifiableMap(caches);
	}

	@Override
	public void clearCaches() {
		getCache().clear();
		super.clearCaches();
	}

}
