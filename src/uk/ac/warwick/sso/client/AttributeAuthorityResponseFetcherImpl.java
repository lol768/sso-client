/*
 * Created on 07-Jun-2005
 *
 */
package uk.ac.warwick.sso.client;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.log4j.Logger;
import org.apache.xml.security.signature.XMLSignature;
import org.opensaml.SAMLAttributeQuery;
import org.opensaml.SAMLAuthenticationStatement;
import org.opensaml.SAMLException;
import org.opensaml.SAMLRequest;
import org.opensaml.SAMLResponse;
import org.opensaml.SAMLSubject;
import org.opensaml.XML;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import uk.ac.warwick.sso.client.ssl.AuthSSLProtocolSocketFactory;
import uk.ac.warwick.sso.client.ssl.KeyStoreHelper;

public class AttributeAuthorityResponseFetcherImpl implements AttributeAuthorityResponseFetcher {

	private static final Logger LOGGER = Logger.getLogger(AttributeAuthorityResponseFetcherImpl.class);

	private Configuration _config;

	public SAMLResponse getSAMLResponse(SAMLAuthenticationStatement authStatement) throws MalformedURLException, IOException{
		String aaLocation = _config.getString("origin.attributeauthority.location");
		LOGGER.info("Shire connecting to AttributeAuthority at " + aaLocation);
		if (aaLocation.startsWith("https")) {
			Protocol authhttps = new Protocol("https", new AuthSSLProtocolSocketFactory(new URL(_config
					.getString("shire.keystore.location")), _config.getString("shire.keystore.password"), new URL(_config
					.getString("shire.keystore.location")), _config.getString("shire.keystore.password")), 443);
			Protocol.registerProtocol("https", authhttps);
		}
		HttpClient client = new HttpClient();
		PostMethod method = new PostMethod(aaLocation);
		method.addRequestHeader("Content-Type", "text/xml");
		SAMLRequest samlRequest = new SAMLRequest();
		SAMLAttributeQuery query = new SAMLAttributeQuery();
		query.setResource(_config.getString("shire.providerid"));
		SAMLSubject subject = authStatement.getSubject();
		try {
			query.setSubject(subject);
			samlRequest.setQuery(query);

			signRequest(samlRequest);

		} catch (SAMLException e) {
			LOGGER.error("SAMLException setting up samlRequest", e);
		}

		String fullRequest = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\""
				+ " xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">"
				+ "<soap:Body>";
		fullRequest += samlRequest.toString();
		fullRequest += "</soap:Body></soap:Envelope>";
		method.setRequestBody(fullRequest);
		LOGGER.info("SAMLRequest:" + fullRequest);
		client.executeMethod(method);
		LOGGER.info("Https response:" + method.getResponseBodyAsString());

		if (method.getResponseBodyAsString().indexOf("<soap:Fault><faultcode>") > -1) {
			throw new RuntimeException("Got bad response from AttributeAuthority:" + method.getResponseBodyAsString());
		}

		// turn https response into a SAML document and get the attributes out
		SAMLResponse samlResp = null;
		try {
			Document document = XML.parserPool.parse(method.getResponseBodyAsStream());
			samlResp = new SAMLResponse((Element) document.getDocumentElement().getFirstChild().getFirstChild());
		} catch (SAMLException e) {
			throw new RuntimeException("Could not create SAMLResponse from stream", e);
		} catch (SAXException e) {
			throw new RuntimeException("Could not create SAMLResponse from stream", e);
		} catch (IOException e) {
			throw new RuntimeException("Could not create SAMLResponse from stream", e);
		}
		return samlResp;
	}

	private void signRequest(SAMLRequest samlRequest) throws IOException, MalformedURLException, SAMLException {
		String alias = _config.getString("shire.keystore.shire-alias");
		List certChain = new ArrayList();
		certChain.add(getCertificate(alias));
		samlRequest.sign(XMLSignature.ALGO_ID_SIGNATURE_RSA_SHA1, getKey(alias), certChain);
	}

	private Key getKey(final String alias) throws IOException, MalformedURLException {

		try {
			KeyStore keyStore = getKeyStore();
			Key key = keyStore.getKey(alias, _config.getString("shire.keystore.password").toCharArray());
			return key;
		} catch (KeyStoreException e) {
			LOGGER.error("Could not create keystore", e);
			throw new RuntimeException("Could not create keystore", e);
		} catch (CertificateException e) {
			LOGGER.error("Could not create keystore", e);
			throw new RuntimeException("Could not create keystore", e);
		} catch (NoSuchAlgorithmException e) {
			LOGGER.error("Could not create keystore", e);
			throw new RuntimeException("Could not create keystore", e);
		} catch (UnrecoverableKeyException e) {
			LOGGER.error("Could not get key from keystore", e);
			throw new RuntimeException("Could not get key from keystore", e);
		}

	}

	/**
	 * @return
	 * @throws KeyStoreException
	 * @throws NoSuchAlgorithmException
	 * @throws CertificateException
	 * @throws IOException
	 * @throws MalformedURLException
	 */
	private Certificate getCertificate(final String alias) throws IOException, MalformedURLException {

		try {
			KeyStore keyStore = getKeyStore();
			Certificate originCert = keyStore.getCertificate(alias);
			return originCert;
		} catch (KeyStoreException e) {
			LOGGER.error("Could not create keystore", e);
			throw new RuntimeException("Could not create keystore", e);
		} catch (CertificateException e) {
			LOGGER.error("Could not create keystore", e);
			throw new RuntimeException("Could not create keystore", e);
		} catch (NoSuchAlgorithmException e) {
			LOGGER.error("Could not create keystore", e);
			throw new RuntimeException("Could not create keystore", e);
		}

	}

	

	/**
	 * @return
	 * @throws KeyStoreException
	 * @throws NoSuchAlgorithmException
	 * @throws CertificateException
	 * @throws IOException
	 * @throws MalformedURLException
	 */
	private KeyStore getKeyStore() throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException,
			MalformedURLException {
		KeyStoreHelper helper = new KeyStoreHelper();
		KeyStore keyStore = helper.createKeyStore(new URL(_config.getString("shire.keystore.location")), _config
				.getString("shire.keystore.password"));
		return keyStore;
	}

	public final Configuration getConfig() {
		return _config;
	}

	public final void setConfig(Configuration config) {
		_config = config;
	}

}
