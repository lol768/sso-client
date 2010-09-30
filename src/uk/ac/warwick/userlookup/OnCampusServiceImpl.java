package uk.ac.warwick.userlookup;

import java.util.LinkedList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

class OnCampusServiceImpl implements OnCampusService {
	private static final String LOCALHOST = "127.0.0.1";
	private static final String WWWCACHE = UserLookup.getConfigProperty("ssoclient.oncampus.wwwcache").trim();
	private static final String[] MACHINE_RANGES = UserLookup.getConfigProperty("ssoclient.oncampus.machine.ranges").trim().split(",");
	private static final String[] CAMPUS_RANGES = UserLookup.getConfigProperty("ssoclient.oncampus.ranges").trim().split(",");
	private static final String X_FORWARDED_FOR = "X-FORWARDED-FOR";

	public boolean isOnCampus(HttpServletRequest request) {
		String remoteAddr = request.getRemoteAddr();
		//first, see if we can get by on just the remoteAddr
		if (isWwwcache(remoteAddr)) { //direct from wwwcache. win!
			return true;
		} else if (!isLocalhost(remoteAddr) && !isCampus(remoteAddr)) { //direct from offcampus. lose!
			return false;
		}
		
		//beyond here, we need to start checking the XFF header.
		List<String> chain = getIpChain(request);
		boolean onCampus = false;
		for (String ip : chain) {
			if (isCampusServer(ip)) {
				continue;
		    } else if (isWwwcache(ip)) {
				onCampus = true;
				break;
			} else if (isCampus(ip)) {
				onCampus = true;
			} else {
				onCampus = false;
				break;
			}
		}
		return onCampus;
	}
	
	/**
	 * Return a list of the IP chain, with the nearest IP at the start.
	 * If the remoteAddr was not localhost, it is added to the start of
	 * the list from XFF.
	 * 
	 * It's reversed from the XFF order because we want to iterate
	 * starting at the nearest IP.
	 */
	private List<String> getIpChain(HttpServletRequest request) {
		LinkedList<String> result = new LinkedList<String>();
		String header = request.getHeader(X_FORWARDED_FOR);
		if (header != null) {
			String[] xff = header.split(",\\s*");
			for (int i=0; i<xff.length; i++) {
				result.addFirst(xff[i]);
			}
		}
		if (!isLocalhost(request.getRemoteAddr())) {
			result.addFirst(request.getRemoteAddr());
		}
		return result;
	}

	/** Is this single IP in the oncampus ranges? */
	boolean isCampus(String ip) {
		for (int i=0; i<CAMPUS_RANGES.length; i++) {
			if (ip.startsWith(CAMPUS_RANGES[i])) {
				return true;
			}
		}
		return false;
	}
	
	/** Is this single IP wwwcache.warwick.ac.uk? */
	boolean isWwwcache(String ip) {
		return WWWCACHE.equals(ip);
	}
	
	/** Is this single IP one of our internal servers? */
	boolean isCampusServer(String ip) {
		for (int i=0; i<MACHINE_RANGES.length; i++) {
			if (ip.startsWith(MACHINE_RANGES[i])) {
				return true;
			}
		}
		return false;
	}
	
	/** Is this single IP the local machine? */
	boolean isLocalhost(String ip) {
		return LOCALHOST.equals(ip);
	}
}
