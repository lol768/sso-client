/*
 * Created on 9 Mar 2007
 *
 */
package uk.ac.warwick.sso.client;

import org.jmock.cglib.MockObjectTestCase;
import org.springframework.mock.web.MockServletContext;

import uk.ac.warwick.sso.client.cache.spring.DatabaseUserCache;
import uk.ac.warwick.sso.client.cache.InMemoryUserCache;

public final class SSOConfigLoaderTest extends MockObjectTestCase {

	public void testConfigLoader() throws Exception {

		MockServletContext context = new MockServletContext();
		context.addInitParameter("ssoclient.config","sso-config.xml");

		SSOConfigLoader loader = new SSOConfigLoader();
		loader.loadSSOConfig(context);

		Object cache = context.getAttribute(SSOConfigLoader.SSO_CACHE_KEY);
		
		assertNotNull(cache);
		
		assertTrue(cache instanceof InMemoryUserCache);

		Object config = context.getAttribute(SSOConfigLoader.SSO_CONFIG_KEY);
		assertNotNull(config);

	}
	
	public void testConfigLoaderClustered() throws Exception {

		MockServletContext context = new MockServletContext();
		context.addInitParameter("ssoclient.config","sso-config-clustered.xml");

		SSOConfigLoader loader = new SSOConfigLoader();
		loader.loadSSOConfig(context);
		
		Object cache = context.getAttribute(SSOConfigLoader.SSO_CACHE_KEY);
		
		assertNotNull(cache);
		
		assertTrue(cache instanceof DatabaseUserCache);

		Object config = context.getAttribute(SSOConfigLoader.SSO_CONFIG_KEY);
		assertNotNull(config);

	}

}
