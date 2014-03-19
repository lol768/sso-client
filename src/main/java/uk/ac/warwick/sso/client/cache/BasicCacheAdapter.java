package uk.ac.warwick.sso.client.cache;

import uk.ac.warwick.sso.client.SSOToken;
import uk.ac.warwick.util.cache.Cache;
import uk.ac.warwick.util.cache.CacheEntry;
import uk.ac.warwick.util.cache.CacheEntryUpdateException;
import uk.ac.warwick.util.cache.CacheStoreUnavailableException;

/**
 * Adapter for creating a UserCache implementation (for SSOClient)
 * powered by a Cache implementation (from WarwickUtils-Cache).
 */
public class BasicCacheAdapter implements UserCache {

	private final Cache<SSOToken, UserCacheItem> cache;
	
	public BasicCacheAdapter(Cache<SSOToken, UserCacheItem> cache) {
		this.cache = cache;
	}

	public UserCacheItem get(SSOToken key) {
		try {
			return cache.get(key);
		} catch (CacheEntryUpdateException e) {
			throw e.getRuntimeException();
		}
	}

	public void put(SSOToken key, UserCacheItem value) {
		cache.put(new CacheEntry<SSOToken, UserCacheItem>(key, value));
	}

	public void remove(SSOToken token) {
		cache.remove(token);
	}

	public void clear() {
		cache.clear();
	}
	
	public int size() {
        try {
		    return (int)cache.getStatistics().getCacheSize();
        } catch (CacheStoreUnavailableException e) {
            return 0;
        }
	}
	
	public void setMaxSize(int size) {
		cache.setMaxSize(size);
	}
	
	public void setTimeout(int seconds) {
		cache.setTimeout(seconds);
	}
}
