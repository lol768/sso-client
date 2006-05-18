/*
 * Created on 01-Aug-2005
 *
 */
package uk.ac.warwick.sso.client;

import javax.servlet.http.HttpServletResponse;

import junit.framework.TestCase;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpClientParams;


public class BasicAuthTests extends TestCase {
	
	public final void testBasicAuth() throws Exception {
		
		HttpClient client = new HttpClient();
		
		Credentials creds = new UsernamePasswordCredentials("blogtest2","blogtest2");
		
		AuthScope authScope = new AuthScope("ext-users.warwick.ac.uk",443);
		client.getState().setCredentials(authScope,creds);
		
		HttpClientParams params = new HttpClientParams();
		params.setAuthenticationPreemptive(true);
		
		client.getParams().setAuthenticationPreemptive(true);
		
		GetMethod method = new GetMethod("https://ext-users.warwick.ac.uk/");
		
		int status = client.executeMethod(method);
		
		assertEquals("Should have found good response",HttpServletResponse.SC_OK,status);
		
		String responseBody = method.getResponseBodyAsString();
		
		assertTrue("Should have logged in content",responseBody.indexOf("Sign out") > -1);
		
		assertEquals("Should have logged in content",-1,responseBody.indexOf("Sign in"));
		
	
	}
	
	
}
