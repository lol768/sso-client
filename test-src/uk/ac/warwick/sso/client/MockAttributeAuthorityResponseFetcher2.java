/*
 * Created on 30-Jun-2005
 *
 */
package uk.ac.warwick.sso.client;

import org.opensaml.SAMLResponse;
import org.opensaml.SAMLSubject;

public class MockAttributeAuthorityResponseFetcher2 extends AttributeAuthorityResponseFetcherImpl {

	private SAMLResponse _resp;

	public final SAMLResponse getSAMLResponse(final SAMLSubject subject) throws SSOException {
		return _resp;
	}

	public final void setSAMLResponse(final SAMLResponse resp) {
		_resp = resp;
	}

}
