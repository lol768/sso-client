package uk.ac.warwick.userlookup;

import uk.ac.warwick.sso.client.core.*;

import uk.ac.warwick.util.cache.Cache;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Adapter for UserLookupInterface to allow you to override behaviour without
 * needing to implement every method. 
 */
public abstract class UserLookupAdapter implements UserLookupInterface {

	protected final UserLookupInterface delegate;
	
	public UserLookupAdapter(UserLookupInterface userLookup) {
		this.delegate = userLookup;
	}
	
	public Map<String, Set<Cache<?, ?>>> getCaches() {
		return delegate.getCaches();
	}

	public void clearCaches() {
		delegate.clearCaches();
	}

	public List<User> findUsersWithFilter(Map<String, String> filterValues) {
		return delegate.findUsersWithFilter(filterValues);
	}

	public List<User> findUsersWithFilter(Map<String, String> filterValues,
			boolean returnDisabledUsers) {
		return delegate.findUsersWithFilter(filterValues, returnDisabledUsers);
	}

	public GroupService getGroupService() {
		return delegate.getGroupService();
	}

	public OnCampusService getOnCampusService() {
		return delegate.getOnCampusService();
	}

	public User getUserByToken(String token) {
		return delegate.getUserByToken(token);
	}

	public User getUserByWarwickUniId(String warwickUniId) {
		return delegate.getUserByWarwickUniId(warwickUniId);
	}

	public User getUserByWarwickUniId(String warwickUniId,
			boolean includeDisabledLogins) {
		return delegate.getUserByWarwickUniId(warwickUniId, includeDisabledLogins);
	}

	public Map<String, User> getUsersByUserIds(List<String> userIdList) {
		return delegate.getUsersByUserIds(userIdList);
	}

	public List<User> getUsersInDepartment(String department) {
		return delegate.getUsersInDepartment(department);
	}

	public List<User> getUsersInDepartmentCode(String department) {
		return delegate.getUsersInDepartmentCode(department);
	}

	public User getUserByUserId(String uncheckedUserId) {
		return delegate.getUserByUserId(uncheckedUserId);
	}
	
	public User getUserByIdAndPassNonLoggingIn(String uncheckedUserId,
			String uncheckedPass) throws UserLookupException {
		return delegate.getUserByIdAndPassNonLoggingIn(uncheckedUserId, uncheckedPass);
	}

	public void requestClearWebGroup(String groupName)
			throws UserLookupException {
		delegate.requestClearWebGroup(groupName);
	}

}
