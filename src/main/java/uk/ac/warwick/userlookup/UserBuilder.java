package uk.ac.warwick.userlookup;

import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

public final class UserBuilder {
	public static final org.apache.log4j.Logger LOGGER = Logger.getLogger(UserBuilder.class);
	
	public User populateUser(final Map<String,String> results) {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Creating user from SSO results");
			for (Entry<String, String> entry : results.entrySet()) {
				LOGGER.debug(entry.getKey() + "=" + entry.getValue());
			}
		}
		
		User user = new User();
		user.setEmail(getResult(results, "email"));
		user.setFullName(getResult(results, "name"));
		user.setFirstName(getResult(results, "firstname"));
		user.setLastName(getResult(results, "lastname"));
		user.setUserId(getResult(results, "user"));
		user.setOldWarwickSSOToken(getResult(results, "token"));
		user.setDepartment(getResult(results, "dept"));
		user.setDepartmentCode(getResult(results, "deptcode"));
		user.setShortDepartment(getResult(results, "deptshort"));
		// new user type attribute from SSO #SSO-358 #USL-41
		user.setUserType(getResult(results, "urn:websignon:usertype"));
		user.setFoundUser(true);
		user.setIsLoggedIn(true);
		user.setWarwickId(getResult(results, "id"));

		if ("true".equals(getResult(results, "staff"))) {
			user.setStaff(true);
		}
		if ("true".equals(getResult(results, "student"))) {
			user.setStudent(true);
		}
		if ("true".equals(getResult(results,"alumni")) || "Alumni".equals(user.getUserType())) {
		    user.setAlumni(true);
		}

		if (getResult(results, "logindisabled") != null && Boolean.valueOf(getResult(results, "logindisabled")).booleanValue()) {
			user.setLoginDisabled(true);
		}

		// put all of the results in the extraproperties map
		user.getExtraProperties().putAll(results);

		return user;
	}
	
	/**
	 * After doing the SSO check, find out what was the value of a particular key in SSO's response
	 */
	private String getResult(final Map<String,String> resultSet, final String key) {
		if (resultSet == null) {
			return null;
		}
		return resultSet.get(key);
	}
}
