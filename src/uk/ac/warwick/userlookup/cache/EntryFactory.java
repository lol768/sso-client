package uk.ac.warwick.userlookup.cache;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Interface to a factory that will create a value appropriate
 * to be placed in the given key of the cache. It should
 * create a new object each time.
 * <p>
 * T is a type of exception that this factory may throw.
 * If you don't have any required exceptions that need to be
 * declared, you can put RuntimeException here.
 */
public interface EntryFactory<K extends Serializable,V extends Serializable> {
	
	int TIME_TO_LIVE_ETERNITY = -1;
	
	/**
	 * @param key Cache key to generate a value for
	 * @param data Optional data from the get() request that won't be stored but may be useful in generating the value.
	 */
	V create(K key, Object data) throws EntryUpdateException;
	
	/**
	 * If supported. If not supported, this should throw
	 * {@link UnsupportedOperationException} and {@link #isSupportsMultiLookups()}
	 * should return false.
	 */
	Map<K,V> create(List<K> keys) throws EntryUpdateException;
	
	/**
	 * @return Whether this factory supports the {@link #create(List)}
	 * 	method. If false, that method should throw an {@link UnsupportedOperationException}. 
	 */
	boolean isSupportsMultiLookups();
	
	/**
	 * Allows the EntryFactory to tell the cache whether this value
	 * should be stored in the cache, or it should just be passed back
	 * without caching.
	 * 
	 * The vast majority of the time this should return true, but
	 * there are cases such as when a lookup only partially succeeds,
	 * that we want to return an object but we don't want to cache it,
	 * such as an UnverifiedUser.
	 */
	boolean shouldBeCached(V val);
	
	/**
	 * Return how many seconds this entry should be cached for. Note that after
	 * this time the entry is eligible to be REMOVED from cache, i.e. you won't
	 * have a stale copy to do asynchronous updates. You only want to set a time
	 * to live if you want the cache system to be allowed to completely sweep away
	 * this entry after this time.
	 * 
	 * Return -1 to never totally expire the value from cache. It will still do
	 * asyncrhonous updates when stale, using the separate expiry time.
	 */
	int secondsToLive(V val);
}
