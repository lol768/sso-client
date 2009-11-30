/*
 * Created on 11-Aug-2004
 *
 */
package uk.ac.warwick.userlookup;

import java.io.Serializable;
import java.util.Date;
import java.util.List;


/**
 * @author Kieran Shaw
 *
 */
@Api
public interface Group extends Serializable {
	
	/**
	 * Name of this group, for instance "webdevwest"
	 * @return
	 */
	String getName();
	
	/**
	 * Title of this group, longer than the name, for instance "WebDev westwood"
	 * @return
	 */
	String getTitle();

	
	/**
	 * A list of Strings containing the usercodes of those who are members of this group
	 * @return
	 */
	List<String> getUserCodes();
	
	/**
	 * A list of Strings containing the usercodes of those who are owners of this group
	 * @return
	 */
	List<String> getOwners();
	
	/**
	 * Get the type of group, eg. Module, Department, Tutor Group, etc..
	 * @return
	 */
	String getType();

	/**
	 * @return the Department this group belongs to.  Initially set to the 
	 * department of the user who created this group.
	 */
	String getDepartment();
	
	/**
	 * @return The department code this group belongs to. For example in, es, ib meaning (IT Services, Engineering, Business School)
	 */
	String getDepartmentCode();
	
	Date getLastUpdatedDate();

}
