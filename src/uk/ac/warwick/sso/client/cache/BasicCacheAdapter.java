package uk.ac.warwick.sso.client.cache;

import uk.ac.warwick.sso.client.SSOToken;
import uk.ac.warwick.userlookup.cache.Cache;
import uk.ac.warwick.userlookup.cache.Entry;
import uk.ac.warwick.userlookup.cache.EntryUpdateException;

public class BasicCacheAdapter implements UserCache {

	private final Cache<SSOToken, UserCacheItem> cache;
	
	public BasicCacheAdapter(Cache<SSOToken, UserCacheItem> cache) {
		this.cache = cache;
	}

	public UserCacheItem get(SSOToken key) {
		try {
			return cache.get(key);
		} catch (EntryUpdateException e) {
			throw e.getRuntimeException();
		}
	}

	public void put(SSOToken key, UserCacheItem value) {
		cache.put(new Entry<SSOToken, UserCacheItem>(key, value));
	}

	public void remove(SSOToken token) {
		cache.remove(token);
	}

	public void clear() {
		cache.clear();
	}
	
	public int size() {
		return (int)cache.getStatistics().getCacheSize();
	}
	
	public void setMaxSize(int size) {
		cache.setMaxSize(size);
	}
	
	public void setTimeout(int seconds) {
		cache.setTimeout(seconds);
	}
}
