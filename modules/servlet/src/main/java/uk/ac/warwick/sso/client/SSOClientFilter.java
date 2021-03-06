/*
 * Created on 18-Mar-2005
 *
 */
package uk.ac.warwick.sso.client;

import java.io.IOException;
import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.warwick.sso.client.cache.UserCache;
import uk.ac.warwick.sso.client.core.*;
import uk.ac.warwick.sso.client.core.OnCampusService;
import uk.ac.warwick.userlookup.*;

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
public final class SSOClientFilter extends HandleFilter implements Filter {

	private static final int BASIC_AUTH_CACHE_TIME_SECONDS = SSOClientHandlerImpl.BASIC_AUTH_CACHE_TIME_SECONDS;

	private static final String WARWICK_SSO = SSOClientHandlerImpl.WARWICK_SSO;

	public static final String USER_KEY = SSOClientHandlerImpl.USER_KEY;

	public static final String ACTUAL_USER_KEY = SSOClientHandlerImpl.ACTUAL_USER_KEY;

	public static final String GLOBAL_LOGIN_COOKIE_NAME = SSOClientHandlerImpl.GLOBAL_LOGIN_COOKIE_NAME;

	public static final String PROXY_TICKET_COOKIE_NAME = SSOClientHandlerImpl.PROXY_TICKET_COOKIE_NAME;

	private static final Logger LOGGER = LoggerFactory.getLogger(SSOClientFilter.class);

	private SSOClientHandler handler;

	private SSOConfiguration _config;

	private String _configLocation;

	private String _configSuffix = "";

	private UserLookupInterface _userLookup;

	private boolean detectAnonymousOnCampusUsers;

	private boolean redirectToRefreshSession = true;

	private AttributeAuthorityResponseFetcher _aaFetcher;

	public SSOClientFilter() {
		super();
	}

	public void init(final FilterConfig ctx) throws ServletException {
		if (ctx.getInitParameter("configsuffix") != null) {
			setConfigSuffix(ctx.getInitParameter("configsuffix"));
		}

		// config is already loaded, probably through spring injection
		if (_config == null) {
			ServletContext servletContext = ctx.getServletContext();
			_config = ((SSOConfiguration) servletContext.getAttribute(SSOConfigLoader.SSO_CONFIG_KEY + _configSuffix));

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

		// Handler may already exist through Spring injection
		if (handler == null) {
			ServletContext servletContext = ctx.getServletContext();
			UserCache userCache = (UserCache) servletContext.getAttribute(SSOConfigLoader.SSO_CACHE_KEY + _configSuffix);

			// legacy campus service contains a core instance, so pull it out
			OnCampusService coreCampusService = getUserLookup().getOnCampusService();

			handler = new SSOClientHandlerImpl(_config, getUserLookup(), userCache, coreCampusService);

			handler.setDetectAnonymousOnCampusUsers(detectAnonymousOnCampusUsers);
			handler.setRedirectToRefreshSession(redirectToRefreshSession);

			if (_aaFetcher != null) handler.setAaFetcher(_aaFetcher);
		}
	}

	@Override
	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
		filterWithHandler((HttpServletRequest) servletRequest, (HttpServletResponse) servletResponse, filterChain);
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

	public static String getUserKey() {
		String userKey = USER_KEY;

		if (SSOConfiguration.getConfig() != null) {
			userKey = getUserKey(SSOConfiguration.getConfig());
		} else {
			LOGGER.warn("No SSOConfiguration object found, this request probably didn't go through the SSOClientFilter");
		}

		return userKey;
	}

	private static String getUserKey(SSOConfiguration config) {
		return config.getString("shire.filteruserkey", USER_KEY);
	}

	private static String getActualUserKey(SSOConfiguration config) {
		return config.getString("shire.filteractualuserkey", ACTUAL_USER_KEY);
	}

	@Override
	public SSOConfiguration getConfig() {
	    return _config;
	}

	@Override
	public SSOHandler getHandler() {
	    return handler;
	}

	public void destroy() {
		// don't need to do any destroying
	}

	public void setDetectAnonymousOnCampusUsers(boolean detectAnonymousOnCampusUsers) {
		this.detectAnonymousOnCampusUsers = detectAnonymousOnCampusUsers;
	}

	public void setConfigSuffix(String configSuffix) {
		_configSuffix = configSuffix;
	}

	public void setHandler(SSOClientHandler handler) {
		this.handler = handler;
	}

	public void setUserLookup(UserLookupInterface userLookup) {
		_userLookup = userLookup;
	}

	public AttributeAuthorityResponseFetcher getAaFetcher() {
		return handler.getAaFetcher();
	}

	public String getConfigSuffix() {
		return _configSuffix;
	}

	public UserLookupInterface getUserLookup() {
		if (_userLookup == null) {
			_userLookup = UserLookupFactory.getInstance();
		}
		return _userLookup;
	}

	public void setRedirectToRefreshSession(boolean redirectToRefreshSession) {
		this.redirectToRefreshSession = redirectToRefreshSession;
	}

	public void setConfigLocation(String path) {
		_configLocation = path;
	}

	public void setAaFetcher(AttributeAuthorityResponseFetcher aaFetcher) {
		this._aaFetcher = aaFetcher;
	}

	public void setConfig(SSOConfiguration _config) {
		this._config = _config;
	}
}
