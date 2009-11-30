package uk.ac.warwick.userlookup.cache;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;

import org.apache.log4j.Logger;

import uk.ac.warwick.userlookup.cache.BasicCache.KeyEntry;

final class UpdateEntryTask<K extends Serializable,V extends Serializable> implements Runnable {

	private final static Logger LOGGER = Logger.getLogger(UpdateEntryTask.class);
	
	private final Collection<KeyEntry<K,V>> entries;
	
	private final BasicCache<K,V> owner;

	public UpdateEntryTask(BasicCache<K,V> cache, KeyEntry<K,V> entry) {
		this(cache, Collections.singletonList(entry));
	}
	
	public UpdateEntryTask(BasicCache<K,V> cache, Collection<KeyEntry<K,V>> entries) {
		super();
		this.owner = cache;
		this.entries = entries;
	}
	
	public static <K extends Serializable,V extends Serializable> UpdateEntryTask<K,V> task(BasicCache<K,V> cache, KeyEntry<K,V> entry) {
		return new UpdateEntryTask<K,V>(cache,entry);
	}
	
	public static <K extends Serializable,V extends Serializable> UpdateEntryTask<K,V> task(BasicCache<K,V> cache, Collection<KeyEntry<K,V>> entry) {
		return new UpdateEntryTask<K,V>(cache,entry);
	}
	
	public void run() {
		try {
			if (entries.size() == 1) {
				owner.updateEntry(entries.iterator().next());
			} else {
				owner.updateEntries(entries);
			}
		} catch (EntryUpdateException e) {
			LOGGER.error("Failed to update entry asynchronously", e);
		}
	}

}
