package uk.ac.warwick.userlookup;

import uk.ac.warwick.util.cache.Cache;

import java.util.Map;
import java.util.Set;

public interface CacheingService {
	
	/**
	 * Returns a Map of all the caches currently in use. The key to the map is the store name
	 * of the cache.
	 * <p>
	 * This returns a mapping from the cache store name to the set of caches using this name, reflecting
	 * the fact that multiple caches may be using the same backing store.
	 */
	Map<String, Set<Cache<?, ?>>> getCaches();
	
	/**
	 * Clears any caches associated with this service.
	 */
	void clearCaches();

}
