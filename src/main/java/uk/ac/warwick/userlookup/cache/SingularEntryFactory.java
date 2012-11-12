package uk.ac.warwick.userlookup.cache;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * EntryFactory which doesn't support batch lookups.
 */
public abstract class SingularEntryFactory<K extends Serializable,V extends Serializable> implements EntryFactory<K, V> {
	public boolean isSupportsMultiLookups() {
		return false;
	}
		
	public Map<K,V> create(List<K> keys) throws EntryUpdateException {
		throw new UnsupportedOperationException();
	}
	
	
	/**
	 * Default implementation tells the cache store not to automatically expire
	 * any items, leaving staleness and expiry up to our Cache implementation.
	 */
	public int secondsToLive(V val) {
		return TIME_TO_LIVE_ETERNITY; // always eternal
	}
}
