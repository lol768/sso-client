/*
 * Created on 11-Aug-2004
 *
 */
package uk.ac.warwick.userlookup;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * A group looked up from Webgroups. It can contain individuals user codes,
 * or other groups, or a combination of both. A Group is generally obtained
 * from {@link GroupService}, which you can access via {@link UserLookup#getGroupService()}.
 */
public interface Group extends Serializable {
	
	/**
	 * @return Name of this group, for instance "webdevwest"
	 */
	String getName();
	
	/**
	 * @return Title of this group, longer than the name, for instance "WebDev westwood"
	 */
	String getTitle();
	
	/**
	 * @return A list of Strings containing the usercodes of those who are members of this group
	 */
	List<String> getUserCodes();
	
	/**
	 * @return A list of Strings containing the usercodes of those who are owners of this group
	 */
	List<String> getOwners();
	
	/**
	 * @return The type of group, eg. Module, Department, Tutor Group, etc..
	 */
	String getType();

	/**
	 * @return The Department this group belongs to.  Initially set to the 
	 * department of the user who created this group.
	 */
	String getDepartment();
	
	/**
	 * @return The department code this group belongs to. For example in, es, ib meaning (IT Services, Engineering, Business School)
	 */
	String getDepartmentCode();
	
	Date getLastUpdatedDate();
	
	/**
	 * Whether this Group is a result of a valid lookup. If this is false,
	 * it means there was some problem communicating with the server.
	 */
	boolean isVerified();

	/**
	 * Whether this Group is restricted. The members of restricted groups
	 * are only visible to their owners.
	 */
    boolean isRestricted();

}
