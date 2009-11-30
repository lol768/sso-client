package uk.ac.warwick.userlookup;

import junit.framework.TestCase;
import uk.ac.warwick.userlookup.webgroups.GroupServiceAdapter;

/**
 * @author xusqac
 */
public final class GroupNameAwareGroupServiceTest extends TestCase {
	final String testUserId = "user";
	final String testGroup = "group";
	
	public void testValidGroup() {
		GroupService decorated = new GroupServiceAdapter(null) {
			public boolean isUserInGroup(String userId, String group) {
				assertEquals("user", testUserId, userId);
				assertEquals("group", testGroup, group);
				return true;
			}
		};
		
		GroupNameCheckerGroupService groupNameAware = new GroupNameCheckerGroupService(decorated);
		assertFalse(groupNameAware.isUserInGroup(testUserId, testGroup));	// force the assertions check.
	}
	
	public void testSillyGroup() {
		GroupService decorated = new GroupServiceAdapter(null) {
			public boolean isUserInGroup(String userId, String group) {
				assertEquals("user", testUserId, userId);
				assertEquals("group", testGroup, group);
				throw new IllegalStateException("should never have been called!");
			}
		};
		
		
		GroupNameCheckerGroupService groupNameAware = new GroupNameCheckerGroupService(decorated);
		assertFalse(groupNameAware.isUserInGroup(testUserId, testGroup));	// force the assertions check.
	}
}
