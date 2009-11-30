package uk.ac.warwick.userlookup.webgroups;

import java.util.List;

import uk.ac.warwick.userlookup.Group;
import uk.ac.warwick.userlookup.GroupService;
import uk.ac.warwick.userlookup.WebServiceTimeoutConfig;

public abstract class GroupServiceAdapter implements GroupService {
	private final GroupService _decorated;

	public GroupServiceAdapter(GroupService decorated) {
		_decorated = decorated;
	}

	public Group getGroupByName(final String name)
			throws GroupNotFoundException {
		return _decorated.getGroupByName(name);
	}

	public List<Group> getGroupsForUser(final String userId) {
		return _decorated.getGroupsForUser(userId);
	}

	public List<String> getGroupsNamesForUser(final String userId) {
		return _decorated.getGroupsNamesForUser(userId);
	}

	public List<String> getUserCodesInGroup(final String group) {
		return _decorated.getUserCodesInGroup(group);
	}

	public List<Group> getRelatedGroups(final String group) {
		return _decorated.getRelatedGroups(group);
	}

	public void setTimeoutConfig(final WebServiceTimeoutConfig config) {
		_decorated.setTimeoutConfig(config);
	}

	public boolean isUserInGroup(final String userId, final String group) {
		return _decorated.isUserInGroup(userId, group);
	}

	protected GroupService getDecorated() {
		return _decorated;
	}

	public List<Group> getGroupsForDeptCode(final String deptCode) {
		return _decorated.getGroupsForDeptCode(deptCode);
	}

	public List<Group> getGroupsForQuery(final String search) {
		return _decorated.getGroupsForQuery(search);
	}

	public GroupInfo getGroupInfo(String name) throws GroupNotFoundException {
		return _decorated.getGroupInfo(name);
	}

}
