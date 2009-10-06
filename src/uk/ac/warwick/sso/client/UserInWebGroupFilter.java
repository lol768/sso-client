/*
 * Created on 26 May 2006
 *
 */
package uk.ac.warwick.sso.client;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import uk.ac.warwick.sso.client.tags.SSOLoginLinkGenerator;
import uk.ac.warwick.userlookup.GroupService;
import uk.ac.warwick.userlookup.User;
import uk.ac.warwick.userlookup.UserLookupFactory;

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

	private GroupService _groupService;

	public void destroy() {
		// nothing
	}

	public final void doFilter(final ServletRequest req, final ServletResponse res, final FilterChain chain) throws IOException,
			ServletException {

		HttpServletRequest request = (HttpServletRequest) req;

		String shireLocation = SSOConfiguration.getConfig().getString("shire.location");
		String logoutLocation = SSOConfiguration.getConfig().getString("logout.location");

		URL target = getTarget(request);
		LOGGER.debug("Target=" + target);

		if (target.toExternalForm().equals(shireLocation) || target.toExternalForm().equals(logoutLocation)) {
			LOGGER.debug("Letting request through without filtering because it is a shire or logout request");
			chain.doFilter(req, res);
			return;
		}

		User user = SSOClientFilter.getUserFromRequest((HttpServletRequest) req);

		if (user.isLoggedIn() && getGroupService().isUserInGroup(user.getUserId(), _group)) {
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
		if (StringUtils.isEmpty(_group)) {
			_group = config.getInitParameter("group");
		}
	}

	public final GroupService getGroupService() {
		if (_groupService == null) {
			return UserLookupFactory.getInstance().getGroupService();
		}
		return _groupService;
	}

	public final void setGroupService(final GroupService groupService) {
		_groupService = groupService;
	}

	public final String getGroup() {
		return _group;
	}

	public final void setGroup(final String group) {
		_group = group;
	}

	/**
	 * @param request
	 * @return
	 */
	private URL getTarget(final HttpServletRequest request) {

		SSOLoginLinkGenerator generator = new SSOLoginLinkGenerator();
		generator.setRequest(request);
		try {
			return new URL(generator.getTarget());
		} catch (MalformedURLException e) {
			LOGGER.warn("Target is an invalid url", e);
			return null;
		}

	}

}
