package uk.ac.warwick.userlookup;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a user obtained from Single Sign On. A user can either be logged in or anonymous. Not all attributes will
 * always be populated. Although you can get at most of the users attributes via the accessor methods, not all
 * attributes from SSO have their own accessor. To get at all of the attributes, use the getExtraProperties() methods.
 * <p>
 * Note: User is not a persistent object so any changes made with setter methods are not saved anywhere.
 */
public class User implements Serializable, ExtraProperties {

	private static final long serialVersionUID = 8170132489896721008L;

	private String email;
	
	private String userType;

	private String userId;

	private String token;

	private boolean loginDisabled;

	private boolean foundUser;

	private boolean staff;

	private boolean student;
	
	private boolean alumni;

	private boolean applicant;

	private String department;

	private String departmentCode;
	
	private String shortDepartment;

	private String firstName = "";

	private String lastName = "";

	private String fullName = "";

	private String warwickId = "";

	private String userSource;

	private boolean verified = true;

	private boolean oauthUser;

    private boolean trustedApplicationsUser;

	private boolean onCampus;

	private boolean warwickPrimary;

	private Map<String, String> extraProperties = new HashMap<>();

	/**
	 * The user was not obtained from LDAP or SSO
	 */
	public static final String UNKNOWN_USER_TYPE = "";

	private boolean isLoggedIn;

	/**
	 * Make an empty User instance
	 */
	public User() {
		super();
	}
	
	public User(String userId) {
		super();
		setUserId(userId);
	}
	
	/**
	 * Verified means we were able to find out definitively the state of the user.
	 * If it's true, then we either got a User or the user is definitely not logged in.
	 * 
	 * However, if it is false then this is an UnverifiedUser, who counts as anonymous
	 * only because there was a problem fetching the user's session. If it is false you
	 * can check for UnverifiedUser.getVerificationException() to investigate.
	 */
	public boolean isVerified() {
		return verified;
	}

	public void setVerified(boolean verified) {
		this.verified = verified;
	}

	/**
	 * Returns the email address.
	 * 
	 * @return String
	 */
	public final String getEmail() {

		return email;
	}

	/**
	 * Returns the Full Name.
	 * 
	 * @return String
	 */
	public final String getFullName() {

		if (fullName == null || fullName.equals("")) {
			String fullName = firstName + " " + lastName;
			if (fullName.equals(" ")) {
				fullName = "";
			}

			return fullName;

		}
		return fullName;

	}
	
	

	/**
	 * Tells whether this user object was obtained from a logged-in SSO cookie, or whether from an LDAP lookup. N.B.
	 * This does not answer the question "is the person with this login code logged in anywhere"
	 * 
	 * @return boolean
	 */
	public final boolean isLoggedIn() {
		return isLoggedIn;
	}

	/**
	 * Returns the userId. (e.g. "cusaab")
	 * 
	 * @return String
	 */
	public final String getUserId() {
		return userId;
	}

	/**
	 * Sets the email address: N.B. changes the object only, does not persist changes back to LDAP or SSO.
	 * 
	 * @param email
	 *            The email to set
	 */
	public final void setEmail(final String email) {
		this.email = email;
	}

	/**
	 * Sets the isLoggedIn. N.B. changes the object only, does not persist changes back to LDAP or SSO
	 * 
	 * @param isLoggedIn
	 *            The isLoggedIn to set
	 */
	public final void setIsLoggedIn(final boolean isLoggedIn) {
		this.isLoggedIn = isLoggedIn;
	}

	/**
	 * Sets the userId. N.B. changes the object only, does not persist changes back to LDAP or SSO
	 * 
	 * @param userId
	 *            The userId to set
	 */
	public final void setUserId(final String userId) {
		this.userId = userId;
	}

	/**
	 * Now deprecated and should generally not be used. If you must have access to the old WarwickSSO token, use
	 * setOldWarwickSSOToken
	 * 
	 * @return String
	 * @deprecated
	 */
	public final String getToken() {
		return token;
	}

	/**
	 * This will only get populated when a use has a WarwickSSO token which is not always guaranteed. Should only be
	 * used for sending credentials to old applications that are not going to support SSOv3
	 * 
	 * @return
	 */
	public final String getOldWarwickSSOToken() {
		return token;
	}

	/**
	 * Now deprecated and should generally not be used. If you must have access to the old WarwickSSO token, use
	 * setOldWarwickSSOToken
	 * 
	 * @param token
	 *            The token to set
	 * @deprecated
	 */
	public final void setToken(final String token) {
		this.token = token;
	}

	/**
	 * This should only be populated with a WarwickSSO token value which might then be used for passing old WarwickSSO
	 * credentials onto another application that doesn't support SSOv3
	 * 
	 * @param token
	 */
	public final void setOldWarwickSSOToken(final String token) {
		this.token = token;
	}

	/**
	 * Tells whether this is a valid user or not.
	 * 
	 * @return boolean
	 */
	public final boolean isFoundUser() {
		return foundUser;
	}

	/**
	 * Sets the foundUser.N.B. changes the object only, does not persist changes back to LDAP or SSO
	 * 
	 * @param foundUser
	 *            The foundUser to set
	 */
	public final void setFoundUser(final boolean foundUser) {
		this.foundUser = foundUser;
	}

	/**
	 * Returns the userType.
	 * 
	 * @return String
	 */
	public final String getUserType() {
		return userType;
	}

	/**
	 * Sets the userType. N.B. changes the object only, does not persist changes back to LDAP or SSO
	 * 
	 * @param userType
	 *            The userType to set
	 */
	public final void setUserType(final String userType) {
		this.userType = userType;
	}

	/**
	 * Tells whether the user is a member of staff or not (n.b. this is unreliable).
	 * 
	 * @return boolean
	 */
	public final boolean isStaff() {
		return staff;
	}

	/**
	 * Tells whether the user is a student or not (n.b. this is unreliable).
	 * 
	 * @return boolean
	 */
	public final boolean isStudent() {
		return student;
	}
	
	/**
	 * Tells whether the user is alumni or not (n.b. this is unreliable).
	 * 
	 * @return boolean
	 */
	public boolean isAlumni() {
        return alumni;
    }
    
	/**
     * Sets whether the user is alumni. N.B. changes the object only, does not persist changes back to
     * LDAP or SSO
     * 
     * @param alumni
     *            Whether the user is alumni
     */
    public void setAlumni(final boolean alumni) {
        this.alumni = alumni;
    }

	/**
	 * Sets whether the user is a member of staff or not. N.B. changes the object only, does not persist changes back to
	 * LDAP or SSO
	 * 
	 * @param staff
	 *            The staff to set
	 */
	public final void setStaff(final boolean staff) {
		this.staff = staff;
	}

	/**
	 * Sets whether the user is a student or not. N.B. changes the object only, does not persist changes back to LDAP or
	 * SSO
	 * 
	 * @param student
	 *            The student to set
	 */
	public final void setStudent(final boolean student) {
		this.student = student;
	}

	/**
	 * Get the department to which the user belongs
	 * 
	 * @return
	 */
	public final String getDepartment() {
		return department;
	}

	/**
	 * Sets the users department. N.B. changes the object only, does not persist changes back to LDAP or SSO
	 * 
	 * @param string
	 */
	public final void setDepartment(final String string) {
		department = string;
	}

	/**
	 * Get the first name
	 * 
	 * @return
	 */
	public final String getFirstName() {
		return firstName;
	}

	/**
	 * Get the last name
	 * 
	 * @return
	 */
	public final String getLastName() {
		return lastName;
	}

	/**
	 * 
	 * Sets the users first name. N.B. changes the object only, does not persist changes back to LDAP or SSO
	 * 
	 * @param string
	 */
	public final void setFirstName(final String string) {
		firstName = string;
	}

	/**
	 * Sets the users last name. N.B. changes the object only, does not persist changes back to LDAP or SSO
	 * 
	 * @param string
	 */
	public final void setLastName(final String string) {
		lastName = string;
	}

	/**
	 * Sets the users full name. N.B. changes the object only, does not persist changes back to LDAP or SSO
	 * 
	 * @param string
	 */
	public final void setFullName(final String string) {
		fullName = string;
	}


	/**
	 * @return
	 */
	public final String getWarwickId() {
		return warwickId;
	}

	/**
	 * @param string
	 */
	public final void setWarwickId(final String string) {
		warwickId = string;
	}

	/**
	 * Department code is a shortened/coded version of what department a user is in, eg. IN, CC, etc.. Not yet populated
	 * correctly, will only be populated on searches and not by getting users by id or token
	 * 
	 * @return
	 */
	public final String getDepartmentCode() {
		return departmentCode;
	}

	public final void setDepartmentCode(final String departmentCode) {
		this.departmentCode = departmentCode;
	}

	public final Map<String, String> getExtraProperties() {
		return extraProperties;
	}

	public final void setExtraProperties(final Map<String, String> extraProperties) {
		this.extraProperties = extraProperties;
	}

	public final Object getExtraProperty(final String key) {
		return getExtraProperties().get(key);
	}

	public final boolean isLoginDisabled() {
		return loginDisabled;
	}

	public final void setLoginDisabled(final boolean loginDisabled) {
		this.loginDisabled = loginDisabled;
	}

	public final String getUserSource() { return userSource; }

	public final void setUserSource(String userSource) { this.userSource = userSource; }


	public final int hashCode() {
		return (getUserId() + "").hashCode();
	}

	public final boolean equals(final Object o) {
		if (!(o instanceof User)) {
			return false;
		}
		return equals((User) o);
	}

	private boolean equals(final User o) {
		return (getUserId() + "").equals(o.getUserId() + "");
	}

	public String getShortDepartment() {
		if (shortDepartment != null && shortDepartment.length() > 0) {
			return shortDepartment;
		}
		return getDepartment();
	}
	
	public String getShortDepartmentHtml() {
		String dept = getShortDepartment();
		if (dept == null) {
			return null;
		}
		if (dept.matches("^[A-Z]+$")) {
			String fullDeptEscaped = escapeHtml(getDepartment());
			dept = "<abbr title=\""+fullDeptEscaped+"\">" + dept + "</abbr>";
		}
		return dept;
	}

	public void setShortDepartment(String department) {
		shortDepartment = department;
	}

	private String escapeHtml(String input) {
		return (input==null)?"":input
			.replaceAll("&", "&amp;")
			.replaceAll("\"", "&quot;")
			.replaceAll("<", "&lt;")
			.replaceAll(">", "&gt;");
	}

    /**
     * Will return true if the user was generated from an OAuth Access token
     * rather than a cookie or a username and password.
     */
	public boolean isOAuthUser() {
	    return oauthUser;
	}
	
	public void setOAuthUser(boolean isOAuthUser) {
	    this.oauthUser = isOAuthUser;
	}

    /**
     * Will return true if the user was generated from a Trusted Applications request
     * rather than a cookie or a username and password.
     */
    public boolean isTrustedApplicationsUser() {
        return trustedApplicationsUser;
    }

    public void setTrustedApplicationsUser(boolean isTrustedApplicationsUser) {
        this.trustedApplicationsUser = isTrustedApplicationsUser;
    }

	public boolean isOnCampus() {
		return onCampus;
	}

	public void setOnCampus(boolean onCampus) {
		this.onCampus = onCampus;
	}

	public boolean isWarwickPrimary() {
		return warwickPrimary;
	}

	public void setWarwickPrimary(boolean warwickPrimary) {
		this.warwickPrimary = warwickPrimary;
	}

	public void setApplicant(boolean applicant) {
		this.applicant = applicant;
	}

	public boolean isApplicant() {
		return applicant;
	}
}