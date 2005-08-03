/*
 * Created on 03-Aug-2005
 *
 */
package uk.ac.warwick.sso.client;

import java.net.URLEncoder;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.XMLConfiguration;

import uk.ac.warwick.sso.client.tags.SSOLoginLinkGenerator;
import uk.ac.warwick.sso.client.tags.SSOLogoutLinkGenerator;
import junit.framework.TestCase;

public class SSOLinkGeneratingTests extends TestCase {

	public final void testSSOLogoutLinkGenerator() throws Exception {

		SSOLogoutLinkGenerator generator = new SSOLogoutLinkGenerator();

		String target = "http://www.warwick.ac.uk";
		generator.setTarget(target);

		Configuration config = new XMLConfiguration(getClass().getResource("/sso-config.xml"));

		generator.setConfig(config);

		String logoutUrl = generator.getLogoutUrl();

		assertEquals("Should have right url", "http://moleman.warwick.ac.uk/origin/logout?target="
				+ URLEncoder.encode(target, "UTF-8"), logoutUrl);

	}

	public final void testSSOLoginLinkGenerator() throws Exception {

		SSOLoginLinkGenerator generator = new SSOLoginLinkGenerator();

		String target = "http://www.warwick.ac.uk";
		generator.setTarget(target);

		Configuration config = new XMLConfiguration(getClass().getResource("/sso-config.xml"));

		generator.setConfig(config);

		String loginUrl = generator.getLoginUrl();

		String expectedUrl = "http://moleman.warwick.ac.uk/origin/hs?shire=http%3A%2F%2Fmoleman.warwick.ac.uk%2Fsso-client%2Fshire&providerId=urn%3Amoleman.warwick.ac.uk%3Asso-client%3Aservice&target="
				+ URLEncoder.encode(target, "UTF-8");
		assertEquals("Should have right url", expectedUrl, loginUrl);

	}
}
