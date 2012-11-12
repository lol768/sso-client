package uk.ac.warwick.sso.client.ssl;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.junit.Test;

import uk.ac.warwick.sso.client.KeyAuthentication;
import uk.ac.warwick.sso.client.SSOConfiguration;


public class AuthSSLProtocolSocketFactoryTest {
	/*
	 * Connects to a server socket, which will get as far as setting up the keymanager
	 * though I don't think it will have got as far as doing an SSL handshake.
	 */
	@Test public void useAuth() throws Exception {
		SSOConfiguration config = new SSOConfiguration(new PropertiesConfiguration(getClass().getResource("/resources/certs/selfsign-pkcs1.properties")));
		KeyAuthentication authenticationDetails = config.getAuthenticationDetails();
		
		SSLServerSocket ss = (SSLServerSocket) SSLServerSocketFactory.getDefault().createServerSocket(0);
		try {
			AuthSSLProtocolSocketFactory factory = new AuthSSLProtocolSocketFactory(authenticationDetails);
			SSLSocket socket = (SSLSocket) factory.createSocket("127.0.0.1", ss.getLocalPort());
			socket.close();
		} finally {
			ss.close();
		}
	}
}
