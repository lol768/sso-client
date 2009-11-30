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
}
