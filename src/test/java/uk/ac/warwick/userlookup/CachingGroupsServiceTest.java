package uk.ac.warwick.userlookup;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;

import org.jmock.Expectations;
import org.jmock.Mockery;

import uk.ac.warwick.userlookup.cache.Cache;
import uk.ac.warwick.userlookup.cache.CacheListener;
import uk.ac.warwick.userlookup.cache.Entry;
import uk.ac.warwick.userlookup.cache.Pair;
import uk.ac.warwick.userlookup.webgroups.GroupInfo;
import uk.ac.warwick.userlookup.webgroups.GroupNotFoundException;

public final class CachingGroupsServiceTest extends TestCase {
	
	private Mockery m = new Mockery();

	@SuppressWarnings("unchecked")
	public void testDelegates() throws Exception{
		final String testUserId = "user";
		final String testGroupName = "group";
		final Group testGroup = new GroupImpl();
		final WebServiceTimeoutConfig testConfig = new WebServiceTimeoutConfig();
		
		final Cache<String, Group> cache = m.mock(Cache.class);

		GroupService decorated = new GroupService() {

			public List getGroupsForUser(final String userId) {
				assertEquals("user", testUserId, userId);
				return Collections.EMPTY_LIST;
			}

			public List getGroupsNamesForUser(final String userId) {
				assertEquals("user", testUserId, userId);
				return Collections.EMPTY_LIST;
			}

			public boolean isUserInGroup(final String userId, final String groupName) {
				assertEquals("user", testUserId, userId);
				assertEquals("group", testGroupName, groupName);
				return true;
			}

			public List getUserCodesInGroup(final String groupName) {
				assertEquals("group", testGroupName, groupName);
				return Collections.EMPTY_LIST;
			}

			public List getRelatedGroups(final String groupName) {
				assertEquals("group", testGroupName, groupName);
				return Collections.EMPTY_LIST;
			}

			public Group getGroupByName(final String groupName) throws GroupNotFoundException {
				assertEquals("group", testGroupName, groupName);
				return testGroup;
			}

			public void setTimeoutConfig(final WebServiceTimeoutConfig config) {
				assertEquals("timeoutConfig", testConfig, config);
			}
			
			public List getGroupsForDeptCode(String deptCode) {
				return null;
			}

			public List getGroupsForQuery(String search) {
				return null;
			}

			public GroupInfo getGroupInfo(String name)
					throws GroupNotFoundException {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public Map<String, Set<Cache<?, ?>>> getCaches() {
				final Set<Cache<?, ?>> cacheSet = new HashSet<Cache<?, ?>>();
				cacheSet.add(cache);
				final Map<String, Set<Cache<?, ?>>> caches = new HashMap<String, Set<Cache<?, ?>>>();
				caches.put(UserLookup.GROUP_CACHE_NAME, Collections.unmodifiableSet(cacheSet));
				return caches;
			}

			@Override
			public void clearCaches() {
				// TODO Auto-generated method stub
				
			}
		};
		
		m.checking(new Expectations() {{
			one(cache).contains("group");
			one(cache).get("group");
		}});

		IsUserInGroupCachingGroupsService cachingService = new IsUserInGroupCachingGroupsService(decorated);
		assertEquals("getGroupsForUser", decorated.getGroupsForUser(testUserId), cachingService.getGroupsForUser(testUserId));
		assertEquals("getGroupsNamesForUser", decorated.getGroupsNamesForUser(testUserId), cachingService
				.getGroupsNamesForUser(testUserId));
		assertEquals("isUserInGroup", decorated.isUserInGroup(testUserId, testGroupName), cachingService.isUserInGroup(
				testUserId, testGroupName));
		assertEquals("getUserCodesInGroup", decorated.getUserCodesInGroup(testGroupName), cachingService
				.getUserCodesInGroup(testGroupName));
		assertEquals("getRelatedGroups", decorated.getRelatedGroups(testGroupName), cachingService
				.getRelatedGroups(testGroupName));
		assertEquals("getGroupByName", decorated.getGroupByName(testGroupName), cachingService.getGroupByName(testGroupName));
		cachingService.setTimeoutConfig(testConfig); // for the assertion on
		// setTimeout
	}

	public void testCacheWorks() throws Exception {
		final String firstUserId = "userIdA";
		final String firstGroupName = "groupName";
		final Cache<String, Group> cache = m.mock(Cache.class);

		TestCacheListener listener = new TestCacheListener();

		GroupService decorated = new GroupService() {

			public List getGroupsForUser(final String userId) {
				return null;
			}

			public List getGroupsNamesForUser(final String userId) {
				return null;
			}

			public boolean isUserInGroup(final String userId, final String group) {
				return false;
			}

			public List getUserCodesInGroup(final String group) {
				return null;
			}

			public List getRelatedGroups(final String group) {
				return null;
			}

			public Group getGroupByName(final String name) {
				return null;
			}

			public void setTimeoutConfig(final WebServiceTimeoutConfig config) {
				// does nothing
			}
			
			public List getGroupsForDeptCode(String deptCode) {
				return null;
			}

			public List getGroupsForQuery(String search) {
				return null;
			}

			public GroupInfo getGroupInfo(String name)
					throws GroupNotFoundException {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public Map<String, Set<Cache<?, ?>>> getCaches() {
				final Set<Cache<?, ?>> cacheSet = new HashSet<Cache<?, ?>>();
				cacheSet.add(cache);
				final Map<String, Set<Cache<?, ?>>> caches = new HashMap<String, Set<Cache<?, ?>>>();
				caches.put(UserLookup.GROUP_CACHE_NAME, Collections.unmodifiableSet(cacheSet));
				return caches;
			}

			@Override
			public void clearCaches() {
				// TODO Auto-generated method stub
				
			}
		};
		
		m.checking(new Expectations() {{
			allowing(cache).contains("groupName"); will(returnValue(true));
			one(cache).get("groupName");
			one(cache).contains("groupName0"); will(returnValue(true));
			one(cache).get("groupName0");
			one(cache).contains("groupName1"); will(returnValue(true));
			one(cache).get("groupName1");
			one(cache).contains("groupName2"); will(returnValue(true));
			one(cache).get("groupName2");
		}});
		
		IsUserInGroupCachingGroupsService cachingService = new IsUserInGroupCachingGroupsService(decorated);
		cachingService.addCacheListener(listener);

		cachingService.isUserInGroup(firstUserId, firstGroupName);
		cachingService.isUserInGroup(firstUserId + "0", firstGroupName + "0");
		cachingService.isUserInGroup(firstUserId + "1", firstGroupName + "1");
		// expect 3 misses
		assertEquals("number of misses", 3, listener.numberOfCacheMisses);
		assertEquals("number of hits", 0, listener.numberOfCacheHits);
		// force a cache hit
		cachingService.isUserInGroup(firstUserId, firstGroupName);
		assertEquals("number of misses", 3, listener.numberOfCacheMisses);
		assertEquals("expected a hit", 1, listener.numberOfCacheHits);
		// force a purge
		cachingService.isUserInGroup(firstUserId + "2", firstGroupName + "2");
	}

	public void testGroupByNameCache() throws Exception {

		final String groupName = "@IN";

		GroupService decorated = new GroupService() {

			public List getGroupsForUser(final String userId) {
				return null;
			}

			public List getGroupsNamesForUser(final String userId) {
				return null;
			}

			public boolean isUserInGroup(final String userId, final String group) {
				return false;
			}

			public List getUserCodesInGroup(final String group) {
				return null;
			}

			public List getRelatedGroups(final String group) {
				return null;
			}

			public Group getGroupByName(final String name) {
				GroupImpl group = new GroupImpl();
				group.setName(name);
				group.setTitle("A group");
				group.setUserCodes(getUserCodesInGroup(groupName));
				return group;
			}

			public void setTimeoutConfig(final WebServiceTimeoutConfig config) {
				// does nothing
			}
			
			public List getGroupsForDeptCode(String deptCode) {
				return null;
			}

			public List getGroupsForQuery(String search) {
				return null;
			}

			public GroupInfo getGroupInfo(String name)
					throws GroupNotFoundException {
				return null;
			}

			@Override
			public Map<String, Set<Cache<?, ?>>> getCaches() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public void clearCaches() {
				// TODO Auto-generated method stub
				
			}
		};

		GroupByNameCachingGroupsService cachingService = new GroupByNameCachingGroupsService(decorated);
		TestGroupByNameCacheListener listener = new TestGroupByNameCacheListener();
		cachingService.addCacheListener(listener);

		Group group = cachingService.getGroupByName(groupName);
		assertEquals("Should have right group","A group",group.getTitle());
		assertEquals("Should have had a cache miss", 1, listener.numberOfCacheMisses);
		assertEquals("Should have had no cache hits", 0, listener.numberOfCacheHits);

		group = cachingService.getGroupByName(groupName);
		assertEquals("Should have right group","A group",group.getTitle());
		assertEquals("Should have had no more cache misses", 1, listener.numberOfCacheMisses);
		assertEquals("Should have had a cache hit", 1, listener.numberOfCacheHits);

	}
}

class TestCacheListener implements CacheListener<Pair<String,String>,Boolean> {

	public int numberOfCacheMisses;

	public int numberOfCacheHits;

	public void cacheMiss(final Pair<String,String> userId, final Entry<Pair<String,String>,Boolean> val) {
		numberOfCacheMisses++;
	}

	public void cacheHit(final Pair<String,String> userId, final Entry<Pair<String,String>,Boolean> val) {
		numberOfCacheHits++;
	}
}

class TestGroupByNameCacheListener implements CacheListener<String,Group> {

	public int numberOfCacheMisses;

	public int numberOfCacheHits;

	public void cacheMiss(final String groupName, final Entry<String,Group> val) {
		numberOfCacheMisses++;
	}

	public void cacheHit(final String groupName, final Entry<String,Group> val) {
		numberOfCacheHits++;
	}
}