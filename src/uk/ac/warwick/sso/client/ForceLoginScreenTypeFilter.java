/*
 * Created on 25 Jul 2007
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

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

public class ForceLoginScreenTypeFilter implements Filter {

	private static final Logger LOGGER = Logger.getLogger(ForceOldModeFilter.class);

	public static final String SSO_SCREEN_TYPE_KEY = "SSO_SCREEN_TYPE";

	private String _screenType;

	public void destroy() {
		// nothing to destroy
	}

	public final void doFilter(final ServletRequest req, final ServletResponse res, final FilterChain chain) throws IOException,
			ServletException {

		LOGGER.debug("Forcing login screen type to " + getScreenType() + " for " + ((HttpServletRequest) req).getRequestURI());
		req.setAttribute(SSO_SCREEN_TYPE_KEY, getScreenType());

		chain.doFilter(req, res);

	}

	public final void init(final FilterConfig config) throws ServletException {
		if (StringUtils.isEmpty(_screenType)) {
			_screenType = config.getInitParameter("screenType");
		}
	}

	public final String getScreenType() {
		return _screenType;
	}

	public final void setScreenType(final String screenType) {
		_screenType = screenType;
	}

}
