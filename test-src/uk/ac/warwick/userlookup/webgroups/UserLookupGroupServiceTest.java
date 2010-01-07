package uk.ac.warwick.userlookup.webgroups;

import org.jmock.Expectations;
import org.jmock.Mockery;

import uk.ac.warwick.userlookup.Group;
import uk.ac.warwick.userlookup.GroupService;
import uk.ac.warwick.userlookup.UserLookup;
import junit.framework.TestCase;

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
			Group groupByName = userLookup.getGroupService().getGroupByName(nonExistentGroupName);
			fail("Should have thrown exception");
		} catch (GroupNotFoundException e) {
			
		}
	}
}
