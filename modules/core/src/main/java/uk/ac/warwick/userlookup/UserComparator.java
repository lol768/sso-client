/*
 * Created on 23-Jul-2003
 *
 */
package uk.ac.warwick.userlookup;

import java.io.Serializable;
import java.util.Comparator;

/**
 * A java.util.Comparator to sort users by last name
 *
 */
public final class UserComparator implements Comparator<User>, Serializable {
	private static final long serialVersionUID = 1L;

	private static final UserComparator INSTANCE = new UserComparator();
	
	public static UserComparator getInstance() {
		return INSTANCE;
	}
	
	public int compare(User user1, User user2) {
		return user1.getLastName().compareTo(user2.getLastName());
	}

}
