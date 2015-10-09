package uk.ac.warwick.userlookup;

import uk.ac.warwick.sso.client.core.HttpRequest;
import uk.ac.warwick.sso.client.core.ServletRequestAdapter;

import java.util.LinkedList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

public class OnCampusServiceImpl implements OnCampusService {
	uk.ac.warwick.sso.client.core.OnCampusService coreServiceImpl =
			new uk.ac.warwick.sso.client.core.OnCampusServiceImpl();

	public boolean isOnCampus(HttpServletRequest request) {
		HttpRequest req = new ServletRequestAdapter(request);
		return coreServiceImpl.isOnCampus(req);
	}
	
	public boolean isOnCampus(String remoteAddr) {
	    return coreServiceImpl.isOnCampus(remoteAddr);
	}

	/**
	 * For internal use.
	 */
	public uk.ac.warwick.sso.client.core.OnCampusService coreImpl() {
		return coreServiceImpl;
	}
}
