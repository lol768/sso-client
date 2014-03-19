/*
 * Created on 04-Aug-2003
 *
 */
package uk.ac.warwick.sso.client.cache;

import static java.lang.Integer.*;
import static uk.ac.warwick.userlookup.UserLookup.*;
import uk.ac.warwick.sso.client.SSOToken;
import uk.ac.warwick.userlookup.UserLookup;
import uk.ac.warwick.util.cache.*;

/**
 * 
 * A cache of <code>User</code> to prevent applications from repeatedly calling the SSO service for the same user.
 * Caches for 12 hours.
 * 
 */
public class InMemoryUserCache extends BasicCacheAdapter {

	private static final int DEFAULT_MAX_ENTRIES = parseInt(getConfigProperty("ssoclient.sessioncache.memory.max-size"));;

	private static final int DEFAULT_TIME_OUT = parseInt(getConfigProperty("ssoclient.sessioncache.memory.timeout.secs"));

	public InMemoryUserCache() {
		super(newCache());
	}

	private static Cache<SSOToken, UserCacheItem> newCache() {
		// used only for putting new values in. This should access the same map
		// that UserLookup uses for storing users.
		
		// Create a new BasicCache. The EntryFactory always returns null, because SSOClient doesn't expect
		// asynchronous updates or self-population. It will receive the null and generate a new value.		
		final Cache<SSOToken, UserCacheItem> newCache = Caches.newCache(UserLookup.USER_CACHE_NAME, new SingularCacheEntryFactory<SSOToken, UserCacheItem>() {
			public UserCacheItem create(SSOToken item) throws CacheEntryUpdateException {
				return null;
			}
			public boolean shouldBeCached(UserCacheItem item) {
				return item != null;
			}
		}, DEFAULT_TIME_OUT, Caches.CacheStrategy.valueOf(getConfigProperty("ssoclient.cache.strategy")));

		newCache.setMaxSize(DEFAULT_MAX_ENTRIES); //ignored if we are using Ehcache.
		return newCache;
	}
}