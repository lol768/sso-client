/*
 * Created on 08-Aug-2005
 *
 */
package uk.ac.warwick.sso.client;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.configuration.XMLConfiguration;
import org.apache.http.cookie.Cookie;
import org.opensaml.SAMLAssertion;
import org.opensaml.SAMLAttribute;
import org.opensaml.SAMLAttributeStatement;
import org.opensaml.SAMLException;
import org.opensaml.SAMLResponse;

import uk.ac.warwick.userlookup.User;

import junit.framework.TestCase;


public class SSOProxyCookieHelperTests extends TestCase {
	
	public final void testProxyCookie() throws Exception {
		
		SSOProxyCookieHelper helper = new SSOProxyCookieHelper();
		SSOConfiguration config = new SSOConfiguration( new XMLConfiguration(getClass().getResource("sso-config.xml")) );
		
		MockAttributeAuthorityResponseFetcher fetcher = new MockAttributeAuthorityResponseFetcher();
		fetcher.setConfig(config);
		fetcher.setResponse(generateMockResponse());
		helper.setAttributeAuthorityResponseFetcher(fetcher);
		helper.setConfig(config);
		
		User user = new User();
		user.setUserId("cusyac");
		user.getExtraProperties().put(SSOToken.PROXY_GRANTING_TICKET_TYPE,"blah");
		
		Cookie cookie = helper.getProxyHttpClientCookie(new URL("http://blah.com"),user);
		
		assertNotNull(cookie);
		assertEquals("Should have right cookie value","PROXY_TICKET_VALUE",cookie.getValue());
		
	}
	
	public final void testProxyCookieNoSuchTarget() throws Exception {
		
		SSOProxyCookieHelper helper = new SSOProxyCookieHelper();
		SSOConfiguration config = new SSOConfiguration(new XMLConfiguration(getClass().getResource("sso-config.xml")));
	
		
		MockAttributeAuthorityResponseFetcher fetcher = new MockAttributeAuthorityResponseFetcher();
		fetcher.setConfig(config);
		fetcher.setResponse(generateBadMockResponse());
		helper.setAttributeAuthorityResponseFetcher(fetcher);
		helper.setConfig(config);
		
		User user = new User();
		user.setUserId("cusyac");
		user.getExtraProperties().put(SSOToken.PROXY_GRANTING_TICKET_TYPE,"blah");
		
		Cookie cookie = helper.getProxyHttpClientCookie(new URL("http://blah.com"),user);
		
		assertNull(cookie);
		
	}
	
	/**
	 * @return
	 * @throws SAMLException
	 */
	private SAMLResponse generateMockResponse() throws SAMLException {
		SAMLResponse resp = new SAMLResponse();
		SAMLAssertion assertion = new SAMLAssertion();
		SAMLAttributeStatement statement = new SAMLAttributeStatement();
		List values = new ArrayList();
		values.add("PROXY_TICKET_VALUE");
		SAMLAttribute attr = new SAMLAttribute(SSOToken.PROXY_TICKET_TYPE,null,null,0,values);
		statement.addAttribute(attr);
		assertion.addStatement(statement);
		resp.addAssertion(assertion);
		return resp;
	}
	
	/**
	 * @return
	 * @throws SAMLException
	 */
	private SAMLResponse generateBadMockResponse() throws SAMLException {
		SAMLResponse resp = new SAMLResponse();
		SAMLAssertion assertion = new SAMLAssertion();
		SAMLAttributeStatement statement = new SAMLAttributeStatement();
		List values = new ArrayList();
		values.add("PROXY_TICKET_VALUE");
		assertion.addStatement(statement);
		resp.addAssertion(assertion);
		return resp;
	}

}
