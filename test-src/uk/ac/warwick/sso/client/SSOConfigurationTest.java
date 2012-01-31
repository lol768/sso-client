package uk.ac.warwick.sso.client;

import static org.junit.Assert.*;

import java.security.KeyStore;
import java.security.KeyStore.PasswordProtection;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.junit.Test;



public class SSOConfigurationTest {
	
	/* OpenSSL rsa command normally creates keys encoded to PKCS1 standard.
	 * In PEM format they are identified by "BEGIN RSA PRIVATE KEY" 
	 */
	@Test public void loadCredsFromOpenssl() throws Exception {
		SSOConfiguration config = new SSOConfiguration(new PropertiesConfiguration(getClass().getResource("/resources/certs/selfsign-pkcs1.properties")));
		KeyAuthentication authenticationDetails = config.getAuthenticationDetails();
		assertEquals("X.509", authenticationDetails.getCertificate().getType());
		
		Certificate cert = authenticationDetails.getCertificate();
		cert.verify( cert.getPublicKey() ); // verify is self-signed
		
		assertEquals(3, authenticationDetails.getCertificates().length);
		
		// test programmatic keystore
		KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
		ks.load(null, null);
		ks.setEntry("mykey", authenticationDetails.toPrivateKeyEntry(), new KeyStore.PasswordProtection("changeit".toCharArray()));
		ks.getEntry("mykey", new KeyStore.PasswordProtection("changeit".toCharArray()));
	}
	
	/* Keys created within a keystore are by default PKCS8.
	 * In PEM format they are identified by "BEGIN PRIVATE KEY"
	 */
	@Test public void loadCredsFromKeytool() throws Exception {
		SSOConfiguration config = new SSOConfiguration(new PropertiesConfiguration(getClass().getResource("/resources/certs/selfsign-pkcs8.properties")));
		KeyAuthentication authenticationDetails = config.getAuthenticationDetails();
		assertEquals("X.509", authenticationDetails.getCertificate().getType());
		
		Certificate cert = authenticationDetails.getCertificate();
		cert.verify( cert.getPublicKey() ); // verify is self-signed
	}
	
	/**
	 * Keystore with a proper chain (though the CA and intermediate are fake, it's a proper chain)
	 * @throws Exception
	 */
	@Test public void loadCredsFromJksKeystore() throws Exception {
		SSOConfiguration config = new SSOConfiguration(new PropertiesConfiguration(getClass().getResource("/resources/certs/oauthclient-signed.properties")));
		KeyAuthentication authenticationDetails = config.getAuthenticationDetails();
		
		X509Certificate[] certs = (X509Certificate[]) authenticationDetails.getCertificates();
		assertEquals(3, certs.length);
		
		certs[0].verify(certs[1].getPublicKey());
		certs[1].verify(certs[2].getPublicKey());
		certs[2].verify(certs[2].getPublicKey());
	}
	
	@Test public void defaultValues() throws Exception {
		BaseConfiguration configuration = new BaseConfiguration();
		configuration.addProperty("mode", "new");		
		SSOConfiguration config = new SSOConfiguration(configuration);
		
		assertEquals(config.getString("origin.originid"), "urn:mace:eduserv.org.uk:athens:provider:warwick.ac.uk");
		assertEquals(config.getString("origin.login.location"), "https://websignon.warwick.ac.uk/origin/hs");
		assertEquals(config.getString("origin.logout.location"), "https://websignon.warwick.ac.uk/origin/logout");
		assertEquals(config.getString("origin.attributeauthority.location"), "https://websignon.warwick.ac.uk/origin/aa");
	}
}
