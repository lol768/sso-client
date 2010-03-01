package uk.ac.warwick.userlookup;

/**
 * A user who is not logged in, but has been detected as being on campus.
 * This includes accessing through the wwwcache proxy server, and should
 * also work with local Apache reverse proxies.
 * <p>
 * Making authorization decisions based on whether a user is on-campus is
 * an out-dated concept and there is no fully reliable detection mechanism either.
 * However, this may still be useful as a hint to display slightly
 * different information to the user.
 */
public class AnonymousOnCampusUser extends AnonymousUser {
	private static final long serialVersionUID = 1L;

}
