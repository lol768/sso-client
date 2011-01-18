/*
 /*
 * $Header$
 * $Revision$
 * $Date$
 *
 * ====================================================================
 *
 *  Copyright 2002-2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */

package uk.ac.warwick.sso.client.ssl;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Enumeration;

import org.apache.commons.httpclient.ConnectTimeoutException;
import org.apache.commons.httpclient.params.HttpConnectionParams;
import org.apache.commons.httpclient.protocol.ControllerThreadSocketFactory;
import org.apache.commons.httpclient.protocol.SecureProtocolSocketFactory;
import org.apache.log4j.Logger;

import uk.ac.warwick.sso.client.KeyAuthentication;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.ManagerFactoryParameters;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

/**
 * <p>
 * AuthSSLProtocolSocketFactory can be used to validate the identity of the HTTPS server against a list of trusted
 * certificates and to authenticate to the HTTPS server using a private key.
 * </p>
 * 
 * <p>
 * AuthSSLProtocolSocketFactory will enable server authentication when supplied with a {@link KeyStore truststore} file
 * containg one or several trusted certificates. The client secure socket will reject the connection during the SSL
 * session handshake if the target HTTPS server attempts to authenticate itself with a non-trusted certificate.
 * </p>
 * 
 * <p>
 * Use JDK keytool utility to import a trusted certificate and generate a truststore file:
 * 
 * <pre>
 *     
 *          keytool -import -alias &quot;my server cert&quot; -file server.crt -keystore my.truststore
 *         
 * </pre>
 * 
 * </p>
 * 
 * <p>
 * AuthSSLProtocolSocketFactory will enable client authentication when supplied with a {@link KeyStore keystore} file
 * containg a private key/public certificate pair. The client secure socket will use the private key to authenticate
 * itself to the target HTTPS server during the SSL session handshake if requested to do so by the server. The target
 * HTTPS server will in its turn verify the certificate presented by the client in order to establish client's
 * authenticity
 * </p>
 * 
 * <p>
 * Use the following sequence of actions to generate a keystore file
 * </p>
 * <ul>
 * <li>
 * <p>
 * Use JDK keytool utility to generate a new key
 * 
 * <pre>
 *     keytool -genkey -v -alias &quot;my client key&quot; -validity 365 -keystore my.keystore
 * </pre>
 * 
 * For simplicity use the same password for the key as that of the keystore
 * </p>
 * </li>
 * <li>
 * <p>
 * Issue a certificate signing request (CSR)
 * 
 * <pre>
 *     keytool -certreq -alias &quot;my client key&quot; -file mycertreq.csr -keystore my.keystore
 * </pre>
 * 
 * </p>
 * </li>
 * <li>
 * <p>
 * Send the certificate request to the trusted Certificate Authority for signature. One may choose to act as her own CA
 * and sign the certificate request using a PKI tool, such as OpenSSL.
 * </p>
 * </li>
 * <li>
 * <p>
 * Import the trusted CA root certificate
 * 
 * <pre>
 *     keytool -import -alias &quot;my trusted ca&quot; -file caroot.crt -keystore my.keystore
 * </pre>
 * 
 * </p>
 * </li>
 * <li>
 * <p>
 * Import the PKCS#7 file containg the complete certificate chain
 * 
 * <pre>
 *     keytool -import -alias &quot;my client key&quot; -file mycert.p7 -keystore my.keystore
 * </pre>
 * 
 * </p>
 * </li>
 * <li>
 * <p>
 * Verify the content the resultant keystore file
 * 
 * <pre>
 *     keytool -list -v -keystore my.keystore
 * </pre>
 * 
 * </p>
 * </li>
 * </ul>
 * <p>
 * Example of using custom protocol socket factory for a specific host:
 * 
 * <pre>
 * Protocol authhttps = new Protocol(&quot;https&quot;, new AuthSSLProtocolSocketFactory(new URL(&quot;file:my.keystore&quot;), &quot;mypassword&quot;, new URL(
 * 		&quot;file:my.truststore&quot;), &quot;mypassword&quot;), 443);
 * 
 * HttpClient client = new HttpClient();
 * client.getHostConfiguration().setHost(&quot;localhost&quot;, 443, authhttps);
 * // use relative url only
 * GetMethod httpget = new GetMethod(&quot;/&quot;);
 * client.executeMethod(httpget);
 * </pre>
 * 
 * </p>
 * <p>
 * Example of using custom protocol socket factory per default instead of the standard one:
 * 
 * <pre>
 * Protocol authhttps = new Protocol(&quot;https&quot;, new AuthSSLProtocolSocketFactory(new URL(&quot;file:my.keystore&quot;), &quot;mypassword&quot;, new URL(
 * 		&quot;file:my.truststore&quot;), &quot;mypassword&quot;), 443);
 * Protocol.registerProtocol(&quot;https&quot;, authhttps);
 * 
 * HttpClient client = new HttpClient();
 * GetMethod httpget = new GetMethod(&quot;https://localhost/&quot;);
 * client.executeMethod(httpget);
 * </pre>
 * 
 * </p>
 * 
 * @author <a href="mailto:oleg -at- ural.ru">Oleg Kalnichevski</a>
 * 
 * <p>
 * DISCLAIMER: HttpClient developers DO NOT actively support this component. The component is provided as a reference
 * material, which may be inappropriate to be used without additional customization.
 * </p>
 */

public class AuthSSLProtocolSocketFactory implements SecureProtocolSocketFactory {

	private static final char[] STANDARD_STOREPASS = "changeit".toCharArray();

	/** Log object for this class. */
	private static final Logger LOGGER = Logger.getLogger(AuthSSLProtocolSocketFactory.class);

//	private URL keystoreUrl = null;
//
//	private String keystorePassword = null;

	private URL truststoreUrl = null;

	private String truststorePassword = null;

	private SSLContext sslcontext = null;

	private KeyAuthentication authenticationDetails;

	private KeyStore keystore;

	/**
	 * Constructor for AuthSSLProtocolSocketFactory. Either a keystore or truststore file must be given. Otherwise SSL
	 * context initialization error will result.
	 * 
	 * @param keystoreUrl
	 *            URL of the keystore file. May be <tt>null</tt> if HTTPS client authentication is not to be used.
	 * @param keystorePassword
	 *            Password to unlock the keystore. IMPORTANT: this implementation assumes that the same password is used
	 *            to protect the key and the keystore itself.
	 * @param truststoreUrl
	 *            URL of the truststore file. May be <tt>null</tt> if HTTPS server authentication is not to be used.
	 * @param truststorePassword
	 *            Password to unlock the truststore.
	 */
//	public AuthSSLProtocolSocketFactory(final URL keystoreUrl, final String keystorePassword, final URL truststoreUrl,
//			final String truststorePassword) {
//		super();
//		this.keystoreUrl = keystoreUrl;
//		this.keystorePassword = keystorePassword;
//		this.truststoreUrl = truststoreUrl;
//		this.truststorePassword = truststorePassword;
//	}

	public AuthSSLProtocolSocketFactory(KeyAuthentication authenticationDetails) {
		this.authenticationDetails = authenticationDetails;
		this.truststoreUrl = authenticationDetails.getCacertsLocation();
		this.truststorePassword = authenticationDetails.getCacertsPassword();
		try {
			// create a dummy keystore holding our credentials, SSL factories are happiest with a keystore
			keystore = KeyStore.getInstance(KeyStore.getDefaultType());
			keystore.load(null, null);
			keystore.setEntry("mykey", authenticationDetails.toPrivateKeyEntry(), new KeyStore.PasswordProtection(STANDARD_STOREPASS));
		} catch (KeyStoreException e) {
			throw new RuntimeException(e);
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		} catch (CertificateException e) {
			throw new IllegalStateException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private KeyManager[] createKeyManagers() throws KeyStoreException,
			NoSuchAlgorithmException, UnrecoverableKeyException, InvalidAlgorithmParameterException {
		if (keystore == null) {
			throw new IllegalArgumentException("Keystore may not be null");
		}
		LOGGER.debug("Initializing key manager");
		KeyManagerFactory kmfactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
		kmfactory.init(keystore, STANDARD_STOREPASS);
		return kmfactory.getKeyManagers();
	}

	private static TrustManager[] createTrustManagers(final KeyStore keystore) throws KeyStoreException, NoSuchAlgorithmException {
		if (keystore == null) {
			throw new IllegalArgumentException("Keystore may not be null");
		}
		LOGGER.debug("Initializing trust manager");
		TrustManagerFactory tmfactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
		tmfactory.init(keystore);
		TrustManager[] trustmanagers = tmfactory.getTrustManagers();
		for (int i = 0; i < trustmanagers.length; i++) {
			if (trustmanagers[i] instanceof X509TrustManager) {
				trustmanagers[i] = new AuthSSLX509TrustManager((X509TrustManager) trustmanagers[i]);
			}
		}
		return trustmanagers;
	}

	private SSLContext createSSLContext() {
		try {
			KeyManager[] keymanagers = createKeyManagers();
			TrustManager[] trustmanagers = null;
			if (this.truststoreUrl != null) {
				KeyStoreHelper helper = new KeyStoreHelper();
				KeyStore keystore = helper.createKeyStore(this.truststoreUrl, this.truststorePassword);
				trustmanagers = createTrustManagers(keystore);
			}
			SSLContext ctx = SSLContext.getInstance("SSL");
			ctx.init(keymanagers, trustmanagers, null);
			return ctx;
		} catch (NoSuchAlgorithmException e) {
			LOGGER.error(e.getMessage(), e);
			throw new AuthSSLInitializationError("Unsupported algorithm exception: " + e.getMessage());
		} catch (KeyStoreException e) {
			LOGGER.error(e.getMessage(), e);
			throw new AuthSSLInitializationError("Keystore exception: " + e.getMessage());
		} catch (GeneralSecurityException e) {
			LOGGER.error(e.getMessage(), e);
			throw new AuthSSLInitializationError("Key management exception: " + e.getMessage());
		} catch (IOException e) {
			LOGGER.error(e.getMessage(), e);
			throw new AuthSSLInitializationError("I/O error reading keystore/truststore file: " + e.getMessage());
		}
	}

	private SSLContext getSSLContext() {
		if (this.sslcontext == null) {
			this.sslcontext = createSSLContext();
		}
		return this.sslcontext;
	}

	/**
	 * @see SecureProtocolSocketFactory#createSocket(java.lang.String,int,java.net.InetAddress,int)
	 */
	public Socket createSocket(String host, int port, InetAddress clientHost, int clientPort) throws IOException,
			UnknownHostException {
		return getSSLContext().getSocketFactory().createSocket(host, port, clientHost, clientPort);
	}

	/**
	 * @see SecureProtocolSocketFactory#createSocket(java.lang.String,int)
	 */
	public Socket createSocket(String host, int port) throws IOException, UnknownHostException {
		return getSSLContext().getSocketFactory().createSocket(host, port);
	}

	/**
	 * @see SecureProtocolSocketFactory#createSocket(java.net.Socket,java.lang.String,int,boolean)
	 */
	public Socket createSocket(Socket socket, String host, int port, boolean autoClose) throws IOException, UnknownHostException {
		return getSSLContext().getSocketFactory().createSocket(socket, host, port, autoClose);
	}

	public Socket createSocket(String host, int port, InetAddress localAddress, int localPort, HttpConnectionParams params)
			throws IOException, UnknownHostException, ConnectTimeoutException {

		if (params == null) {
			throw new IllegalArgumentException("Parameters may not be null");
		}
		
		int timeout = params.getConnectionTimeout();
		
		if (timeout == 0) {
			return createSocket(host, port, localAddress, localPort);
		}
		return ControllerThreadSocketFactory.createSocket(this, host, port, localAddress, localPort, timeout);

	}
}