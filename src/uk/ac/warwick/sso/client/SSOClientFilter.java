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
public class SSOClientFilter implements Filter {

	public final static String USER_KEY = "SSO-USER";

	public static final String GLOBAL_LOGIN_COOKIE_NAME = "SSO-LTC";

	private Configuration _config;

	public SSOClientFilter() {
		super();
	}

	public void init(FilterConfig arg0) throws ServletException {

		_config = (Configuration) arg0.getServletContext().getAttribute(SSOConfigLoader.SSO_CONFIG_KEY);

	}

	public void doFilter(ServletRequest arg0, ServletResponse arg1, FilterChain chain) throws IOException, ServletException {
		HttpServletRequest request = (HttpServletRequest) arg0;
		HttpServletResponse response = (HttpServletResponse) arg1;

		// redirect to login screen if user is already logged in globally, but
		// not yet locally with this service
		Cookie LTC = getCookie(request.getCookies(), GLOBAL_LOGIN_COOKIE_NAME);
		Cookie SSC = getCookie(request.getCookies(), _config.getString("shire.sscookie.name"));
		if (LTC != null && SSC == null) {
			response.setStatus(302);
			response.setHeader("Location", _config.getString("origin.login.location") + "?shire="
					+ URLEncoder.encode(_config.getString("shire.location"), "UTF-8") + "&providerId="
					+ URLEncoder.encode(_config.getString("shire.providerid"), "UTF-8") + "&target="
					+ URLEncoder.encode(request.getRequestURL().toString(), "UTF-8"));
			return;
		}

		// get user from cookie and put in request
		User user = getUserFromRequest(request);
		
		if (!user.isLoggedIn()) {
			// didn't find user, so cookie is invalid, destroy it!
			Cookie cookie = new Cookie(_config.getString("shire.sscookie.name"), "");
			cookie.setPath(_config.getString("shire.sscookie.path"));
			cookie.setDomain(_config.getString("shire.sscookie.domain"));
			cookie.setMaxAge(0);
			response.addCookie(cookie);
		}
		
		request.setAttribute(USER_KEY, user);

		// redirect onto underlying page
		chain.doFilter(arg0, arg1);

	}

	public final User getUserFromRequest(final HttpServletRequest request) {
		Cookie cookie = getCookie(request.getCookies(), _config.getString("shire.sscookie.name"));
		User user = new AnonymousUser();
		if (cookie != null) {
			user = UserLookup.getInstance().getUserByToken(cookie.getValue(), false);
		}
		return user;
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
