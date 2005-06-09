/*
 * Created on 18-Mar-2005
 *
 */
package uk.ac.warwick.sso.client;

import java.io.IOException;
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

import uk.ac.warwick.userlookup.AnonymousUser;
import uk.ac.warwick.userlookup.User;
import uk.ac.warwick.userlookup.UserLookup;

/**
 * SSOClientFilter gets a User object from the request (via a cookie or a
 * proxyticket) and puts it in the request.
 * 
 * 
 * @author Kieran Shaw
 * 
 */
public final class SSOClientFilter implements Filter {

	public static final String USER_KEY = "SSO-USER";

	public static final String GLOBAL_LOGIN_COOKIE_NAME = "SSO-LTC";

	private static final Logger LOGGER = Logger.getLogger(SSOClientFilter.class);

	private Configuration _config;

	public SSOClientFilter() {
		super();
	}

	public void init(final FilterConfig arg0) throws ServletException {

		_config = (Configuration) arg0.getServletContext().getAttribute(SSOConfigLoader.SSO_CONFIG_KEY);

	}

	public void doFilter(final ServletRequest arg0, final ServletResponse arg1, final FilterChain chain) throws IOException, ServletException {
		HttpServletRequest request = (HttpServletRequest) arg0;
		HttpServletResponse response = (HttpServletResponse) arg1;

		// redirect to login screen if user is already logged in globally, but
		// not yet locally with this service
		Cookie loginTicketCookie = getCookie(request.getCookies(), GLOBAL_LOGIN_COOKIE_NAME);
		Cookie serviceSpecificCookie = getCookie(request.getCookies(), _config.getString("shire.sscookie.name"));

		User user = new AnonymousUser();

		if (loginTicketCookie != null && serviceSpecificCookie == null) {

			response.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);

			String target = getTarget(request);

			response.setHeader("Location", _config.getString("origin.login.location") + "?shire="
					+ URLEncoder.encode(_config.getString("shire.location"), "UTF-8") + "&providerId="
					+ URLEncoder.encode(_config.getString("shire.providerid"), "UTF-8") + "&target="
					+ URLEncoder.encode(target, "UTF-8"));

			LOGGER.debug("Found global login cookie (" + loginTicketCookie.getValue() + "), but not SSC, redirecting to Handle Service "
					+ _config.getString("origin.login.location"));

			return;
		} else if (serviceSpecificCookie != null) {

			LOGGER.debug("Found SSC (" + serviceSpecificCookie.getValue() + ")");

			// get user from cookie and put in request
			user = UserLookup.getInstance().getUserByToken(serviceSpecificCookie.getValue(), false);

			if (!user.isLoggedIn()) {
				LOGGER.debug("Didn't find user from SSC (" + serviceSpecificCookie.getValue() + "), so invalidating SSC");
				// didn't find user, so cookie is invalid, destroy it!
				Cookie cookie = new Cookie(_config.getString("shire.sscookie.name"), "");
				cookie.setPath(_config.getString("shire.sscookie.path"));
				cookie.setDomain(_config.getString("shire.sscookie.domain"));
				cookie.setMaxAge(0);
				response.addCookie(cookie);
			}
		}

		request.setAttribute(USER_KEY, user);

		// redirect onto underlying page
		chain.doFilter(arg0, arg1);

	}

	/**
	 * @param request
	 * @return
	 */
	private String getTarget(final HttpServletRequest request) {
		String target = request.getRequestURL().toString();

		String urlParamKey = _config.getString("shire.urlparamkey");
		if (urlParamKey != null) {
			// try looking in the request for the real url of this page
			if (request.getParameter(urlParamKey) != null) {
				target = request.getParameter(urlParamKey);
			} else {
				target = request.getScheme() + "://" + request.getServerName() + request.getRequestURI();
				if (request.getQueryString() != null) {
					target += "?" + request.getQueryString();
				}
			}

		}
		return target;
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

}
