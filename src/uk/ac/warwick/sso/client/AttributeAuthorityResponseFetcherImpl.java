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
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.log4j.Logger;
import org.apache.xml.security.signature.XMLSignature;
import org.opensaml.SAMLAssertion;
import org.opensaml.SAMLAttribute;
import org.opensaml.SAMLAttributeQuery;
import org.opensaml.SAMLAttributeStatement;
import org.opensaml.SAMLException;
import org.opensaml.SAMLRequest;
import org.opensaml.SAMLResponse;
import org.opensaml.SAMLSubject;
import org.opensaml.XML;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import uk.ac.warwick.sso.client.ssl.KeyStoreHelper;
import uk.ac.warwick.userlookup.User;

public class AttributeAuthorityResponseFetcherImpl implements AttributeAuthorityResponseFetcher {

	private static final Logger LOGGER = Logger.getLogger(AttributeAuthorityResponseFetcherImpl.class);

	private Configuration _config;

	private String _version;

	public AttributeAuthorityResponseFetcherImpl() {
		// default empty constructor
	}

	public AttributeAuthorityResponseFetcherImpl(final Configuration config) {
		_config = config;

		_version = SSOClientVersionLoader.getVersion();

	}

	public SAMLResponse getSAMLResponse(final SAMLSubject subject) throws SSOException {
		return getSAMLResponse(subject, _config.getString("shire.providerid"));
	}

	private SAMLResponse getSAMLResponse(final SAMLSubject subject, final String resource) throws SSOException {
		String aaLocation = _config.getString("origin.attributeauthority.location");
		LOGGER.info("Shire connecting to AttributeAuthority at " + aaLocation);
		HttpClient client = new HttpClient();
		PostMethod method = new PostMethod(aaLocation);

		if (_version == null) {
			method.addRequestHeader("User-Agent", "SSOClient");
		} else {
			method.addRequestHeader("User-Agent", "SSOClient " + _version);
		}

		method.addRequestHeader("Content-Type", "text/xml");
		SAMLRequest samlRequest = new SAMLRequest();
		SAMLAttributeQuery query = new SAMLAttributeQuery();
		query.setResource(resource);
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
		LOGGER.debug("SAMLRequest:" + fullRequest);
		String body;
		try {
			client.executeMethod(method);
			body = method.getResponseBodyAsString();
		} catch (IOException e) {
			LOGGER.error("Attribute request failed at client.executeMethod", e);
			throw new SSOException("Attribute request failed at client.executeMethod", e);
		}

		LOGGER.debug("Https response:" + body);

		if (body.indexOf("<soap:Fault><faultcode>") > -1) {
			throw new RuntimeException("Got bad response from AttributeAuthority:" + body);
		}

		// turn https response into a SAML document and get the attributes out
		SAMLResponse samlResp = null;
		try {
			Document document = XML.parserPool.parse(method.getResponseBodyAsStream());
			samlResp = new SAMLResponse((Element) document.getDocumentElement().getFirstChild().getFirstChild());
		} catch (SAMLException e) {
			throw new SSOException("Could not create SAMLResponse from stream", e);
			// throw new RuntimeException("Could not create SAMLResponse from stream", e);
		} catch (SAXException e) {
			throw new RuntimeException("Could not create SAMLResponse from stream", e);
		} catch (IOException e) {
			throw new RuntimeException("Could not create SAMLResponse from stream", e);
		}
		return samlResp;
	}

	public final String getProxyTicket(final SAMLSubject subject, final String resource) throws SSOException {
		SAMLResponse response = getSAMLResponse(subject, resource);
		Properties attributes = getAttributesFromResponse(response);
		return getValueFromAttribute(SSOToken.PROXY_TICKET_TYPE, attributes);
	}

	public final User getUserFromSubject(final SAMLSubject subject) throws SSOException {

		SAMLResponse response = getSAMLResponse(subject);

		Properties attributes = getAttributesFromResponse(response);

		User user = createUserFromAttributes(attributes);

		LOGGER.info("Returning user " + user.getFullName() + "(" + user.getUserId() + ") from SAMLSubject");

		return user;

	}

	private void signRequest(final SAMLRequest samlRequest) {
		String alias = _config.getString("shire.keystore.shire-alias");
		List certChain = new ArrayList();
		certChain.add(getCertificate(alias));
		try {
			samlRequest.sign(XMLSignature.ALGO_ID_SIGNATURE_RSA_SHA1, getKey(alias), certChain);
		} catch (SAMLException e) {
			LOGGER.error("Could not sign SAML request", e);
			throw new RuntimeException("Could not sign SAML request", e);
		}
	}

	private Key getKey(final String alias) {

		try {
			KeyStore keyStore = getKeyStore();
			Key key = keyStore.getKey(alias, _config.getString("shire.keystore.password").toCharArray());
			return key;
		} catch (Exception e) {
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
	private Certificate getCertificate(final String alias) {

		try {
			KeyStore keyStore = getKeyStore();
			Certificate originCert = keyStore.getCertificate(alias);
			return originCert;
		} catch (Exception e) {
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
	private KeyStore getKeyStore() throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
		KeyStoreHelper helper = new KeyStoreHelper();
		KeyStore keyStore = helper.createKeyStore(new URL(_config.getString("shire.keystore.location")), _config
				.getString("shire.keystore.password"));
		return keyStore;
	}

	private String getValueFromAttribute(final String key, final Properties attributes) {

		if (attributes == null || attributes.get(key) == null) {
			return null;
		}

		return (String) ((SAMLAttribute) attributes.get(key)).getValues().next();
	}

	/**
	 * @param samlResp
	 * @return
	 */
	private User createUserFromAttributes(final Properties attributes) {
		User user = new User();
		user.setUserId(getValueFromAttribute("cn", attributes));
		user.setLastName(getValueFromAttribute("sn", attributes));
		user.setFirstName(getValueFromAttribute("givenName", attributes));
		user.setWarwickId(getValueFromAttribute("warwickuniid", attributes));
		user.setDepartmentCode(getValueFromAttribute("warwickdeptcode", attributes));
		user.setDepartment(getValueFromAttribute("ou", attributes));
		user.setEmail(getValueFromAttribute("mail", attributes));

		user.setUserType(getValueFromAttribute("urn:websignon:usertype", attributes));

		if ("true".equals(getValueFromAttribute("staff", attributes))) {
			user.setStaff(true);
		}
		if ("true".equals(getValueFromAttribute("student", attributes))) {
			user.setStudent(true);
		}

		if (getValueFromAttribute("urn:websignon:loggedin", attributes) != null
				&& getValueFromAttribute("urn:websignon:loggedin", attributes).equals("true")) {
			user.setIsLoggedIn(true);
		}

		if (getValueFromAttribute("logindisabled", attributes) != null
				&& Boolean.valueOf(getValueFromAttribute("logindisabled", attributes)).booleanValue()) {
			user.setLoginDisabled(true);
		}

		user.setFoundUser(true);

		// dump all attributes as keys and strings into extraproperties map on the user
		Iterator it = attributes.keySet().iterator();
		while (it.hasNext()) {
			String attrName = (String) it.next();
			String value = getValueFromAttribute(attrName, attributes);
			LOGGER.debug("Adding " + attrName + "=" + value + " to users extra properties");
			user.getExtraProperties().put(attrName, value);
		}

		return user;
	}

	/**
	 * @param samlResp
	 * @return
	 */
	private Properties getAttributesFromResponse(final SAMLResponse samlResp) {
		Properties attributes = new Properties();

		if (samlResp.getAssertions() == null || !samlResp.getAssertions().hasNext()) {
			return attributes;
		}

		SAMLAssertion attributeAssertion = (SAMLAssertion) samlResp.getAssertions().next();
		SAMLAttributeStatement attributeStatement = (SAMLAttributeStatement) attributeAssertion.getStatements().next();
		Iterator it = attributeStatement.getAttributes();
		while (it.hasNext()) {
			SAMLAttribute attribute = (SAMLAttribute) it.next();
			String name = attribute.getName();
			// String value = (String) attribute.getValues().next();
			LOGGER.debug(name + "=" + attribute);
			attributes.put(name, attribute);
		}
		return attributes;
	}

	public final Configuration getConfig() {
		return _config;
	}

	public final void setConfig(final Configuration config) {
		_config = config;
	}

}
