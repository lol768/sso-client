package uk.ac.warwick.userlookup;

/**
 * This type of AnonymousUser is only created if there was a problem fetching someone's
 * data. The user may have valid cookies but a timeout to Websignon or something.
 * On the other hand, the cookies may be invalid and we just don't know that yet,
 * so basically you can't be sure either way.
 */
public class UnverifiedUser extends AnonymousUser {
	private static final long serialVersionUID = 1L;
	
	private Exception exception;
	
	public UnverifiedUser(Exception theCausingException) {
		super();
		setVerified(false);
		this.exception = theCausingException;
	}
	
	/**
	 * Returns the Exception that caused the failure to find the user's details.
	 * I don't know if an exception will always be the cause so best to check that
	 * this is not null before playing with it.
	 */
	public Exception getVerificationException() {
		return exception;
	}
}
