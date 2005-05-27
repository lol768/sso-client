/*
 * Created on 11-Mar-2005
 *
 */
package uk.ac.warwick.sso.client;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.Date;
import java.util.Iterator;
import java.util.Properties;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.log4j.Logger;
import org.opensaml.SAMLAssertion;
import org.opensaml.SAMLAttribute;
import org.opensaml.SAMLAttributeQuery;
import org.opensaml.SAMLAttributeStatement;
import org.opensaml.SAMLAuthenticationStatement;
import org.opensaml.SAMLException;
import org.opensaml.SAMLPOSTProfile;
import org.opensaml.SAMLRequest;
import org.opensaml.SAMLResponse;
import org.opensaml.SAMLStatement;
import org.opensaml.SAMLSubject;
import org.opensaml.XML;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import uk.ac.warwick.sso.client.ssl.AuthSSLProtocolSocketFactory;
import uk.ac.warwick.sso.client.ssl.KeyStoreHelper;
import uk.ac.warwick.userlookup.User;
import uk.ac.warwick.userlookup.UserCacheItem;
import uk.ac.warwick.userlookup.UserLookup;

/**
 * @author Kieran Shaw
 * 
 */
public class ShireServlet extends HttpServlet {

	private static final Logger LOGGER = Logger.getLogger(ShireServlet.class);

	private Configuration _config;

	public ShireServlet() {
		super();
	}

	protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {

		process(req, res);
	}

	protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {

		process(req, res);

	}

	/**
	 * @param req
	 * @throws IOException
	 * @throws HttpException
	 */
	private void process(HttpServletRequest req, HttpServletResponse res) throws IOException, HttpException {
		SAMLResponse samlResponse = null;
		String saml64 = req.getParameter("SAMLResponse");
		String target = req.getParameter("TARGET");
		LOGGER.debug("TARGET:" + target);
		LOGGER.debug("SAML64:" + saml64);
		if (target == null || saml64 == null) {
			LOGGER.error("Must have a SAMLResponse and a TARGET");
			throw new RuntimeException("Must have a SAMLResponse and a TARGET");
		}
		// check we've got a valid SAML request
		try {
			samlResponse = SAMLPOSTProfile.accept(saml64.getBytes(), _config.getString("shire.providerid"), 5, false);
		} catch (SAMLException e) {
			LOGGER.error("SAMLException accepting SAMLPOSTProfile", e);
			throw new RuntimeException("SAMLException thrown accepting POST profile", e);
		}

		boolean validResponse = verifySAMLResponse(samlResponse);
		if (!validResponse) {
			LOGGER.info("Signed SAMLResponse was not verified against origin certificate, so rejecting!");
			throw new RuntimeException("Signed SAMLResponse was not verified against origin certificate, so rejecting!");
		}

		LOGGER.info("SAML:" + samlResponse.toString());
		SAMLAssertion assertion = (SAMLAssertion) samlResponse.getAssertions().next();
		LOGGER.info("Assertion:" + assertion.toString());
		SAMLStatement statement = (SAMLStatement) assertion.getStatements().next();
		LOGGER.info("Statement:" + statement.toString());
		SAMLAuthenticationStatement authStatement = (SAMLAuthenticationStatement) statement;
		LOGGER.info("Auth Statement:" + authStatement.toString());
		LOGGER.info("Subject name:" + authStatement.getSubject().getName().toString());
		LOGGER.info("Subject name:" + authStatement.getSubject().getName().getName());

		SAMLResponse samlResp = getAttrRespFromAuthStatement(authStatement);

		Properties attributes = getAttributesFromResponse(samlResp);

		User user = createUserFromAttributes(attributes);

		String SSC = attributes.getProperty("urn:websignon:ssc");
		user.setToken(SSC);
		user.setIsLoggedIn(true);

		UserCacheItem item = new UserCacheItem(user, new Date().getTime(), SSC);

		UserLookup.getInstance().getUserCache().put(SSC, item);
		UserLookup.getInstance().getUserByToken(SSC, false);

		// create cookie so that service can retrieve user from cache

		Cookie cookie = new Cookie(_config.getString("shire.sscookie.name"), SSC);
		cookie.setPath(_config.getString("shire.sscookie.path"));
		cookie.setDomain(_config.getString("shire.sscookie.domain"));
		res.addCookie(cookie);
		
		LOGGER.debug("Adding SSC (" + SSC + " ) to response");
		
		res.setHeader("Location", target);
		res.setStatus(302);

	}

	/**
	 * @param samlResponse
	 * @throws IOException
	 * @throws MalformedURLException
	 */
	private boolean verifySAMLResponse(SAMLResponse samlResponse) throws IOException, MalformedURLException {
		try {
			KeyStoreHelper helper = new KeyStoreHelper();
			KeyStore keyStore = helper.createKeyStore(new URL(_config.getString("shire.keystore.location")), _config
					.getString("shire.keystore.password"));
			Certificate originCert = keyStore.getCertificate(_config.getString("shire.keystore.origin-alias"));

			samlResponse.verify(originCert);
			return true;
		} catch (KeyStoreException e) {
			LOGGER.error("Could not create keystore", e);
			throw new RuntimeException("Could not create keystore", e);
		} catch (CertificateException e) {
			LOGGER.error("Could not create keystore", e);
			throw new RuntimeException("Could not create keystore", e);
		} catch (NoSuchAlgorithmException e) {
			LOGGER.error("Could not create keystore", e);
			throw new RuntimeException("Could not create keystore", e);
		} catch (SAMLException e) {
			LOGGER.error("Could not verify SAMLResponse", e);
			return false;
		}

	}

	/**
	 * @param authStatement
	 * @return
	 * @throws MalformedURLException
	 * @throws IOException
	 * @throws HttpException
	 */
	private SAMLResponse getAttrRespFromAuthStatement(SAMLAuthenticationStatement authStatement) throws MalformedURLException,
			IOException, HttpException {
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

	/**
	 * @param samlResp
	 * @return
	 */
	private User createUserFromAttributes(Properties attributes) {
		User user = new User();
		user.setUserId((String) attributes.get("cn"));
		user.setLastName((String) attributes.get("sn"));
		user.setFirstName((String) attributes.get("givenName"));
		user.setWarwickId((String) attributes.get("warwickuniid"));
		user.setDepartmentCode((String) attributes.get("warwickdeptcode"));
		user.setDepartment((String) attributes.get("ou"));
		user.setEmail((String) attributes.get("email"));
		
		user.getExtraProperties().putAll(attributes);
		
		return user;
	}

	/**
	 * @param samlResp
	 * @return
	 */
	private Properties getAttributesFromResponse(SAMLResponse samlResp) {
		Properties attributes = new Properties();
		SAMLAssertion attributeAssertion = (SAMLAssertion) samlResp.getAssertions().next();
		SAMLAttributeStatement attributeStatement = (SAMLAttributeStatement) attributeAssertion.getStatements().next();
		Iterator it = attributeStatement.getAttributes();
		while (it.hasNext()) {
			SAMLAttribute attribute = (SAMLAttribute) it.next();
			String name = attribute.getName();
			String value = (String) attribute.getValues().next();
			LOGGER.info(name + "=" + value);
			attributes.put(name, value);
		}
		return attributes;
	}

	public void init(ServletConfig ctx) throws ServletException {
		super.init(ctx);

		_config = (Configuration) ctx.getServletContext().getAttribute(SSOConfigLoader.SSO_CONFIG_KEY);

	}
}
