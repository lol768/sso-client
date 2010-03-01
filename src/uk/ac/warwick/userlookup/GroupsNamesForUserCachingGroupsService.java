package uk.ac.warwick.userlookup;

import java.util.ArrayList;
import java.util.List;

import uk.ac.warwick.userlookup.cache.Cache;
import uk.ac.warwick.userlookup.cache.CacheListener;
import uk.ac.warwick.userlookup.cache.Caches;
import uk.ac.warwick.userlookup.cache.EntryUpdateException;
import uk.ac.warwick.userlookup.cache.SerializeUtils;
import uk.ac.warwick.userlookup.cache.SingularEntryFactory;
import uk.ac.warwick.userlookup.webgroups.GroupServiceAdapter;
import uk.ac.warwick.userlookup.webgroups.GroupServiceException;

/**
 * Decorator which will cache Groups by name from the GroupService.
 * 
 * @author cusyac
 */
final class GroupsNamesForUserCachingGroupsService extends GroupServiceAdapter {

	public static final long DEFAULT_TIMEOUT_SECS = 18000;

	private static final String CACHE_TIMEOUT_SECS = "userlookup.cache.groupbyname.timeout";

	private static final long DEFAULT_CACHE_TIMEOUT_SECS = 60 * 10;

	private Cache<String, ArrayList<String>> _cache;

	public GroupsNamesForUserCachingGroupsService(final GroupService theGroupService) {
		super(theGroupService);
		_cache = Caches.newCache(UserLookup.USER_GROUPS_CACHE_NAME, new SingularEntryFactory<String, ArrayList<String>>() {
			public ArrayList<String> create(final String key) throws EntryUpdateException {
				try {
					return SerializeUtils.arrayList(getDecorated().getGroupsNamesForUser(key));
				} catch (GroupServiceException e) {
					throw new EntryUpdateException(e);
				}
			}
			public boolean shouldBeCached(ArrayList<String> val) {
				return true;
			}
		}, determineCacheTimeOut());
	}

	private long determineCacheTimeOut() {
		return Long.valueOf(UserLookup.getConfigProperty(CACHE_TIMEOUT_SECS, DEFAULT_CACHE_TIMEOUT_SECS + "")).longValue();
	}

	public void addCacheListener(final CacheListener<String, ArrayList<String>> listener) {
		_cache.addCacheListener(listener);
	}

	public List<String> getGroupsNamesForUser(final String userId) throws GroupServiceException {
		try {
			return _cache.get(userId);
		} catch (EntryUpdateException e) {
			if (e.getCause() instanceof GroupServiceException) {
				throw (GroupServiceException)e.getCause();
			}
			throw e.getRuntimeException();
		}
	}
}
