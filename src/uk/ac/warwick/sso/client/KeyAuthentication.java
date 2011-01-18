package uk.ac.warwick.sso.client;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.PrivateKey;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.cert.Certificate;

/**
 * Stores a key and certificate in the form of either a keystore or
 */
public class KeyAuthentication {
	private final PrivateKey key;
	private final Certificate[] certificateChain;
    
    private URL cacertsLocation;
    private String cacertsPassword;
	
	public KeyAuthentication(PrivateKey key, Certificate[] certificateChain) {
		super();
		this.key = key;
		this.certificateChain = certificateChain;
		if (certificateChain == null || certificateChain.length == 0) {
			throw new IllegalArgumentException("certificateChain must have at least one Certificate");
		}
	}

	public void setCacerts(URL location, String password) {
		this.cacertsLocation = location;
		this.cacertsPassword = password;
	}
	
	public PrivateKey getKey() {
		return key;
	}
	
	public PrivateKeyEntry toPrivateKeyEntry() {
		return new PrivateKeyEntry(getKey(), getCertificates());
	}
	
	public Certificate[] getCertificates() {
		return certificateChain;
	}
	
	public Certificate getCertificate() {
		return certificateChain[0];
	}

	public URL getCacertsLocation() {
		return cacertsLocation;
	}

	public String getCacertsPassword() {
		return cacertsPassword;
	}

}
