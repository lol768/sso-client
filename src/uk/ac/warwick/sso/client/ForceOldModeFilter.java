/*
 * Created on 12 Jun 2006
 *
 */
package uk.ac.warwick.sso.client;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;

public class ForceOldModeFilter implements Filter {

	private static final Logger LOGGER = Logger.getLogger(ForceOldModeFilter.class);

	public static final String ALLOW_OLD_KEY = "SSO_ALLOW_OLD_MODE";

	public void destroy() {
		// nothing to destroy
	}

	public final void doFilter(final ServletRequest req, final ServletResponse res, final FilterChain chain) throws IOException,
			ServletException {

		LOGGER.debug("Allowing access to URL " + ((HttpServletRequest) req).getRequestURI() + " in old SSO mode");
		req.setAttribute(ALLOW_OLD_KEY, Boolean.TRUE);

		chain.doFilter(req, res);

	}

	public final void init(final FilterConfig config) throws ServletException {
		// nothing to init
	}

}
