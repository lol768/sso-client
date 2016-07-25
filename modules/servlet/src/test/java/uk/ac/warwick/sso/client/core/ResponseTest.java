package uk.ac.warwick.sso.client.core;

import junit.framework.TestCase;
import org.apache.http.Header;

import java.util.List;

public class ResponseTest extends TestCase {

    public void testReplacingHeaders() throws Exception {
        Response response = new Response();

        response.setHeader("x-powered-by", "Electricity");
        response.setHeader("X-Powered-By", "Rube Goldberg Machine");
        response.setHeader("Something-Else", "deny");

        List<Header> headers = response.getHeaders();
        assertEquals(2, headers.size());

        Header firstHeader = headers.get(0);
        assertEquals("X-Powered-By", firstHeader.getName());
        assertEquals("Rube Goldberg Machine", firstHeader.getValue());
    }

}
