/*
 * Created on 30-Jun-2005
 *
 */
package uk.ac.warwick.sso.client;

import org.apache.commons.configuration.Configuration;
import org.opensaml.SAMLResponse;
import org.opensaml.SAMLSubject;

public class MockAttributeAuthorityResponseFetcher2 extends AttributeAuthorityResponseFetcherImpl {

	public MockAttributeAuthorityResponseFetcher2(SSOConfiguration config) {
		super(config);
		// TODO Auto-generated constructor stub
	}

	public MockAttributeAuthorityResponseFetcher2() {
		super();
	}

	private SAMLResponse _resp;

	public final SAMLResponse getSAMLResponse(final SAMLSubject subject) throws SSOException {
		return _resp;
	}

	public final void setSAMLResponse(final SAMLResponse resp) {
		_resp = resp;
	}

}
