/*
 * Created on 31-Mar-2005
 *
 */
package uk.ac.warwick.userlookup;

import java.util.List;

import uk.ac.warwick.userlookup.webgroups.GroupInfo;
import uk.ac.warwick.userlookup.webgroups.GroupNotFoundException;

public interface GroupService extends WebService {

	List<Group> getGroupsForUser(String userId);

	List<String> getGroupsNamesForUser(String userId);

	boolean isUserInGroup(String userId, String group);

	List<String> getUserCodesInGroup(String group);

	List<Group> getRelatedGroups(String group);

	/**
	 * Will return an Group object
	 * 
	 * @param name
	 * @return
	 */
	Group getGroupByName(String name) throws GroupNotFoundException;

	List<Group> getGroupsForDeptCode(String deptCode);

	List<Group> getGroupsForQuery(String search);
	
	GroupInfo getGroupInfo(String name) throws GroupNotFoundException;

}
