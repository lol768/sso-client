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

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
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
import uk.ac.warwick.userlookup.AnonymousUser;
import uk.ac.warwick.userlookup.User;
import uk.ac.warwick.userlookup.UserLookup;
import uk.ac.warwick.userlookup.UserLookupException;

/**
 * SSOClientFilter gets a User object from the request (via a cookie or a proxyticket) and puts it in the request.
 * 
 * 
 * @author Kieran Shaw
 * 
 */
public final class SSOClientFilter implements Filter {

	public static final String USER_KEY = "SSO_USER";

	public static final String GLOBAL_LOGIN_COOKIE_NAME = "SSO-LTC";

	public static final String PROXY_TICKET_COOKIE_NAME = "SSO-Proxy";

	private static final Logger LOGGER = Logger.getLogger(SSOClientFilter.class);

	private Configuration _config;

	private AttributeAuthorityResponseFetcher _aaFetcher;

	private UserCache _cache;

	public SSOClientFilter() {
		super();
	}

	public void init(final FilterConfig ctx) throws ServletException {
		String suffix = "";
		if (ctx.getInitParameter("configsuffix") != null) {
			suffix = ctx.getInitParameter("configsuffix");
		}
		_config = (Configuration) ctx.getServletContext().getAttribute(SSOConfigLoader.SSO_CONFIG_KEY + suffix);

		if (_config == null) {
			LOGGER.warn("Could not find sso config in servlet context attribute " + SSOConfigLoader.SSO_CONFIG_KEY + suffix);
		} else {
			LOGGER.info("Found sso config");
		}

		setAaFetcher(new AttributeAuthorityResponseFetcherImpl(_config));
		setCache((UserCache) ctx.getServletContext().getAttribute(SSOConfigLoader.SSO_CACHE_KEY + suffix));
	}

	

	public void doFilter(final ServletRequest arg0, final ServletResponse arg1, final FilterChain chain) throws IOException,
			ServletException {
		HttpServletRequest request = (HttpServletRequest) arg0;
		HttpServletResponse response = (HttpServletResponse) arg1;

		SSOConfiguration config = new SSOConfiguration();
		config.setConfig(_config);
		
		URL target = getTarget(request);
		
		// prevent ssoclientfilter from sitting in front of shire and logout servlets
		String shireLocation = _config.getString("shire.location");
		String logoutLocation = _config.getString("logout.location");
		if (target.toExternalForm().equals(shireLocation) || target.toExternalForm().equals(logoutLocation)) {
			chain.doFilter(arg0, arg1);
			return;
		}

		User user = new AnonymousUser();

		boolean allowBasic = allowHttpBasic(target);

		if (allowBasic && "true".equals(request.getParameter("forcebasic")) && request.getHeader("Authorization") == null) {
			String authHeader = "Basic realm=\"" + _config.getString("shire.providerid") + "\"";
			LOGGER.info("Client is requesting forcing HTTP Basic Auth, sending WWW-Authenticate=" + authHeader);
			response.setHeader("WWW-Authenticate", authHeader);
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			return;
		}

		Cookie[] cookies = request.getCookies();

		if (allowBasic && request.getHeader("Authorization") != null) {
			user = doBasicAuth(request);
		} else if (_config.getString("mode").equals("old")) {
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
				try {
					SAMLSubject subject = new SAMLSubject();
					SAMLNameIdentifier nameId = new SAMLNameIdentifier(proxyTicketCookie.getValue(), _config
							.getString("origin.originid"), SSOToken.PROXY_TICKET_TYPE);
					subject.setName(nameId);
					LOGGER.info("Trying to get user from proxy cookie:" + nameId);
					user = getAaFetcher().getUserFromSubject(subject);
				} catch (SSOException e) {
					LOGGER.error("Could not get user from proxy cookie", e);
				} catch (SAMLException e) {
					LOGGER.error("Could not get user from proxy cookie", e);
				}

			} else if (serviceSpecificCookie != null) {

				LOGGER.debug("Found SSC (" + serviceSpecificCookie.getValue() + ")");

				SSOToken token = new SSOToken(serviceSpecificCookie.getValue(), SSOToken.SSC_TICKET_TYPE);
				UserCacheItem item = (UserCacheItem) getCache().get(token);

				if ((item == null || !item.getUser().isLoggedIn()) && loginTicketCookie != null) {
					redirectToLogin(response, target, loginTicketCookie);
					// didn't find user, so cookie is invalid, destroy it!
					Cookie cookie = new Cookie(_config.getString("shire.sscookie.name"), "");
					cookie.setPath(_config.getString("shire.sscookie.path"));
					cookie.setDomain(_config.getString("shire.sscookie.domain"));
					cookie.setMaxAge(0);
					response.addCookie(cookie);
					return;
				} else if (item != null && item.getUser().isLoggedIn()) {
					user = item.getUser();
				}

			}
		}

		if (_config.getString("shire.filteruserkey") != null) {
			request.setAttribute(_config.getString("shire.filteruserkey"), user);
		} else {
			request.setAttribute(USER_KEY, user);
		}

		checkIpAddress(request, user);

		// redirect onto underlying page
		chain.doFilter(arg0, arg1);

	}

	/**
	 * @param target
	 * @param allowBasic
	 * @return
	 */
	private boolean allowHttpBasic(final URL target) {		
		
		if (!_config.getBoolean("httpbasic.allow")) {
			return false;
		}
		
		if ("https".equalsIgnoreCase(target.getProtocol()) || target.getHost().equalsIgnoreCase("localhost")) {
			LOGGER.info("HTTP Basic Auth is allowed, target:" + target);
			return true;
		}
		
		return false;
	}
	
	public static User getUserFromRequest(final HttpServletRequest req) {

		SSOConfiguration config = new SSOConfiguration();

		if (config.getConfig() == null) {
			LOGGER.warn("No SSOConfiguration object created, this request probably didn't go through the SSOClientFilter");
			throw new RuntimeException(
					"No SSOConfiguration object created, this request probably didn't go through the SSOClientFilter");
		}

		String userKey = config.getConfig().getString("shire.filteruserkey");

		if (userKey == null) {
			userKey = USER_KEY;
		}
		User user = (User) req.getAttribute(userKey);
		if (user == null) {
			user = new AnonymousUser();
		}

		return user;

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
				LOGGER.info("Users SSOClientFilter request is from same host as they logged in from: SSOClientFilter&Login="
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
		Cookie warwickSSO = getCookie(cookies, "WarwickSSO");
		if (warwickSSO != null) {
			user = UserLookup.getInstance().getUserByToken(warwickSSO.getValue(), false);
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
		LOGGER.info("Doing BASIC auth:" + auth);
		String userName = auth.split(":")[0];
		String password = auth.split(":")[1];

		try {
			return UserLookup.getInstance().getUserByIdAndPassNonLoggingIn(userName, password);
		} catch (UserLookupException e) {
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
		String target = request.getRequestURL().toString();
		if (request.getQueryString() != null) {
			target += "?" + request.getQueryString();
		}
		LOGGER.debug("Target from request.getRequestURL()=" + target);

		String urlParamKey = _config.getString("shire.urlparamkey");
		LOGGER.debug("shire.urlparamkey:" + urlParamKey);
		if (urlParamKey != null && request.getParameter(urlParamKey) != null) {
			target = request.getParameter(urlParamKey);
			String queryString = request.getQueryString().replaceFirst(urlParamKey + "=" + target, "");
			target = target.replaceAll("&&", "&");
			if (queryString != null && !queryString.equals("")) {
				target += "?" + queryString;
			}
			LOGGER.debug("Found target from paramter " + urlParamKey + "=" + target);
		}
		try {
			return new URL(target);
		} catch (MalformedURLException e) {
			LOGGER.warn("Target is an invalid url: " + target);
			return null;
		}
	}

	private Cookie getCookie(final Cookie[] cookies, final String name) {
		if (cookies != null) {
			for (int i = 0; i < cookies.length; i++) {
				Cookie cookie = cookies[i];
				if (cookie.getName().equals(name)) {
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

}
