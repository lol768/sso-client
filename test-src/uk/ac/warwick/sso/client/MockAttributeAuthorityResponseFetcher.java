/*
 * Created on 07-Jun-2005
 *
 */
package uk.ac.warwick.sso.client;

import java.util.Iterator;
import java.util.Properties;

import org.apache.commons.configuration.Configuration;
import org.opensaml.SAMLAssertion;
import org.opensaml.SAMLAttribute;
import org.opensaml.SAMLAttributeStatement;
import org.opensaml.SAMLResponse;
import org.opensaml.SAMLSubject;

import uk.ac.warwick.userlookup.User;

public class MockAttributeAuthorityResponseFetcher implements AttributeAuthorityResponseFetcher {

	private SAMLResponse _response;

	public final SAMLResponse getSAMLResponse(final SAMLSubject subject) throws SSOException {

		return _response;
	}

	public final void setConfig(final Configuration config) {
		// don't need a config;
	}

	public final SAMLResponse getResponse() {
		return _response;
	}

	public final void setResponse(final SAMLResponse response) {
		_response = response;
	}

	public final User getUserFromSubject(final SAMLSubject subject) throws SSOException {

		return null;
	}

	public final String getProxyTicket(final SAMLSubject subject, final String resource) throws SSOException {
		SAMLResponse response = getSAMLResponse(subject);
		Properties attributes = getAttributesFromResponse(response);
		return getValueFromAttribute(SSOToken.PROXY_TICKET_TYPE, attributes);
	}

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
			attributes.put(name, attribute);
		}
		return attributes;
	}

	private String getValueFromAttribute(final String key, final Properties attributes) {

		if (attributes == null || attributes.get(key) == null) {
			return null;
		}

		return (String) ((SAMLAttribute) attributes.get(key)).getValues().next();
	}

}
