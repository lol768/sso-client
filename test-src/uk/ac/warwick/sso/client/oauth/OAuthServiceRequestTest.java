package uk.ac.warwick.sso.client.oauth;

import static org.junit.Assert.*;
import static uk.ac.warwick.sso.client.oauth.OAuthServiceRequestSecurityRuleChain.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.List;

import javax.security.auth.x500.X500Principal;
import javax.servlet.http.HttpServletRequest;

import org.apache.xml.security.signature.XMLSignature;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.util.FileCopyUtils;
import org.w3c.dom.Element;

import uk.ac.warwick.sso.client.internal.KeyAndCertUtils;
import uk.ac.warwick.sso.client.oauth.OAuthServiceRequest.GetTokenRequest;


public class OAuthServiceRequestTest {
	
	private static final String RESOURCE_NAME = "https://megawebs.example.com/oauth-sp";

	static {
		org.apache.xml.security.Init.init();
	}
	
	private boolean subjectMismatch;
	
	@Before public void before() {
		subjectMismatch = false;
	}

	/**
	 * We have two security rules here:
	 * - Signature matches embedded certificate.
	 * - Certificate name matches a name we've provided.
	 */
	private OAuthServiceRequestSecurityRuleChain securityRuleChain(
			final X500Principal expectedSubject) {
		OAuthServiceRequestSecurityRuleChain chain = chainOf(new SignatureMatchesRule(), new OAuthServiceRequestSecurityRule() {
			public void run(HttpServletRequest req, List<X509Certificate> cert,
					XMLSignature signature, Element signedContent) {
				String resource = signedContent.getAttribute("Resource");
				assertEquals(RESOURCE_NAME, resource);
				
				// In practice, we'd use the resource name to look up the expected subject in metadata.
				
				if(!expectedSubject.equals(cert.get(0).getSubjectX500Principal())) {
					subjectMismatch = true;
				}
			}
		});
		return chain;
	}
	
	/**
	 * In this test the subject name is what we expect, so no error is logged.
	 */
	@Test public void certificateSubjectVerify() throws Exception {
		// First create a signed request, then save it to String
		X509Certificate cert = readCert();
		MockHttpServletRequest request = createHttpRequest(cert);
		final X500Principal expectedSubject = new X500Principal("CN=oauthclient.warwick.ac.uk, O=The University of Warwick, L=Coventry, C=GB");
		
		OAuthServiceRequestSecurityRuleChain chain = securityRuleChain(expectedSubject);
		
		OAuthServiceRequest serviceRequest = OAuthServiceRequest.fromHttpServletRequest(request, chain);
		assertEquals(GetTokenRequest.VERB, serviceRequest.getVerb());

		assertFalse(subjectMismatch);
	}
	
	/**
	 * Cert is valid but doesn't match the subject we have stored in metadata. What will happen!
	 */
	@Test public void certificateSubjectVerifyMismatch() throws Exception {
		// First create a signed request, then save it to String
		X509Certificate cert = readCert();
		MockHttpServletRequest request = createHttpRequest(cert);
		X500Principal expectedSubject = new X500Principal("CN=oauthclient.warwick.ac.uk, O=Warwick University, L=Coventry, C=GB");
		
		OAuthServiceRequestSecurityRuleChain chain = securityRuleChain(expectedSubject);
		
		OAuthServiceRequest serviceRequest = OAuthServiceRequest.fromHttpServletRequest(request, chain);
		assertEquals(GetTokenRequest.VERB, serviceRequest.getVerb());
		
		assertTrue(subjectMismatch);
	}

	private MockHttpServletRequest createHttpRequest(X509Certificate cert)
			throws UnsupportedEncodingException, IOException,
			InvalidKeySpecException, NoSuchAlgorithmException {
		String signedXML = createSignedRequest(cert);
		
		FileCopyUtils.copy(new StringReader(signedXML), new FileWriter(new File("/tmp/request.xml")));
		
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setContent(signedXML.getBytes("UTF-8"));
		request.setMethod("POST");
		request.setContentType("text/xml");
		return request;
	}

	private String createSignedRequest(X509Certificate cert)
			throws UnsupportedEncodingException, IOException,
			InvalidKeySpecException, NoSuchAlgorithmException {
		PrivateKey privateKey = KeyAndCertUtils.decodeRSAPrivateKey( getClass().getResourceAsStream("/resources/certs/oauthclient.pk8key") );
		OAuthServiceRequest r = new OAuthServiceRequest.GetTokenRequest("abc123", RESOURCE_NAME);
		String signedXML = r.toSignedXML(XMLSignature.ALGO_ID_SIGNATURE_RSA_SHA1, privateKey, Arrays.<Certificate>asList(cert));
		return signedXML;
	}

	private X509Certificate readCert() throws CertificateException {
		X509Certificate cert = (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate( getClass().getResourceAsStream("/resources/certs/oauthclient.crt") );
		return cert;
	}

}
