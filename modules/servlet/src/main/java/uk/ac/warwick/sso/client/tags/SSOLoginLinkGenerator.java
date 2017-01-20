/*
 * Created on 03-Aug-2005
 *
 */
package uk.ac.warwick.sso.client.tags;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import uk.ac.warwick.sso.client.ForceLoginScreenTypeFilter;
import uk.ac.warwick.sso.client.SSOClientFilter;
import uk.ac.warwick.userlookup.User;

public class SSOLoginLinkGenerator extends SSOLinkGenerator {

	public SSOLoginLinkGenerator() {
		super();
	}

	public final String getLoginUrl() {

		String loginLocation = getConfig().getString("origin.login.location");
		if (loginLocation == null || loginLocation.equals("")) {
			throw new RuntimeException("SSOLoginLinkTag needs the property: origin.login.location");
		}
		String shireLocation = getConfig().getString("shire.location");
		if (shireLocation == null || shireLocation.equals("")) {
			throw new RuntimeException("SSOLoginLinkTag needs the property: shire.location");
		}
		String providerId = getConfig().getString("shire.providerid");
		if (providerId == null || providerId.equals("")) {
			throw new RuntimeException("SSOLoginLinkTag needs the property: shire.providerid");
		}

		String linkUrl;
		try {
			linkUrl = loginLocation + "?shire=" + URLEncoder.encode(shireLocation, "UTF-8") + "&providerId="
					+ URLEncoder.encode(providerId, "UTF-8") + "&target=" + URLEncoder.encode(getTarget(), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e.getMessage());
		}

		return linkUrl;

	}
	

	/**
	 * Will use the notloggedin link if the user isn't logged in.
	 * This is safe to do, because if the user is not logged in then
	 * the two attributes have the exact same behaviour. If they
	 * are signed in but they aren't allowed to access a resource, then
	 * they genuinely do need status=permdenied so they get the
	 * page to login again.
	 */
	public final String getPermissionDeniedLink() {
		User user = SSOClientFilter.getUserFromRequest(getRequest());
		if (user.isFoundUser()) {
			return getRealPermissionDeniedLink();
		} else {
			return getNotLoggedInLink();
		}
	}
	
	public final String getRealPermissionDeniedLink() {
		return getLoginUrl() + "&status=permdenied";
	}
	
	public final String getNotLoggedInLink() {
		return getLoginUrl() + "&status=notloggedin";
	}

}
