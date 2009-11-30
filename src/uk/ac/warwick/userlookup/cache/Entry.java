package uk.ac.warwick.userlookup.cache;

import java.io.Serializable;

/**
 * Represents an entry in the cache. Contains the key and the value.
 * Both must implement Serializable (this isn't mandatory for most of our cache stores
 * but it's better to enforce it now rather than find out later that you can't
 * use a disk-based backend or use any clustering).
 */
public class Entry<K extends Serializable, V extends Serializable> implements Serializable {
	private static final long serialVersionUID = -4384852442875029950L;
	private final K key;
	private final V value;
	private final long created = System.currentTimeMillis();
	private transient volatile boolean updating;
	
	public Entry(K k, V val) {
		this.key = k;
		this.value = val;
	}
	
	public K getKey() {
		return key;
	}
	
	public V getValue() {
		return value;
	}
	
	public long getTimestamp() {
		return created;
	}

	public boolean isUpdating() {
		return updating;
	}

	public void setUpdating(boolean updating) {
		this.updating = updating;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Entry) {
			Entry e = (Entry) obj;
			return key.equals(e.key) && e.value == value;
		}
		return false;
	}
}
