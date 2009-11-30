package uk.ac.warwick.userlookup;

/**
 * Superinterface to UserLookupInterface, for compatibility
 * reasons. You should now just use the UserLookupInterface interface.
 */
public interface UserResolver {
	/**
	 * Will return a single populated user or an anonymous user if the user can not be found.
	 * 
	 * Even if LDAP lookup fails, it will return an anonymous user and put an error in the logs explaining what went
	 * wrong.
	 * 
	 * @param userId
	 * @return
	 */
	User getUserByUserId(final String uncheckedUserId);
}
