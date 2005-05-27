/*
 * Created on 21-Apr-2005
 *
 */
package uk.ac.warwick.sso.client;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.security.KeyStore;
import java.security.cert.Certificate;

import junit.framework.TestCase;

import org.apache.commons.httpclient.util.Base64;
import org.opensaml.SAMLException;
import org.opensaml.SAMLPOSTProfile;
import org.opensaml.SAMLResponse;

import uk.ac.warwick.sso.client.ssl.KeyStoreHelper;

public class DecodingTests extends TestCase {

	public final void testDecode64() throws Exception {

		InputStream is = getClass().getResourceAsStream("64encodedauthstatement.txt");

		assertNotNull(is);

		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		String content = "";

		while (reader.ready()) {
			content += reader.readLine();
		}

		reader.close();
		is.close();

		SAMLResponse samlResponse = SAMLPOSTProfile.accept(content.getBytes(), "urn:moleman.warwick.ac.uk:blogbuilder:service",
				5, false);

		try {
			samlResponse.verify();
			assertTrue("Verified signature!", true);
		} catch (SAMLException e) {
			fail("Did not verify signature:" + e.getMessage());
		}

	}

	public final void testDecode64b() throws Exception {

		InputStream is = getClass().getResourceAsStream("64encodedauthstatement2.txt");

		assertNotNull(is);

		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		String content = "";

		while (reader.ready()) {
			content += reader.readLine();
		}

		reader.close();
		is.close();

		SAMLResponse samlResponse = SAMLPOSTProfile.accept(content.getBytes(), "urn:moleman.warwick.ac.uk:blogbuilder:service",
				5, false);

		try {
			
			KeyStoreHelper helper = new KeyStoreHelper();
			KeyStore keyStore = helper.createKeyStore(new URL("file:/j2sdk1.4.2_02/jre/lib/security/moleman.warwick.ac.uk.keystore"), "changeit");
			Certificate originCert = keyStore.getCertificate("testsso.warwick.ac.uk");
			
			samlResponse.verify();
			
			samlResponse.verify(originCert);
			
			assertTrue("Verified signature!", true);
		} catch (SAMLException e) {
			fail("Did not verify signature:" + e.getMessage());
		}

	}

	public final void testCheckSignatureOfValidAuthStatement() throws Exception {

		InputStream is = getClass().getResourceAsStream("validauthstatement.xml");

		assertNotNull(is);

		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		String content = "";

		while (reader.ready()) {
			content += reader.readLine() + "\n";
		}

		reader.close();
		is.close();

		SAMLResponse samlResponse = SAMLPOSTProfile.accept(Base64.encode(content.getBytes()),
				"urn:moleman.warwick.ac.uk:blogbuilder:service", 5, false);

		KeyStoreHelper helper = new KeyStoreHelper();
		InputStream keyStream = getClass().getResourceAsStream("/moleman.warwick.ac.uk.keystore");
		KeyStore store = helper.createKeyStore(keyStream, "changeit");
		Certificate cert = store.getCertificate("moleman.warwick.ac.uk");

		try {
			samlResponse.verify(cert);
			assertTrue("Verified signature!", true);
		} catch (SAMLException e) {
			fail("Did not verify signature:" + e.getMessage());
		}

	}

	public final void testCheckSignatureOfInValidAuthStatement() throws Exception {

		InputStream is = getClass().getResourceAsStream("invalidauthstatement.xml");

		assertNotNull(is);

		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		String content = "";

		while (reader.ready()) {
			content += reader.readLine() + "\n";
		}

		reader.close();
		is.close();

		SAMLResponse samlResponse = SAMLPOSTProfile.accept(Base64.encode(content.getBytes()),
				"urn:moleman.warwick.ac.uk:blogbuilder:service", 5, false);

		try {
			samlResponse.verify();
			fail("Should not have verified signature");
		} catch (SAMLException e) {
			assertTrue("Failed to verifty signature!", true);
		}

	}

}
