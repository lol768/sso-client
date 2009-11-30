package uk.ac.warwick.userlookup.cache;

/**
 * Exception that is thrown when an exception occurs while fetching a
 * new value for a cache entry.
 * 
 * The cause is the original exception thrown.
 */
public class EntryUpdateException extends Exception {
	private static final long serialVersionUID = -3544723761000900132L;
	
	public EntryUpdateException(Throwable cause) {
		super(cause);
	}
	
	/**
	 * If the caller doesn't expect any exceptions to get thrown,
	 * it can use this to throw it as a RuntimeException, which will
	 * be the cause if that is a RuntimeException, otherwise it will
	 * wrap the cause in one.
	 */
	public RuntimeException getRuntimeException() {
		if (getCause() instanceof RuntimeException) {
			return (RuntimeException)getCause();
		} else {
			return new RuntimeException(getCause());
		}
	}
}
