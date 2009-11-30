package uk.ac.warwick.userlookup;

import uk.ac.warwick.userlookup.webgroups.GroupServiceAdapter;


/**
 * Implementation which will avoid expensive isUserInGroup if the Group name is determined to *not* be
 * a valid group.
 *
 * @author xusqac
 */
public final class GroupNameCheckerGroupService extends GroupServiceAdapter implements GroupService {
	public GroupNameCheckerGroupService(final GroupService decorated) {
		super(decorated);
	}

	public boolean isUserInGroup(final String userId, final String group) {
		return group != null && group.indexOf("-") >= 0 && getDecorated().isUserInGroup(userId, group);
	}
}
