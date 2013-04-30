package uk.ac.warwick.userlookup;

import java.util.ArrayList;
import java.util.List;

import uk.ac.warwick.userlookup.cache.Caches;
import uk.ac.warwick.userlookup.cache.EntryUpdateException;
import uk.ac.warwick.userlookup.cache.SerializeUtils;
import uk.ac.warwick.userlookup.cache.SingularEntryFactory;
import uk.ac.warwick.userlookup.webgroups.GroupServiceException;

/**
 * Decorater which will cache getUserCodesInGroup results from the GroupService
 *
 * @author cusyac
 */
public final class UsersInGroupCachingGroupsService extends CacheingGroupServiceAdapter<String, ArrayList<String>> {

    private static final String CACHE_TIMEOUT_SECS="userlookup.cache.usercodesingroup.timeout";

    private static final long DEFAULT_CACHE_TIMEOUT_SECS=Long.parseLong(UserLookup.getConfigProperty("ssoclient.groupservice.cache.usersingroup.timeout"));

    public UsersInGroupCachingGroupsService(final GroupService theGroupService) {
        super(theGroupService);
        setCache(Caches.newCache(UserLookup.GROUP_MEMBER_CACHE_NAME, new SingularEntryFactory<String, ArrayList<String>>() {
			public ArrayList<String> create(final String group, final Object data) throws EntryUpdateException {
				try {
					return SerializeUtils.arrayList(getDecorated().getUserCodesInGroup(group));
				} catch (GroupServiceException e) {
					throw new EntryUpdateException(e);
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

    public List<String> getUserCodesInGroup(final String group) throws GroupServiceException {
    	try {
			return getCache().get(group);
		} catch (EntryUpdateException e) {
			if (e.getCause() instanceof GroupServiceException){
				throw (GroupServiceException)e.getCause();
			} else {
				throw e.getRuntimeException();
			}
		}
    }
}
