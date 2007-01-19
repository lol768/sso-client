/*
 * Created on 03-Aug-2005
 *
 */
package uk.ac.warwick.sso.client;

import java.net.URLEncoder;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.XMLConfiguration;
import org.springframework.mock.web.MockHttpServletRequest;

import uk.ac.warwick.sso.client.tags.SSOLoginLinkGenerator;
import uk.ac.warwick.sso.client.tags.SSOLogoutLinkGenerator;
import junit.framework.TestCase;

public class SSOLinkGeneratingTests extends TestCase {

	public final void testSSOLogoutLinkGenerator() throws Exception {

		SSOLogoutLinkGenerator generator = new SSOLogoutLinkGenerator();

		String target = "http://www.warwick.ac.uk/test%20test/";
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

	public final void testRequestedUrl() throws Exception {

		SSOLoginLinkGenerator generator = new SSOLoginLinkGenerator();
		Configuration config = new XMLConfiguration(getClass().getResource("/sso-config.xml"));
		generator.setConfig(config);

		MockHttpServletRequest req = new MockHttpServletRequest();
		String requestedUrl = "http://www.warwick.ac.uk/test test/";
		String requestedUrlEncoded = "http://www.warwick.ac.uk/test%20test/";
		req.setQueryString("requestedUrl=" + requestedUrlEncoded);
		req.setRequestURI("http://www.warwick.ac.uk/?requestedUrl=" + requestedUrl);
		req.setParameter("requestedUrl", requestedUrl);

		generator.setRequest(req);

		String loginUrl = generator.getTarget();

		final String expectedUrl = requestedUrl;
		assertEquals("Should have right url", expectedUrl, loginUrl);

	}
}
