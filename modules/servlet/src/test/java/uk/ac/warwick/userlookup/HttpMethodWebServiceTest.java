package uk.ac.warwick.userlookup;

import java.net.URI;
import java.net.URL;

import junit.framework.TestCase;
import org.apache.http.client.methods.HttpGet;
import uk.ac.warwick.util.web.Uri;
import uk.ac.warwick.util.web.UriBuilder;

public class HttpMethodWebServiceTest extends TestCase {

	public void testAppendingApiKey() throws Exception {
		HttpMethodWebService service = new HttpMethodWebService(new URL("http://www.example.com/service"),
				null, null, "", "mykey");
		
		HttpGet method = new HttpGet(URI.create("http://warwick.ac.uk"));
		service.addApiKeyToUrl(method);
		assertEquals("wsos_api_key=mykey", new UriBuilder(Uri.fromJavaUri(method.getURI())).getQuery());
		
		method = new HttpGet(URI.create("http://warwick.ac.uk?myparam=somethinggreat&apppass=123"));
		service.addApiKeyToUrl(method);
		assertEquals("myparam=somethinggreat&apppass=123&wsos_api_key=mykey", new UriBuilder(Uri.fromJavaUri(method.getURI())).getQuery());
	}
}
