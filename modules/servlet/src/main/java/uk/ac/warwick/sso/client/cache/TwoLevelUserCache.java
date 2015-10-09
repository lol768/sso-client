/*
 * Created on 07-Dec-2005
 *
 */
package uk.ac.warwick.sso.client.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.warwick.sso.client.SSOToken;

/**
 * Two level cache that takes a level one cache (most likely an InMemoryUserCache) and a level two cache (most likely a
 * DatabaseUserCache) and keeps the two in synch. This is used for caching user details in a clustered environment where
 * the user details are always pushed into the database level two cache and then normally read out of the level one
 * memory cache.
 * 
 * @author Kieran Shaw
 * 
 */
public class TwoLevelUserCache implements UserCache {

	private static final Logger LOGGER = LoggerFactory.getLogger(TwoLevelUserCache.class);

	private UserCache _levelOneCache;

	private UserCache _levelTwoCache;

	public TwoLevelUserCache(final UserCache levelOneCache, final UserCache levelTwoCache) {
		_levelOneCache = levelOneCache;
		_levelTwoCache = levelTwoCache;
	}

	public TwoLevelUserCache() {
		// default constructor
	}

	public final UserCacheItem get(final SSOToken key) {

		UserCacheItem item = _levelOneCache.get(key);
		if (item != null) {
			LOGGER.debug("Item found in level one cache under key " + key);
			return item;
		}

		item = _levelTwoCache.get(key);
		if (item == null) {
			LOGGER.debug("Item not found in level two cache under key " + key);
			return null;
		}
		LOGGER.debug("Item found in level two cache under key " + key);
		_levelOneCache.put(key, item);
		return item;

	}

	public final void put(final SSOToken key, final UserCacheItem value) {
		LOGGER.debug("Adding item to both caches under key " + key);
		_levelOneCache.put(key, value);
		_levelTwoCache.put(key, value);

	}

	public final void remove(final SSOToken key) {
		LOGGER.debug("Removing item from both caches under key " + key);
		_levelOneCache.remove(key);
		_levelTwoCache.remove(key);

	}

	public final UserCache getLevelOneCache() {
		return _levelOneCache;
	}

	public final void setLevelOneCache(final UserCache levelOneCache) {
		_levelOneCache = levelOneCache;
	}

	public final UserCache getLevelTwoCache() {
		return _levelTwoCache;
	}

	public final void setLevelTwoCache(final UserCache levelTwoCache) {
		_levelTwoCache = levelTwoCache;
	}

}
