/*
 * Created on 01-Aug-2005
 *
 */
package uk.ac.warwick.sso.client;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import junit.framework.TestCase;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpClientParams;

import sun.misc.BASE64Encoder;

import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;

public class BasicAuthTests extends TestCase {

	public final void testBasicAuth() throws Exception {

		HttpClient client = new HttpClient();

		Credentials creds = new UsernamePasswordCredentials("blogtest2", "blogtest2");

		final int httpsPort = 443;
		AuthScope authScope = new AuthScope("ext-users.warwick.ac.uk", httpsPort);
		client.getState().setCredentials(authScope, creds);

		HttpClientParams params = new HttpClientParams();
		params.setAuthenticationPreemptive(true);

		client.getParams().setAuthenticationPreemptive(true);

		GetMethod method = new GetMethod("https://ext-users.warwick.ac.uk/");

		int status = client.executeMethod(method);

		String responseBody = method.getResponseBodyAsString();

		checkResponse(responseBody, status);

	}

	/**
	 * @param method
	 * @param status
	 * @throws IOException
	 */
	private void checkResponse(final String responseBody, final int status) {
		assertEquals("Should have found good response", HttpServletResponse.SC_OK, status);

		assertTrue("Should have logged in content", responseBody.indexOf("Sign out") > -1);

		assertEquals("Should have logged in content", -1, responseBody.indexOf("Sign in"));
	}

	public final void testBasicAuth2() throws Exception {

		HttpClient client = new HttpClient();

		String username = "blogtest2";
		String password = "blogtest2";

		BASE64Encoder encoder = new BASE64Encoder();

		String userandpass = username + ":" + password;
		String auth = "Basic " + encoder.encode(userandpass.getBytes());

		GetMethod method = new GetMethod("https://ext-users.warwick.ac.uk/");

		method.addRequestHeader("Authorization", auth);

		int status = client.executeMethod(method);

		String responseBody = method.getResponseBodyAsString();

		checkResponse(responseBody, status);

	}

	public final void testBasicAuth3() throws Exception {

		WebConversation wc = new WebConversation();

		String username = "blogtest2";
		String password = "blogtest2";

		BASE64Encoder encoder = new BASE64Encoder();

		String userandpass = username + ":" + password;
		String auth = "Basic " + encoder.encode(userandpass.getBytes());

		wc.setHeaderField("Authorization", auth);

		WebRequest wr = new GetMethodWebRequest("https://ext-users.warwick.ac.uk/");

		WebResponse resp = wc.getResponse(wr);

		int status = resp.getResponseCode();

		assertEquals("Should have found good response", HttpServletResponse.SC_OK, status);

		String responseBody = resp.getText();

		assertTrue("Should have logged in content", responseBody.indexOf("Sign out") > -1);

		assertEquals("Should have logged in content", -1, responseBody.indexOf("Sign in"));

	}
}
