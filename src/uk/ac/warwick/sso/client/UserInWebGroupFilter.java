/*
 * Created on 26 May 2006
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
import uk.ac.warwick.userlookup.GroupService;
import uk.ac.warwick.userlookup.User;
import uk.ac.warwick.userlookup.UserLookup;

/**
 * Configure with an init-param called "group" which is a group name from WebGroups. Only people in that group will be
 * allowed through
 * 
 * @author Kieran Shaw
 * 
 */
public class UserInWebGroupFilter implements Filter {

	private static final Logger LOGGER = Logger.getLogger(UserInWebGroupFilter.class);

	private String _group;

	public void destroy() {
		// nothing
	}

	public final void doFilter(final ServletRequest req, final ServletResponse res, final FilterChain chain) throws IOException,
			ServletException {

		User user = SSOClientFilter.getUserFromRequest((HttpServletRequest) req);

		UserLookup userLookup = UserLookup.getInstance();
		GroupService groupService = userLookup.getGroupService();
		if (user.isLoggedIn() && groupService.isUserInGroup(user.getUserId(), _group)) {
			LOGGER.debug("User " + user.getUserId() + " is in group " + _group + " so allowing through filter");
			chain.doFilter(req, res);
			return;
		}

		LOGGER.info("User " + user.getUserId() + " is NOT in group " + _group + " so NOT allowing through filter");

		SSOLoginLinkGenerator generator = new SSOLoginLinkGenerator();
		generator.setRequest((HttpServletRequest) req);
		String permDeniedUrl = generator.getPermissionDeniedLink();
		((HttpServletResponse) res).sendRedirect(permDeniedUrl);
		return;

	}

	public final void init(final FilterConfig config) throws ServletException {
		_group = config.getInitParameter("group");
	}

}
