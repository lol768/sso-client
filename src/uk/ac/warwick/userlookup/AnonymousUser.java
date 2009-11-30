package uk.ac.warwick.userlookup;

/**
 * An anonymousUser object represents a user who hasn't logged in to the Single-Sign on service yet.
 * 
 * It returns false for getFoundUser(), isLoggedIn(), isStaff() and isStudent(), and empty strings for 
 * all other fields.
 * 
 * getUserType returns <code>User.UNKNOWN_USER_TYPE</code>
 *
 */
@Api
public class AnonymousUser extends User {

	private static final long serialVersionUID = 1L;

	/**
	 * Constructor for AnonymousUser.
	 */
	public AnonymousUser() {
		super();
		setEmail("");
		setFoundUser(false);
		setLastName("");
		setFirstName("");
		setIsLoggedIn(false);
		setStaff(false);
		setStudent(false);
		setOldWarwickSSOToken("");
		setUserId("");
		setUserType(UNKNOWN_USER_TYPE);
		
	}

}
