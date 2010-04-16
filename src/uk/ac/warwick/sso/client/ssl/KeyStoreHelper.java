/*
 * Created on 21-Apr-2005
 *
 */
package uk.ac.warwick.sso.client.ssl;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import org.apache.log4j.Logger;

public class KeyStoreHelper {

	private static final Logger LOGGER = Logger.getLogger(KeyStoreHelper.class);

	public KeyStore createKeyStore(final URL url, final String password) throws KeyStoreException, NoSuchAlgorithmException,
			CertificateException, IOException {
		if (url == null) {
			throw new IllegalArgumentException("Keystore url may not be null");
		}
		InputStream openStream = url.openStream();
		return createKeyStore(openStream, password);
	}

	public KeyStore createKeyStore(final InputStream keyStream, final String password) throws KeyStoreException,
			NoSuchAlgorithmException, CertificateException, IOException {
		LOGGER.debug("Initializing key store from stream");
		KeyStore keystore = KeyStore.getInstance("jks");
		keystore.load(keyStream, password != null ? password.toCharArray() : null);
		return keystore;
	}

}
