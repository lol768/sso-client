package uk.ac.warwick.userlookup;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MockUserLookupBackend implements UserLookupBackend {
	
	// Users with this string in their usercode will be "not found"
	public static final String MISSING_USER_STRING = "fake";
	public static final String SAMPLE_TOKEN = "bababababababababababa";
	
	private boolean blockLookup;
	
	private Object lookupLock = new Object();
	
	public Object getLookupLock() {
		return lookupLock;
	}

	public boolean isBlockLookup() {
		return blockLookup;
	}

	/**
	 * When set to true, user lookups will block until set to false again.
	 */
	public void setBlockLookup(boolean blockLookup) {
		synchronized(lookupLock) {
			this.blockLookup = blockLookup;
			if (!blockLookup) {
				lookupLock.notifyAll();
			}
		}
	}

	public boolean getSupportsBatchLookup() {
		return true;
	}

	public User getUserById(String userId) throws UserLookupException {
		synchronized(lookupLock) {
			while (blockLookup) {
				try {
					lookupLock.wait();
				} catch (InterruptedException e) {
					//Just wait a bit longer
				}
			}
			
			User user;
			if (userId.contains(MISSING_USER_STRING)) {
				user = new AnonymousUser();
				user.setUserId(userId);
			} else {
				user = new User();
				user.setUserId(userId);
			}
			return user;
		}
	}

	public User getUserByToken(String token) throws UserLookupException {
		User user;
		if (token.equals(SAMPLE_TOKEN)) {
			user = new User();
			user.setUserId("tokenGuy");
		} else {
			user = new AnonymousUser();
		}
		return user;
	}

	public User getUserByUserIdAndPassNonLoggingIn(String usercode, String password) throws UserLookupException {
		return new AnonymousUser();
	}

	public Map<String, User> getUsersById(List<String> userIds) throws UserLookupException {
		Map<String, User> users = new HashMap<String, User>();
		for (String id : userIds) {
			users.put(id, getUserById(id));
		}
		return users;
	}

	public User signIn(String usercode, String password) throws UserLookupException {
		return new AnonymousUser();
	}

	public void signOut(String token) throws UserLookupException {}
	
	public void requestClearWebGroup(String groupName) throws UserLookupException {}

}
