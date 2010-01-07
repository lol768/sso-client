package uk.ac.warwick.userlookup;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.log4j.Logger;

import uk.ac.warwick.userlookup.cache.Cache;
import uk.ac.warwick.userlookup.cache.CacheListener;
import uk.ac.warwick.userlookup.cache.Caches;
import uk.ac.warwick.userlookup.cache.Entry;
import uk.ac.warwick.userlookup.cache.EntryFactory;
import uk.ac.warwick.userlookup.cache.EntryUpdateException;
import uk.ac.warwick.userlookup.cache.SingularEntryFactory;
import uk.ac.warwick.userlookup.webgroups.WarwickGroupsService;

/**
 * A class to look up arbitrary users from Single Sign-on.
 */
public class UserLookup implements UserLookupInterface {

	private static final String TOKEN_PREFIX = "token::";

	public static String NOT_LOGGED_IN_TOKEN = "na";

	public static String LDAP_LAST_NAME_KEY = "sn";

	public static String LDAP_FIRST_NAME_KEY = "givenName";

	public static String LDAP_USER_CODE_KEY = "cn";

	public static String LDAP_DEPARTMENT_KEY = "ou";
	
	/**
	 * Default timeout in seconds for the userId cache, this can be large
	 */
	private static final int DEFAULT_USERID_CACHE_TIMEOUT = 7200;

	/**
	 * Default timeout in seconds for the token cache, this should be fairly short because of logging in/out issues
	 */
	private static final int DEFAULT_TOKEN_CACHE_TIMEOUT = 1800;

	private static final int DEFAULT_USERID_CACHE_SIZE = 10000;

	private static final int DEFAULT_TOKEN_CACHE_SIZE = 10000;

	public static final int DEFAULT_CONNECTION_TIMEOUT = 10000;

	public static final int DEFAULT_DATA_TIMEOUT = 10000;
	
	// Names of UserLookup caches, to use as reference to external cache stores like Ehcache.
	public static final String USER_CACHE_NAME = "UserLookupCache";
	public static final String GROUP_CACHE_NAME = "WebgroupCache";
	public static final String IN_GROUP_CACHE_NAME = "InWebgroupCache";
	public static final String GROUP_MEMBER_CACHE_NAME = "WebgroupMemberCache";
	public static final String USER_GROUPS_CACHE_NAME = "UserGroupsCache";
	
	private static final Logger LOGGER = Logger.getLogger(UserLookup.class);


	private static UserLookup INSTANCE;

	private static Properties configProperties;
	
	private Cache<String,User> _userByTokenCache;

	private Cache<String,User> _userByUserIdCache;

	private String _ssosUrl;
	
	private String _apiKey;

	private int _httpDataTimeout;

	private int _httpConnectionTimeout;

	private GroupService _groupService;
	
	// the innermmost groupService.
	private GroupService _groupServiceBackend;

	private String _version;
	
	private OnCampusService _onCampusService;
	
	private String groupServiceLocation;

	private boolean asynchronousUpdates = true;

	public static UserLookup getInstance() {
		if (INSTANCE == null) {
			LOGGER.warn("UserLookup not initialized - creating new lookup with default settings...");
			INSTANCE = new UserLookup();
		}
		return INSTANCE;
	}

	public UserLookup() {
		_onCampusService = new OnCampusServiceImpl();
		
		_userByTokenCache = Caches.newCache(USER_CACHE_NAME, new SingularEntryFactory<String, User>() {
			public User create(String key) throws EntryUpdateException {
				try {
					if (key.startsWith(TOKEN_PREFIX)) {
						return getSpecificUserLookupType().getUserByToken(key.substring(TOKEN_PREFIX.length()));
					} else {
						// The prefix should be always added by internal code so this indicates a UserLookup bug.
						throw new IllegalArgumentException("Requests for tokens from cache must start with " + TOKEN_PREFIX);
					}
				} catch (UserLookupException e) {
					return new UnverifiedUser(e);
				}
			}
			public boolean shouldBeCached(User val) {
				return val.isVerified();
			}
		
		}, DEFAULT_TOKEN_CACHE_TIMEOUT);
		_userByTokenCache.setMaxSize(DEFAULT_TOKEN_CACHE_SIZE);
		_userByTokenCache.setAsynchronousUpdateEnabled(false);
		_userByTokenCache.addCacheListener(new CacheListener<String, User>() {
			// When we update a user entry from a token, push it on to the user Id cache
			// (as long as it's a valid user with a user ID) 
			public void cacheMiss(String key, uk.ac.warwick.userlookup.cache.Entry<String, User> newEntry) {
				User user = newEntry.getValue();
				String userId = user.getUserId();
				if (user.isFoundUser() && userId != null && !"".equals(userId.trim())) {
					if (LOGGER.isDebugEnabled()) LOGGER.debug("Updated token cache - copying to user ID cache");
					_userByUserIdCache.put(new Entry<String, User>(userId, user));
				}
			}
			public void cacheHit(String key, Entry<String, User> entry) {}
		});
		
		_userByUserIdCache = Caches.newCache(USER_CACHE_NAME, new EntryFactory<String, User>() {
			public User create(String key) throws EntryUpdateException {
				try {
					return getSpecificUserLookupType().getUserById(key);
				} catch (UserLookupException e) {
					throw new EntryUpdateException(e);
				}
			}
			public Map<String, User> create(List<String> keys) throws EntryUpdateException {
				try {
					Map<String, User> usersById = getSpecificUserLookupType().getUsersById(keys);
					for (String key : keys) {
						if (!usersById.containsKey(key)) {
							usersById.put(key, new AnonymousUser());
						}
					}
					return usersById;
				} catch (UserLookupException e) {
					throw new EntryUpdateException(e);
				}
			}
			public boolean isSupportsMultiLookups() {
				return true;
			}
			public boolean shouldBeCached(User val) {
				return val.isVerified();
			}
		}, DEFAULT_USERID_CACHE_TIMEOUT);
		_userByUserIdCache.setMaxSize(DEFAULT_USERID_CACHE_SIZE);
		_userByUserIdCache.setAsynchronousUpdateEnabled(true);

		

		if (UserLookup.getConfigProperty("userlookup.useridcachesize") != null) {
			_userByUserIdCache.setMaxSize(Integer.parseInt(UserLookup.getConfigProperty("userlookup.useridcachesize")));
		}
		if (UserLookup.getConfigProperty("userlookup.useridcachetimeout") != null) {
			_userByUserIdCache.setTimeout(Integer.parseInt(UserLookup.getConfigProperty("userlookup.useridcachetimeout")));
		}
		if (UserLookup.getConfigProperty("userlookup.tokencachesize") != null) {
			_userByTokenCache.setMaxSize(Integer.parseInt(UserLookup.getConfigProperty("userlookup.tokencachesize")));
		}
		if (UserLookup.getConfigProperty("userlookup.tokencachetimeout") != null) {
			_userByTokenCache.setTimeout(Integer.parseInt(UserLookup.getConfigProperty("userlookup.tokencachetimeout")));
		}
		if (UserLookup.getConfigProperty("userlookup.connectiontimeout") != null) {
			_httpConnectionTimeout = Integer.parseInt(UserLookup.getConfigProperty("userlookup.connectiontimeout"));
		} else {
			_httpConnectionTimeout = DEFAULT_CONNECTION_TIMEOUT;
		}
		if (UserLookup.getConfigProperty("userlookup.datatimeout") != null) {
			_httpDataTimeout = Integer.parseInt(UserLookup.getConfigProperty("userlookup.datatimeout"));
		} else {
			_httpDataTimeout = DEFAULT_DATA_TIMEOUT;
		}
		
		_apiKey = UserLookup.getConfigProperty("userlookup.ssos.apiKey");

		if (_version == null || _version.equals("")) {
			_version = UserLookupVersionLoader.getVersion();
		}
	}

	public static final String generateRandomTicket() {
		SecureRandom generator;
		try {
			generator = SecureRandom.getInstance("SHA1PRNG");
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
		final int numRandomChars = 30;
		final int numAlphaChars = 25;
		final int hexStart = 97;
		byte[] randomBytes = new byte[numRandomChars];
		for (int i = 0; i < numRandomChars; i++) {
			byte myByte = (byte) (generator.nextInt(numAlphaChars) + hexStart);
			randomBytes[i] = myByte;
		}
		String newToken = new String(randomBytes);

		return "sbr" + newToken;
	}

	/**
	 * Do a userlookup that returns all users in a given department, eg. Information Technology Services
	 * 
	 * @param department
	 * @return
	 */
	public final List<User> getUsersInDepartment(final String department) {

		if (department == null || department.equals("")) {
			return new ArrayList<User>();
		}

		Map<String,String> filterValues = new HashMap<String,String>();
		filterValues.put("ou", department);
		// the warwickuniid * line is to get only real users as there appear to
		// be fake users with no library card numbers
		filterValues.put("warwickuniid", "*");
		List<User> users = findUsersWithFilter(filterValues, false);
		return users;

	}

	/**
	 * Do a userlookup from LDAP that returns all users in a given department code, eg. IN, AR, BO
	 * 
	 * @param department
	 * @return
	 */
	public final List<User> getUsersInDepartmentCode(final String department) {

		if (department == null || department.equals("")) {
			return new ArrayList<User>();
		}

		Map<String, String> filterValues = new HashMap<String, String>();
		filterValues.put("warwickdeptcode", department);
		// the warwickuniid * line is to get only real users as there appear to
		// be fake users with no library card numbers
		filterValues.put("warwickuniid", "*");
		List<User> users = findUsersWithFilter(filterValues, false);
		return users;

	}

	/**
	 * Get user by token. It will cache entries, but when expired it WILL NOT
	 * do an asynchronous update. If we did this we'd have to return stale data
	 * for the token request which could make people be logged in/out for longer
	 * than they should be
	 */
	public User getUserByToken(final String uncheckedToken) {
		if (uncheckedToken == null || uncheckedToken.equals("") || uncheckedToken.equals(NOT_LOGGED_IN_TOKEN)) {
			return new AnonymousUser();
		}
		String token = uncheckedToken.trim();
		try {
			User user = getUserByTokenCache().get(TOKEN_PREFIX + token);
			return user;
		} catch (EntryUpdateException e) {
			throw e.getRuntimeException();
		}
	}

	/**
	 * Will return a single populated user or an anonymous user if the user can not be found.
	 * 
	 * Even if LDAP lookup fails, it will return an anonymous user and put an error in the logs explaining what went
	 * wrong.
	 * 
	 * @param userId
	 * @return
	 */
	public final User getUserByUserId(final String uncheckedUserId) {
		if (uncheckedUserId == null || uncheckedUserId.equals("") || uncheckedUserId.equals(NOT_LOGGED_IN_TOKEN)) {
			User anon = new AnonymousUser();
			return anon;
		}
		String userId = uncheckedUserId.trim();
		try {
			return getUserByUserIdCache().get(userId);
		} catch (EntryUpdateException e) {
			throw e.getRuntimeException();
		}
	}
	
	/**
	 * Takes a List of userIds, and returns a Map that maps userIds to Users. Users found
	 * in the local cache will be taken from there (and not searched for), and all other
	 * users will be searched for and entered into the cache.
	 * 
	 * All userIds will be returned in the Map, but ones that weren't found will map to
	 * AnonymousUser objects.
	 * 
	 * @param userIdList List[String]
	 * @return Map[String,User]
	 */
	public final Map<String, User> getUsersByUserIds(final List<String> userIdList) {	
		Set<String> distinctIds = new HashSet<String>(userIdList);
		try {
			return getUserByUserIdCache().get(new ArrayList<String>(distinctIds));
		} catch (EntryUpdateException e) {
			throw e.getRuntimeException();
		}
	}


	public final void signOut(final String token) throws UserLookupException {
		_userByTokenCache.remove(token);
		getSpecificUserLookupType().signOut(token);
	}

	private UserLookupBackend getSpecificUserLookupType() {
		return new WebUserLookup(_ssosUrl, new WebServiceTimeoutConfig(getHttpConnectionTimeout(), getHttpDataTimeout()),
				_version, _apiKey);
	}

	/**
	 * @deprecated
	 * 
	 * @param user
	 * @param pass
	 * @return
	 * @throws UserLookupException
	 */
	public final User getUserByIdAndPass(final String uncheckedUserId, final String uncheckedPass) throws UserLookupException {

		LOGGER.debug("Trying SSO for user " + uncheckedUserId);

		if (uncheckedUserId == null || uncheckedPass == null || uncheckedUserId.equals("")) {
			return new AnonymousUser();
		}

		String userId = uncheckedUserId.trim();
		String pass = uncheckedPass.trim();

		User user = getSpecificUserLookupType().signIn(userId, pass);

		return user;

	}


	public final User getUserByIdAndPassNonLoggingIn(final String uncheckedUserId, final String uncheckedPass)
			throws UserLookupException {

		LOGGER.debug("Trying non logging in SSO for user " + uncheckedUserId);

		if (uncheckedUserId == null || uncheckedPass == null || uncheckedUserId.equals("")) {
			return new AnonymousUser();
		}

		String userId = uncheckedUserId.trim();
		String pass = uncheckedPass.trim();

		return getSpecificUserLookupType().getUserByUserIdAndPassNonLoggingIn(userId, pass);

	}

	
	/**
	 * Will return just a single user or an anonymous user that matches the warwickUniId passed in. It is possible that
	 * it will not be the right user depending on how many users are against this warwickUniId and if their
	 * login_disabled attributes are correctly populated.
	 * 
	 * Even if LDAP lookup fails, it will return an anonymous user and put an error in the logs explaining what went
	 * wrong.
	 * 
	 * This method will show users whose login is disabled. To ignore these, use the method with the extra argument.
	 * 
	 * @param warwickUniId
	 */
	public final User getUserByWarwickUniId(final String warwickUniId) {
		return getUserByWarwickUniId(warwickUniId, true);
	}

	/**
	 * Will return just a single user or an anonymous user that matches the warwickUniId passed in. It is possible that
	 * it will not be the right user depending on how many users are against this warwickUniId and if their
	 * login_disabled attributes are correctly populated.
	 * 
	 * Even if LDAP lookup fails, it will return an anonymous user and put an error in the logs explaining what went
	 * wrong.
	 * 
	 * @param warwickUniId
	 * @param returnDisabledUsers if false, will only return enabled accounts (ie skips logindisabled=true)
	 * @return
	 */
	public final User getUserByWarwickUniId(final String warwickUniId, boolean returnDisabledUsers) {

		Map<String,String> filterValues = new HashMap<String,String>();
		filterValues.put("warwickuniid", warwickUniId);
		List<User> users = findUsersWithFilter(filterValues, returnDisabledUsers);

		if (users.isEmpty()) {
			LOGGER.debug("No user found that matches Warwick Uni Id:" + warwickUniId);
			return new AnonymousUser();
		}
		
		for (User user : users) {
			if (user.getEmail() != null && !user.getEmail().equals("")) {
				LOGGER.info("Returning user with email address (" + user.getUserId() + ") of " + users.size()
						+ " users that matches Warwick Uni Id:" + warwickUniId);
				return getUserByUserId(user.getUserId());
			}
		}

		User user = (User) users.get(0);
		if (!returnDisabledUsers && user.isLoginDisabled()) {
			LOGGER.info("No active user for Warwick Uni Id:" + warwickUniId + ". Returning anonymous");
			user = new AnonymousUser();
		}
		
		return getUserByUserId(user.getUserId());
	}
	
	public final List<User> findUsersWithFilter(final Map<String,String> filterValues) {
		return findUsersWithFilter(filterValues, false);
	}

	/**
	 * Return a list of users with names matching the parameters passed in FilterValues.
	 * 
	 * @see LDAPUserLookup#findUsersWithFilter(HashMap)
	 */
	public final List<User> findUsersWithFilter(final Map<String,String> filterValues, boolean returnDisabledUsers) {
		try {
			List<User> list = new SSOUserLookup(_ssosUrl, _apiKey).findUsersWithFilter(filterValues, returnDisabledUsers);
			final Cache<String, User> cache = getUserByUserIdCache();
			for (User user : list) {
				if (user.isFoundUser()) {
					String userId = user.getUserId();
					if (userId != null && !"".equals(userId.trim())) {
						cache.put(new Entry<String,User>(userId, user));
					}
				}
			}
			return list;
		} catch (UserLookupException e) {
			LOGGER.warn("findUsersWithFilter failed, returning empty list", e);
			return new ArrayList<User>();
		}
	}

	/**
	 * @param ssosUrl
	 *            The ssosUrl to set.
	 */
	public final void setSsosUrl(final String ssosUrl) {
		_ssosUrl = ssosUrl;
	}
	
	/**
	 * Deprecated since UserLookup no longer calls LDAP directly.
	 * @deprecated
	 */
	public final void setLdapUrl(final String ignored) {
		LOGGER.error("setLdapUrl() is deprecated and no longer does anything. You should stop using it.");
	}

	/**
	 * Time in milliseconds to allow for a connection to the single sign on
	 */
	public final int getHttpConnectionTimeout() {
		return _httpConnectionTimeout;
	}

	/**
	 * Set the http connection timeout in milliseconds, you can also use the userlookup.connectiontimeout system
	 * property
	 */
	public final void setHttpConnectionTimeout(final int httpConnectionTimeout) {
		_httpConnectionTimeout = httpConnectionTimeout;
	}

	/**
	 * Time in milliseconds to allow for data to be send from the single sign on
	 */
	public final int getHttpDataTimeout() {
		return _httpDataTimeout;
	}

	/**
	 * Set the http data timeout in milliseconds, you can also use the userlookup.datatimeout system property
	 */
	public final void setHttpDataTimeout(final int httpDataTimeout) {
		_httpDataTimeout = httpDataTimeout;
	}

	/**
	 * You can set this here, or use the system property "userlookup.tokencachesize"
	 * 
	 * @param userCacheSize
	 */
	public final void setTokenCacheSize(final int userCacheSize) {
		getUserByTokenCache().setMaxSize(userCacheSize);
	}

	/**
	 * You can set this here, or use the system property "userlookup.tokencachetimeout"
	 * 
	 * @param userCacheSize
	 */
	public final void setTokenCacheTimeout(final int userCacheTimeout) {
		getUserByTokenCache().setTimeout(userCacheTimeout);
	}

	/**
	 * You can set this here, or use the system property "userlookup.useridcachesize"
	 * 
	 * @param userCacheSize
	 */
	public final void setUserIdCacheSize(final int userCacheSize) {
		getUserByUserIdCache().setMaxSize(userCacheSize);
	}

	/**
	 * You can set this here, or use the system property "userlookup.useridcachetimeout"
	 * 
	 * @param userCacheSize
	 */
	public final void setUserIdCacheTimeout(final int userCacheTimeout) {
		getUserByUserIdCache().setTimeout(userCacheTimeout);
	}

	public final GroupService getGroupService() {
		if (_groupService == null) {
			String location = this.groupServiceLocation; 
			if (location == null) {
				location = UserLookup.getConfigProperty("userlookup.groupservice.location"); 
			}
			if (location != null || _groupServiceBackend != null) {
				if (_groupServiceBackend == null) {
					_groupServiceBackend = new WarwickGroupsService(location);
					_groupServiceBackend.setTimeoutConfig(new WebServiceTimeoutConfig(getHttpConnectionTimeout(), getHttpDataTimeout()));
				}
				// cache the groups
				_groupService = new GroupAliasAwareGroupService(new GroupNameCheckerGroupService(
						new IsUserInGroupCachingGroupsService(new GroupByNameCachingGroupsService(
								new UsersInGroupCachingGroupsService(new GroupsNamesForUserCachingGroupsService(_groupServiceBackend))))),
						this);

			} else {
				LOGGER.info("Creating DefaultGroupService that returns no groups, because of no userlookup.groupservice.location property");
				_groupService = new GroupAliasAwareGroupService(new GroupNameCheckerGroupService(new DefaultGroupService()), this);
			}
		}
		return _groupService;
	}
	
	final boolean isUserByUserIdCacheEmpty() {
		return _userByUserIdCache.getStatistics().getCacheSize() == 0;
	}

	public final void setGroupService(final GroupService groupService) {
		_groupService = groupService;
	}

	public final String getVersion() {
		return _version;
	}

	public final void setVersion(String version) {
		_version = version;
	}

	public final void clearCaches() {
		getUserByTokenCache().clear();
		getUserByUserIdCache().clear();
	}

	public OnCampusService getOnCampusService() {
		return _onCampusService;
	}

	public void setOnCampusService(OnCampusService onCampusService) {
		this._onCampusService = onCampusService;
	}
	
	public boolean isAsynchronousUpdates() {
		return this.asynchronousUpdates;
	}
	
	public void setAsynchronousUpdates(final boolean async) {
		this.asynchronousUpdates = async;
	}

	/**
	 * This will go and get all the usercodes and then turn those codes into User objects. This will return a fully
	 * populated list of User's.
	 * 
	 * @param userCodes
	 * @return
	 * 
	 * @deprecated Use {@link UserLookup#getUsersByUserIds(List)}.
	 */
	public final List<User> convertUserCodesIntoUsers(final List<String> userCodes) {
		List<User> users = new ArrayList<User>();
		for (String userCode : userCodes) {
			users.add(getUserByUserId(userCode));
		}
		return users;
	}


	public void setApiKey(String apiKey) {
		_apiKey = apiKey;
	}


	public void setGroupServiceLocation(String groupServiceLocation) {
		this.groupServiceLocation = groupServiceLocation;
	}
	
	/**
	 * If you provide UserLookup with a Properties object here, it will
	 * use it to check for configuration properties instead of using system
	 * properties.
	 */
	public static void setConfigProperties(Properties props) {
		LOGGER.info("Using configuration from a Properties object instead of System properties");
		configProperties = props;
	}
	
	public static String getConfigProperty(String propertyName) {
		if (configProperties != null) {
			return configProperties.getProperty(propertyName);
		}
		return System.getProperty(propertyName);
	}
	
	public static String getConfigProperty(String propertyName, String def) {
		String result;
		if (configProperties != null) {
			result = configProperties.getProperty(propertyName);
		} else {
			result = System.getProperty(propertyName);
		}
		if (result == null) {
			if (LOGGER.isDebugEnabled()) LOGGER.debug("Property " + propertyName + " missing, using default of " + def);
			result = def;
		}
		return result;
	}


	public Cache<String,User> getUserByTokenCache() {
		return _userByTokenCache;
	}
	
	public Cache<String,User> getUserByUserIdCache() {
		return _userByUserIdCache;
	}

	void shutdown() {
		_userByTokenCache.shutdown();
		_userByUserIdCache.shutdown();
	}

	/**
	 * If groupService has not yet been created, we can use this to set the innermost
	 * GroupService backend that will get wrapped with the other GroupService instances.
	 * This is probably only going to be useful for testing the behaviour of the
	 * wrapping GroupServices.
	 */
	public final void setGroupServiceBackend(GroupService groupServiceBackend) {
		if (_groupService != null) {
			throw new IllegalStateException("Can only set backend before groupService has been created");
		}
		_groupServiceBackend = groupServiceBackend;
	}
}