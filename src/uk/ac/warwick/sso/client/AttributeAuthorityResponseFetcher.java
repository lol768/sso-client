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


public interface AttributeAuthorityResponseFetcher {
	
	SAMLResponse getSAMLResponse(SAMLAuthenticationStatement authStatement) throws MalformedURLException, IOException;
	
	void setConfig(Configuration config);

}
