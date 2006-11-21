/*
 * Created on 03-Aug-2005
 *
 */
package uk.ac.warwick.sso.client.tags;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class SSOLogoutLinkGenerator extends SSOLinkGenerator {

	/**
	 * SSOLogoutLinkGenerator will try and get a configuration from the ThreadLocal based SSOConfiguration, but you can
	 * override this by just setting a Configuration manually
	 * 
	 */
	public SSOLogoutLinkGenerator() {
		super();
	}

	public final String getLogoutUrl()  {

		if (getConfig() == null) {
			throw new RuntimeException("Should not find the SSO config");
		}

		String logoutLocation = getConfig().getString("origin.logout.location");
		if (logoutLocation == null || logoutLocation.equals("")) {
			throw new RuntimeException("SSOLogoutLinkGenerator needs a logout location origin.logout.location");
		}

		String linkUrl;
		try {
			linkUrl = logoutLocation + "?target=" + URLEncoder.encode(getTarget(), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e.getMessage());
		}

		return linkUrl;

	}
}
