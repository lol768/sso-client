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

/**
 * Decorater which will cache getUserCodesInGroup results from the GroupService
 *
 * @author cusyac
 */
public final class UsersInGroupCachingGroupsService extends GroupServiceAdapter {

    private static final String CACHE_TIMEOUT_SECS="userlookup.cache.usercodesingroup.timeout";

    private static final long DEFAULT_CACHE_TIMEOUT_SECS=60*10;

    private Cache<String,ArrayList<String>> _cache;

    public UsersInGroupCachingGroupsService(final GroupService theGroupService) {
        super(theGroupService);
        _cache = Caches.newCache(UserLookup.GROUP_MEMBER_CACHE_NAME, new SingularEntryFactory<String, ArrayList<String>>() {
			public ArrayList<String> create(final String group) throws RuntimeException {
				return SerializeUtils.arrayList(getDecorated().getUserCodesInGroup(group));
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

    public List<String> getUserCodesInGroup(final String group) {
    	try {
			return _cache.get(group);
		} catch (EntryUpdateException e) {
			// don't expect any exceptions
			throw new RuntimeException(e);
		}
    }
}