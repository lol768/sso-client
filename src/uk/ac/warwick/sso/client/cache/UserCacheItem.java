/*
 * Created on 04-Aug-2003
 *
 */
package uk.ac.warwick.sso.client.cache;

import uk.ac.warwick.sso.client.SSOToken;
import uk.ac.warwick.userlookup.User;

/**
 * A wrapper for users that are cached by the UserCache
 * 
 */
public class UserCacheItem {

	private User _user;

	private long _inTime;

	private SSOToken _token;

	/**
	 * Needs a User object, a "new Date().getTime()" and the key for this user
	 * 
	 * @param user
	 * @param time
	 * @param token
	 */
	public UserCacheItem(final User user, final long time, final SSOToken token) {
		_user = user;
		_inTime = time;
		_token = token;
	}

	/**
	 * @return
	 */
	public final long getInTime() {
		return _inTime;
	}

	/**
	 * @return
	 */
	public final SSOToken getToken() {
		return _token;
	}

	/**
	 * @return
	 */
	public final User getUser() {
		return _user;
	}

	/**
	 * @param l
	 */
	public final void setInTime(final long l) {
		_inTime = l;
	}

	/**
	 * @param string
	 */
	public final void setToken(final SSOToken token) {
		_token = token;
	}

	/**
	 * @param user
	 */
	public final void setUser(final User user) {
		_user = user;
	}

}
