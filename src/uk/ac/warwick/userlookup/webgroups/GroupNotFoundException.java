package uk.ac.warwick.userlookup.webgroups;

/**
 * Exception to indicate that the request group was not found.
 */
public class GroupNotFoundException extends Exception {

	public GroupNotFoundException(final String groupName) {
		super("Group not found: " + groupName);
	}

}
