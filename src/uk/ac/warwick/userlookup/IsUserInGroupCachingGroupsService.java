package uk.ac.warwick.userlookup;

import uk.ac.warwick.userlookup.cache.Cache;
import uk.ac.warwick.userlookup.cache.CacheListener;
import uk.ac.warwick.userlookup.cache.Caches;
import uk.ac.warwick.userlookup.cache.EntryUpdateException;
import uk.ac.warwick.userlookup.cache.Pair;
import uk.ac.warwick.userlookup.cache.SingularEntryFactory;
import uk.ac.warwick.userlookup.webgroups.GroupServiceAdapter;

/**
 * Decorater which will cache isUserInGroup from the GroupService.
 *
 * @author xusqac
 */
public final class IsUserInGroupCachingGroupsService extends GroupServiceAdapter {
    private static final String CACHE_TIMEOUT_SECS="userlookup.cache.isuseringroup.timeout";
    private static final long DEFAULT_CACHE_TIMEOUT_SECS=60*10;

    private Cache<Pair<String,String>,Boolean> cache;

    public IsUserInGroupCachingGroupsService(final GroupService theGroupService) {
        super(theGroupService);
        cache = Caches.newCache(UserLookup.IN_GROUP_CACHE_NAME, new SingularEntryFactory<Pair<String,String>, Boolean>() {
			public Boolean create(Pair<String,String> key) {
				// we munged the two arguments into a key - now to get them out.
				// It might be better to extend the Cache API to allow secondary data to
				// be passed to cache.get(), which will get sent here.
				String userId = key.getFirst();
				String group = key.getSecond();
				return getDecorated().isUserInGroup(userId, group);
			}
			public boolean shouldBeCached(Boolean val) {
				return true;
			}
		}, determineCacheTimeOut());
    }

    private long determineCacheTimeOut() {
        return Long.valueOf(UserLookup.getConfigProperty(CACHE_TIMEOUT_SECS, DEFAULT_CACHE_TIMEOUT_SECS + "")).longValue();
    }

    public void addCacheListener(final CacheListener<Pair<String,String>, Boolean> listener) {
        cache.addCacheListener(listener);
    }

    public boolean isUserInGroup(final String userId, final String group) {
    	try {
			return cache.get(Pair.of(userId, group));
		} catch (EntryUpdateException e) {
			throw e.getRuntimeException();
		}
    }

}
