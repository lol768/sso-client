package uk.ac.warwick.userlookup;

import java.util.ArrayList;
import java.util.List;

import uk.ac.warwick.util.cache.Caches;
import uk.ac.warwick.util.cache.CacheEntryUpdateException;
import uk.ac.warwick.sso.client.cache.SerializeUtils;
import uk.ac.warwick.util.cache.SingularCacheEntryFactory;
import uk.ac.warwick.userlookup.webgroups.GroupServiceException;

/**
 * Decorator which will cache Groups by name from the GroupService.
 * 
 * @author cusyac
 */
final class GroupsNamesForUserCachingGroupsService extends CacheingGroupServiceAdapter<String, ArrayList<String>> {

	public static final long DEFAULT_TIMEOUT_SECS = 18000;

	private static final String CACHE_TIMEOUT_SECS = "userlookup.cache.groupbyname.timeout";

	private static final long DEFAULT_CACHE_TIMEOUT_SECS = Long.parseLong(UserLookup.getConfigProperty("ssoclient.groupservice.cache.groupsforuser.timeout"));

	public GroupsNamesForUserCachingGroupsService(final GroupService theGroupService) {
		super(theGroupService);
		setCache(Caches.newCache(UserLookup.USER_GROUPS_CACHE_NAME, new SingularCacheEntryFactory<String, ArrayList<String>>() {
			public ArrayList<String> create(final String key) throws CacheEntryUpdateException {
				try {
					return SerializeUtils.arrayList(getDecorated().getGroupsNamesForUser(key));
				} catch (GroupServiceException e) {
					throw new CacheEntryUpdateException(e);
				}
			}
			public boolean shouldBeCached(ArrayList<String> val) {
				return true;
			}
		}, determineCacheTimeOut()));
	}

	private long determineCacheTimeOut() {
		return Long.valueOf(UserLookup.getConfigProperty(CACHE_TIMEOUT_SECS, DEFAULT_CACHE_TIMEOUT_SECS + "")).longValue();
	}

	public List<String> getGroupsNamesForUser(final String userId) throws GroupServiceException {
		try {
			return getCache().get(userId);
		} catch (CacheEntryUpdateException e) {
			if (e.getCause() instanceof GroupServiceException) {
				throw (GroupServiceException)e.getCause();
			}
			throw e.getRuntimeException();
		}
	}
}
