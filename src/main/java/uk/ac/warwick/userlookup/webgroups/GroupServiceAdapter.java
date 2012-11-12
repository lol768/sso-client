package uk.ac.warwick.userlookup.webgroups;

import java.util.List;

import uk.ac.warwick.userlookup.Group;
import uk.ac.warwick.userlookup.GroupService;
import uk.ac.warwick.userlookup.WebServiceTimeoutConfig;
import uk.ac.warwick.userlookup.cache.EntryUpdateException;

public abstract class GroupServiceAdapter implements GroupService {
	private final GroupService _decorated;

	public GroupServiceAdapter(GroupService decorated) {
		_decorated = decorated;
	}

	public Group getGroupByName(final String name)
			throws GroupNotFoundException, GroupServiceException {
		return _decorated.getGroupByName(name);
	}

	public List<Group> getGroupsForUser(final String userId) throws GroupServiceException {
		return _decorated.getGroupsForUser(userId);
	}

	public List<String> getGroupsNamesForUser(final String userId) throws GroupServiceException {
		return _decorated.getGroupsNamesForUser(userId);
	}

	public List<String> getUserCodesInGroup(final String group) throws GroupServiceException {
		return _decorated.getUserCodesInGroup(group);
	}

	public List<Group> getRelatedGroups(final String group) throws GroupServiceException {
		return _decorated.getRelatedGroups(group);
	}

	public void setTimeoutConfig(final WebServiceTimeoutConfig config) {
		_decorated.setTimeoutConfig(config);
	}

	public boolean isUserInGroup(final String userId, final String group) throws GroupServiceException {
		return _decorated.isUserInGroup(userId, group);
	}

	protected GroupService getDecorated() {
		return _decorated;
	}

	public List<Group> getGroupsForDeptCode(final String deptCode) throws GroupServiceException {
		return _decorated.getGroupsForDeptCode(deptCode);
	}

	public List<Group> getGroupsForQuery(final String search) throws GroupServiceException {
		return _decorated.getGroupsForQuery(search);
	}

	public GroupInfo getGroupInfo(String name) throws GroupNotFoundException, GroupServiceException {
		return _decorated.getGroupInfo(name);
	}
	
	protected void handleMissingOrServiceException(EntryUpdateException e) throws GroupServiceException, GroupNotFoundException {
		if (e.getCause() instanceof GroupNotFoundException) {
			throw (GroupNotFoundException)e.getCause();
		}
		handleServiceException(e);
	}
	
	protected void handleServiceException(EntryUpdateException e) throws GroupServiceException {
		if (e.getCause() instanceof GroupServiceException) {
			throw (GroupServiceException)e.getCause();
		}
		throw e.getRuntimeException();
	}

}
