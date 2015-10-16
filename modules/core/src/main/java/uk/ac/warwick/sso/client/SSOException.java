/*
 * Created on 10-Jun-2005
 *
 */
package uk.ac.warwick.sso.client;

public class SSOException extends Exception {
	private static final long serialVersionUID = 6514257682248567187L;

	/**
	 * 
	 */
	public SSOException() {
		super();
	}

	/**
	 * @param message
	 * @param cause
	 */
	public SSOException(final String message, final Throwable cause) {
		super(message, cause);
	}

	/**
	 * @param message
	 */
	public SSOException(final String message) {
		super(message);
	}

	/**
	 * @param cause
	 */
	public SSOException(final Throwable cause) {
		super(cause);
	}

}
