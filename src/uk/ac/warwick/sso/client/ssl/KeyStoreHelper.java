/*
 * Created on 21-Apr-2005
 *
 */
package uk.ac.warwick.sso.client.ssl;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableEntryException;
import java.security.KeyStore.Entry;
import java.security.KeyStore.PasswordProtection;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.Enumeration;

import org.apache.log4j.Logger;

public class KeyStoreHelper {

	private static final Logger LOGGER = Logger.getLogger(KeyStoreHelper.class);

	public KeyStore createKeyStore(final URL url, final String password) throws KeyStoreException, NoSuchAlgorithmException,
			CertificateException, IOException {
		if (url == null) {
			throw new IllegalArgumentException("Keystore url may not be null");
		}
		InputStream openStream = url.openStream();
		if (openStream == null) {
			throw new IllegalArgumentException("Couldn't find specified keystore");
		}
		return createKeyStore(openStream, password);
	}

	public KeyStore createKeyStore(final InputStream keyStream, final String password) throws KeyStoreException,
			NoSuchAlgorithmException, CertificateException, IOException {
		LOGGER.debug("Initializing key store from stream");
		KeyStore keystore = KeyStore.getInstance("jks");
		keystore.load(keyStream, password != null ? password.toCharArray() : null);
		return keystore;
	}
	
	/**
     * Finds a Key entry in the given alias of the keystore.
     * An exception is thrown if the alias isn't found in the keystore,
     * or if the keystore couldn't be loaded.
     */
    public Key getKey(final KeyStore keyStore, final String alias, final String password) {
        try {
        	if (password == null) {
        		throw new RuntimeException("No keystore password has been specified under shire.keystore.password");
        	}
			Key key = keyStore.getKey(alias, password.toCharArray());
        	if (key == null) {
        		StringBuilder sb = new StringBuilder("");
        		for (Enumeration<String> aliases = keyStore.aliases(); aliases.hasMoreElements();) {
        			String a = aliases.nextElement();
        			sb.append(" ").append(a);
        		}
        		throw new RuntimeException("Key with alias "+alias+" was not found in the keystore. Aliases in keystore:"+sb.toString());
        	}
        	return key;
        } catch (Exception e) {
            throw new RuntimeException("Could not load key from keystore", e);
        }

    }
    
    public Entry getEntry(final KeyStore keyStore, final String alias, final String password) throws NoSuchAlgorithmException, UnrecoverableEntryException, KeyStoreException {
    	return keyStore.getEntry(alias, new PasswordProtection(password.toCharArray()));
    }

    /**
     * @return
     * @throws KeyStoreException
     * @throws NoSuchAlgorithmException
     * @throws CertificateException
     * @throws IOException
     * @throws MalformedURLException
     */
    public Certificate getCertificate(final KeyStore keyStore, final String alias) {
        try {
            if (alias == null) {
            	throw new IllegalArgumentException("Tried to request a null alias from a keystore");
            }
            Certificate originCert = keyStore.getCertificate(alias);
            if (originCert == null) {
            	
            	throw new IllegalArgumentException("Couldn't find a certificate under alias '"
            			+alias+"' in keystore, aliases: " + toString(keyStore.aliases()));
            }
            return originCert;
        } catch (Exception e) {
            LOGGER.error("Could not load keystore", e);
            throw new RuntimeException("Could not load keystore", e);
        }

    }
    
    private String toString(Enumeration<String> enumer) {
		StringBuilder sb = new StringBuilder("[");
		while (enumer.hasMoreElements()) {
			sb.append(enumer.nextElement());
			sb.append(','); // Yeah trailing comma, big whoop.
		}
		sb.append("]");
		return sb.toString();
	}

}
