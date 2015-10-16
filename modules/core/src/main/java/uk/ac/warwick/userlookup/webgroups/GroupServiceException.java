package uk.ac.warwick.userlookup.webgroups;

/**
 * This exception is thrown when there has been an actual problem with
 * looking up Webgroups data - either the server is not responding, or
 * is responding with garbage, or the network is unavailable.
 */
public class GroupServiceException extends Exception {

	private static final long serialVersionUID = 1L;
	
	public GroupServiceException(final String msg) {
		super(msg);
	}

	public GroupServiceException(final String msg, final Throwable cause) {
		super(msg, cause);
	}

}
