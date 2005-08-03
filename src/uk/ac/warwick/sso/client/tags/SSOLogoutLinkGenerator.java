/*
 * Created on 03-Aug-2005
 *
 */
package uk.ac.warwick.sso.client.tags;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.apache.commons.configuration.ConfigurationException;

public class SSOLogoutLinkGenerator extends SSOLinkGenerator {

	/**
	 * SSOLogoutLinkGenerator will try and get a configuration from the ThreadLocal based SSOConfiguration, but you can
	 * override this by just setting a Configuration manually
	 * 
	 */
	public SSOLogoutLinkGenerator() {
		super();
	}

	public final String getLogoutUrl() throws ConfigurationException {

		if (getConfig() == null) {
			throw new ConfigurationException("Should find no SSO config");
		}

		String logoutLocation = getConfig().getString("origin.logout.location");
		if (logoutLocation == null || logoutLocation.equals("")) {
			throw new ConfigurationException("SSOLogoutLinkGenerator needs a logout location origin.logout.location");
		}

		String linkUrl;
		try {
			linkUrl = logoutLocation + "?target=" + URLEncoder.encode(getTarget(), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new ConfigurationException(e.getMessage());
		}

		return linkUrl;

	}
}
