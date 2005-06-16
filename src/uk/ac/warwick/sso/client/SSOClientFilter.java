/*
 * Created on 18-Mar-2005
 *
 */
package uk.ac.warwick.sso.client;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
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

import uk.ac.warwick.sso.client.cache.UserCache;
import uk.ac.warwick.sso.client.cache.UserCacheItem;
import uk.ac.warwick.userlookup.AnonymousUser;
import uk.ac.warwick.userlookup.User;

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

	public static final String PROXY_TICKET_COOKIE_NAME = "SSO-Proxy";

	private static final Logger LOGGER = Logger.getLogger(SSOClientFilter.class);

	private Configuration _config;

	private AttributeAuthorityResponseFetcher _aaFetcher;
	
	private UserCache _cache;

	public SSOClientFilter() {
		super();
	}

	public void init(final FilterConfig ctx) throws ServletException {
		_config = (Configuration) ctx.getServletContext().getAttribute(SSOConfigLoader.SSO_CONFIG_KEY);
		setAaFetcher(new AttributeAuthorityResponseFetcherImpl(_config));
		setCache((UserCache) ctx.getServletContext().getAttribute(SSOConfigLoader.SSO_CACHE_KEY));
	}

	public void doFilter(final ServletRequest arg0, final ServletResponse arg1, final FilterChain chain) throws IOException,
			ServletException {
		HttpServletRequest request = (HttpServletRequest) arg0;
		HttpServletResponse response = (HttpServletResponse) arg1;
		response.setContentType("text/html");

		Cookie[] cookies = request.getCookies();
		String target = getTarget(request);

		User user = new AnonymousUser();

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

			// get user from cookie and put in request
			//user = UserLookup.getInstance().getUserByToken(serviceSpecificCookie.getValue(), false);
			SSOToken token = new SSOToken(serviceSpecificCookie.getValue(),SSOToken.SSC_TICKET_TYPE);
			UserCacheItem item = (UserCacheItem) getCache().get(token);

			if (item == null || !item.getUser().isLoggedIn() && loginTicketCookie != null) {
				redirectToLogin(response, target, loginTicketCookie);
				// didn't find user, so cookie is invalid, destroy it!
				Cookie cookie = new Cookie(_config.getString("shire.sscookie.name"), "");
				cookie.setPath(_config.getString("shire.sscookie.path"));
				cookie.setDomain(_config.getString("shire.sscookie.domain"));
				cookie.setMaxAge(0);
				response.addCookie(cookie);
				return;
			}
			user = item.getUser();

		}

		if (_config.getString("shire.filteruserkey") != null) {
			request.setAttribute(_config.getString("shire.filteruserkey"), user);
		} else {
			request.setAttribute(USER_KEY, user);
		}

		// redirect onto underlying page
		chain.doFilter(arg0, arg1);

	}

	/**
	 * @param response
	 * @param target
	 * @param loginTicketCookie
	 * @throws UnsupportedEncodingException
	 */
	private void redirectToLogin(final HttpServletResponse response, final String target, final Cookie loginTicketCookie)
			throws UnsupportedEncodingException {
		response.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);

		response.setHeader("Location", _config.getString("origin.login.location") + "?shire="
				+ URLEncoder.encode(_config.getString("shire.location"), "UTF-8") + "&providerId="
				+ URLEncoder.encode(_config.getString("shire.providerid"), "UTF-8") + "&target="
				+ URLEncoder.encode(target, "UTF-8"));

		LOGGER.debug("Found global login cookie (" + loginTicketCookie.getValue()
				+ "), but not a valid SSC, redirecting to Handle Service " + _config.getString("origin.login.location"));
	}

	/**
	 * @param request
	 * @return
	 */
	private String getTarget(final HttpServletRequest request) {
		String target = request.getRequestURL().toString();
		if (request.getQueryString() != null) {
			target += "?" + request.getQueryString();
		}

		String urlParamKey = _config.getString("shire.urlparamkey");
		if (urlParamKey != null && request.getParameter(urlParamKey) != null) {
			target = request.getParameter(urlParamKey);
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
