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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This filter only makes sense if you are using a new-mode application. By
 * mapping this filter to certain URLs, it will cause SSOClientFilter to use
 * old-mode authentication (i.e. just checking the shared WarwickSSO cookie)
 * instead of new-mode. Most people shouldn't need this most of the time, but
 * here it is in case you do.
 * <p>
 * The mapping should be <i>before</i> the mapping for {@link SSOClientFilter},
 * otherwise it will be too late.
 */
public class ForceOldModeFilter implements Filter {

	private static final Logger LOGGER = LoggerFactory.getLogger(ForceOldModeFilter.class);

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
