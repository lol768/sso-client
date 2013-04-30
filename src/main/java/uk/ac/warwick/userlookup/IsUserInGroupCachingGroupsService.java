package uk.ac.warwick.userlookup;

import uk.ac.warwick.userlookup.cache.Caches;
import uk.ac.warwick.userlookup.cache.EntryUpdateException;
import uk.ac.warwick.userlookup.cache.Pair;
import uk.ac.warwick.userlookup.cache.SingularEntryFactory;
import uk.ac.warwick.userlookup.webgroups.GroupServiceException;

/**
 * Decorater which will cache isUserInGroup from the GroupService.
 *
 * @author xusqac
 */
public final class IsUserInGroupCachingGroupsService extends CacheingGroupServiceAdapter<Pair<String, String>, Boolean> {
    private static final String CACHE_TIMEOUT_SECS="userlookup.cache.isuseringroup.timeout";
    private static final long DEFAULT_CACHE_TIMEOUT_SECS=Long.parseLong(UserLookup.getConfigProperty("ssoclient.groupservice.cache.isuseringroup.timeout"));

    public IsUserInGroupCachingGroupsService(final GroupService theGroupService) {
        super(theGroupService);
        setCache(Caches.newCache(UserLookup.IN_GROUP_CACHE_NAME, new SingularEntryFactory<Pair<String,String>, Boolean>() {
			public Boolean create(Pair<String,String> key, Object data) throws EntryUpdateException {
				// we munged the two arguments into a key - now to get them out.
				// It might be better to extend the Cache API to allow secondary data to
				// be passed to cache.get(), which will get sent here.
				String userId = key.getFirst();
				String group = key.getSecond();
				try {
					return getDecorated().isUserInGroup(userId, group);
				} catch (GroupServiceException e) {
					throw new EntryUpdateException(e);
				}
			}
			public boolean shouldBeCached(Boolean val) {
				return true;
			}
		}, determineCacheTimeOut()));
    }

    private long determineCacheTimeOut() {
        return Long.valueOf(UserLookup.getConfigProperty(CACHE_TIMEOUT_SECS, DEFAULT_CACHE_TIMEOUT_SECS + "")).longValue();
    }

    public boolean isUserInGroup(final String userId, final String group) throws GroupServiceException {
    	try {
			return getCache().get(Pair.of(userId, group));
		} catch (EntryUpdateException e) {
			if (e.getCause() instanceof GroupServiceException){
				throw (GroupServiceException)e.getCause();
			} else {
				throw e.getRuntimeException();
			}
		}
    }

}
