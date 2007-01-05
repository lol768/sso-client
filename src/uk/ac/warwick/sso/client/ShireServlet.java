/*
 * Created on 11-Mar-2005
 *
 */
package uk.ac.warwick.sso.client;

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.configuration.Configuration;
import org.apache.log4j.Logger;

import uk.ac.warwick.sso.client.cache.UserCache;

/**
 * @author Kieran Shaw
 * 
 */
public class ShireServlet extends HttpServlet {

	private static final Logger LOGGER = Logger.getLogger(ShireServlet.class);

	private Configuration _config;

	private UserCache _cache;

	public ShireServlet() {
		super();
	}

	protected final void doGet(final HttpServletRequest req, final HttpServletResponse res) throws ServletException, IOException {

		process(req, res);
	}

	protected final void doPost(final HttpServletRequest req, final HttpServletResponse res) throws ServletException, IOException {

		process(req, res);

	}

	/**
	 * @param req
	 * @throws IOException
	 * @throws HttpException
	 */
	private void process(final HttpServletRequest req, final HttpServletResponse res) {

		String saml64 = req.getParameter("SAMLResponse");
		String target = req.getParameter("TARGET");

		String remoteHost = req.getRemoteHost();
		if (req.getHeader("x-forwarded-for") != null) {
			remoteHost = req.getHeader("x-forwarded-for");
		}

		ShireCommand command = new ShireCommand();

		command.setRemoteHost(remoteHost);
		command.setCache(_cache);

		AttributeAuthorityResponseFetcher fetcher = new AttributeAuthorityResponseFetcherImpl();
		fetcher.setConfig(_config);
		command.setAaFetcher(fetcher);
		command.setConfig(_config);
		Cookie cookie = null;
		try {
			cookie = command.process(saml64, target);
		} catch (SSOException e) {
			LOGGER.warn("Could not generate cookie");
		}

		if (cookie != null) {
			LOGGER.debug("Adding SSC (" + cookie.getValue() + " ) to response");
			res.addCookie(cookie);
			LOGGER.debug("User being redirected to target with new SSC");
		} else if (getCookie(req.getCookies(), _config.getString("shire.sscookie.name")) == null) {
			LOGGER.warn("No SSC cookie returned to client, nor do they have a previous SSC");
			// if you couldn't get a service specific cookie set, then we must
			// destory the auto login cookie because it will keep redirecting
			// the user
			Cookie ltc = new Cookie("SSO-LTC", "");
			ltc.setDomain(".warwick.ac.uk");
			ltc.setPath("/");
			ltc.setMaxAge(0);
			res.addCookie(ltc);
			LOGGER.debug("User being redirected to target but they didn't get a new SSC so we are clearing the SSO-LTC");
		} else {
			LOGGER.debug("User being redirected to target but they didn't get a new SSC, so we are reusing the old one");
		}

		res.setHeader("Location", target);
		res.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);

	}

	public final void init(final ServletConfig ctx) throws ServletException {
		super.init(ctx);

		String suffix = "";
		if (ctx.getInitParameter("configsuffix") != null) {
			suffix = ctx.getInitParameter("configsuffix");
		}

		_config = (Configuration) ctx.getServletContext().getAttribute(SSOConfigLoader.SSO_CONFIG_KEY + suffix);

		_cache = (UserCache) ctx.getServletContext().getAttribute(SSOConfigLoader.SSO_CACHE_KEY + suffix);

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

}
