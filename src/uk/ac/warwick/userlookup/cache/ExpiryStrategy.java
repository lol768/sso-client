package uk.ac.warwick.userlookup.cache;

import java.io.Serializable;

public interface ExpiryStrategy<K extends Serializable,V extends Serializable> {
	boolean isExpired(Entry<K,V> entry);
}
