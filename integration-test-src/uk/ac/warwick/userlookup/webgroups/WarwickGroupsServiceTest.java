package uk.ac.warwick.userlookup.webgroups;

import java.util.Collection;

import uk.ac.warwick.userlookup.ResultAwareWebServiceResponseHandler;

import junit.framework.TestCase;

/**
 * Test the implementation of the WarwickGroupsService.
 * 
 * For the most part, only the URL that has been generated is tested. Most of the logic will be in the parsing of the
 * XML which is tested elsewhere.
 */
public final class WarwickGroupsServiceTest extends TestCase {

	public void testGetGroupsForUser() throws Exception {
		WarwickGroupsService service = new WarwickGroupsService("xxx");
		final String userName = "nonExistantUser";
		service.setEngine(new WarwickGroupsService.ExecuteAndParseEngine() {

			public void execute(String urlPath, ResultAwareWebServiceResponseHandler handler) {
				assertEquals("url", "xxx/query/user/" + userName + "/groups", urlPath);
			}
		});
		service.getGroupsForUser(userName);
	}

	public void testIsUserInGroup() throws Exception {
		WarwickGroupsService service = new WarwickGroupsService("xxx");
		final String userName = "user";
		final String group = "group";
		service.setEngine(new WarwickGroupsService.ExecuteAndParseEngine() {

			public void execute(String url, ResultAwareWebServiceResponseHandler handler) {
				assertEquals("url", "xxx/query/user/" + userName + "/member/" + group, url);
			}
		});
		service.isUserInGroup(userName, group);
	}

	public void testGetNonExistantGroup() throws Exception {
		WarwickGroupsService service = new WarwickGroupsService("xxx");
		final String groupName = "nonExistantGroup";
		service.setEngine(new WarwickGroupsService.ExecuteAndParseEngine() {

			public void execute(String url, ResultAwareWebServiceResponseHandler handler) {
				assertEquals("url", "xxx/query/group/" + groupName + "/details", url);
			}
		});
		try {
			service.getGroupByName(groupName);
			fail("Should not have found group");
		} catch (GroupNotFoundException e) {
			// failed as expected
		}
	}

	public void testRelatedGroups() throws Exception {
		WarwickGroupsService service = new WarwickGroupsService("xxx");
		final String groupName = "groupName";
		service.setEngine(new WarwickGroupsService.ExecuteAndParseEngine() {

			public void execute(String url, ResultAwareWebServiceResponseHandler handler) {
				assertEquals("url", "xxx/query/group/" + groupName + "/groups", url);
			}
		});

		service.getRelatedGroups(groupName);

	}

	public void testGetGroupNamesForUser() throws Exception {
		WarwickGroupsService service = new WarwickGroupsService("xxx");
		final String userId = "userId";
		final String groupName = "myGroup";
		service.setEngine(new WarwickGroupsService.ExecuteAndParseEngine() {

			public void execute(String url, ResultAwareWebServiceResponseHandler handler) {
				assertEquals("url", "xxx/query/user/" + userId + "/groups", url);
			}
		});
		Collection names = service.getGroupsNamesForUser(userId);
	}

	public void testGroupServiceDown() throws Exception {
		WarwickGroupsService service = new WarwickGroupsService(null);
		final String groupName = "nonExistantGroup";
		try {
			service.getGroupByName(groupName);
			fail("Should have failed with GroupNotFoundException");
		} catch (GroupNotFoundException e) {
			// fine
		}

	}
}