/*
 * Created on 03-Aug-2005
 *
 */
package uk.ac.warwick.sso.client.tags;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

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

}
