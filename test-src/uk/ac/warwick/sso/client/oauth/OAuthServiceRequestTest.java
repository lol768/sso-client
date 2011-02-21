package uk.ac.warwick.sso.client.oauth;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;

import javax.security.auth.x500.X500Principal;

import org.apache.log4j.Logger;
import org.apache.xml.security.signature.XMLSignature;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import uk.ac.warwick.sso.client.TestLogAppender;
import uk.ac.warwick.sso.client.internal.KeyAndCertUtils;
import uk.ac.warwick.sso.client.oauth.OAuthServiceRequest.GetTokenRequest;


public class OAuthServiceRequestTest {
	
	// As we only log subject mismatches at the moment, we'll just check for them. 
	private static final String SUBJECT_MISMATCH_LOG_MESSAGE = "Signature cert doesn't match subject DN";

	static {
		org.apache.xml.security.Init.init();
	}
	
	private TestLogAppender appender = new TestLogAppender();
	private Logger logger = Logger.getLogger(OAuthServiceRequest.class);
	
	@Before public void before() {
		logger.addAppender(appender);
	}
	
	@After public void after() {
		logger.removeAppender(appender);
		appender.clear();
	}
	
	/**
	 * In this test the subject name is what we expect, so no error is logged.
	 */
	@Test public void certificateSubjectVerify() throws Exception {
		// First create a signed request, then save it to String
		X509Certificate cert = readCert();
		MockHttpServletRequest request = createHttpRequest(cert);
		X500Principal expectedSubject = new X500Principal("CN=oauthclient.warwick.ac.uk, O=The University of Warwick, L=Coventry, C=GB");
		
		OAuthServiceRequest serviceRequest = OAuthServiceRequest.fromHttpServletRequest(request, cert, expectedSubject);
		assertEquals(GetTokenRequest.VERB, serviceRequest.getVerb());
		
		assertFalse( appender.contains("A string not in logs")); // sanity check the appender.contains() method
		assertFalse( appender.contains(SUBJECT_MISMATCH_LOG_MESSAGE));
	}
	
	/**
	 * Cert is valid but doesn't match the subject we have stored in metadata. What will happen!
	 * 
	 * It'll log a message, that's what.
	 */
	@Test public void certificateSubjectVerifyMismatch() throws Exception {
		// First create a signed request, then save it to String
		X509Certificate cert = readCert();
		MockHttpServletRequest request = createHttpRequest(cert);
		X500Principal expectedSubject = new X500Principal("CN=oauthclient.warwick.ac.uk, O=Warwick University, L=Coventry, C=GB");
		
		OAuthServiceRequest serviceRequest = OAuthServiceRequest.fromHttpServletRequest(request, cert, expectedSubject);
		assertEquals(GetTokenRequest.VERB, serviceRequest.getVerb());
		
		assertFalse( appender.contains("A string not in logs")); // sanity check the appender.contains() method
		assertTrue( appender.contains(SUBJECT_MISMATCH_LOG_MESSAGE));
	}

	private MockHttpServletRequest createHttpRequest(X509Certificate cert)
			throws UnsupportedEncodingException, IOException,
			InvalidKeySpecException, NoSuchAlgorithmException {
		PrivateKey privateKey = KeyAndCertUtils.decodeRSAPrivateKey( getClass().getResourceAsStream("/resources/certs/oauthclient.pk8key") );
		OAuthServiceRequest r = new OAuthServiceRequest.GetTokenRequest("abc123", "https://megawebs.example.com/oauth-sp");
		String signedXML = r.toSignedXML(XMLSignature.ALGO_ID_SIGNATURE_RSA_SHA1, privateKey, Arrays.<Certificate>asList(cert));
		
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setContent(signedXML.getBytes("UTF-8"));
		request.setMethod("POST");
		request.setContentType("text/xml");
		return request;
	}

	private X509Certificate readCert() throws CertificateException {
		X509Certificate cert = (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate( getClass().getResourceAsStream("/resources/certs/oauthclient.crt") );
		return cert;
	}

}
