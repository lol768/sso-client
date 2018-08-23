/*
 * Created on 02-Aug-2005
 *
 */
package uk.ac.warwick.sso.client;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.NoSuchElementException;

import org.apache.commons.configuration.*;

import uk.ac.warwick.sso.client.internal.KeyAndCertUtils;
import uk.ac.warwick.sso.client.ssl.KeyStoreHelper;

/**
 * Holder for a Thread-local SSOConfiguration. Also wraps the loaded
 * Commons Configuration and provides useful methods, so that you don't
 * have to fetch everything by key name.
 * 
 * TODO bust all the Key/cert setup code into another class?
 */
public class SSOConfiguration extends CompositeConfiguration {
	
	private static final ThreadLocal<SSOConfiguration> THREAD_LOCAL = new ThreadLocal<SSOConfiguration>();
	
	private static final Certificate[] CERT_ARRAY = new Certificate[0];
	
	private static final String LOGIN_LOCATION_KEY = "origin.login.location";
	
	private KeyAuthentication authenticationDetails;

	// These properties come first so they override any other properties.
	private BaseConfiguration overrides = new BaseConfiguration();
	
	public SSOConfiguration(Configuration delegate){
		addConfiguration(overrides);
		addConfiguration(delegate);
		addDefaultConfiguration();
		setThrowExceptionOnMissing(true);
		
		String loginLocation = getString(LOGIN_LOCATION_KEY, null);
		if (loginLocation == null) {
			String mode = getString("mode", null);
			if ("new".equals(mode)) {
				addProperty(LOGIN_LOCATION_KEY, "https://websignon.warwick.ac.uk/origin/hs");
			} else if ("old".equals(mode)) {
				addProperty(LOGIN_LOCATION_KEY, "https://websignon.warwick.ac.uk/origin/slogin");
			}
		}

		// Copy some properties to legacy locations, so we can use the newer property name
		// without breaking compatibility.
		copyString("origin.location", "userlookup.ssosUrl");
		copyString("webgroups.location", "userlookup.groupservice.location");
	}

	private void copyString(String from, String to) {
		final String value = getString(from, null);
		if (value != null) {
			addOverride(to, value);
		}
	}

	public void addOverride(String key, Object value) {
		overrides.addProperty(key, value);
	}

	void addDefaultConfiguration() {
		try {
			addConfiguration( new PropertiesConfiguration(SSOConfiguration.class.getResource("/default-ssoclient.properties")) );
		} catch (ConfigurationException e) {
		}
	}
	
	/**
	 * Look for either a keystore or a set of PEM-format files.
	 * 
	 * Doesn't currently support files, though the code converts the PEM to DER
	 * before giving it to Java so it should in theory be possible to add support for
	 * DER. 
	 */
	private void initialiseAuthenticationDetails() {
		String keystoreLocation = getString("shire.keystore.location", null);
		String keystorePassword = getString("shire.keystore.password", null);
		String keystoreAlias = getString("shire.keystore.shire-alias", null);
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
				String certLocation = getString("credentials.certificate", null);
				String keyLocation = getString("credentials.key", null);
				String chainLocation = getString("credentials.chain", null);
				
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
				key = KeyAndCertUtils.decodeRSAPrivateKey(keyLocationUrl.openStream());
				
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
		String cacertsLocation = getString("cacertskeystore.location", null);
		try {
			URL cacertsURL = cacertsLocation==null ? null : new URL(cacertsLocation);
			authenticationDetails.setCacerts(
					cacertsURL,
					getString("cacertskeystore.password", null)
			);
		} catch (MalformedURLException e) {
			throw new IllegalArgumentException("cacertskeystore.location was an invalid URL", e);
		}
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
	
	static final void removeConfig() {
	    THREAD_LOCAL.remove();
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
