/*
 * Created on 11-Mar-2005
 *
 */
package uk.ac.warwick.sso.client;

import java.io.IOException;
import java.net.URL;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.httpclient.protocol.SSLProtocolSocketFactory;
import org.apache.log4j.Logger;
import org.opensaml.SAMLAssertion;
import org.opensaml.SAMLAttributeQuery;
import org.opensaml.SAMLAuthenticationStatement;
import org.opensaml.SAMLException;
import org.opensaml.SAMLNameIdentifier;
import org.opensaml.SAMLPOSTProfile;
import org.opensaml.SAMLRequest;
import org.opensaml.SAMLResponse;
import org.opensaml.SAMLStatement;
import org.opensaml.SAMLSubject;

public class ShireServlet extends HttpServlet {

	private static final Logger LOGGER = Logger.getLogger(ShireServlet.class);

	public ShireServlet() {
		super();
		// TODO Auto-generated constructor stub
	}

	protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {

		process(req);
	}

	protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {

		process(req);

	}

	/**
	 * @param req
	 * @throws IOException
	 * @throws HttpException
	 */
	private void process(HttpServletRequest req) throws IOException, HttpException {
		SAMLResponse samlResponse = null;
		String saml64 = req.getParameter("SAMLResponse");
		// String target = req.getParameter("TARGET");
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
				+ "xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">"
				+ "<soap:Body>";

				
		fullRequest += samlRequest.toString();
		fullRequest += "</soap:Body></soap:Envelope>";
		
		method.setRequestBody(fullRequest);
		
		LOGGER.info("SAMLRequest:" + fullRequest);
		
		int respCode = client.executeMethod(method);
		LOGGER.info("Https response:" + method.getResponseBodyAsString());
	}
}
