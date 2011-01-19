/*
 * Created on 02-Aug-2005
 *
 */
package uk.ac.warwick.sso.client;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableEntryException;
import java.security.KeyStore.Entry;
import java.security.KeyStore.PasswordProtection;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.Configuration;

import uk.ac.warwick.sso.client.ssl.KeyStoreHelper;
import uk.ac.warwick.sso.client.ssl.PKCS1EncodedKeySpec;

/**
 * Holder for a Thread-local SSOConfiguration. Also wraps the loaded
 * Commons Configuration and provides useful methods, so that you don't
 * have to fetch everything by key name.
 * 
 * TODO bust all the Key/cert setup code into another class?
 */
public class SSOConfiguration extends CompositeConfiguration {
	/*
	 * PKCS#1 RSAPrivateKey** (PEM header: BEGIN RSA PRIVATE KEY)
     * PKCS#8 PrivateKeyInfo* (PEM header: BEGIN PRIVATE KEY)
	 */
	private static final String PKCS1_HEADER = "-----BEGIN RSA PRIVATE KEY-----";
	private static final String PKCS1_FOOTER = "-----END RSA PRIVATE KEY-----";
	private static final String PKCS8_HEADER = "-----BEGIN PRIVATE KEY-----";
	private static final String PKCS8_FOOTER = "-----END PRIVATE KEY-----";
	
	private static final ThreadLocal<SSOConfiguration> THREAD_LOCAL = new ThreadLocal<SSOConfiguration>();
	
	private static final Certificate[] CERT_ARRAY = new Certificate[0];
	
	private KeyAuthentication authenticationDetails;
	
	public SSOConfiguration(Configuration delegate){
		addConfiguration(delegate);
	}
	
	/**
	 * Look for either a keystore or a set of PEM-format files.
	 * 
	 * Doesn't currently support files, though the code converts the PEM to DER
	 * before giving it to Java so it should in theory be possible to add support for
	 * DER. 
	 */
	private void initialiseAuthenticationDetails() {
		String keystoreLocation = getString("shire.keystore.location");
		String keystorePassword = getString("shire.keystore.password");
		String keystoreAlias = getString("shire.keystore.shire-alias");
		KeyStoreHelper helper = new KeyStoreHelper();
		Certificate[] certificateChain;
		PrivateKey key;

		try {
			if (keystoreLocation != null) {
				if (keystorePassword == null) {
					throw new IllegalArgumentException("shire.keystore.location was specified but shire.keystore.password wasn't");
				}
				if (keystoreAlias == null) {
					throw new IllegalArgumentException("shire.keystore.location was specified but shire.keystore.shire-alias wasn't");
				}
				try {
					KeyStore keystore = helper.createKeyStore(new URL(resolveClasspathUrl(keystoreLocation)), keystorePassword);
					Entry entry = keystore.getEntry(keystoreAlias, new PasswordProtection(keystorePassword.toCharArray()));
					if (entry == null) {
						throw new RuntimeException("Keystore alias "+keystoreAlias+" not found in "+keystoreLocation);
					} else if ( ! (entry instanceof PrivateKeyEntry) ) {
						throw new RuntimeException("Keystore alias "+keystoreAlias+" is not a PrivateKeyEntry");
					}
					PrivateKeyEntry pke = (PrivateKeyEntry) entry;
					key = pke.getPrivateKey();
					certificateChain = pke.getCertificateChain();
				} catch (CertificateException e) {
					throw new RuntimeException("Error reading certificate from keystore", e);
				}
			} else {
				String certLocation = getString("credentials.certificate");
				String keyLocation = getString("credentials.key");
				String chainLocation = getString("credentials.chain");
				
				ArrayList<Certificate> certificates = new ArrayList<Certificate>();
				
				if (certLocation == null) {
					throw new RuntimeException("credentials.certificate and shire.keystore.location properties missing, you need one of them");
				}
				if (keyLocation == null) {
					throw new RuntimeException("credentials.key property missing");
				}
				
				try {
					URL certLocationUrl = new URL(resolveClasspathUrl(certLocation));
					CertificateFactory fact = CertificateFactory.getInstance("X.509");
					Certificate certificate = fact.generateCertificate(certLocationUrl.openStream());
					certificates.add(certificate);
				} catch (CertificateException e) {
					throw new RuntimeException("Error parsing server certificate - ensure you have no extra whitespace at start or end of file", e);
				}
				
				// optional intermediate chain
				if (chainLocation != null) {
					try {
						URL chainLocationUrl = new URL(resolveClasspathUrl(chainLocation));
						CertificateFactory chainFact = CertificateFactory.getInstance("X.509");
						Collection<? extends Certificate> chain = chainFact.generateCertificates(chainLocationUrl.openStream());
						certificates.addAll(chain);
					} catch (CertificateException e) {
						// The parser is totally unforgiving and will throw an exception if there is any
						// whitespace other than a single newline after each certificate.
						throw new RuntimeException("Error parsing certificate chain - ensure you have no extra lines between certificates, and no extra whitespace at start or end of file", e);
					}
				}
				
				URL keyLocationUrl = new URL(resolveClasspathUrl(keyLocation));
				KeyFactory kact = KeyFactory.getInstance("RSA");
				String data = new String(readToArray(keyLocationUrl.openStream()), "ASCII");
				byte[] bytes = extractPem(data, PKCS1_HEADER, PKCS1_FOOTER);
				if ((bytes = extractPem(data, PKCS1_HEADER, PKCS1_FOOTER)) != null) {
					key = kact.generatePrivate(new PKCS1EncodedKeySpec(bytes).getKeySpec());
				} else if ((bytes = extractPem(data, PKCS8_HEADER, PKCS8_FOOTER)) != null) {
					key = kact.generatePrivate(new PKCS8EncodedKeySpec(bytes));
				} else {
					throw new IllegalArgumentException("Unrecognised key format, must be RSA PEM key in PKCS1 or PKCS8");
				}
				
				certificateChain = certificates.toArray(CERT_ARRAY);
			}
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (UnrecoverableEntryException e) {
			throw new RuntimeException(e);
		} catch (KeyStoreException e) {
			throw new RuntimeException(e);
		} catch (InvalidKeySpecException e) {
			throw new RuntimeException(e);
		}
		
		authenticationDetails = new KeyAuthentication(key, certificateChain);
		String cacertsLocation = getString("cacertskeystore.location");
		try {
			URL cacertsURL = cacertsLocation==null ? null : new URL(cacertsLocation);
			authenticationDetails.setCacerts(
					cacertsURL,
					getString("cacertskeystore.password")
			);
		} catch (MalformedURLException e) {
			throw new IllegalArgumentException("cacertskeystore.location was an invalid URL", e);
		}
	}

	private byte[] readToArray(InputStream dis) throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		try {
			byte[] buf = new byte[4096];
			int r = -1;
			while ((r=dis.read(buf)) > -1) {
				bos.write(buf, 0, r);
			}
		} finally {
			dis.close();
		}
		return bos.toByteArray();
	}
	
	private byte[] extractPem(String data, String start, String end) throws UnsupportedEncodingException {
		int i0 = data.indexOf(start);
		int i1 = data.indexOf(end);
		if (i0 > -1 && i1 > -1) {
			String base64 = data.substring(i0+start.length(), i1);
			return new Base64().decode(base64.replaceAll("\\s", "").getBytes("ASCII"));
		}
		return null;
	}
	
	// lay-zee
	public KeyAuthentication getAuthenticationDetails() {
		if (authenticationDetails == null) {
			initialiseAuthenticationDetails();
		}
		return authenticationDetails;
	}

	public static final SSOConfiguration getConfig() {
		return THREAD_LOCAL.get();
	}

	public static final void setConfig(final SSOConfiguration config) {
		THREAD_LOCAL.set(config);
	}
	
	// Support Spring-style classpath protocol for resources.
	public String resolveClasspathUrl(String url) {
		if (url.startsWith("classpath:")) {
			URL url2 = getClass().getResource(url.substring("classpath:".length()));
			if (url2 != null) {
				return "file:"+url2.getFile();
			}
		}
		return url;
	}

}
