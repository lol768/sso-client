package uk.ac.warwick.userlookup;

import java.util.List;
import java.util.Map;

public interface UserFilter {

	/**
	 * Find all users with names matching a given set of criteria. filterValues can contain any valid set of filter
	 * criteria for an LDAP search (name=..., ou=..., etc)
	 * 
	 * @param filterValues Map[String,String] or Map[String,Iterable[String]]
	 * @return List[User]
	 */
	List<User> findUsersWithFilter(final Map<String,Object> filterValues, boolean includeDisabledUsers)
			throws UserLookupException;

}