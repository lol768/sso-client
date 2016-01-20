/*
 * Created on 18-Mar-2005
 *
 */
package uk.ac.warwick.sso.client;

import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map.Entry;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.configuration.Configuration;
import org.apache.log4j.Logger;
import org.opensaml.SAMLException;
import org.opensaml.SAMLNameIdentifier;
import org.opensaml.SAMLSubject;

import sun.misc.BASE64Decoder;
import uk.ac.warwick.sso.client.cache.UserCache;
import uk.ac.warwick.sso.client.cache.UserCacheItem;
import uk.ac.warwick.sso.client.tags.SSOLoginLinkGenerator;
import uk.ac.warwick.userlookup.AnonymousOnCampusUser;
import uk.ac.warwick.userlookup.AnonymousUser;
import uk.ac.warwick.userlookup.User;
import uk.ac.warwick.userlookup.UserLookupException;
import uk.ac.warwick.userlookup.UserLookupFactory;
import uk.ac.warwick.userlookup.UserLookupInterface;
import uk.ac.warwick.userlookup.cache.Cache;
import uk.ac.warwick.userlookup.cache.Caches;
import uk.ac.warwick.userlookup.cache.EntryUpdateException;
import uk.ac.warwick.userlookup.cache.SingularEntryFactory;

/**
 * SSOClientFilter is responsible for checking cookies for an existing session,
 * looking up the details of the user if one is found, and placing this User object
 * into a request attribute. Even if no user is found, it will place an AnonymousUser
 * object in the request.
 * <p>
 * It is generally appropriate to map this filter to run for all your application requests,
 * i.e. mapped to "/*". It will know not to run when your ShireServlet or LogoutServlet are
 * requested, as long as these are specified correctly in your sso-config.xml file. 
 * <p>
 * {@link SSOClientFilter#getUserFromRequest(HttpServletRequest)} can be used to conveniently
 * fetch the current User object from the appropriate request attribute.
 */
public final class SSOClientFilter implements Filter {

	private static final int BASIC_AUTH_CACHE_TIME_SECONDS = 300;

	private static final String WARWICK_SSO = "WarwickSSO";

	public static final String USER_KEY = "SSO_USER";

	public static final String GLOBAL_LOGIN_COOKIE_NAME = "SSO-LTC";

	public static final String PROXY_TICKET_COOKIE_NAME = "SSO-Proxy";

	private static final Logger LOGGER = Logger.getLogger(SSOClientFilter.class);

	private SSOConfiguration _config;

	private AttributeAuthorityResponseFetcher _aaFetcher;

	private UserCache _cache;

	private UserLookupInterface _userLookup;
	
	private Cache<String, UserAndHash> _basicAuthCache;

	private String _configSuffix = "";

	private boolean detectAnonymousOnCampusUsers;
	
	private boolean redirectToRefreshSession = true;

	private String _configLocation;

	public SSOClientFilter() {
		super();
	}

	public void init(final FilterConfig ctx) throws ServletException {
		if (ctx.getInitParameter("configsuffix") != null) {
			_configSuffix = ctx.getInitParameter("configsuffix");
		}

		// config is already loaded, probably through spring injection
		if (_config == null) {
			ServletContext servletContext = ctx.getServletContext();
			_config = (SSOConfiguration) servletContext.getAttribute(SSOConfigLoader.SSO_CONFIG_KEY + _configSuffix);

			if (_config == null) {
				// try to load the sso config for instances where the Listener cannot be used (e.g. JRun)
				LOGGER.warn("Could not find sso config in servlet context attribute " + SSOConfigLoader.SSO_CONFIG_KEY
						+ _configSuffix + "; attempting to load sso config");
				SSOConfigLoader loader = new SSOConfigLoader();
				if (_configLocation != null) {
					LOGGER.info("Loading from location " + _configLocation);
					_config = loader.loadSSOConfig(_configLocation);
					loader.storeConfig(servletContext, _configSuffix, _config);
				} else {
					loader.loadSSOConfig(servletContext);
					_config = (SSOConfiguration) servletContext.getAttribute(SSOConfigLoader.SSO_CONFIG_KEY + _configSuffix);
				}
				
			}

			if (_config == null) {
				LOGGER.warn("Could not find sso config in servlet context attribute " + SSOConfigLoader.SSO_CONFIG_KEY
						+ _configSuffix);
			} else {
				LOGGER.info("Found sso config");
			}
		}
		
		if (_config != null) {
			runSanityCheck(_config);
		}

		// AttributeAuthorityResponseFetcher already loaded, probably through spring injection
		if (getAaFetcher() == null) {
			setAaFetcher(new AttributeAuthorityResponseFetcherImpl(_config));
		}

		// Cache already loaded, probably through spring injection
		if (getCache() == null) {
			setCache((UserCache) ctx.getServletContext().getAttribute(SSOConfigLoader.SSO_CACHE_KEY + _configSuffix));
		}

	}

	private void runSanityCheck(Configuration config) {
		String loginLocation = config.getString("origin.login.location");
		String mode = config.getString("mode");
		
		if (mode.equals("old") && loginLocation.contains("/hs")) {
			LOGGER.warn("Possible misconfiguration: you are using old mode but the login location contains /hs, which is for new mode. Do you mean /slogin?");
		} else if (mode.equals("new") && loginLocation.contains("/slogin")) {
			LOGGER.warn("Possible misconfiguration: you are using new mode but the login location contains /slogin, which is for old mode. Do you mean /hs?");
		}
	}

	public void doFilter(final ServletRequest arg0, final ServletResponse arg1, final FilterChain chain) throws IOException,
			ServletException {
		HttpServletRequest request = (HttpServletRequest) arg0;
		HttpServletResponse response = (HttpServletResponse) arg1;

		SSOConfiguration.setConfig(_config);
		try {
    		URL target = getTarget(request);
    		LOGGER.debug("Target=" + target);
    
    		// prevent ssoclientfilter from sitting in front of shire and logout servlets
    		String shireLocation = _config.getString("shire.location");
    		String logoutLocation = _config.getString("logout.location");
    
    		if (LOGGER.isDebugEnabled()) {
    			LOGGER.debug("shire.location=" + shireLocation);
    			LOGGER.debug("logout.location=" + logoutLocation);
    		}
    
    		if (target.toExternalForm().equals(shireLocation) || target.toExternalForm().equals(logoutLocation)) {
    			LOGGER.debug("Letting request through without filtering because it is a shire or logout request");
    			chain.doFilter(arg0, arg1);
    			return;
    		}
    
    		User user = new AnonymousUser();
    
    		boolean allowBasic = allowHttpBasic(target, request);
    		
    		/* [SSO-550] These variables are for handling sending WarwickSSO as a parameter,
    		 * useful in limited cases like Flash apps who can't send cookies 
    		 */
    		String requestToken = request.getParameter(WARWICK_SSO);
    		boolean postCookies = ("POST".equalsIgnoreCase(request.getMethod()) && requestToken != null);
    
    		if (allowBasic && "true".equals(request.getParameter("forcebasic")) && !isBasicAuthRequest(request)) {
    			sendBasicAuthHeaders(response);
    			return;
    		}
    
    		Cookie[] cookies = request.getCookies();
    
    		if (allowBasic && isBasicAuthRequest(request)) {
    			user = doBasicAuth(request);
    		} else if (_config.getString("mode").equals("old") || request.getAttribute(ForceOldModeFilter.ALLOW_OLD_KEY) != null) {
    			// do old style single sign on via WarwickSSO cookie
    			user = doGetUserByOldSSO(cookies);
    		} else if (postCookies) {
    			user = getUserLookup().getUserByToken(requestToken);
    		} else {
    			// do new style single sign on with shibboleth
    			Cookie loginTicketCookie = getCookie(cookies, GLOBAL_LOGIN_COOKIE_NAME);
    			Cookie serviceSpecificCookie = getCookie(cookies, _config.getString("shire.sscookie.name"));
    
    			Cookie proxyTicketCookie = getCookie(cookies, PROXY_TICKET_COOKIE_NAME);
    
    			if (loginTicketCookie != null && serviceSpecificCookie == null) {
    				if (redirectToRefreshSession) {
    					redirectToLogin(response, request, loginTicketCookie);
    					return;
    				}
    			} else {
    				if (proxyTicketCookie != null) {
    					user = getUserFromProxyTicket(proxyTicketCookie);
    				} else if (serviceSpecificCookie != null) {
    					LOGGER.debug("Found SSC (" + serviceSpecificCookie.getValue() + ")");
    	
    					SSOToken token = new SSOToken(serviceSpecificCookie.getValue(), SSOToken.SSC_TICKET_TYPE);
    					UserCacheItem item = getCache().get(token);
    	
    					if (redirectToRefreshSession && (item == null || !item.getUser().isLoggedIn()) && loginTicketCookie != null) {
    						redirectToLogin(response, request, loginTicketCookie);
    						// didn't find user, so cookie is invalid, destroy it!
    						destroySSC(response);
    						return;
    					} else if (item != null && item.getUser().isLoggedIn()) {
    						user = item.getUser();
    					} else {
    						// user has SSC but is not actually logged in
    						LOGGER.debug("Invalid SSC as user was not found in cache");
    					}
    	
    				}
    			}
    		}
    		
    		user = handleOnCampusUsers(user, request);
    
    		HeaderSettingHttpServletRequest wrappedRequest = new HeaderSettingHttpServletRequest(request);
    
    		putUserAndAttributesInRequest(wrappedRequest, user);
    
    		setOldWarwickSSOToken(user, cookies);
    
    		checkIpAddress(wrappedRequest, user);
    
    		// redirect onto underlying page
    		chain.doFilter(wrappedRequest, arg1);
		} finally {
            // SSO-1489
		    //SSOConfiguration.removeConfig();
		}
	}

	/**
	 * If user is anonymous and they are on campus, replace the instance of User
	 * with an AnonymousOnCampusUser. It extends AnonymousUser so it won't affect
	 * current code, but you can use instanceof to check if they're on campus.
	 */
	private User handleOnCampusUsers(User user, HttpServletRequest request) {
		if (detectAnonymousOnCampusUsers) {
			if (getUserLookup().getOnCampusService().isOnCampus(request)) {
				if (user instanceof AnonymousUser) {
					return new AnonymousOnCampusUser();
				}
				user.setOnCampus(true);
			}
		}
		return user;
	}

	/**
	 * @param request
	 * @param user
	 */
	private void putUserAndAttributesInRequest(final HeaderSettingHttpServletRequest request, final User user) {

		String userKey = _config.getString("shire.filteruserkey");
		if (userKey == null) {
			userKey = USER_KEY;
		}

		request.setRemoteUser(user.getUserId());

		request.setAttribute(userKey, user);

		if (!user.getExtraProperties().isEmpty()) {
			for (Entry<String,String> entry : user.getExtraProperties().entrySet()) {
				String key = entry.getKey();
				String value = entry.getValue();
				request.setAttribute(userKey + "_" + key, value);
				request.addHeader(userKey + "_" + key, value);
			}
		}

		request.addHeader(userKey + "_groups", "");
	}

	/**
	 * @param user
	 * @param cookies
	 */
	private void setOldWarwickSSOToken(final User user, final Cookie[] cookies) {
		// set the old WarwickSSO token for legacy reasons
		if (user.getOldWarwickSSOToken() == null) {
			Cookie warwickSSO = getCookie(cookies, WARWICK_SSO);
			if (warwickSSO != null) {
				user.setOldWarwickSSOToken(warwickSSO.getValue());
			}
		}
	}

	/**
	 * @param response
	 */
	private void destroySSC(final HttpServletResponse response) {
		Cookie cookie = new Cookie(_config.getString("shire.sscookie.name"), "");
		cookie.setPath(_config.getString("shire.sscookie.path"));
		cookie.setDomain(_config.getString("shire.sscookie.domain"));
		cookie.setMaxAge(0);
		response.addCookie(cookie);
	}

	/**
	 * @param response
	 */
	private void sendBasicAuthHeaders(final HttpServletResponse response) {
		String authHeader = "Basic realm=\"" + _config.getString("shire.providerid") + "\"";
		LOGGER.info("Client is requesting forcing HTTP Basic Auth, sending WWW-Authenticate=" + authHeader);
		response.setHeader("WWW-Authenticate", authHeader);
		response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
	}

	/**
	 * @param user
	 * @param proxyTicketCookie
	 * @return
	 */
	private User getUserFromProxyTicket(final Cookie proxyTicketCookie) {
		User user = new AnonymousUser();
		try {
			SAMLSubject subject = new SAMLSubject();
			SAMLNameIdentifier nameId = new SAMLNameIdentifier(proxyTicketCookie.getValue(),
					_config.getString("origin.originid"), SSOToken.PROXY_TICKET_TYPE);
			subject.setName(nameId);
			LOGGER.info("Trying to get user from proxy cookie:" + nameId);
			user = getAaFetcher().getUserFromSubject(subject);
		} catch (SSOException e) {
			LOGGER.error("Could not get user from proxy cookie", e);
		} catch (SAMLException e) {
			LOGGER.error("Could not get user from proxy cookie", e);
		}
		return user;
	}

	/**
	 * @param target
	 * @param allowBasic
	 * @return
	 */
	private boolean allowHttpBasic(final URL target, final HttpServletRequest request) {

		if (!_config.getBoolean("httpbasic.allow")) {
			return false;
		}

		boolean jBossLocalhost = false;
		boolean hasXForwardedFor = false;
		boolean jBossSSL = false;

		URL realURL = null;
		try {
			realURL = new URL(request.getRequestURL().toString());
			if (realURL.getHost().equalsIgnoreCase("localhost") || realURL.getHost().equalsIgnoreCase("localhost.warwick.ac.uk")) {
				jBossLocalhost = true;
			}
			if (realURL.getProtocol().equalsIgnoreCase("https")) {
				jBossSSL = true;
			}
		} catch (MalformedURLException e) {
			throw new RuntimeException("Could not make URL out of:" + request.getRequestURI());
		}

		if (request.getHeader("x-forwarded-for") != null) {
			hasXForwardedFor = true;
		}

		if (hasXForwardedFor) {
			// was proxied...probably
			if ("https".equalsIgnoreCase(target.getProtocol()) || target.getHost().equalsIgnoreCase("localhost")
					|| target.getHost().equalsIgnoreCase("localhost.warwick.ac.uk")) {
				LOGGER.debug("HTTP Basic Auth is allowed because it is proxied but has a sensible target:" + target);
				return true;
			}
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("HTTP Basic Auth NOT allowed because it is proxied but does NOT have sensible target:" + target);
			}

		} else {
			// was not proxied...probably
			if (jBossSSL || jBossLocalhost) {
				LOGGER.debug("HTTP Basic Auth is allowed because jboss is running on localhost or SSL and is not proxied");
				return true;
			}
			if (LOGGER.isDebugEnabled()) {
				LOGGER
						.debug("HTTP Basic Auth is NOT allowed because jboss is NOT running on localhost or SSL and is not proxied");
			}
		}

		return false;
	}

	/**
	 * Will always return an AnonymousUser if there is either no config or no user in the request
	 * 
	 * @param req
	 * @return
	 */
	public static User getUserFromRequest(final HttpServletRequest req) {

		String userKey = getUserKey();

		User user = (User) req.getAttribute(userKey);
		if (user == null) {
			LOGGER.warn("No user, not even an AnonymousUser found in the request, so returning a new AnonymousUser");
			user = new AnonymousUser();
		}

		return user;

	}

	/**
	 * @param config
	 * @return
	 */
	public static String getUserKey() {
		String userKey = null;

		if (SSOConfiguration.getConfig() != null) {
			userKey = SSOConfiguration.getConfig().getString("shire.filteruserkey");
			// throw new RuntimeException(
			// "No SSOConfiguration object created, this request probably didn't go through the SSOClientFilter");
		} else {
			LOGGER.warn("No SSOConfiguration object found, this request probably didn't go through the SSOClientFilter");
		}

		if (userKey == null) {
			userKey = USER_KEY;
		}
		return userKey;
	}

	/**
	 * @param request
	 * @param user
	 */
	private void checkIpAddress(final HttpServletRequest request, final User user) {
		String remoteHost = request.getRemoteHost();
		if (request.getHeader("x-forwarded-for") != null) {
			remoteHost = request.getHeader("x-forwarded-for");
		}

		if (user.getExtraProperty("urn:websignon:ipaddress") != null) {
			if (user.getExtraProperty("urn:websignon:ipaddress").equals(remoteHost)) {
				LOGGER.debug("Users SSOClientFilter request is from same host as they logged in from: SSOClientFilter&Login="
						+ remoteHost);
			} else {
				LOGGER.warn("Users SSOClientFilter request is NOT from same host as they logged in from. Login="
						+ user.getExtraProperty("urn:websignon:ipaddress") + ", SSOClientFilter=" + remoteHost);
			}
		}
	}

	/**
	 * @param cookies
	 * @param user
	 * @return
	 */
	private User doGetUserByOldSSO(final Cookie[] cookies) {
		User user = new AnonymousUser();
		Cookie warwickSSO = getCookie(cookies, WARWICK_SSO);
		if (warwickSSO != null) {
			user = getUserLookup().getUserByToken(warwickSSO.getValue());
		}
		return user;
	}

	private boolean isBasicAuthRequest(final HttpServletRequest request) {
		String header = request.getHeader("Authorization");
		return header != null && header.startsWith("Basic ");
	}

	private User doBasicAuth(final HttpServletRequest request) throws IOException {

		String auth64 = request.getHeader("Authorization");
		LOGGER.info("Doing BASIC auth:" + auth64);
		final int authStartPos = 6;
		auth64 = auth64.substring(authStartPos);
		BASE64Decoder decoder = new BASE64Decoder();
		String auth = new String(decoder.decodeBuffer(auth64.trim()));
		
		if (auth.indexOf(":") == -1) {
			LOGGER.debug("Returning anon user as auth was invalid: " + auth);
			return new AnonymousUser();
		}
		try {
			// self-populating cache makes the actual request to wsos. see #getBasicAuthCache
			String[] split = auth.split(":");
			if (split.length != 2) {
				throw new IllegalArgumentException("Malformed auth string - wrong number of colons.");
			}
			String userName = split[0];
			String password = split[1];
			
			UserAndHash userAndHash = getBasicAuthCache().get(userName, password);
			User user = userAndHash.getUser();
			String hash = userAndHash.getHash();
			
			if (hash != null && !SaltedDigest.matches(hash, password)) {
				// entry was previously valid, but pass doesn't match.
				getBasicAuthCache().remove(userName);
				userAndHash = getBasicAuthCache().get(userName, password);
				user = userAndHash.getUser();
				// don't need to check pass hash because it's just been generated based on a response.
			}
			
			return user;
			
		} catch (Exception e) {
			LOGGER.warn("Exception making basic auth request - using anonymous user", e);
			return new AnonymousUser();
		}

	}

	private User authUserWithCache(String userName, String password)
			throws EntryUpdateException {
		UserAndHash userAndHash = getBasicAuthCache().get(userName);
		User user = userAndHash.getUser();
		String hash = userAndHash.getHash();
		
		if (hash == null || !SaltedDigest.matches(hash, password)) {
			user = null;
		}
		return user;
	}

	private Cache<String, UserAndHash> getBasicAuthCache() {
		if (_basicAuthCache == null) {
			_basicAuthCache = Caches.newCache("BasicAuthCache", new SingularEntryFactory<String, UserAndHash>() {
				public UserAndHash create(String userName, Object data) throws EntryUpdateException {
					String password = (String) data;
					try {
						User user = getUserLookup().getUserByIdAndPassNonLoggingIn(userName, password);
						String hash = null;
						if (user.isFoundUser()) {
							hash = SaltedDigest.generate(password);
						}
						return new UserAndHash(user, hash);
					} catch (UserLookupException e) {
						throw new EntryUpdateException(e);
					}
				}
				// only cache fully sucessful requests
				public boolean shouldBeCached(UserAndHash uah) {
					return uah.getUser().isFoundUser();
				}
			}, BASIC_AUTH_CACHE_TIME_SECONDS);
		}
		return _basicAuthCache;
	}
	
	static class UserAndHash implements Serializable {
		private static final long serialVersionUID = 4707495652163070391L;
		private final User user;
		private final String hash;
		public UserAndHash(User user, String hash) {
			super();
			this.user = user;
			this.hash = hash;
		}
		public User getUser() {
			return user;
		}
		public String getHash() {
			return hash;
		}
	}

	/**
	 * @param response
	 * @param target
	 * @param loginTicketCookie
	 * @throws UnsupportedEncodingException
	 */
	private void redirectToLogin(final HttpServletResponse response, final HttpServletRequest request,
			final Cookie loginTicketCookie) throws UnsupportedEncodingException, IOException {

		SSOLoginLinkGenerator generator = new SSOLoginLinkGenerator();
		generator.setRequest(request);
		String loginLink = generator.getLoginUrl();
		response.sendRedirect(loginLink);

		LOGGER.debug("Found global login cookie (" + loginTicketCookie.getValue()
				+ "), but not a valid SSC, redirecting to Handle Service " + _config.getString("origin.login.location"));
	}

	/**
	 * @param request
	 * @return
	 */
	private URL getTarget(final HttpServletRequest request) {

		SSOLoginLinkGenerator generator = new SSOLoginLinkGenerator();
		generator.setRequest(request);
		try {
			return new URL(generator.getTarget());
		} catch (MalformedURLException e) {
			LOGGER.warn("Target is an invalid url", e);
			throw new RuntimeException("Target is an invalid url", e);
		}

	}

	private Cookie getCookie(final Cookie[] cookies, final String name) {
		if (cookies != null) {
			for (int i = 0; i < cookies.length; i++) {
				Cookie cookie = cookies[i];
				if (cookie.getName().equals(name)) {
					LOGGER.debug("Found cookie:" + name + "=" + cookie.getValue());
					return cookie;
				}
			}
		}
		return null;
	}

	public void destroy() {
		// don't need to do any destroying
	}

	public AttributeAuthorityResponseFetcher getAaFetcher() {
		return _aaFetcher;
	}

	public void setAaFetcher(final AttributeAuthorityResponseFetcher aaFetcher) {
		_aaFetcher = aaFetcher;
	}

	public UserCache getCache() {
		return _cache;
	}

	public void setCache(final UserCache cache) {
		_cache = cache;
	}

	public UserLookupInterface getUserLookup() {
		if (_userLookup == null) {
			_userLookup = UserLookupFactory.getInstance();
		}
		return _userLookup;
	}

	public void setUserLookup(final UserLookupInterface userLookup) {
		_userLookup = userLookup;
	}

	public SSOConfiguration getConfig() {
		return _config;
	}

	public void setConfig(final SSOConfiguration config) {
		_config = config;
	}
	
	public void setConfigLocation(final String path) {
		this._configLocation = path;
	}

	public String getConfigSuffix() {
		return _configSuffix;
	}

	public void setConfigSuffix(final String configSuffix) {
		_configSuffix = configSuffix;
	}

	public boolean isDetectAnonymousOnCampusUsers() {
		return detectAnonymousOnCampusUsers;
	}

	public void setDetectAnonymousOnCampusUsers(boolean detectAnonymousOnCampusUsers) {
		this.detectAnonymousOnCampusUsers = detectAnonymousOnCampusUsers;
	}
	

	/**
	 * If true (the default), then if the filter finds cookies relating to an existing session that is
	 * no longer valid, it will redirect to Websignon to get those cookies refreshed. You may want to disable
	 * this for places where you wish to know about a user who might be logged in, but don't ever want
	 * SSOClient to automatically redirect the user agent away. In these cases, an AnonymousUser will
	 * be returned instead.
	 */
	public boolean isRedirectToRefreshSession() {
		return redirectToRefreshSession;
	}

	/**
	 * @see #isRedirectToRefreshSession()
	 */
	public void setRedirectToRefreshSession(boolean redirectToRefreshSession) {
		this.redirectToRefreshSession = redirectToRefreshSession;
	}

}
