/*
 * Created on 18-Mar-2005
 *
 */
package uk.ac.warwick.sso.client;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Iterator;

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
import uk.ac.warwick.userlookup.AnonymousUser;
import uk.ac.warwick.userlookup.User;
import uk.ac.warwick.userlookup.UserLookup;

/**
 * SSOClientFilter gets a User object from the request (via a cookie or a proxyticket) and puts it in the request.
 * 
 * 
 * @author Kieran Shaw
 * 
 */
public final class SSOClientFilter implements Filter {

	private static final String WARWICK_SSO = "WarwickSSO";

	public static final String USER_KEY = "SSO_USER";

	public static final String GLOBAL_LOGIN_COOKIE_NAME = "SSO-LTC";

	public static final String PROXY_TICKET_COOKIE_NAME = "SSO-Proxy";

	private static final Logger LOGGER = Logger.getLogger(SSOClientFilter.class);

	private Configuration _config;

	private AttributeAuthorityResponseFetcher _aaFetcher;

	private UserCache _cache;

	private UserLookup _userLookup;

	private String _configSuffix;

	public SSOClientFilter() {
		super();
	}

	public void init(final FilterConfig ctx) throws ServletException {
		if (ctx.getInitParameter("configsuffix") != null) {
			_configSuffix = ctx.getInitParameter("configsuffix");
		}

		// config is already loaded, probably through spring injection
		if (_config != null) {
			ServletContext servletContext = ctx.getServletContext();
			_config = (Configuration) servletContext.getAttribute(SSOConfigLoader.SSO_CONFIG_KEY + _configSuffix);

			if (_config == null) {
				// try to load the sso config for instances where the Listener cannot be used (e.g. JRun)
				LOGGER.warn("Could not find sso config in servlet context attribute " + SSOConfigLoader.SSO_CONFIG_KEY
						+ _configSuffix + "; attempting to load sso config");
				SSOConfigLoader loader = new SSOConfigLoader();
				loader.loadSSOConfig(servletContext);
				_config = (Configuration) servletContext.getAttribute(SSOConfigLoader.SSO_CONFIG_KEY + _configSuffix);
			}

			if (_config == null) {
				LOGGER.warn("Could not find sso config in servlet context attribute " + SSOConfigLoader.SSO_CONFIG_KEY
						+ _configSuffix);
			} else {
				LOGGER.info("Found sso config");
			}
		}

		// AttributeAuthorityResponseFetcher already loaded, probably through spring injection
		if (getAaFetcher() != null) {
			setAaFetcher(new AttributeAuthorityResponseFetcherImpl(_config));
		}

		// Cache already loaded, probably through spring injection
		if (getCache() != null) {
			setCache((UserCache) ctx.getServletContext().getAttribute(SSOConfigLoader.SSO_CACHE_KEY + _configSuffix));
		}

	}

	public void doFilter(final ServletRequest arg0, final ServletResponse arg1, final FilterChain chain) throws IOException,
			ServletException {
		HttpServletRequest request = (HttpServletRequest) arg0;
		HttpServletResponse response = (HttpServletResponse) arg1;

		SSOConfiguration config = new SSOConfiguration();
		config.setConfig(_config);

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

		if (allowBasic && "true".equals(request.getParameter("forcebasic")) && request.getHeader("Authorization") == null) {
			sendBasicAuthHeaders(response);
			return;
		}

		Cookie[] cookies = request.getCookies();

		if (allowBasic && request.getHeader("Authorization") != null) {
			user = doBasicAuth(request);
		} else if (_config.getString("mode").equals("old") || request.getAttribute(ForceOldModeFilter.ALLOW_OLD_KEY) != null) {
			// do old style single sign on via WarwickSSO cookie
			user = doGetUserByOldSSO(cookies);
		} else {
			// do new style single sign on with shibboleth
			Cookie loginTicketCookie = getCookie(cookies, GLOBAL_LOGIN_COOKIE_NAME);
			Cookie serviceSpecificCookie = getCookie(cookies, _config.getString("shire.sscookie.name"));

			Cookie proxyTicketCookie = getCookie(cookies, PROXY_TICKET_COOKIE_NAME);

			if (loginTicketCookie != null && serviceSpecificCookie == null) {
				redirectToLogin(response, target, loginTicketCookie);
				return;
			}

			if (proxyTicketCookie != null) {
				user = getUserFromProxyTicket(proxyTicketCookie);
			} else if (serviceSpecificCookie != null) {
				LOGGER.debug("Found SSC (" + serviceSpecificCookie.getValue() + ")");

				SSOToken token = new SSOToken(serviceSpecificCookie.getValue(), SSOToken.SSC_TICKET_TYPE);
				UserCacheItem item = getCache().get(token);

				if ((item == null || !item.getUser().isLoggedIn()) && loginTicketCookie != null) {
					redirectToLogin(response, target, loginTicketCookie);
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

		HeaderSettingHttpServletRequest wrappedRequest = new HeaderSettingHttpServletRequest(request);

		putUserAndAttributesInRequest(wrappedRequest, user);

		setOldWarwickSSOToken(user, cookies);

		checkIpAddress(wrappedRequest, user);

		// redirect onto underlying page
		chain.doFilter(wrappedRequest, arg1);

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

		request.setAttribute(userKey, user);

		if (!user.getExtraProperties().isEmpty()) {
			Iterator it = user.getExtraProperties().keySet().iterator();
			while (it.hasNext()) {
				String key = (String) it.next();
				request.setAttribute(userKey + "_" + key, user.getExtraProperty(key));
				request.addHeader(userKey + "_" + key, (String) user.getExtraProperty(key));
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

		SSOConfiguration config = new SSOConfiguration();

		String userKey = getUserKey(config);

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
	public static String getUserKey(SSOConfiguration config) {
		String userKey = null;

		if (config.getConfig() != null) {
			userKey = config.getConfig().getString("shire.filteruserkey");
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

	private User doBasicAuth(final HttpServletRequest request) throws IOException {

		String auth64 = request.getHeader("Authorization");
		LOGGER.info("Doing BASIC auth:" + auth64);
		final int authStartPos = 6;
		auth64 = auth64.substring(authStartPos);
		BASE64Decoder decoder = new BASE64Decoder();
		String auth = new String(decoder.decodeBuffer(auth64.trim()));
		// LOGGER.debug("Doing BASIC auth:" + auth);
		if (auth.indexOf(":") == -1) {
			LOGGER.debug("Returning anon user as auth was invalid: " + auth);
			return new AnonymousUser();
		}
		try {
			String userName = auth.split(":")[0];
			String password = auth.split(":")[1];
			return getUserLookup().getUserByIdAndPassNonLoggingIn(userName, password);
		} catch (Exception e) {
			return new AnonymousUser();
		}

	}

	/**
	 * @param response
	 * @param target
	 * @param loginTicketCookie
	 * @throws UnsupportedEncodingException
	 */
	private void redirectToLogin(final HttpServletResponse response, final URL target, final Cookie loginTicketCookie)
			throws UnsupportedEncodingException {
		response.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);

		response.setHeader("Location", _config.getString("origin.login.location") + "?shire="
				+ URLEncoder.encode(_config.getString("shire.location"), "UTF-8") + "&providerId="
				+ URLEncoder.encode(_config.getString("shire.providerid"), "UTF-8") + "&target="
				+ URLEncoder.encode(target.toExternalForm(), "UTF-8"));

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

	public UserLookup getUserLookup() {

		if (_userLookup == null) {
			return UserLookup.getInstance();
		}

		return _userLookup;
	}

	public void setUserLookup(final UserLookup userLookup) {
		_userLookup = userLookup;
	}

	public Configuration getConfig() {
		return _config;
	}

	public void setConfig(final Configuration config) {
		_config = config;
	}

	public String getConfigSuffix() {
		return _configSuffix;
	}

	public void setConfigSuffix(final String configSuffix) {
		_configSuffix = configSuffix;
	}

}
