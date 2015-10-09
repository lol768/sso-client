/*
 * Created on 07-Jun-2005
 *
 */
package uk.ac.warwick.sso.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.XMLConfiguration;
import org.opensaml.SAMLAssertion;
import org.opensaml.SAMLAttribute;
import org.opensaml.SAMLAttributeStatement;
import org.opensaml.SAMLException;
import org.opensaml.SAMLResponse;
import org.springframework.util.FileCopyUtils;

import uk.ac.warwick.sso.client.cache.InMemoryUserCache;
import uk.ac.warwick.userlookup.User;
import uk.ac.warwick.userlookup.UserLookup;
import uk.ac.warwick.sso.client.core.Cookie;
import uk.ac.warwick.util.cache.Caches;

public class ShireCommandTests extends TestCase {

	public final void testShireCommand() throws Exception {

		ShireCommand command = new ShireCommand(Caches.<String, User>newCache(UserLookup.USER_CACHE_NAME, null, 0));

		Configuration config = new XMLConfiguration(getClass().getResource("sso-config.xml"));

		MockAttributeAuthorityResponseFetcher fetcher = new MockAttributeAuthorityResponseFetcher();
		fetcher.setConfig(new SSOConfiguration(config));

		SAMLResponse resp = generateMockResponse();
		fetcher.setResponse(resp);
		command.setAaFetcher(fetcher);

		String saml = loadFromClasspath("samplesaml.xml").trim();
		
		String saml64 = new String(Base64.encodeBase64(saml.getBytes()));
		
		String target = "https%3A%2F%2Fmyapp.warwick.ac.uk%2Forigin%2Fsysadmin%2Fviewauthlogs.htm";
		target = URLDecoder.decode(target, "UTF-8");
		
		System.err.println(saml64);

		command.setConfig(config);
		command.setCache(new InMemoryUserCache());
		Cookie cookie = command.process(saml64, target);

		assertNotNull(cookie);
		
		assertEquals("Should have right cookie","Testing123",cookie.getValue());

	}

	private String loadFromClasspath(String string) throws IOException {
		InputStream resourceAsStream = getClass().getResourceAsStream(string);
		return FileCopyUtils.copyToString(new InputStreamReader(resourceAsStream));
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
		values.add("SSC_VALUE");
		SAMLAttribute attr = new SAMLAttribute("urn:websignon:ssc",null,null,0,values);
		statement.addAttribute(attr);
		assertion.addStatement(statement);
		resp.addAssertion(assertion);
		return resp;
	}

}
