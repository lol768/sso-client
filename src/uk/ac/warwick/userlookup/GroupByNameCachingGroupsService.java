package uk.ac.warwick.userlookup;

import uk.ac.warwick.userlookup.cache.Cache;
import uk.ac.warwick.userlookup.cache.CacheListener;
import uk.ac.warwick.userlookup.cache.Caches;
import uk.ac.warwick.userlookup.cache.EntryUpdateException;
import uk.ac.warwick.userlookup.cache.SingularEntryFactory;
import uk.ac.warwick.userlookup.webgroups.GroupNotFoundException;
import uk.ac.warwick.userlookup.webgroups.GroupServiceAdapter;

/**
 * Decorator which will cache Groups by name from the GroupService.
 */
final class GroupByNameCachingGroupsService extends GroupServiceAdapter {

    public static final long DEFAULT_TIMEOUT_SECS = 18000;

    private static final String CACHE_TIMEOUT_SECS="userlookup.cache.groupbyname.timeout";

    private static final long DEFAULT_CACHE_TIMEOUT_SECS=60*10;

    private Cache<String,Group> _cache;

    public GroupByNameCachingGroupsService(final GroupService theGroupService) {
        super(theGroupService);
        _cache = Caches.newCache(UserLookup.GROUP_CACHE_NAME, new SingularEntryFactory<String, Group>() {
			public Group create(final String key) throws EntryUpdateException {
				try {
					return getDecorated().getGroupByName(key);
				} catch (GroupNotFoundException e) {
					throw new EntryUpdateException(e);
				}
			}
			public boolean shouldBeCached(Group val) {
				return true;
			}
		}, determineCacheTimeOut());
    }

    private long determineCacheTimeOut() {
        return Long.valueOf(UserLookup.getConfigProperty(CACHE_TIMEOUT_SECS, DEFAULT_CACHE_TIMEOUT_SECS + "")).longValue();
    }

    public void addCacheListener(final CacheListener<String,Group> listener) {
        _cache.addCacheListener(listener);
    }

    public Group getGroupByName(final String name) throws GroupNotFoundException {
        try {
			return _cache.get(name);
		} catch (EntryUpdateException e) {
			if (e.getCause() instanceof GroupNotFoundException) {
				throw (GroupNotFoundException)e.getCause();
			} else {
				throw e.getRuntimeException();
			}
		}
    }
}
