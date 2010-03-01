/*
 * Created on 21 Jul 2006
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

import uk.ac.warwick.userlookup.Group;
import uk.ac.warwick.userlookup.GroupService;
import uk.ac.warwick.userlookup.User;
import uk.ac.warwick.userlookup.UserLookupFactory;
import uk.ac.warwick.userlookup.webgroups.GroupServiceException;

/**
 * A simple convenience filter that gets the current user, finds out which
 * Webgroups they are in (if any) and places the names of the groups in a
 * comma-separated list in a request header, which by default is called
 * SSO_USER_groups.
 * 
 * @deprecated This class seems rather pointless - it's very easy to get the
 * 			user's groups yourself.
 */
public class PutGroupsInHeadersFilter implements Filter {

	private GroupService _groupService;

	public final void destroy() {
		// nothing
	}

	public final void doFilter(final ServletRequest req, final ServletResponse res, final FilterChain chain) throws IOException,
			ServletException {

		User user = SSOClientFilter.getUserFromRequest((HttpServletRequest) req);

		HeaderSettingHttpServletRequest request;
		if (req instanceof HeaderSettingHttpServletRequest) {
			request = (HeaderSettingHttpServletRequest) req;
		} else {
			request = new HeaderSettingHttpServletRequest((HttpServletRequest) req);
		}

		String sep = "";
		StringBuilder groupList = new StringBuilder();
		try {
			for (Group group : getGroupService().getGroupsForUser(user.getUserId())) {
				groupList.append(sep).append(group.getName());
				sep = ",";
			}
			String userKey = SSOClientFilter.getUserKey();
			request.addHeader(userKey + "_groups", groupList.toString());
		} catch (GroupServiceException e) {
			// Failed to look up groups. This is deprecated so I don't really care.
		}		

		chain.doFilter(request, res);

	}

	public final void init(final FilterConfig arg0) throws ServletException {
		// nothing
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

}
