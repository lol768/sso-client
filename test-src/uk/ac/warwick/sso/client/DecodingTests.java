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

import org.apache.commons.codec.binary.Base64;
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

		SAMLResponse samlResponse = SAMLPOSTProfile.accept(content.getBytes("US-ASCII"), "urn:moleman.warwick.ac.uk:blogbuilder:service",
				5, false);

		try {
			samlResponse.verify();
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
		
		Base64 base64 = new Base64();
		
		SAMLResponse samlResponse = SAMLPOSTProfile.accept(base64.encode(content.getBytes()),
				"urn:moleman.warwick.ac.uk:blogbuilder:service", 5, false);

		KeyStoreHelper helper = new KeyStoreHelper();
		InputStream keyStream = getClass().getResourceAsStream("/myapp.warwick.ac.uk.keystore");
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

		Base64 base64 = new Base64();
		
		SAMLResponse samlResponse = SAMLPOSTProfile.accept(base64.encode(content.getBytes()),
				"urn:moleman.warwick.ac.uk:blogbuilder:service", 5, false);

		try {
			samlResponse.verify();
			fail("Should not have verified signature");
		} catch (SAMLException e) {
			assertTrue("Failed to verifty signature!", true);
		}

	}

}
