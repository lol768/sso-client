package uk.ac.warwick.userlookup.webgroups;

import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;

import org.jmock.Expectations;
import org.jmock.Mockery;

import uk.ac.warwick.userlookup.GroupService;
import uk.ac.warwick.userlookup.UserLookup;
import uk.ac.warwick.userlookup.cache.Cache;

public class UserLookupGroupServiceTest extends TestCase {
	
	private Mockery m = new Mockery();
	
	public void testGetGroup() throws Exception {
		final String nonExistentGroupName = "fijifejulu";
		
		final GroupService backend = m.mock(GroupService.class);
		m.checking(new Expectations(){{
			one(backend).getGroupByName(nonExistentGroupName); will(throwException(new GroupNotFoundException("This group is not found")));
		}});
		
		UserLookup userLookup = new UserLookup();
		userLookup.setGroupServiceBackend(backend);
		try {
			userLookup.getGroupService().getGroupByName(nonExistentGroupName);
			fail("Should have thrown exception");
		} catch (GroupNotFoundException e) {
			
		}
	}
	
	public void testCachesCorrectReturned() {
		UserLookup userLookup = new UserLookup();
		userLookup.setGroupServiceLocation("http://webgroups.warwick.ac.uk");
		Map<String, Set<Cache<?, ?>>> caches = userLookup.getGroupService().getCaches();
		
		assertTrue("Caches created and returned", caches.size() > 0);
		assertTrue("Has GROUP_CACHE_NAME", caches.containsKey(UserLookup.GROUP_CACHE_NAME));
		assertTrue("Has IN_GROUP_CACHE_NAME", caches.containsKey(UserLookup.IN_GROUP_CACHE_NAME));
		assertTrue("Has GROUP_MEMBER_CACHE_NAME", caches.containsKey(UserLookup.GROUP_MEMBER_CACHE_NAME));
		assertTrue("Has USER_GROUPS_CACHE_NAME", caches.containsKey(UserLookup.USER_GROUPS_CACHE_NAME));
	}
}
