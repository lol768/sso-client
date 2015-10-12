package uk.ac.warwick.userlookup;

import java.util.ArrayList;
import java.util.List;

import uk.ac.warwick.sso.client.cache.SerializeUtils;
import uk.ac.warwick.util.cache.Caches;
import uk.ac.warwick.util.cache.CacheEntryUpdateException;
import uk.ac.warwick.util.cache.SingularCacheEntryFactory;
import uk.ac.warwick.userlookup.webgroups.GroupServiceException;

import static uk.ac.warwick.userlookup.UserLookup.getConfigProperty;

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
        setCache(Caches.newCache(UserLookup.GROUP_MEMBER_CACHE_NAME, new SingularCacheEntryFactory<String, ArrayList<String>>() {
			public ArrayList<String> create(final String group) throws CacheEntryUpdateException {
				try {
					return SerializeUtils.arrayList(getDecorated().getUserCodesInGroup(group));
				} catch (GroupServiceException e) {
					throw new CacheEntryUpdateException(e);
				}
			}
			public boolean shouldBeCached(ArrayList<String> val) {
				return true;
			}
		}, determineCacheTimeOut(), Caches.CacheStrategy.valueOf(getConfigProperty("ssoclient.cache.strategy"))));
    }

    private long determineCacheTimeOut() {
        return Long.valueOf(UserLookup.getConfigProperty(CACHE_TIMEOUT_SECS, DEFAULT_CACHE_TIMEOUT_SECS + "")).longValue();
    }

    public List<String> getUserCodesInGroup(final String group) throws GroupServiceException {
    	try {
			return getCache().get(group);
		} catch (CacheEntryUpdateException e) {
			if (e.getCause() instanceof GroupServiceException){
				throw (GroupServiceException)e.getCause();
			} else {
				throw e.getRuntimeException();
			}
		}
    }
}
