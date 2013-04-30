/*
 * Created on 31-Mar-2005
 *
 */
package uk.ac.warwick.userlookup;

import java.util.Collections;
import java.util.List;

import uk.ac.warwick.userlookup.webgroups.GroupInfo;
import uk.ac.warwick.userlookup.webgroups.GroupNotFoundException;
import uk.ac.warwick.userlookup.webgroups.GroupServiceException;

/**
 * An interface to the Webgroups query service. Allows you to
 * look up particular Webgroups, see what Webgroups a user is in,
 * and other things.
 * <p>
 * Usually you will access this class via via {@link UserLookup#getGroupService()}.
 */
public interface GroupService extends WebService, CacheingService {

	public static final List<Group> PROBLEM_FINDING_GROUPS = Collections.unmodifiableList(Collections.<Group>emptyList());
	public static final Group PROBLEM_FINDING_GROUP = new GroupImpl();
	
	/**
	 * Fetches a list of Group objects that the given user ID is in.
	 * If the user does not exist, an empty list is returned.
	 * @param userId
	 * @throws GroupServiceException
	 * @see {@link #getGroupsNamesForUser(String)}
	 */
	List<Group> getGroupsForUser(String userId) throws GroupServiceException;

	/**
	 * Fetches a list of group names that the given user ID is in.
	 * If the user does not exist, an empty list is returned.
	 * @param userId
	 * @throws GroupServiceException
	 * @see {@link #getGroupsForUser(String)}
	 */
	List<String> getGroupsNamesForUser(String userId) throws GroupServiceException;

	/**
	 * Returns whether userId is found in group. If group does not exist, false is returned.
	 * 
	 * @param userId The user ID
	 * @param group The full group name
	 */
	boolean isUserInGroup(String userId, String group) throws GroupServiceException;

	/**
	 * Return a list of user IDs who are members of the given group. If the group
	 * is not found, an empty list is returned. 
	 * @param group
	 * @throws GroupServiceException
	 */
	List<String> getUserCodesInGroup(String group) throws GroupServiceException;

	/**
	 * Return a list groups that are sub-groups of this group. If the given group
	 * is not found, an empty list is returned.
	 * @param group
	 * @throws GroupServiceException
	 */
	List<Group> getRelatedGroups(String group) throws GroupServiceException;

	/**
	 * Will return a Group object for the given group name.
	 * 
	 * @param name The full name of the Webgroup
	 * @throws GroupNotFoundException if the requested group did not exist
	 * @throws GroupServiceException
	 */
	Group getGroupByName(String name) throws GroupNotFoundException, GroupServiceException;

	/**
	 * Returns a list of Group objects that start with the given deptCode, such
	 * as "in" or "ch".
	 * @param deptCode
	 * @throws GroupServiceException
	 */
	List<Group> getGroupsForDeptCode(String deptCode) throws GroupServiceException;

	/**
	 * Returns groups where the search matches part of the title or description. 
	 * @param search The search term.
	 * @throws GroupServiceException
	 */
	List<Group> getGroupsForQuery(String search) throws GroupServiceException;
	
	/**
	 * Fetches some extended information about a particular group, such as
	 * the number of members. 
	 * 
	 * @param name The group name
	 * @throws GroupNotFoundException If the group does not exist
	 * @throws GroupServiceException
	 */
	GroupInfo getGroupInfo(String name) throws GroupNotFoundException, GroupServiceException;

}
