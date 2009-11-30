package uk.ac.warwick.userlookup.webgroups;

/**
 * Exception to indicate that there was a problem looking up a user
 */
public class GroupNotFoundException extends Exception {

	/**
	 * Constructor for UserLookupException.
	 */
	public GroupNotFoundException() {
		super();
	}

	/**
	 * Constructor for UserLookupException.
	 * @param arg0
	 */
	public GroupNotFoundException(final String arg0) {
		super(arg0);
	}

	/**
	 * Constructor for UserLookupException.
	 * @param arg0
	 * @param arg1
	 */
	public GroupNotFoundException(final String arg0, final Throwable arg1) {
		super(arg0, arg1);
	}

	/**
	 * Constructor for UserLookupException.
	 * @param arg0
	 */
	public GroupNotFoundException(final Throwable arg0) {
		super(arg0);
	}

}
