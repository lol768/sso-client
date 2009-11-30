package uk.ac.warwick.userlookup;

import javax.servlet.http.HttpServletRequest;

public interface OnCampusService {
	boolean isOnCampus(HttpServletRequest request);
}
