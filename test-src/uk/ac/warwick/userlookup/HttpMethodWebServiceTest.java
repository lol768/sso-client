package uk.ac.warwick.userlookup;

import java.net.URL;

import org.apache.commons.httpclient.methods.GetMethod;

import junit.framework.TestCase;

public class HttpMethodWebServiceTest extends TestCase {

	public void testAppendingApiKey() throws Exception {
		HttpMethodWebService service = new HttpMethodWebService(new URL("http://www.example.com/service"),
				null, null, "", "mykey");
		
		GetMethod method = new GetMethod();		
		service.addApiKeyToUrl(method);
		assertEquals("wsos_api_key=mykey", method.getQueryString());
		
		method = new GetMethod();
		method.setQueryString("myparam=somethinggreat&apppass=123");
		service.addApiKeyToUrl(method);
		assertEquals("myparam=somethinggreat&apppass=123&wsos_api_key=mykey", method.getQueryString());
	}
}
