/*
 * Created on 05-Dec-2005
 *
 */
package uk.ac.warwick.sso.client.cache;

public interface UserCache {

	/**
	 * Get a user from the cache. Returns null if user not in cache.
	 * 
	 */
	Object get(final Object key);

	Object put(final Object key, final UserCacheItem value);

	void remove(final Object token);

}