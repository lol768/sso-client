package uk.ac.warwick.sso.client;

import static org.junit.Assert.*;

import java.security.KeyStore;
import java.security.KeyStore.PasswordProtection;
import java.security.cert.Certificate;

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
}
