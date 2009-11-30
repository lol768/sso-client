package uk.ac.warwick.userlookup.cache;

import java.io.Serializable;

/**
 * {@link CacheStore} implements a simple store of cache elements - 
 * it doesn't do anything clever with them.  
 * 
 * Implementations must be thread safe, in that concurrent calls mustn't
 * result in corrupt data or infinite loops. 
 */
public interface CacheStore<K extends Serializable,V extends Serializable> {
	Entry<K,V> get(K key);
	void put(Entry<K,V> entry);
	boolean remove(K key);
	
	CacheStatistics getStatistics();
	
	void setMaxSize(int max);
	boolean clear();
	boolean contains(K key);
	
	void shutdown();
}
