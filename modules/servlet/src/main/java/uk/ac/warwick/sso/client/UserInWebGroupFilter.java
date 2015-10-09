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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.warwick.sso.client.tags.SSOLoginLinkGenerator;
import uk.ac.warwick.userlookup.GroupService;
import uk.ac.warwick.userlookup.User;
import uk.ac.warwick.userlookup.UserLookupFactory;
import uk.ac.warwick.userlookup.webgroups.GroupServiceException;

/**
 * 
 * A filter which will allow through requests only if the user is a member of a particular
 * Webgroup - otherwise it will redirect to the login screen.
 * <p>
 * <b>Usage:</b> In the filter definition in web.xml, use an init-param called "group" whose value is the
 * name of the Webgroup you want to use.
 *
 */
public class UserInWebGroupFilter implements Filter {

	private static final Logger LOGGER = LoggerFactory.getLogger(UserInWebGroupFilter.class);

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

		try {
			if (user.isLoggedIn() && getGroupService().isUserInGroup(user.getUserId(), _group)) {
				LOGGER.debug("User " + user.getUserId() + " is in group " + _group + " so allowing through filter");
				chain.doFilter(req, res);
				return;
			}
		} catch (GroupServiceException e) {
			LOGGER.warn("Error looking up group, continuing assuming user is not in group.", e);
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
