package uk.ac.warwick.userlookup.cache.ehcache;

import java.io.File;
import java.io.Serializable;
import java.net.URL;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;

import org.apache.log4j.Logger;

import uk.ac.warwick.userlookup.UserLookup;
import uk.ac.warwick.userlookup.cache.CacheStatistics;
import uk.ac.warwick.userlookup.cache.CacheStore;
import uk.ac.warwick.userlookup.cache.Entry;
import uk.ac.warwick.userlookup.cache.EntryFactory;

/**
 * Cache implementation which uses EhCache, of course.
 */
public final class EhCacheStore<K extends Serializable,V extends Serializable> implements CacheStore<K, V> {

	/**
	 * The classpath location that this class will check in for a custom EhCache
	 * configuration file.
	 */
	public static final String CUSTOM_CONFIG_URL = "ehcache-userlookup.xml";

	private static final Logger LOGGER = Logger.getLogger(EhCacheStore.class);
	
	private static CacheManager defaultCacheManager;
	
	private CacheManager cacheManager;
	private Ehcache cache;
	private final String cacheName;
	
	
	/**
	 * Create an EhCacheStore using an EhCache configuration found
	 * at the given URL. The name is used as the cache name.
	 * 
	 * You should manage the lifecycle of this CacheManager yourself.
	 */
	public EhCacheStore(final String name, final CacheManager manager) {
		cacheName = name;
		cacheManager = manager;
		init();
	}
	
	/**
	 * Creates an EhCacheStore using a shared CacheManager loaded from either
	 * a default classpath location, or one specified by the system property 
	 * userlookup.ehcache.config. Subsequent EhCacheStores created with this
	 * constructor will use the same CacheManager.
	 */
	public EhCacheStore(final String name) {
		cacheName = name;
		if (defaultCacheManager == null) {
			configureDiskStoreLocation();
			
			String location = UserLookup.getConfigProperty("userlookup.ehcache.config");
			if (location == null || location.equals("")) {
				location = CUSTOM_CONFIG_URL;
			}
			URL url = getClass().getResource(location);
			if (url == null) {
				LOGGER.info("No custom "+location+" found, using default configuration");
				String defaultUrl = "/ehcache-userlookup-default.xml";
				url = getClass().getResource(defaultUrl);
				if (url == null) {
					throw new IllegalStateException("Could not load " + defaultUrl);
				}
			} else {
				LOGGER.info("Loading configuration from custom "+location+" file");
			}
			defaultCacheManager = new CacheManager(url);
		}
		cacheManager = defaultCacheManager;
		init();
	}

	public String getName() {
		return this.cacheName;
	}

	/**
	 * If no property is set, the OS tmp dir is used for disk cache.
	 * 
	 * If ehcache.disk.store.dir is set, that is used instead.
	 * If userlookup.disk.store.dir is set, this overrides the previous property. So you
	 * can use either but the latter is useful if you are using ehcache.disk.store.dir for
	 * another generic Ehcache instance and you want the locations to be different. 
	 */
	private void configureDiskStoreLocation() {
		String diskStoreDir = System.getProperty("ehcache.disk.store.dir");
		String userLookupDir = System.getProperty("userlookup.disk.store.dir");
		if (userLookupDir == null) {
			if (diskStoreDir != null) {
				System.setProperty("userlookup.disk.store.dir", diskStoreDir);
			} else {
				// This shouldn't be run in production. The Caches factory method will avoid
				// creating an EhCacheStore if neither property is available.
				LOGGER.error("Either ehcache.disk.store.dir or userlookup.disk.store.dir should be set - using java.io.tmpdir for disk cache instead");
				System.setProperty("userlookup.disk.store.dir", System.getProperty("java.io.tmpdir") + File.separatorChar + System.currentTimeMillis());
			}
		}
		
		LOGGER.info("Cache disk store location: " + System.getProperty("userlookup.disk.store.dir"));
	}

	private void init() {
		cache = cacheManager.getEhcache(cacheName);
		if (cache == null) {
			throw new IllegalStateException("Could not find an Ehcache named " + cacheName);
		}
	}

	@SuppressWarnings("unchecked")
	public Entry<K,V> get(K key) {
		Element element = cache.get(key);
		if (element == null) {
			return null;
		}
		return (Entry<K,V>)element.getValue();
	}
	
	public void put(Entry<K,V> entry) {
		Element element = new Element(entry.getKey(), entry);
		if (entry.getTimeToLive() == EntryFactory.TIME_TO_LIVE_ETERNITY) {
			element.setEternal(true);
		} else {
			element.setTimeToLive(entry.getTimeToLive());
		}
		cache.put(element);
	}
	
	public boolean remove(K key) {
		return cache.remove(key);
	}

	public CacheStatistics getStatistics() {
		return new CacheStatistics(
			cache.getStatistics().getObjectCount()
		);
	}

	public void setMaxSize(int maxSize) {
		LOGGER.error("Can't change Ehcache size at runtime - use the Ehcache configuration XML");
	}

	public boolean clear() {
		cache.removeAll();
		return true;
	};
	
	public boolean contains(K key) {
		return cache.isKeyInCache(key);
	}

	public void shutdown() {
		LOGGER.info("Shutting down CacheManager");
		clear();
		if (cacheManager != defaultCacheManager) {
			cacheManager.shutdown();
		}
	}
	
	public void flush() {
	}
	
	/**
	 * Normally you can allow the shutdown hooks to 
	 */
	public static final void shutdownDefaultCacheManager() {
		if (defaultCacheManager != null) {
			defaultCacheManager.shutdown();
			defaultCacheManager = null;
		}
	}

	public final CacheManager getCacheManager() {
		return cacheManager;
	}

}
