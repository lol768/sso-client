package uk.ac.warwick.sso.client;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.URL;
import java.security.KeyStore;
import java.security.cert.Certificate;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;
import org.junit.Test;
import org.opensaml.SAMLBinding;
import org.opensaml.SAMLNameIdentifier;
import org.opensaml.SAMLRequest;
import org.opensaml.SAMLSOAPBinding;
import org.opensaml.SAMLSubject;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockHttpServletRequest;

import uk.ac.warwick.sso.client.ssl.KeyStoreHelper;


public class AttributeAuthorityResponseFetcherImplTest {

	@Test
	public void generateValidXmlRequest() throws Exception {
		SAMLSubject subject = generateMockSubject();
		AttributeAuthorityResponseFetcherImpl aaf = new AttributeAuthorityResponseFetcherImpl();
		Configuration config = new BaseConfiguration();
		
		String keystoreUrl = classpathResourceToUrl("myapp.warwick.ac.uk.keystore");
		config.setProperty("shire.keystore.location", keystoreUrl);
		config.setProperty("shire.keystore.shire-alias", "moleman.warwick.ac.uk");
		config.setProperty("shire.keystore.password", "changeit");
		aaf.setConfig(config);
		
		String xml = aaf.generateSAMLRequestXml(subject, "resourcename");
		
		// Wrap the xml in an HTTPRequest so that SAMLSOAPBinding can parse it
		MockHttpServletRequest httpRequest = new MockHttpServletRequest();
		httpRequest.setMethod("POST");
		httpRequest.setContentType("text/xml");
		httpRequest.setContent(xml.getBytes("UTF-8"));
		SAMLBinding binding = new SAMLSOAPBinding();
		
		SAMLRequest request = binding.receive(httpRequest);
		
		// verify against the certificate we used to sign it
		KeyStore keystore = new KeyStoreHelper().createKeyStore(new URL(keystoreUrl), "changeit");
		Certificate cert = keystore.getCertificate("moleman.warwick.ac.uk");
		request.verify(cert);
	}
	
	private SAMLSubject generateMockSubject() throws Exception {
		SAMLSubject subject = new SAMLSubject();
		SAMLNameIdentifier nameId = new SAMLNameIdentifier("cusebr", "origin", "tickettype");
		subject.setName(nameId);
		return subject;
	}
	
	private String classpathResourceToUrl(String classpathResource) throws IOException {
		ClassPathResource r = new ClassPathResource(classpathResource);
		assertTrue(classpathResource + " not found", r.exists());
		return r.getURL().toExternalForm();
	}
}
