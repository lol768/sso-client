package uk.ac.warwick.userlookup;

import uk.ac.warwick.util.cache.Cache;
import uk.ac.warwick.util.cache.Caches;
import uk.ac.warwick.util.cache.CacheEntryUpdateException;
import uk.ac.warwick.util.cache.SingularCacheEntryFactory;
import uk.ac.warwick.userlookup.webgroups.GroupNotFoundException;
import uk.ac.warwick.userlookup.webgroups.GroupServiceException;
import uk.ac.warwick.util.collections.Pair;

/**
 * Decorater which will cache isUserInGroup from the GroupService.
 *
 * @author xusqac
 */
public final class IsUserInGroupCachingGroupsService extends CacheingGroupServiceAdapter<Pair<String, String>, Boolean> {
    private static final String CACHE_TIMEOUT_SECS="userlookup.cache.isuseringroup.timeout";
    private static final long DEFAULT_CACHE_TIMEOUT_SECS=Long.parseLong(UserLookup.getConfigProperty("ssoclient.groupservice.cache.isuseringroup.timeout"));
    
    private Cache<String, Group> groupCache;

    @SuppressWarnings("unchecked")
	public IsUserInGroupCachingGroupsService(final GroupService theGroupService) {
        super(theGroupService);
        setCache(Caches.newCache(UserLookup.IN_GROUP_CACHE_NAME, new SingularCacheEntryFactory<Pair<String,String>, Boolean>() {
			public Boolean create(Pair<String,String> key) throws CacheEntryUpdateException {
				// we munged the two arguments into a key - now to get them out.
				// It might be better to extend the Cache API to allow secondary data to
				// be passed to cache.get(), which will get sent here.
				String userId = key.getLeft();
				String group = key.getRight();
				try {
					return getDecorated().isUserInGroup(userId, group);
				} catch (GroupServiceException e) {
					throw new CacheEntryUpdateException(e);
				}
			}
			public boolean shouldBeCached(Boolean val) {
				return true;
			}
		}, determineCacheTimeOut()));
        setGroupCache((Cache<String, Group>) theGroupService.getCaches().get(UserLookup.GROUP_CACHE_NAME).iterator().next());
    }

    private long determineCacheTimeOut() {
        return Long.valueOf(UserLookup.getConfigProperty(CACHE_TIMEOUT_SECS, DEFAULT_CACHE_TIMEOUT_SECS + "")).longValue();
    }

    public boolean isUserInGroup(final String userId, final String group) throws GroupServiceException {
    	// SSO-1372
		if (!getGroupCache().contains(group)) {
			try {
				getGroupCache().get(group);
			} catch (CacheEntryUpdateException e) {
				if (e.getCause() instanceof GroupServiceException){
					throw (GroupServiceException)e.getCause();
				} else if (e.getCause() instanceof GroupNotFoundException) {
					// do nothing, WarwickGroupService.isUserinGroup handles missing groups
				} else {
					throw e.getRuntimeException();
				}
			}
			getCache().remove(Pair.of(userId, group));
		}
    	try {
    		return getCache().get(Pair.of(userId, group));
		} catch (CacheEntryUpdateException e) {
			if (e.getCause() instanceof GroupServiceException){
				throw (GroupServiceException)e.getCause();
			} else {
				throw e.getRuntimeException();
			}
		}
    }

	public Cache<String, Group> getGroupCache() {
		return groupCache;
	}

	public void setGroupCache(Cache<String, Group> groupCache) {
		this.groupCache = groupCache;
	}

}
