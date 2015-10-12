/*
 * Created on 07-Jun-2005
 *
 */
package uk.ac.warwick.sso.client;

import org.apache.commons.configuration.Configuration;
import org.opensaml.SAMLSubject;

import uk.ac.warwick.userlookup.User;

public interface AttributeAuthorityResponseFetcher {
	
	String ALTERNATE_PROTOCOL = "httpssso";

	User getUserFromSubject(SAMLSubject subject) throws SSOException;

	String getProxyTicket(SAMLSubject subject, String resource) throws SSOException;

	void setConfig(SSOConfiguration config);

}
