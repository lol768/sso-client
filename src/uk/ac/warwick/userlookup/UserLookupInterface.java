package uk.ac.warwick.userlookup;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Interface to UserLookup, so that applications can 
 * proxy the interface.
 * 
 * If you are looking for UserLookup.getInstance(), see
 * {@link UserLookupFactory#getInstance()}
 * 
 * @see UserLookupFactory#getInstance()
 */
@Api
public interface UserLookupInterface extends UserResolver {

	/**
	 * Do a userlookup from LDAP that returns all users in a given department, eg. Information Technology Services
	 * 
	 * @param department
	 * @return
	 */
	List<User> getUsersInDepartment(final String department);

	/**
	 * Do a userlookup from LDAP that returns all users in a given department code, eg. IN, AR, BO
	 * 
	 * @param department
	 * @return
	 */
	List<User> getUsersInDepartmentCode(final String department);

	/**
	 * This will only work on tokens got from a WarwickSSO cookie
	 * 
	 * @param token
	 * @return
	 */
	User getUserByToken(final String token);

	
	
	/**
	 * Takes a List of userIds, and returns a Map that maps userIds to Users. Users found
	 * in the local cache will be taken from there (and not searched for), and all other
	 * users will be searched for and entered into the cache.
	 * 
	 * All userIds will be returned in the Map, but ones that weren't found will map to
	 * AnonymousUser objects.
	 * 
	 * @param userIdList List[String]
	 * @return Map[String,User]
	 */
	Map<String, User> getUsersByUserIds(final List<String> userIdList);

	/**
	 * Will return just a single user or an anonymous user that matches the warwickUniId passed in. It is possible that
	 * it will not be the right user depending on how many users are against this warwickUniId and if their
	 * login_disabled attributes are correctly populated.
	 * 
	 * Even if LDAP lookup fails, it will return an anonymous user and put an error in the logs explaining what went
	 * wrong.
	 * 
	 * @param warwickUniId
	 * @return
	 */
	User getUserByWarwickUniId(final String warwickUniId);
	User getUserByWarwickUniId(final String warwickUniId, boolean includeDisabledLogins);
	
	List<User> findUsersWithFilter(final Map<String, String> filterValues);

	/**
	 * Return a list of users with names matching the parameters passed in FilterValues.
	 * 
	 * @see LDAPUserLookup#findUsersWithFilter(HashMap)
	 */
	List<User> findUsersWithFilter(final Map<String, String> filterValues, boolean returnDisabledUsers);
	
	GroupService getGroupService();
	
	OnCampusService getOnCampusService();
	
	void clearCaches();
	
	User getUserByIdAndPassNonLoggingIn(final String uncheckedUserId, final String uncheckedPass) throws UserLookupException;
	
	
}
