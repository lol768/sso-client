/*
 * Created on 21 Jul 2006
 *
 */
package uk.ac.warwick.sso.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

public class HeaderSettingHttpServletRequest extends HttpServletRequestWrapper {

	private Map<String,String> _headers = new HashMap<String,String>();

	private String _remoteUser;

	public HeaderSettingHttpServletRequest(final HttpServletRequest arg0) {
		super(arg0);
	}

	public final String getHeader(final String name) {
		if (_headers.containsKey(name)) {
			return (String) _headers.get(name);
		}
		return super.getHeader(name);
	}

	public final Enumeration<?> getHeaderNames() {
		List<String> headerNames = new ArrayList<String>();
		Enumeration<?> enumeration = super.getHeaderNames();
		while (enumeration.hasMoreElements()) {
			String name = (String) enumeration.nextElement();
			headerNames.add(name);
		}
		headerNames.addAll(_headers.keySet());
		return Collections.enumeration(headerNames);

	}

	public final void addHeader(final String name, final String value) {
		_headers.put(name, value);
	}

	public final String getRemoteUser() {
		if ("".equals(_remoteUser) || _remoteUser == null) {
			return super.getRemoteUser();
		}
		return _remoteUser;
	}

	public final void setRemoteUser(final String remoteUser) {
		_remoteUser = remoteUser;
	}

}
