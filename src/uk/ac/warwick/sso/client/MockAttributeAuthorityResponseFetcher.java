/*
 * Created on 07-Jun-2005
 *
 */
package uk.ac.warwick.sso.client;

import org.apache.commons.configuration.Configuration;
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

		return null;
	}

}
