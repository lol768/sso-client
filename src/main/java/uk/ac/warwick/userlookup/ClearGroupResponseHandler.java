package uk.ac.warwick.userlookup;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HeaderElement;
import org.apache.commons.httpclient.HttpMethod;

import uk.ac.warwick.userlookup.HttpMethodWebService.WebServiceResponseHandler;
import uk.ac.warwick.userlookup.cache.Cache;

public abstract class ClearGroupResponseHandler implements
		WebServiceResponseHandler {

	private final static String CLEAR_WEB_GROUP_HEADER = "Clear-Web-Group";
	private final static int OLD_CACHE_TIME_IN_MINUTES = -30;
	private static Map<String, Date> lastCleared = new HashMap<String, Date>();
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static void staticProcessClearGroupHeader(HttpMethod method) {
		Header header = method.getResponseHeader(CLEAR_WEB_GROUP_HEADER);
		if (header != null) {
			Set<String> groupsToClear = new HashSet<String>();
			for (HeaderElement element : header.getElements()) {
				Date lastClearedDate = ClearGroupResponseHandler.lastCleared
						.get(element.getName());
				try {
					long headerDateMillis = Long.parseLong(element.getValue());
					Date headerDate = new Date(headerDateMillis);
					if (lastClearedDate == null
							|| lastClearedDate.before(headerDate)) {
						ClearGroupResponseHandler.lastCleared.put(
								element.getName(), headerDate);
						groupsToClear.add(element.getName());
					}
				} catch (NumberFormatException e) {
					// invalid number, ignore this header
				}

			}

			if (groupsToClear.size() > 0) {
				clearOutOldEntries();
				Set<Cache<?, ?>> groupMemberCacheSet = UserLookup.getInstance()
						.getGroupService().getCaches()
						.get(UserLookup.GROUP_MEMBER_CACHE_NAME);
				Set<Cache<?, ?>> groupCacheSet = UserLookup.getInstance()
						.getGroupService().getCaches()
						.get(UserLookup.GROUP_CACHE_NAME);
				if (groupMemberCacheSet != null) {
					for (Cache cache : groupMemberCacheSet) {
						for (String groupName : groupsToClear) {
							cache.remove(groupName);
						}
					}
				}
				if (groupCacheSet != null) {
					for (Cache cache : groupCacheSet) {
						for (String groupName : groupsToClear) {
							cache.remove(groupName);
						}
					}
				}
			}
		}

	}

	@Override
	public void processClearGroupHeader(HttpMethod method) {
		ClearGroupResponseHandler.staticProcessClearGroupHeader(method);
	}
	
	private static void clearOutOldEntries() {
		Set<String> groupsToRemove = new HashSet<String>();
		Calendar expiry = Calendar.getInstance();
		expiry.setTime(new Date());
		expiry.add(Calendar.MINUTE, OLD_CACHE_TIME_IN_MINUTES);
		Date expiryDate = expiry.getTime();
		for (Map.Entry<String, Date> pair : lastCleared.entrySet()) {
			if (pair.getValue().before(expiryDate)) {
				groupsToRemove.add(pair.getKey());
			}
		}
		for (String group : groupsToRemove) {
			lastCleared.remove(group);
		}
	}

}
