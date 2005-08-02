/*
 * Created on 01-Aug-2005
 *
 */
package uk.ac.warwick.sso.client;

import junit.framework.TestCase;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.methods.GetMethod;


public class BasicAuthTests extends TestCase {
	
	public final void testBasicAuth() throws Exception {
		
		HttpClient client = new HttpClient();
		
		Credentials creds = new UsernamePasswordCredentials("blogtest3","blogtest3");
		
		client.getState().setCredentials(null,"moleman.warwick.ac.uk",creds);
		
		client.getState().setAuthenticationPreemptive(true);		
	
		GetMethod method = new GetMethod("http://moleman.warwick.ac.uk/blogbuilder/kieranshaw/");
		
		int status = client.executeMethod(method);
		
		assertEquals("Should have found good response",200,status);
		
	}

}
