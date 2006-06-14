/*
 * Created on 10-Nov-2005
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
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import uk.ac.warwick.sso.client.tags.SSOLoginLinkGenerator;
import uk.ac.warwick.userlookup.User;

/**
 * This Filter forces a user to be logged in. This filter works with SSOClientFilter and must come after it as it looks
 * for the user with SSOClientFilter.getUserFromRequest(request); If a user is not logged in, it redirects to the SSO
 * permission denied page
 * 
 * @author Kieran Shaw
 * 
 */
public class ForceLoginFilter implements Filter {

	private static final Logger LOGGER = Logger.getLogger(ForceLoginFilter.class);

	public final void init(final FilterConfig ctx) throws ServletException {
		// nothing
	}

	public final void doFilter(final ServletRequest req, final ServletResponse res, final FilterChain chain) throws IOException,
			ServletException {

		HttpServletRequest request = (HttpServletRequest) req;
		HttpServletResponse response = (HttpServletResponse) res;

		User user = SSOClientFilter.getUserFromRequest(request);
		if (user == null || !user.isLoggedIn()) {
			if (request.getMethod().equalsIgnoreCase("post")) {
				LOGGER.warn("User is posting but is not logged in, so is going to lose data!");
			}
			
			SSOLoginLinkGenerator generator = new SSOLoginLinkGenerator();
			generator.setRequest(request);
			String loginLink = generator.getPermissionDeniedLink();
			response.sendRedirect(loginLink);
			LOGGER.info("Forcing redirect to login screen:" + loginLink);
			return;
		}

		chain.doFilter(req, res);

	}

	public final void destroy() {
		// nothing
	}

}
