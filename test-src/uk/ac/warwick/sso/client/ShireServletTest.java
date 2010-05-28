package uk.ac.warwick.sso.client;

import javax.servlet.http.HttpServletResponse;

import junit.framework.TestCase;

import org.apache.commons.configuration.Configuration;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;

public class ShireServletTest extends TestCase {

	private ShireServlet servlet;
	private MockHttpServletRequest request;
	private MockHttpServletResponse response;

	public void testGet() throws Exception {
		servlet.doGet(request,response);
		assertEquals(HttpServletResponse.SC_METHOD_NOT_ALLOWED, response.getStatus());
		assertTrue(response.getContentAsString().contains("mailto:webteam@warwick.ac.uk"));
	}
	
	/**
	 * Not easy to test a POST as it will create a real AA fetcher that tries
	 * to grab attributes. We test the vast majority of the functionality in
	 * ShireCommandTests already so it doesn't matter - the main thing is to
	 * doublecheck that it has different behaviout to doGet.
	 */
	public void testPost() throws Exception {
		try {
			servlet.doPost(request,response);
			fail("Expected IllegalArgumentException");
		} catch (IllegalArgumentException e) {
			return;
		}
	}
	
	protected void setUp() throws Exception {
		servlet = new ShireServlet();
		
		MockServletContext context = new MockServletContext();
		context.addInitParameter("ssoclient.config","sso-config.xml");

		new SSOConfigLoader().loadSSOConfig(context);
		Object config = context.getAttribute(SSOConfigLoader.SSO_CONFIG_KEY);
		
		servlet.setConfig((Configuration) config);

		request = new MockHttpServletRequest();
		response = new MockHttpServletResponse();
	}

	protected void tearDown() throws Exception {
		
	}

}
