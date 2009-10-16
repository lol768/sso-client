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
		for (Group group : getGroupService().getGroupsForUser(user.getUserId())) {
			groupList.append(sep).append(group.getName());
			sep = ",";
		}

		String userKey = SSOClientFilter.getUserKey();
		request.addHeader(userKey + "_groups", groupList.toString());

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
