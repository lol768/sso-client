/*
 * Created on 30-Jun-2005
 *
 */
package uk.ac.warwick.sso.client;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import junit.framework.TestCase;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.protocol.Protocol;
import org.opensaml.SAMLAssertion;
import org.opensaml.SAMLAttribute;
import org.opensaml.SAMLAttributeStatement;
import org.opensaml.SAMLNameIdentifier;
import org.opensaml.SAMLResponse;
import org.opensaml.SAMLSubject;

import uk.ac.warwick.sso.client.ssl.AuthSSLProtocolSocketFactory;
import uk.ac.warwick.userlookup.User;

public class AttributeAuthorityFetcherTests extends TestCase {

	public final void testFetcher() throws Exception {

		// This test is rubbish because this overrides the getSAMLResponse method
		// that usually does all the work. So we're not testing much.
		MockAttributeAuthorityResponseFetcher2 fetcher = new MockAttributeAuthorityResponseFetcher2();

		fetcher.setSAMLResponse(generateMockResponse());

		User user = fetcher.getUserFromSubject(generateMockSubject());

		assertNotNull(user);

		assertTrue(user.isStaff());

		assertEquals(user.getExtraProperty("givenName"), "Kieran");

	}

	public final void testSelfSignedSSL() throws Exception {

		SSOConfiguration config = newConfig();
		String url = "https://test-ext-users.warwick.ac.uk/extusers/authenticate.spr?username=&password=";

		testSSL(config, url,HttpServletResponse.SC_FORBIDDEN);

	}

	private SSOConfiguration newConfig() throws ConfigurationException {
		SSOConfiguration config = new SSOConfiguration(new XMLConfiguration(getClass().getResource("sso-config.xml")));
		return config;
	}

	

	public final void testRealSSL() throws Exception {
		
		SSOConfiguration config = newConfig();
		String url = "https://secure.wbs.ac.uk/itsauth/authenticate.cfm";

		testSSL(config, url,HttpServletResponse.SC_FORBIDDEN);

	}
	
	public final void testClientAuthSSL() throws Exception {
		
		SSOConfiguration config = newConfig();
		String url = "https://moleman.warwick.ac.uk/origin/aa";

		testSSL(config, url,500);
		
	}

	private SAMLSubject generateMockSubject() throws Exception {

		SAMLSubject subject = new SAMLSubject();
		SAMLNameIdentifier nameId = new SAMLNameIdentifier("12345", "origin", "tickettype");
		subject.setName(nameId);
		return subject;

	}

	private SAMLResponse generateMockResponse() throws Exception {
		SAMLResponse resp = new SAMLResponse();
		SAMLAssertion assertion = new SAMLAssertion();

		SAMLAttributeStatement statement = new SAMLAttributeStatement();

		statement.setSubject(generateMockSubject());
		List values = new ArrayList();
		values.add("SSC_VALUE");
		SAMLAttribute attr = new SAMLAttribute("urn:websignon:ssc", "urn:ac:uk:warwick:websignon", null, 0, values);
		statement.addAttribute(attr);

		values = new ArrayList();
		values.add("Kieran");
		attr = new SAMLAttribute("givenName", "urn:ac:uk:warwick:websignon", null, 0, values);
		statement.addAttribute(attr);

		values = new ArrayList();
		values.add("Shaw");
		attr = new SAMLAttribute("sn", "urn:ac:uk:warwick:websignon", null, 0, values);
		statement.addAttribute(attr);

		values = new ArrayList();
		values.add("true");
		attr = new SAMLAttribute("staff", "urn:ac:uk:warwick:websignon", null, 0, values);
		statement.addAttribute(attr);

		values = new ArrayList();
		values.add("WarwickNDS");
		attr = new SAMLAttribute("urn:websignon:usersource", "urn:ac:uk:warwick:websignon", null, 0, values);
		statement.addAttribute(attr);

		assertion.addStatement(statement);
		resp.addAssertion(assertion);

		return resp;
	}
	
	/**
	 * @param config
	 * @param url
	 */
	private void testSSL(SSOConfiguration config, String url,int expectedStatus) {
		final int standardHttpsPort = 443;
		
		 Protocol authhttps = new Protocol("https", 
				 new AuthSSLProtocolSocketFactory(config.getAuthenticationDetails()),
		 standardHttpsPort);

		Protocol.registerProtocol("https", authhttps);
		

		HttpClient client = new HttpClient();
		PostMethod method = new PostMethod(url);

		int status = 0;
		try {
			status = client.executeMethod(method);
		} catch (HttpException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		// 403 status implies that it got through the SSL and failed at the server response level, which is ok
		assertEquals("Should have right status code", expectedStatus, status);
	}

}
