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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SSO supports a few alternate login screens for mobile and text-only user agents.
 * By default, SSO client won't use either of these but this filter can force the
 * type used for redirects, so that if a subsequent filter sends the user to the
 * login screen then it will be used instead
 * <p>
 * Current known values are "mini" (mobile) and "text" (text only). Leave unset for the default.
 * <p>
 * If you have complex user agent needs then you can write your own filter and configure it
 * to set the attribute {@link #SSO_SCREEN_TYPE_KEY} to "mini" when you want it to show a mobile
 * login screen. You might do some user agent sniffing to detect this.
 * <p>
 * (ForceLoginScreenTypeFilter was a bad name as it suggests it's something to do with
 * ForceLoginFilter, or related to forcing login, which it is not.)
 */
public class ForceLoginScreenTypeFilter implements Filter {

	private static final Logger LOGGER = LoggerFactory.getLogger(ForceOldModeFilter.class);

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
