package uk.ac.warwick.userlookup;

/**
 * A user who is not logged in, but has been detected as being on campus.
 * This includes accessing through the wwwcache proxy server, and should
 * also work with local Apache reverse proxies.
 */
public class AnonymousOnCampusUser extends AnonymousUser {
	private static final long serialVersionUID = 1L;

}
