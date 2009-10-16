/*
 * Created on 04-Aug-2003
 *
 */
package uk.ac.warwick.sso.client.cache;

import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.log4j.Logger;

import uk.ac.warwick.sso.client.SSOToken;

/**
 * 
 * A cache of <code>User</code> to prevent applications from repeatedly calling the SSO service for the same user.
 * Caches for 10 hours.
 * 
 */
public class InMemoryUserCache implements UserCache {

	private static final float DEFAULT_LOAD_FACTOR = 0.75f;

	private static final int DEFAULT_MAX_ENTRIES = 50000;

	private static final int DEFAULT_TIME_OUT = 43200;

	private int _timeout = DEFAULT_TIME_OUT;

	private int _maxEntries = DEFAULT_MAX_ENTRIES;

	private static final Logger LOGGER = Logger.getLogger(InMemoryUserCache.class);

	private Map<SSOToken, UserCacheItem> _cache;

	public InMemoryUserCache() {
		// create a new LinkedHashMap with 36000 initial capacity, 0.75 load and
		// insertion ordering
		this._cache = Collections.synchronizedMap(new LinkedHashMap<SSOToken, UserCacheItem>(DEFAULT_MAX_ENTRIES, DEFAULT_LOAD_FACTOR, false) {
			private static final long serialVersionUID = 4257842076090008574L;
			protected boolean removeEldestEntry(final Entry<SSOToken, UserCacheItem> eldest) {
				boolean doRemove = size() > getMaxEntries();
				if (doRemove && LOGGER.isDebugEnabled()) {
					LOGGER.debug("removeEldestEntry returning true: size = " + size());
				}
				return doRemove;
			}
		});
	}

	/**
	 * @param key
	 * @return
	 */
	public final UserCacheItem get(final SSOToken key) {
		long start = System.currentTimeMillis();
		UserCacheItem item = _cache.get(key);
		if (item != null) {
			final int millisInSec = 1000;
			if ((item.getInTime() + (getTimeout() * millisInSec)) > new Date().getTime()) {
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("GET completed (valid non-null item) in " + (System.currentTimeMillis() - start) + "ms");
				}
				return item;
			}
			_cache.remove(key);
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("GET completed (invalid non-null item) in " + (System.currentTimeMillis() - start) + "ms");
			}
			return null;
		}
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("GET completed (null item) in " + (System.currentTimeMillis() - start) + "ms");
		}
		return null;
	}

	/**
	 * @param key
	 * @param value
	 * @return
	 */
	public final void put(final SSOToken key, final UserCacheItem value) {
		long start = System.currentTimeMillis();
		boolean reInserted = false;
		// remove the element if it exists, to ensure that the insertion-order
		// mapping is updated
		if (_cache.containsKey(key)) {
			reInserted = true;
			_cache.remove(key);
		}
		_cache.put(key, value);
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("PUT completed (re-inserted: " + reInserted + ") in " + (System.currentTimeMillis() - start) + "ms");
		}

	}

	public final int getMaxEntries() {
		return _maxEntries;
	}

	public final int getTimeout() {
		return _timeout;
	}

	/**
	 * Set the maximum size for the cache
	 */
	public final void setMaxEntries(final int i) {
		_maxEntries = i;
	}

	/**
	 * Set the timeout period (in seconds) for the cache
	 */
	public final void setTimeout(final int i) {
		_timeout = i;
	}

	public final int size() {
		return _cache.size();
	}

	/**
	 * @param token
	 */
	public final void remove(final SSOToken token) {
		long start = System.currentTimeMillis();
		_cache.remove(token);
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("REMOVE completed in " + (System.currentTimeMillis() - start) + "ms");
		}

	}

	// for testing
	final void clear() {
		_cache.clear();
	}

	// for testing
	final boolean containsKey(final String string) {
		return _cache.containsKey(string);
	}
}