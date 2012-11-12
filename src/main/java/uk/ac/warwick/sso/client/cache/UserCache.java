/*
 * Created on 05-Dec-2005
 *
 */
package uk.ac.warwick.sso.client.cache;

import uk.ac.warwick.sso.client.SSOToken;

public interface UserCache {

	/**
	 * Get a user from the cache. Returns null if user not in cache.
	 * 
	 */
	UserCacheItem get(final SSOToken key);

	void put(final SSOToken key, final UserCacheItem value);

	void remove(final SSOToken token);

}