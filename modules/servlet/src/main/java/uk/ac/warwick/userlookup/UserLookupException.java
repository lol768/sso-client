package uk.ac.warwick.userlookup;

/**
 * Exception to indicate that there was a problem looking up a user
 */
public class UserLookupException extends Exception {
	private static final long serialVersionUID = 1196632562612007291L;

	/**
	 * Constructor for UserLookupException.
	 */
	public UserLookupException() {
		super();
	}

	/**
	 * Constructor for UserLookupException.
	 * @param arg0
	 */
	public UserLookupException(final String arg0) {
		super(arg0);
	}

	/**
	 * Constructor for UserLookupException.
	 * @param arg0
	 * @param arg1
	 */
	public UserLookupException(final String arg0, final Throwable arg1) {
		super(arg0, arg1);
	}

	/**
	 * Constructor for UserLookupException.
	 * @param arg0
	 */
	public UserLookupException(final Throwable arg0) {
		super(arg0);
	}

}
