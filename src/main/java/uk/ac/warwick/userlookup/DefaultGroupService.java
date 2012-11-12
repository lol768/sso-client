package uk.ac.warwick.userlookup;

import java.util.Collections;
import java.util.List;

import uk.ac.warwick.userlookup.webgroups.GroupInfo;
import uk.ac.warwick.userlookup.webgroups.GroupNotFoundException;

/**
 * The default group service returns empty results for every method.
 */
public class DefaultGroupService implements GroupService {

	public final List<Group> getGroupsForUser(final String userId) {
		return null;
	}

	public final List<String> getGroupsNamesForUser(final String userId) {
		return Collections.emptyList();
	}

	public final boolean isUserInGroup(final String userId, final String group) {
		return false;
	}


	public final Group getGroupByName(final String name) {
		return null;
	}

	public final void setTimeoutConfig(final WebServiceTimeoutConfig config) {
	}


	public GroupInfo getGroupInfo(String name) throws GroupNotFoundException {
		return null;
	}

	public List<Group> getGroupsForDeptCode(String deptCode) {
		return Collections.emptyList();
	}

	public List<Group> getGroupsForQuery(String search) {
		return Collections.emptyList();
	}

	public List<Group> getRelatedGroups(String group) {
		return Collections.emptyList();
	}

	public List<String> getUserCodesInGroup(String group) {
		return Collections.emptyList();
	}
}
