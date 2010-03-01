/*
 * Created on 13-Mar-2006
 *
 */
package uk.ac.warwick.userlookup.webgroups;

import java.util.List;

import uk.ac.warwick.userlookup.Group;
import uk.ac.warwick.userlookup.WebServiceTimeoutConfig;

import junit.framework.TestCase;

public class WebGroupsLiveTest extends TestCase {
	
	

	private static final String HTTP_WEBGROUPS_LOCATION = "http://webgroups.warwick.ac.uk";


	public final void testGetGroups() throws Exception {

		WarwickGroupsService service = new WarwickGroupsService(HTTP_WEBGROUPS_LOCATION);
		service.setTimeoutConfig(new WebServiceTimeoutConfig(5000, 5000));
		List groups = service.getGroupsForUser("cusaab");
		assertTrue(groups.size() > 5);

	}

	public final void testGetGroup() throws Exception {

		WarwickGroupsService service = new WarwickGroupsService(HTTP_WEBGROUPS_LOCATION);
		service.setTimeoutConfig(new WebServiceTimeoutConfig(5000, 5000));
		Group group = service.getGroupByName("in-webgroups-sysadmin");
		assertNotNull(group);

	}


	public final void testGetDeptGroups() throws Exception {

		WarwickGroupsService service = new WarwickGroupsService(HTTP_WEBGROUPS_LOCATION);
		service.setTimeoutConfig(new WebServiceTimeoutConfig(5000, 5000));
		List groups = service.getGroupsForDeptCode("IN");
		assertTrue(groups.size() > 10);

	}

	public final void testGroupSearch() throws Exception {

		WarwickGroupsService service = new WarwickGroupsService(HTTP_WEBGROUPS_LOCATION);
		service.setTimeoutConfig(new WebServiceTimeoutConfig(5000, 5000));
		List groups = service.getGroupsForQuery("test");
		assertTrue(groups.size() > 10);

	}
	

	public final void testGetNonExistantGroup() throws Exception {

		WarwickGroupsService service = new WarwickGroupsService(HTTP_WEBGROUPS_LOCATION);
		service.setTimeoutConfig(new WebServiceTimeoutConfig(5000, 5000));
		try {
			service.getGroupByName("sfsafdlaskjfaslk");
			fail("Should not have found group");
		} catch (GroupNotFoundException e) {
			// fine, as expected
		}		

	}

}
