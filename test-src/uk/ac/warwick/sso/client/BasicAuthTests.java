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
import org.apache.commons.httpclient.methods.GetMethod;

import sun.misc.BASE64Encoder;


public class BasicAuthTests extends TestCase {
	
	public final void testBasicAuth() throws Exception {
		
		HttpClient client = new HttpClient();
		
		Credentials creds = new UsernamePasswordCredentials("blogtest3","blogtest3");
		
		client.getState().setCredentials(null,"moleman.warwick.ac.uk",creds);
		
		client.getState().setAuthenticationPreemptive(true);		
	
		GetMethod method = new GetMethod("http://moleman.warwick.ac.uk/blogbuilder/kieranshaw/");
		
		int status = client.executeMethod(method);
		
		assertEquals("Should have found good response",HttpServletResponse.SC_OK,status);
		
		String responseBody = method.getResponseBodyAsString();
		
		assertTrue("Should have logged in content",responseBody.indexOf("Sign out") > -1);
		
		assertEquals("Should have logged in content",-1,responseBody.indexOf("Sign in"));
		
	
	}
	
	
}
