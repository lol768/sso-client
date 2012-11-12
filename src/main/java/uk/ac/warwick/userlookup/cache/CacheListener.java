package uk.ac.warwick.userlookup.cache;

import java.io.Serializable;

public interface CacheListener<K extends Serializable,V extends Serializable> {
    void cacheMiss(final K key, final Entry<K,V> newEntry);
    void cacheHit(final K key, final Entry<K,V> entry);
}
