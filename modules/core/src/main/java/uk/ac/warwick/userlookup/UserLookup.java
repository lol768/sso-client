package uk.ac.warwick.userlookup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.warwick.sso.client.SSOClientVersionLoader;
import uk.ac.warwick.sso.client.core.OnCampusService;
import uk.ac.warwick.sso.client.core.OnCampusServiceImpl;
import uk.ac.warwick.userlookup.webgroups.WarwickGroupsService;
import uk.ac.warwick.util.cache.*;
import uk.ac.warwick.util.collections.Pair;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.lang.Integer.parseInt;

/**
 * A class to look up arbitrary users from Single Sign-on.
 * <p>
 * The recommended way to get an instance of this is to use
 * {@link UserLookupFactory#getInstance()}, which will return a
 * {@link UserLookupInterface} object. However, it is also
 * acceptable to simply create a UserLookup object yourself. If you
 * do this, it's recommended to make one instance of it for your
 * application and re-use it. If you use a Web framework such as
 * Spring then you can create it as a bean and share it around.
 */
public class UserLookup implements UserLookupInterface {

    private static final int TIME_TO_LIVE_ETERNITY = -1;

	private static final int MILLIS_IN_SEC = 1000;

	private static final String TOKEN_PREFIX = "token::";

	public static String NOT_LOGGED_IN_TOKEN = "na";

	public static String LDAP_LAST_NAME_KEY = "sn";

	public static String LDAP_FIRST_NAME_KEY = "givenName";

	public static String LDAP_USER_CODE_KEY = "cn";

	public static String LDAP_DEPARTMENT_KEY = "ou";
	
	private static Properties configProperties;
	
	private static Object defaultPropertiesLock = new Object();
	private static Properties defaultProperties;

	/**
	 * Default timeout in seconds for the userId cache, this can be large
	 */
	private static final int DEFAULT_USERID_CACHE_TIMEOUT = parseInt(getConfigProperty("ssoclient.cache.userid.timeout.secs"));
	private static final int MISSING_USERID_CACHE_TIMEOUT = parseInt(getConfigProperty("ssoclient.cache.userid-missing.timeout.secs"));

	/**
	 * Default timeout in seconds for the token cache, this should be fairly short because of logging in/out issues
	 */
	private static final int DEFAULT_TOKEN_CACHE_TIMEOUT = parseInt(getConfigProperty("ssoclient.cache.token.timeout.secs"));
	private static final int DEFAULT_USERID_CACHE_SIZE = parseInt(getConfigProperty("ssoclient.cache.userid.size"));
	private static final int DEFAULT_TOKEN_CACHE_SIZE = parseInt(getConfigProperty("ssoclient.cache.token.size"));
	public static final int DEFAULT_CONNECTION_TIMEOUT = parseInt(getConfigProperty("ssoclient.net.connection-timeout.millis"));
	public static final int DEFAULT_DATA_TIMEOUT = parseInt(getConfigProperty("ssoclient.net.data-timeout.millis"));

	// Names of UserLookup caches, to use as reference to external cache stores like Ehcache.
	public static final String USER_CACHE_NAME = "UserLookupCache";
	public static final String USER_BY_UNI_ID_CACHE_NAME = "UserByUniIdLookupCache";
	public static final String GROUP_CACHE_NAME = "WebgroupCache";
	public static final String IN_GROUP_CACHE_NAME = "InWebgroupCache";
	public static final String GROUP_MEMBER_CACHE_NAME = "WebgroupMemberCache";
	public static final String USER_GROUPS_CACHE_NAME = "UserGroupsCache";
	public static final String AUTH_CACHE_NAME = "UserAuthCache";

	private static final Logger LOGGER = LoggerFactory.getLogger(UserLookup.class);


	private static UserLookup INSTANCE;

	private Cache<String,User> _userByTokenCache;

	private Cache<String,User> _userByUserIdCache;

	private Cache<String,User> _userByUniIdCache;

	private CacheWithDataInitialisation<String, Pair<String, User>, String> _authCache;

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

	// optional, will use a web-based default if not set.
	private UserLookupBackend _backend;

	private int userIdCacheTimeout = DEFAULT_USERID_CACHE_TIMEOUT;

	@Deprecated
	public static UserLookup getInstance() {
		if (INSTANCE == null) {
			LOGGER.warn("UserLookup not initialized - creating new lookup with default settings...");
			INSTANCE = new UserLookup();
		}
		return INSTANCE;
	}

	public UserLookup() {
		_onCampusService = new OnCampusServiceImpl();

		final String cacheStrategy = getConfigProperty("ssoclient.cache.strategy");

		// Basic Auth lookup cache.
		// Note that we only return the password digest as a value - the factory itself can't
		// check if it matched the input password, so you need to do that on get.
		_authCache = Caches.newDataInitialisatingCache(
			AUTH_CACHE_NAME,
			new CacheEntryFactoryWithDataInitialisation<String, Pair<String, User>, String>() {
				@Override public Pair<String, User> create(String username, String password) throws CacheEntryUpdateException {
					try {
						User u = getSpecificUserLookupType().getUserByUserIdAndPassNonLoggingIn(username, password);
						return Pair.of(CacheDigests.digest(password), u);
					} catch (UserLookupException e) {
						throw new CacheEntryUpdateException("Error authenticating with credentials", e);
					}
				}

				@Override public boolean shouldBeCached(Pair<String, User> val) {
					return val.getRight().isFoundUser();
				}

				@Override public Map<String, Pair<String, User>> create(List<String> keys) throws CacheEntryUpdateException {
					throw new UnsupportedOperationException();
				}

				@Override public boolean isSupportsMultiLookups() {
					return false;
				}

			},
			parseInt(getConfigProperty("ssoclient.cache.auth.timeout.secs")),
			Caches.CacheStrategy.valueOf(cacheStrategy)
		);

		_userByTokenCache = Caches.newCache(USER_CACHE_NAME, new SingularCacheEntryFactory<String, User>() {
            public User create(String key) throws CacheEntryUpdateException {
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
        }, DEFAULT_TOKEN_CACHE_TIMEOUT, Caches.CacheStrategy.valueOf(getConfigProperty("ssoclient.cache.strategy")));
        _userByTokenCache.setExpiryStrategy(new TTLCacheExpiryStrategy<String, User>() {
            @Override
            public Pair<Number, TimeUnit> getTTL(CacheEntry<String, User> entry) {
                if (entry.getValue().isFoundUser()) {
                    return Pair.of((Number) TIME_TO_LIVE_ETERNITY, TimeUnit.SECONDS);
                } else {
                    return Pair.of((Number) (MISSING_USERID_CACHE_TIMEOUT * 2), TimeUnit.SECONDS); // twice the stale time
                }
            }

            @Override
            public boolean isStale(CacheEntry<String, User> entry) {
                final long staleTime = entry.getTimestamp() + (DEFAULT_TOKEN_CACHE_TIMEOUT * 1000);
                final long now = System.currentTimeMillis();
                return staleTime <= now;
            }
        });

		_userByTokenCache.setMaxSize(DEFAULT_TOKEN_CACHE_SIZE);
		_userByTokenCache.setAsynchronousUpdateEnabled(false);
		_userByTokenCache.addCacheListener(new CacheListener<String, User>() {
            // When we update a user entry from a token, push it on to the user Id cache
            // (as long as it's a valid user with a user ID)
            public void cacheMiss(String key, uk.ac.warwick.util.cache.CacheEntry<String, User> newEntry) {
                User user = newEntry.getValue();
                String userId = user.getUserId();
                if (user.isFoundUser() && userId != null && !"".equals(userId.trim())) {
                    if (LOGGER.isDebugEnabled()) LOGGER.debug("Updated token cache - copying to user ID cache");
                    _userByUserIdCache.put(new CacheEntry<String, User>(userId, user));
                }
            }

            public void cacheHit(String key, CacheEntry<String, User> entry) {
            }
        });

		_userByUniIdCache = Caches.newCache(USER_BY_UNI_ID_CACHE_NAME, new CacheEntryFactory<String, User>() {
			public User create(String key) {
				return getUserByWarwickUniIdUncached(key);
			}

			public Map<String, User> create(List<String> keys) {
				return keys.stream()
						.map(uniId -> new Pair<>(uniId, getUserByWarwickUniIdUncached(uniId)))
						.collect(Collectors.toMap(Pair::getLeft, Pair::getRight));
			}
			public boolean isSupportsMultiLookups() {
				return true;
			}
			public boolean shouldBeCached(User val) {
				return val.isVerified();
			}
			public int secondsToLive(User val) {
				if (val.isFoundUser()) {
					return TIME_TO_LIVE_ETERNITY;
				} else {
					return MISSING_USERID_CACHE_TIMEOUT * 2; // twice the stale time
				}
			}
		}, DEFAULT_USERID_CACHE_TIMEOUT, Caches.CacheStrategy.valueOf(getConfigProperty("ssoclient.cache.strategy")));
		_userByUniIdCache.setMaxSize(DEFAULT_USERID_CACHE_SIZE);
		_userByUniIdCache.setAsynchronousUpdateEnabled(true);
		_userByUniIdCache.setExpiryStrategy(new TTLCacheExpiryStrategy<String, User>() {
			@Override
			public Pair<Number, TimeUnit> getTTL(CacheEntry<String, User> entry) {
				if (entry.getValue().isFoundUser()) {
					return Pair.of((Number) userIdCacheTimeout, TimeUnit.SECONDS);
				} else {
					return Pair.of((Number) (MISSING_USERID_CACHE_TIMEOUT * 2), TimeUnit.SECONDS); // twice the stale time
				}
			}

			@Override
			public boolean isStale(CacheEntry<String, User> entry) {
				final long staleTime = entry.getTimestamp() + (DEFAULT_USERID_CACHE_TIMEOUT * 1000);
				final long now = System.currentTimeMillis();
				return staleTime <= now;
			}
		});

		_userByUserIdCache = Caches.newCache(USER_CACHE_NAME, new CacheEntryFactory<String, User>() {
			public User create(String key) throws CacheEntryUpdateException {
				try {
					return getSpecificUserLookupType().getUserById(key);
				} catch (UserLookupException e) {
					throw new CacheEntryUpdateException(e);
				}
			}
			public Map<String, User> create(List<String> keys) throws CacheEntryUpdateException {
				try {
					Map<String, User> usersById = getSpecificUserLookupType().getUsersById(keys);
					for (String key : keys) {
						if (!usersById.containsKey(key)) {
							AnonymousUser anon = new AnonymousUser();
							anon.setUserId(key);
							usersById.put(key, anon);
						}
					}
					return usersById;
				} catch (UserLookupException e) {
					throw new CacheEntryUpdateException(e);
				}
			}
			public boolean isSupportsMultiLookups() {
				return true;
			}
			public boolean shouldBeCached(User val) {
				return val.isVerified();
			}
			public int secondsToLive(User val) {
				if (val.isFoundUser()) {
					return TIME_TO_LIVE_ETERNITY;
				} else {
					return MISSING_USERID_CACHE_TIMEOUT * 2; // twice the stale time
				}
			}
		}, DEFAULT_USERID_CACHE_TIMEOUT, Caches.CacheStrategy.valueOf(getConfigProperty("ssoclient.cache.strategy")));
		_userByUserIdCache.setMaxSize(DEFAULT_USERID_CACHE_SIZE);
		_userByUserIdCache.setAsynchronousUpdateEnabled(true);
		_userByUserIdCache.setExpiryStrategy(new TTLCacheExpiryStrategy<String, User>() {
            @Override
            public Pair<Number, TimeUnit> getTTL(CacheEntry<String, User> entry) {
                if (entry.getValue().isFoundUser()) {
                    return Pair.of((Number) userIdCacheTimeout, TimeUnit.SECONDS);
                } else {
                    return Pair.of((Number) (MISSING_USERID_CACHE_TIMEOUT * 2), TimeUnit.SECONDS); // twice the stale time
                }
            }

            @Override
            public boolean isStale(CacheEntry<String, User> entry) {
                final long staleTime = entry.getTimestamp() + (DEFAULT_USERID_CACHE_TIMEOUT * 1000);
                final long now = System.currentTimeMillis();
                return staleTime <= now;
            }
		});


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
			_version = SSOClientVersionLoader.getVersion();
		}
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
		} catch (CacheEntryUpdateException e) {
			LOGGER.warn("Couldn't get user by token", e);
			return new UnverifiedUser(e);
		}
	}

	/**
	 * Will return a single populated user or an anonymous user if the user can not be found.
	 * 
	 * Even if LDAP lookup fails, it will return an anonymous user and put an error in the logs explaining what went
	 * wrong.
	 */
	public final User getUserByUserId(final String uncheckedUserId) {
		if (uncheckedUserId == null || uncheckedUserId.equals("") || uncheckedUserId.equals(NOT_LOGGED_IN_TOKEN)) {
			User anon = new AnonymousUser();
			anon.setUserId(uncheckedUserId);
			return anon;
		}
		String userId = uncheckedUserId.trim();
		try {
			return getUserByUserIdCache().get(userId);
		} catch (CacheEntryUpdateException e) {
			LOGGER.warn("Couldn't get user by user ID", e);
			return new UnverifiedUser(e);
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
		} catch (CacheEntryUpdateException e) {
			LOGGER.warn("Couldn't get users by user IDs", e);
			Map<String,User> unverifiedUsers = new HashMap<String, User>();
			for (String id : distinctIds) {
				unverifiedUsers.put(id, new UnverifiedUser(e));
			}
			return unverifiedUsers;
		}
	}


	public final void signOut(final String token) throws UserLookupException {
		_userByTokenCache.remove(token);
		getSpecificUserLookupType().signOut(token);
	}

	protected UserLookupBackend getSpecificUserLookupType() {
		if (_backend != null) {
			return _backend;
		}
		return new WebUserLookup(getSsosUrl(), new WebServiceTimeoutConfig(getHttpConnectionTimeout(), getHttpDataTimeout()),
				_version, _apiKey);
	}

	public void setUserLookupBackend(UserLookupBackend backend) {
		this._backend = backend;
	}


	public final User getUserByIdAndPassNonLoggingIn(final String uncheckedUserId, final String uncheckedPass)
			throws UserLookupException {

		LOGGER.debug("Trying non logging in SSO for user " + uncheckedUserId);

		if (uncheckedUserId == null || uncheckedPass == null || uncheckedUserId.equals("")) {
			return new AnonymousUser();
		}

		String userId = uncheckedUserId.trim();
		String pass = uncheckedPass.trim();

		try {
			// Cache stores pair of password digest and user, so we need to
			// check the password and digest match
			final Pair<String, User> result = _authCache.get(userId, pass);
			if (result.getLeft().equals(CacheDigests.digest(pass))) {
				return result.getRight();
			}
		} catch (CacheEntryUpdateException e) {
			LOGGER.error("Error using basic auth cache for " + userId, e);
		}

		/**
		 * We get here if there was a CacheEntryUpdateException, or if the password was wrong.
		 */
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

	public final User getUserByWarwickUniId(final String warwickUniId, boolean returnDisabledUsers){
		if (warwickUniId == null || warwickUniId.isEmpty()) {
			return new AnonymousUser();
		}
		try {
			User user = getUserByUniIdCache().get(warwickUniId.trim());
			if (!returnDisabledUsers && user.isLoginDisabled()) return new AnonymousUser();
			return user;
		} catch (CacheEntryUpdateException e) {
			LOGGER.warn("Couldn't get user by uniId", e);
			return new UnverifiedUser(e);
		}
	}

	/**
	 * Will return just a single user or an anonymous user that matches the warwickUniId passed in.
	 * Will attempt to return the user whose warwickPrimary is true, if any.
	 * It is possible that it will not be the right user depending on how many users are against
	 * this warwickUniId and if their login_disabled attributes are correctly populated.
	 *
	 * Even if LDAP lookup fails, it will return an anonymous user and put an error in the logs explaining what went
	 * wrong.
	 *
	 * @param warwickUniId
	 * @return
	 */
	private final User getUserByWarwickUniIdUncached(final String warwickUniId) {

		Map<String,String> filterValues = new HashMap<String,String>();
		filterValues.put("warwickuniid", warwickUniId);
		List<User> users;
		try {
			users = findUsersWithFilterUnsafe(filterValues, true);
		} catch (UserLookupException e) {
			LOGGER.warn("Problem looking up user by warwickuniid, returning unverified user");
			return new UnverifiedUser(e);
		}

		if (users.isEmpty()) {
			LOGGER.debug("No user found that matches Warwick Uni Id:" + warwickUniId);
			return new AnonymousUser();
		}

		for (User user : users) {
			if (user.isWarwickPrimary()) {
				LOGGER.info("Returning primary user of " + users.size()
						+ " users that matches Warwick Uni Id:" + warwickUniId);
				return getUserByUserId(user.getUserId());
			}
		}

		for (User user : users) {
			if (user.getEmail() != null && !user.getEmail().equals("")) {
				LOGGER.info("Returning user with email address (" + user.getUserId() + ") of " + users.size()
						+ " users that matches Warwick Uni Id:" + warwickUniId);
				return getUserByUserId(user.getUserId());
			}
		}

		User user = (User) users.get(0);
		return getUserByUserId(user.getUserId());
	}

	public final List<User> findUsersWithFilter(final Map<String,String> filterValues) {
		return findUsersWithFilter(filterValues, false);
	}


	public final List<User> findUsersWithFilter(final Map<String,String> filterValues, boolean returnDisabledUsers) {
		try {
			return findUsersWithFilterUnsafe(filterValues, returnDisabledUsers);
		} catch (UserLookupException e) {
			LOGGER.warn("findUsersWithFilter failed, returning empty list", e);
			return new ArrayList<User>();
		}
	}

	private List<User> findUsersWithFilterUnsafe(
			final Map<String, String> filterValues, boolean returnDisabledUsers)
			throws UserLookupException {
		List<User> list = new SSOUserLookup(getSsosUrl(), _apiKey).findUsersWithFilter(filterValues, returnDisabledUsers);
		// SSO-1147 don't put these low-detail User objects in cache.
//		final Cache<String, User> cache = getUserByUserIdCache();
//		for (User user : list) {
//			if (user.isFoundUser()) {
//				String userId = user.getUserId();
//				if (userId != null && !"".equals(userId.trim())) {
//					cache.put(new Entry<String,User>(userId, user));
//				}
//			}
//		}
		return list;
	}

	public final String getSsosUrl() {
		// A default is used deeper down
//		if (_ssosUrl == null) {
//			LOGGER.error("About to throw an exception because we don't have the SSO URL");
//			throw new IllegalStateException("No URL to SSO has been specified. Either specify the userlookup.ssosUrl system property, or call userLookup.setSsosUrl(...)");
//		}
		return _ssosUrl;
	}

	/**
	 * @param ssosUrl
	 *            The ssosUrl to set.
	 */
	public final void setSsosUrl(final String ssosUrl) {
		_ssosUrl = ssosUrl;
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
	 */
	public final void setUserIdCacheTimeout(final int userCacheTimeout) {
		//getUserByUserIdCache().setTimeout(userCacheTimeout);
		userIdCacheTimeout  = userCacheTimeout;
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
				_groupService =
						new GroupAliasAwareGroupService(
						new GroupNameCheckerGroupService(
						new UsersInGroupCachingGroupsService(
						new IsUserInGroupCachingGroupsService(
						new GroupByNameCachingGroupsService(
						new GroupsNamesForUserCachingGroupsService(_groupServiceBackend))))),
						this);

			} else {
				LOGGER.warn("Creating DefaultGroupService that returns no groups, because of no userlookup.groupservice.location property");
				_groupService = new GroupAliasAwareGroupService(new GroupNameCheckerGroupService(new DefaultGroupService()), this);
			}
		}
		return _groupService;
	}

	final boolean isUserByUserIdCacheEmpty() {
        try {
		    return _userByUserIdCache.getStatistics().getCacheSize() == 0;
        } catch (CacheStoreUnavailableException e) {
            return true;
        }
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

	public final Map<String, Set<Cache<?, ?>>> getCaches() {
		Map<String, Set<Cache<?, ?>>> caches = new HashMap<String, Set<Cache<?,?>>>();

		return Collections.unmodifiableMap(caches);
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
	 *
	 * @deprecated
	 */
	@Deprecated
	public static void setConfigProperties(Properties props) {
		LOGGER.info("Using configuration from a Properties object instead of System properties");
		configProperties = props;
	}

	@Deprecated
	public static String getConfigProperty(String propertyName) {
		if (defaultProperties == null) {
			synchronized(defaultPropertiesLock) {
				if (defaultProperties == null) {
					defaultProperties = new Properties();
					try {
						defaultProperties.load(UserLookup.class.getResourceAsStream("/default-ssoclient.properties"));
					} catch (IOException e) {
						throw new IllegalStateException("Error reading embedded properties file", e);
					}
				}
			}
		}
		String value = null;
		if (configProperties != null) {
			value = configProperties.getProperty(propertyName);
		}

		if (value == null) {
			value = System.getProperty(propertyName);
		}

		if (value == null) {
			value = defaultProperties.getProperty(propertyName);
		}

		return value;
	}

	@Deprecated
	public static String getConfigProperty(String propertyName, String def) {
		String result = getConfigProperty(propertyName);
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

	public Cache<String, User> getUserByUniIdCache() {
	    return _userByUniIdCache;
    }

	Cache<String, Pair<String, User>> getAuthCache() {
		return _authCache;
	}

	void shutdown() {
		_userByTokenCache.shutdown();
		_userByUserIdCache.shutdown();
		_authCache.shutdown();
	}

	/**
	 * Don't use this in regular code.
	 * <p>
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
	
	public final void requestClearWebGroup(final String groupName) throws UserLookupException {
		getSpecificUserLookupType().requestClearWebGroup(groupName);
	}
}
