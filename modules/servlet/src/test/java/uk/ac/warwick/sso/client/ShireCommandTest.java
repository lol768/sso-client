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
import org.junit.Before;
import org.junit.Test;
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
import uk.ac.warwick.util.cache.Cache;
import uk.ac.warwick.util.cache.Caches;

import static org.junit.Assert.*;

public class ShireCommandTest {

	private ShireCommand command;
	private String saml64;
	private String target;
	private InMemoryUserCache cache;
	private Cache<String, User> userIdCache;
	private MockAttributeAuthorityResponseFetcher fetcher;

	@Before
	public void setup() throws Exception {
		userIdCache = Caches.<String, User>newCache(UserLookup.USER_CACHE_NAME, null, 0);
		userIdCache.clear();
		command = new ShireCommand(userIdCache);

		fetcher = new MockAttributeAuthorityResponseFetcher();

		SAMLResponse resp = generateMockResponse();
		fetcher.setResponse(resp);
		command.setAaFetcher(fetcher);

		String saml = loadFromClasspath("samplesaml.xml").trim();

		saml64 = new String(Base64.encodeBase64(saml.getBytes()));

		target = "https%3A%2F%2Fmyapp.warwick.ac.uk%2Forigin%2Fsysadmin%2Fviewauthlogs.htm";
		target = URLDecoder.decode(target, "UTF-8");

		setupWithConfig("sso-config.xml");
	}

	private void setupWithConfig(String name) throws Exception {
		Configuration config = new XMLConfiguration(getClass().getResource(name));
		SSOConfiguration ssoConfig = new SSOConfiguration(config);

		fetcher.setConfig(ssoConfig);

		cache = new InMemoryUserCache(ssoConfig);

		command.setConfig(ssoConfig);
		command.setCache(cache);
	}

	@Test
	public final void testShireCommand() throws Exception {
		Cookie cookie = command.process(saml64, target);
		assertNotNull(cookie);
		User user = userIdCache.get("Test");
		assertNotNull("Expecting a cached user", user);
		assertEquals("Testing123", user.getExtraProperties().get(SSOToken.SSC_TICKET_TYPE));
		assertEquals("Should have right cookie","Testing123",cookie.getValue());
	}

	/**
	 * Shire normally updates the userid cache with the User it's received from SAML,
	 * to speed up lookups by ID. In some cases this is an issue because the extraProperties
	 * have different keys, so we need to be able to disable that behaviour.
	 */
	@Test
	public final void noUpdateCache() throws Exception {
		setupWithConfig("sso-config-noidcache.xml");
		Cookie cookie = command.process(saml64, target);
		assertNotNull(cookie);
		assertFalse("Expecting no cached user", userIdCache.contains("Test"));
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
