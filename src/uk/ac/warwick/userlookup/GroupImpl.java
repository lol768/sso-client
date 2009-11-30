/*
 * Created on 11-Aug-2004
 *
 */
package uk.ac.warwick.userlookup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * @author Kieran Shaw
 * 
 */
public class GroupImpl implements Group {
	private static final long serialVersionUID = 6252054038385472439L;

	private String _name;

	private String _title;

	private List<String> _userCodes = new ArrayList<String>(); 

	private List<String> _owners = new ArrayList<String>(); 

	private List<String> _relatedGroups = new ArrayList<String>(); 

	private String _type;

	private String _department;
	
	private String _departmentCode;
	
	private Date _lastUpdatedDate = new Date();

	/**
	 * Will return a list of usercodes as String's of users in this group
	 * 
	 * @return
	 */
	public final List<String> getUserCodes() {
		if (_userCodes == null) {
			return Collections.emptyList();
		}
		return _userCodes;
	}

	public final void setUserCodes(final List<String> userCodes) {
		_userCodes = userCodes;
	}

	public final String getName() {
		return _name;
	}

	public final void setName(final String name) {
		_name = name;
	}

	public final List<String> getOwners() {
		if (_owners == null) {
			return Collections.emptyList();
		}
		return _owners;
	}

	public final void setOwners(final List<String> owners) {
		_owners = owners;
	}

	public final String getTitle() {
		return _title;
	}

	public final void setTitle(final String title) {
		_title = title;
	}

	public final String getType() {
		return _type;
	}

	public final void setType(final String type) {
		_type = type;
	}

	public final List<String> getRelatedGroups() {
		return _relatedGroups;
	}

	public final void setRelatedGroups(final List<String> relatedGroups) {
		_relatedGroups = relatedGroups;
	}

	public final String getDepartment() {
		return _department;
	}

	public final void setDepartment(final String department) {
		this._department = department;
	}

	public final boolean isUserInGroup(final String userCode) {
		if (userCode == null || userCode.equals("")) {
			return false;
		}
		return (getOwners().contains(userCode) ||
				getUserCodes().contains(userCode));
	}

	
	public Date getLastUpdatedDate() {
		return _lastUpdatedDate;
	}

	
	public void setLastUpdatedDate(final Date lastUpdatedDate) {
		_lastUpdatedDate = lastUpdatedDate;
	}

	
	public String getDepartmentCode() {
		return _departmentCode;
	}

	
	public void setDepartmentCode(final String departmentCode) {
		_departmentCode = departmentCode;
	}

}