/*
 * Created on 21-Mar-2005
 *
 */
package uk.ac.warwick.sso.client;

import junit.framework.TestCase;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.XMLConfiguration;


public class ConfigurationTest extends TestCase {

	public ConfigurationTest() {
		super();
	}

	public final void testLoadConfiguration() throws Exception {
		Configuration config = new XMLConfiguration(getClass().getResource("sso-config.xml"));
		assertNotNull(config);
	}
	
	public final void testLoadFullConfig() throws Exception {
		
		Configuration config = new XMLConfiguration(getClass().getResource("sso-config.xml"));
		String loginLocation = config.getString("origin.login.location");
		assertNotNull(loginLocation);
		String aaLocation = config.getString("origin.attributeauthority.location");
		assertNotNull(aaLocation);
		String shire = config.getString("shire.location");
		assertNotNull(shire);
		String cookieName = config.getString("shire.sscookie.name");
		assertNotNull(cookieName);
		String cookiePath = config.getString("shire.sscookie.path");
		assertNotNull(cookiePath);
		String cookieDomain = config.getString("shire.sscookie.domain");
		assertNotNull(cookieDomain);
		String providerId = config.getString("shire.providerid");
		assertNotNull(providerId);
		String keystoreLocation = config.getString("shire.keystore.location");
		assertNotNull(keystoreLocation);
		String keystorePassword = config.getString("shire.keystore.password");
		assertNotNull(keystorePassword);
		
	}

}
