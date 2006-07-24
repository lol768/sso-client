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

	private Map _headers = new HashMap();

	public HeaderSettingHttpServletRequest(final HttpServletRequest arg0) {
		super(arg0);
	}

	public final String getHeader(final String name) {
		if (_headers.containsKey(name)) {
			return (String) _headers.get(name);
		}
		return super.getHeader(name);
	}

	public final Enumeration getHeaderNames() {
		List headerNames = new ArrayList();
		Enumeration enumeration = super.getHeaderNames();
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

}
