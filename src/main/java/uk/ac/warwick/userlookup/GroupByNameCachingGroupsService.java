package uk.ac.warwick.userlookup;

import uk.ac.warwick.util.cache.Caches;
import uk.ac.warwick.util.cache.CacheEntryUpdateException;
import uk.ac.warwick.util.cache.SingularCacheEntryFactory;
import uk.ac.warwick.userlookup.webgroups.GroupNotFoundException;
import uk.ac.warwick.userlookup.webgroups.GroupServiceException;

import static uk.ac.warwick.userlookup.UserLookup.getConfigProperty;

/**
 * Decorator which will cache Groups by name from the GroupService.
 */
public final class GroupByNameCachingGroupsService extends CacheingGroupServiceAdapter<String, Group> {

    public static final long DEFAULT_TIMEOUT_SECS = 18000;

    private static final String CACHE_TIMEOUT_SECS="userlookup.cache.groupbyname.timeout";

    private static final long DEFAULT_CACHE_TIMEOUT_SECS=Long.parseLong(UserLookup.getConfigProperty("ssoclient.groupservice.cache.groupbyname.timeout"));

    public GroupByNameCachingGroupsService(final GroupService theGroupService) {
        super(theGroupService);
        setCache(Caches.newCache(UserLookup.GROUP_CACHE_NAME, new SingularCacheEntryFactory<String, Group>() {
			public Group create(final String key) throws CacheEntryUpdateException {
				try {
					return getDecorated().getGroupByName(key);
				} catch (GroupNotFoundException e) {
					throw new CacheEntryUpdateException(e);
				} catch (GroupServiceException e) {
					throw new CacheEntryUpdateException(e);
				}
			}
			public boolean shouldBeCached(Group val) {
				return true;
			}
		}, determineCacheTimeOut(), Caches.CacheStrategy.valueOf(getConfigProperty("ssoclient.cache.strategy"))));
    }

    private long determineCacheTimeOut() {
        return Long.valueOf(UserLookup.getConfigProperty(CACHE_TIMEOUT_SECS, DEFAULT_CACHE_TIMEOUT_SECS + "")).longValue();
    }

    public Group getGroupByName(final String name) throws GroupNotFoundException, GroupServiceException {
        try {
			return getCache().get(name);
		} catch (CacheEntryUpdateException e) {
			if (e.getCause() instanceof GroupNotFoundException) {
				throw (GroupNotFoundException)e.getCause();
			} else if (e.getCause() instanceof GroupServiceException){
				throw (GroupServiceException)e.getCause();
			} else {
				throw e.getRuntimeException();
			}
		}
    }
}
