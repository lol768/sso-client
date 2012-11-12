package uk.ac.warwick.userlookup.cache;

public class CacheStatistics {
	private final long cacheSize;
	
	public CacheStatistics(long cacheSize) {
		this.cacheSize = cacheSize;
	}

	/**
	 * Get the size of the cache, including any expired
	 * entries.
	 */
	public long getCacheSize() {
		return cacheSize;
	}
}
