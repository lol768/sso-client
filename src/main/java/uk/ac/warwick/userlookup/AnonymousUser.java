package uk.ac.warwick.userlookup;

/**
 * An anonymousUser object represents a user who hasn't logged in to the Single-Sign on service yet.
 * <p>
 * It returns false for getFoundUser(), isLoggedIn(), isStaff() and isStudent(), and empty strings for 
 * all other fields. It does however, return true for {@link #isVerified()}, because we are sure that
 * this user was not found (rather than not being sure due to some problem).
 * 
 * <p>
 * getUserType returns <code>User.UNKNOWN_USER_TYPE</code>
 *
 */
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
