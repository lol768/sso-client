/*
 * Created on 07-Jun-2005
 *
 */
package uk.ac.warwick.sso.client;

import java.io.IOException;
import java.net.MalformedURLException;

import org.apache.commons.configuration.Configuration;
import org.opensaml.SAMLAuthenticationStatement;
import org.opensaml.SAMLResponse;

public class MockAttributeAuthorityResponseFetcher implements AttributeAuthorityResponseFetcher {

	private SAMLResponse _response;

	public final SAMLResponse getSAMLResponse(SAMLAuthenticationStatement authStatement) throws MalformedURLException,
			IOException {

		return _response;
	}

	public final void setConfig(Configuration config) {
		// don't need a config;
	}

	public final SAMLResponse getResponse() {
		return _response;
	}

	public final void setResponse(final SAMLResponse response) {
		_response = response;
	}

}
