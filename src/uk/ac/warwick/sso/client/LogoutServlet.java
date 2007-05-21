/*
 * Created on 22-Mar-2005
 *
 */
package uk.ac.warwick.sso.client;

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import uk.ac.warwick.sso.client.cache.UserCache;

public class LogoutServlet extends HttpServlet {

	private static final Logger LOGGER = Logger.getLogger(LogoutServlet.class);

	private UserCache _cache;

	private String _configSuffix;

	protected final void doPost(final HttpServletRequest arg0, final HttpServletResponse arg1) throws ServletException,
			IOException {
		processRequest(arg0, arg1);
	}

	private void processRequest(final HttpServletRequest req, final HttpServletResponse res) throws IOException {

		java.io.PrintWriter out = res.getWriter();

		String serviceSpecificCookie = req.getParameter("logoutTicket");
		if (serviceSpecificCookie == null || serviceSpecificCookie.equals("")) {
			out.println("false");
			LOGGER.info("Logout attempt failed because no ssc was passed in");
			return;
		}

		SSOToken token = new SSOToken(serviceSpecificCookie, SSOToken.SSC_TICKET_TYPE);

		if (getCache().get(token) != null) {
			getCache().remove(token);
			out.println("true");
			LOGGER.info("Logout attempt succeeded as ssc (" + token + ") was found in cache");
			return;
		}

		LOGGER.info("Logout attempt failed because the ssc (" + token + ") was not found in the user cache");
		out.println("false");
		return;

	}

	public final UserCache getCache() {
		return _cache;
	}

	public final void setCache(final UserCache cache) {
		_cache = cache;
	}

	public final void init(final ServletConfig ctx) throws ServletException {

		if (_configSuffix == null && ctx.getInitParameter("configsuffix") != null) {
			_configSuffix = ctx.getInitParameter("configsuffix");
		}

		if (getCache() == null) {
			setCache((UserCache) ctx.getServletContext().getAttribute(SSOConfigLoader.SSO_CACHE_KEY + _configSuffix));
		}
	}

	public final String getConfigSuffix() {
		return _configSuffix;
	}

	public final void setConfigSuffix(final String configSuffix) {
		_configSuffix = configSuffix;
	}

}
