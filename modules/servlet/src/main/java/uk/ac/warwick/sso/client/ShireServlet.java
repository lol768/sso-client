package uk.ac.warwick.sso.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.warwick.sso.client.cache.UserCache;
import uk.ac.warwick.sso.client.core.ServletCookies;
import uk.ac.warwick.userlookup.User;
import uk.ac.warwick.userlookup.UserLookup;
import uk.ac.warwick.util.cache.Cache;
import uk.ac.warwick.util.cache.Caches;
import uk.ac.warwick.util.core.StringUtils;

import static uk.ac.warwick.userlookup.UserLookup.getConfigProperty;

/**
 * <h2>What on earth is a shire?</h2>
 * 
 * <p>The shire, which is only used for new-mode SSO apps, is where Websignon
 * sends its security assertion to say that a particular user has just successfully
 * signed in.</p>
 * 
 * <p>After the user has signed in via Websignon, an auto-submitting form containing
 * the security assertion is posted here, to the shire. The shire will do a few things:
 * <ol>
 *   <li>Check that the assertion is valid (not expired, properly signed)
 *   <li>Extract a token out of the assertion
 *   <li>Use the token to request the user's details from the Websignon Attribute Authority URL
 *   <li>Set a Service Specific Cookie (SSC) to say this service is signed in
 * </ol>
 * 
 * The last step is why it's important that this is done by the user's browser - otherwise
 * it wouldn't be able to save this cookie. It then redirects the browser to the originally
 * requested URL.
 * <p>
 * After this is done, {@link SSOClientFilter} will see the SSC and set up the current user
 * in the request.
 * <p>
 * ShireServlet should be defined in web.xml, and <b>must</b> be served over HTTPS. When
 * your application is registered with SSO, the URL to your shire will be one of the
 * pieces of information it knows about.
 */
public class ShireServlet extends HttpServlet {
	private static final long serialVersionUID = 3043814958673574588L;

	private static final Logger LOGGER = LoggerFactory.getLogger(ShireServlet.class);

	private SSOConfiguration _config;

	private UserCache _cache;

	private String _configSuffix = "";
	
	private Cache<String, User> _userIdCache;
	
	private String getMessage = null;

	public ShireServlet() {
		super();
	}

	protected final void doGet(final HttpServletRequest req, final HttpServletResponse res) throws ServletException, IOException {
		res.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
		if (getMessage == null) {
			InputStream page = getClass().getResourceAsStream("/shireget.html");
			getMessage = StringUtils.copyToString(new InputStreamReader(page));
		}
		res.setContentType("text/html");

		Writer out = res.getWriter();
		try {
			out.write(getMessage);
		} finally {
			try {
				out.close();
			} catch (IOException ex) {}
		}
	}

	protected final void doPost(final HttpServletRequest req, final HttpServletResponse res) throws ServletException, IOException {
		process(req, res);
	}
	
	public ShireCommand createShireCommand(String remoteAddress) {
	    ShireCommand command = new ShireCommand(_userIdCache);

        command.setRemoteHost(remoteAddress);
        command.setCache(_cache);

        AttributeAuthorityResponseFetcher fetcher = new AttributeAuthorityResponseFetcherImpl(_config);
        command.setAaFetcher(fetcher);
        command.setConfig(_config);
        
        return command;
	}

	private void process(final HttpServletRequest req, final HttpServletResponse res) {

		String saml64 = req.getParameter("SAMLResponse");
		String target = req.getParameter("TARGET");

		String remoteHost = req.getRemoteHost();
		if (req.getHeader("x-forwarded-for") != null) {
			remoteHost = req.getHeader("x-forwarded-for");
		}

		ShireCommand command = createShireCommand(remoteHost);
		Cookie cookie = null;
		try {
			cookie = ServletCookies.toServlet(command.process(saml64, target));
		} catch (SSOException e) {
			LOGGER.warn("Could not generate cookie", e);
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

		// Cookie policy header so that IE can accept cookies from within iframes
		res.setHeader("P3P", "CP=\"CAO PSA OUR\"");

		res.setHeader("Location", target);
		res.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);

	}

	public final void init(final ServletConfig ctx) throws ServletException {
		super.init(ctx);

		if (ctx.getInitParameter("configsuffix") != null) {
			_configSuffix = ctx.getInitParameter("configsuffix");
		}

		if (getConfig() == null) {
			_config = (SSOConfiguration) ctx.getServletContext().getAttribute(SSOConfigLoader.SSO_CONFIG_KEY + _configSuffix);
		}

		if (getCache() == null) {
			_cache = (UserCache) ctx.getServletContext().getAttribute(SSOConfigLoader.SSO_CACHE_KEY + _configSuffix);
		}

		if (getUserIdCache() == null) {
			_userIdCache = Caches.newCache(UserLookup.USER_CACHE_NAME, null, 0, Caches.CacheStrategy.valueOf(UserLookup.getConfigProperty("ssoclient.cache.strategy")));
		}
	}

	private Cookie getCookie(final Cookie[] cookies, final String name) {

		if (cookies != null) {
			for (Cookie cookie: cookies) {
				if (cookie.getName().equals(name)) {
					return cookie;
				}
			}
		}
		return null;
	}

	public final UserCache getCache() {
		return _cache;
	}

	public final void setCache(final UserCache cache) {
		_cache = cache;
	}

	public final SSOConfiguration getConfig() {
		return _config;
	}

	public final void setConfig(final SSOConfiguration config) {
		_config = config;
	}

	public final String getConfigSuffix() {
		return _configSuffix;
	}

	public final void setConfigSuffix(final String configSuffix) {
		_configSuffix = configSuffix;
	}

	public final Cache<String, User> getUserIdCache() {
		return _userIdCache;
	}

	public final void setUserIdCache(Cache<String, User> userIdCache) {
		_userIdCache = userIdCache;
	}

}
