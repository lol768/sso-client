/*
 * Created on 11-Mar-2005
 *
 */
package uk.ac.warwick.sso.client;

import java.io.IOException;
import java.net.URL;
import java.util.Iterator;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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

public class ShireServlet extends HttpServlet {

	private static final Logger LOGGER = Logger.getLogger(ShireServlet.class);

	public ShireServlet() {
		super();
		// TODO Auto-generated constructor stub
	}

	protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {

		process(req,res);
	}

	protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {

		process(req,res);

	}

	/**
	 * @param req
	 * @throws IOException
	 * @throws HttpException
	 */
	private void process(HttpServletRequest req,HttpServletResponse res) throws IOException, HttpException {
		SAMLResponse samlResponse = null;
		String saml64 = req.getParameter("SAMLResponse");
		String target = req.getParameter("TARGET");
		// check we've got a valid SAML request
		try {
			samlResponse = SAMLPOSTProfile.accept(saml64.getBytes(), "urn:moleman.warwick.ac.uk:blogbuilder:service", 5, false);
		} catch (SAMLException e1) {
			LOGGER.error("SAMLException accepting SAMLPOSTProfile", e1);
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

		Protocol authhttps = new Protocol("https", new AuthSSLProtocolSocketFactory(new URL(
				"file:/j2sdk1.4.2_02/jre/lib/security/moleman.warwick.ac.uk.keystore"), "changeit", new URL(
				"file:/j2sdk1.4.2_02/jre/lib/security/moleman.warwick.ac.uk.keystore"), "changeit"), 443);

		Protocol.registerProtocol("https", authhttps);
		HttpClient client = new HttpClient();
		PostMethod method = new PostMethod("https://moleman.warwick.ac.uk/origin/aa");
		method.addRequestHeader("Content-Type", "text/xml");

		SAMLRequest samlRequest = new SAMLRequest();
		SAMLAttributeQuery query = new SAMLAttributeQuery();
		query.setResource("urn:moleman.warwick.ac.uk:blogbuilder:service");

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
		
		try {
			Document document = XML.parserPool.parse(method.getResponseBodyAsStream());
			samlResponse = new SAMLResponse((Element) document.getDocumentElement().getFirstChild().getFirstChild());
		} catch (SAMLException e) {
			throw new RuntimeException("Could not create SAMLResponse from stream",e);
		} catch (SAXException e) {
			throw new RuntimeException("Could not create SAMLResponse from stream",e);
		} catch (IOException e) {
			throw new RuntimeException("Could not create SAMLResponse from stream",e);
		}
				
		SAMLAssertion attributeAssertion = (SAMLAssertion) samlResponse.getAssertions().next();
		
		SAMLAttributeStatement attributeStatement = (SAMLAttributeStatement) attributeAssertion.getStatements().next();
		Iterator it = attributeStatement.getAttributes();
		while (it.hasNext()) {
			SAMLAttribute attribute = (SAMLAttribute) it.next();
			LOGGER.info(attribute.getName() + "=" + attribute.getValues().next());			
		}
		
		Cookie cookie = new Cookie("SSO-BlogBuilder","1234567890");
		cookie.setPath("/sso-client");
		res.addCookie(cookie);
		res.setHeader("Location",target);
		res.setStatus(302);
		
		
		
	}
}
