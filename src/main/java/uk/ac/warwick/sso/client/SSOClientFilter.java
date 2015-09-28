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
import java.util.HashMap;
import java.util.Map;
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
import org.apache.http.HttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import uk.ac.warwick.util.cache.*;
import uk.ac.warwick.util.core.StringUtils;

import static uk.ac.warwick.userlookup.UserLookup.getConfigProperty;

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

	private static final int BASIC_AUTH_CACHE_TIME_SECONDS = SSOClientHandler.BASIC_AUTH_CACHE_TIME_SECONDS;

	private static final String WARWICK_SSO = SSOClientHandler.WARWICK_SSO;

	public static final String USER_KEY = SSOClientHandler.USER_KEY;

	public static final String GLOBAL_LOGIN_COOKIE_NAME = SSOClientHandler.GLOBAL_LOGIN_COOKIE_NAME;

	public static final String PROXY_TICKET_COOKIE_NAME = SSOClientHandler.PROXY_TICKET_COOKIE_NAME;

	private static final Logger LOGGER = LoggerFactory.getLogger(SSOClientFilter.class);

	private SSOClientHandler handler;

	private SSOConfiguration _config;

	private String _configLocation;

	private String _configSuffix = "";

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

		handler = new SSOClientHandler(_config);
	}

	@Override
	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
		throw new UnsupportedOperationException("Not Implemented");
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

	public void destroy() {
		// don't need to do any destroying
	}

	public void setDetectAnonymousOnCampusUsers(boolean detectAnonymousOnCampusUsers) {
		handler.setDetectAnonymousOnCampusUsers(detectAnonymousOnCampusUsers);
	}

	public boolean isRedirectToRefreshSession() {
		return handler.isRedirectToRefreshSession();
	}

	public void setConfigSuffix(String configSuffix) {
		handler.setConfigSuffix(configSuffix);
	}

	public UserCache getCache() {
		return handler.getCache();
	}

	public void setCache(UserCache cache) {
		handler.setCache(cache);
	}

	public void setUserLookup(UserLookupInterface userLookup) {
		handler.setUserLookup(userLookup);
	}

	public AttributeAuthorityResponseFetcher getAaFetcher() {
		return handler.getAaFetcher();
	}

	public String getConfigSuffix() {
		return handler.getConfigSuffix();
	}

	public UserLookupInterface getUserLookup() {
		return handler.getUserLookup();
	}

	public boolean isDetectAnonymousOnCampusUsers() {
		return handler.isDetectAnonymousOnCampusUsers();
	}

	public void setRedirectToRefreshSession(boolean redirectToRefreshSession) {
		handler.setRedirectToRefreshSession(redirectToRefreshSession);
	}

	public void setConfigLocation(String path) {
		handler.setConfigLocation(path);
	}

	public void setAaFetcher(AttributeAuthorityResponseFetcher aaFetcher) {
		handler.setAaFetcher(aaFetcher);
	}
}
