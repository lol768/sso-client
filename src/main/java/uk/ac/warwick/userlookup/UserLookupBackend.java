/*
 * Created on 02-Dec-2003
 *  
 */
package uk.ac.warwick.userlookup;

import java.util.List;
import java.util.Map;

/**
 * @author Kieran Shaw
 * 
 */
interface UserLookupBackend {

	User getUserById(String userId) throws UserLookupException;

	User getUserByToken(String token) throws UserLookupException;

	/**
	 * Will return an anonymous user if there is no such user.
	 * 
	 * @param usercode
	 * @param password
	 * @return
	 * @throws UserLookupException
	 */
	User signIn(String usercode, String password) throws UserLookupException;

	User getUserByUserIdAndPassNonLoggingIn(String usercode, String password) throws UserLookupException;

	void signOut(String token) throws UserLookupException;
	
	Map<String, User> getUsersById(List<String> userIds) throws UserLookupException;
	
	boolean getSupportsBatchLookup();
}