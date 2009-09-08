/*
 * Created on 03-Aug-2005
 *
 */
package uk.ac.warwick.sso.client;

import java.net.URLEncoder;

import junit.framework.TestCase;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.springframework.mock.web.MockHttpServletRequest;

import uk.ac.warwick.sso.client.tags.SSOLoginLinkGenerator;
import uk.ac.warwick.sso.client.tags.SSOLogoutLinkGenerator;
import uk.ac.warwick.userlookup.User;

public class SSOLinkGeneratingTests extends TestCase {

	public final void testSSOLogoutLinkGenerator() throws Exception {

		SSOLogoutLinkGenerator generator = new SSOLogoutLinkGenerator();

		String target = "http://www.warwick.ac.uk/test%20test/";
		generator.setTarget(target);

		Configuration config = new XMLConfiguration(getClass().getResource("sso-config.xml"));

		generator.setConfig(config);

		String logoutUrl = generator.getLogoutUrl();

		assertEquals("Should have right url", "https://websignon.warwick.ac.uk/origin/logout?target="
				+ URLEncoder.encode(target, "UTF-8"), logoutUrl);

	}
	
	public final void testPermDenied()  throws Exception  {
		SSOLoginLinkGenerator g = new SSOLoginLinkGenerator();
		MockHttpServletRequest request = new MockHttpServletRequest();
		g.setRequest(request);
		Configuration config = new XMLConfiguration(getClass().getResource("sso-config.xml"));
		g.setConfig(config);
		
		assertTrue(g.getPermissionDeniedLink().contains("notloggedin"));
		
		User user = new User();
		user.setFoundUser(true);
		request.setAttribute(SSOClientFilter.USER_KEY, user);
		
		assertTrue(g.getPermissionDeniedLink().contains("permdenied"));
	}

	public final void testSSOLoginLinkGenerator() throws Exception {

		SSOLoginLinkGenerator generator = new SSOLoginLinkGenerator();
		
		MockHttpServletRequest request = new MockHttpServletRequest();
		
		generator.setRequest(request);

		String target = "http://www.warwick.ac.uk";
		generator.setTarget(target);

		Configuration config = new XMLConfiguration(getClass().getResource("sso-config.xml"));

		generator.setConfig(config);

		String loginUrl = generator.getLoginUrl();

		String expectedUrl = "https://websignon.warwick.ac.uk/origin/hs?shire=https%3A%2F%2Fmyapp.warwick.ac.uk%2Fmyapp%2Fshire&providerId=urn%3Amyapp.warwick.ac.uk%3Amyapp%3Aservice&target="
				+ URLEncoder.encode(target, "UTF-8");
		assertEquals("Should have right url", expectedUrl, loginUrl);

	}
	
	public final void testRequestedURIHeader() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		Configuration config = new XMLConfiguration(getClass().getResource("sso-config.xml"));
		SSOLoginLinkGenerator generator = new SSOLoginLinkGenerator();
		generator.setRequest(request);
		generator.setConfig(config);
		
		String target = "http://sitebuilder.warwick.ac.uk/sitebuilder2/edit/control.htm?greeting=hello";
		String encodedTarget = URLEncoder.encode(target, "UTF-8");
		//double check that these have been converted
		assertFalse(encodedTarget.contains("&"));
		assertFalse(encodedTarget.contains("?"));
		assertTrue(encodedTarget.contains("greeting%3Dhello"));
		
		request.addHeader("X-Requested-Uri", target);
		
		String loginUrl = generator.getLoginUrl();
		String expectedUrl = "https://websignon.warwick.ac.uk/origin/hs?shire=https%3A%2F%2Fmyapp.warwick.ac.uk%2Fmyapp%2Fshire&providerId=urn%3Amyapp.warwick.ac.uk%3Amyapp%3Aservice&target="
			+ encodedTarget;
		assertEquals("Should have right url", expectedUrl, loginUrl);
	}
	

	public final void testRequestedUrl() throws Exception {
		String requestedUrl = "http://www.warwick.ac.uk/test%20test/";
		String requestedUrlEncoded = "http://www.warwick.ac.uk/test%20test/";
		compareLinks(requestedUrl, requestedUrlEncoded);
	}

	public final void testRequestedUrl2() throws Exception {
		String requestedUrl = "http://www.warwick.ac.uk/(test%20test/";
		String requestedUrlEncoded = "http://www.warwick.ac.uk/(test%20test/";
		compareLinks(requestedUrl, requestedUrlEncoded);
	}

	public final void testRequestedUrl3() throws Exception {
		String requestedUrl = "http://www.warwick.ac.uk/[test%20test]/";
		String requestedUrlEncoded = "http://www.warwick.ac.uk/[test%20test]/";
		compareLinks(requestedUrl, requestedUrlEncoded);
	}

	public final void testUTF8() throws Exception {
		String requestedUrl = "http://moleman.warwick.ac.uk/%E6%9A%96";
		String requestedUrlEncoded = "http://moleman.warwick.ac.uk/%E6%9A%96";
		compareLinks(requestedUrl, requestedUrlEncoded);
	}


	/**
	 * @param requestedUrl
	 * @param requestedUrlEncoded
	 * @throws ConfigurationException
	 */
	private void compareLinks(final String requestedUrl, final String requestedUrlEncoded) throws ConfigurationException {
		final String paramKey = "requestedUrl";
		SSOLoginLinkGenerator generator = new SSOLoginLinkGenerator();
		Configuration config = new XMLConfiguration(getClass().getResource("sso-config.xml"));
		generator.setConfig(config);

		MockHttpServletRequest req = new MockHttpServletRequest();
		req.setQueryString(paramKey + "=" + requestedUrlEncoded);
		// req.setRequestURI("http://localhost/");

		req.setParameter(paramKey, requestedUrl);

		generator.setRequest(req);

		String loginUrl = generator.getTarget();

		final String expectedUrl = requestedUrl;
		assertEquals("Should have right url", expectedUrl, loginUrl);
	}

}
