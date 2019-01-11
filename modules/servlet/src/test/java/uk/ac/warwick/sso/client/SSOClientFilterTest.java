package uk.ac.warwick.sso.client;

import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.WebAppContext;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import uk.ac.warwick.sso.client.cache.UserCache;
import uk.ac.warwick.sso.client.core.OnCampusService;
import uk.ac.warwick.sso.client.core.OnCampusServiceImpl;
import uk.ac.warwick.userlookup.*;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.Assert.*;

public class SSOClientFilterTest  {
	private JUnit4Mockery m = new JUnit4Mockery();

	private MockHttpServletRequest req;
	private MockHttpServletResponse res;
	private MockFilterChain chain;

	private SSOClientFilter f = new SSOClientFilter();

	private UserLookupInterface userLookup = m.mock(UserLookupInterface.class);
	private OnCampusService onCampusService = new OnCampusServiceImpl();
	private GroupService groupService = m.mock(GroupService.class);

	@Test
	public void testGetUserLookup() {
		UserLookupInterface u = f.getUserLookup();
		assertNotNull(u);
	}
	
	@Before
	public void setUp() throws Exception {
		newRequestGuff();
	}

	private void wireFilter() {
		f.setUserLookup(userLookup);
	}

	private void newRequestGuff() {
		req = new MockHttpServletRequest();
		res = new MockHttpServletResponse();
		chain = new MockFilterChain();
		
		req.setParameter("target", "http://www.example.com/");
		req.setRequestURI("/a");
	}
	
	private void addAuth(String user, String pass) throws Exception {
		req.addHeader("Authorization", String.format("Basic %s", Base64.getEncoder().encodeToString((user + ":" + pass).getBytes(StandardCharsets.UTF_8))));
	}

	@Test
	public void testSetUserLookup() throws Exception {
		UserLookup u = new UserLookup();
		SSOClientFilter f = new SSOClientFilter();
		f.setUserLookup(u);
		// Should be the exact same instance
		assertSame("UserLookup wasn't stored", u, f.getUserLookup());
	}

	private void setConfigAndHandler(Configuration configuration) {
		SSOConfiguration config = new SSOConfiguration(configuration);
		f.setConfig(config);
		f.setHandler(new SSOClientHandlerImpl(config, userLookup, m.mock(UserCache.class), onCampusService));
	}

	@Test
	public void testBasicAuthCache() throws Exception {
		wireFilter();
		
		BaseConfiguration configuration = new BaseConfiguration();
		configuration.addProperty("httpbasic.allow", true);
		configuration.addProperty("mode", "old");
		setConfigAndHandler(configuration);
		
		addAuth("brian","potato");
		
		final User brian = new User("brian");
		brian.setFoundUser(true);
		
		m.checking(new Expectations(){{
			one(userLookup).getUserByIdAndPassNonLoggingIn("brian", "potato");
				will(returnValue(brian));
			one(userLookup).getOnCampusService(); will(returnValue(onCampusService));
		}});
		
		f.doFilter(req, res, chain);
		
		User user = (User) req.getAttribute(SSOClientFilter.USER_KEY);
		assertNotNull(user);
		assertTrue("should be found user", user.isFoundUser());
		assertEquals("brian", ((HttpServletRequest)chain.getRequest()).getRemoteUser());
		
		newRequestGuff();
		addAuth("brian","potato");
		// do the same request again - should use the cache!		
		f.doFilter(req, res, chain);
		user = (User) req.getAttribute(SSOClientFilter.USER_KEY);
		assertNotNull(user);
		assertTrue("should be found user", user.isFoundUser());
		
		m.checking(new Expectations(){{
			one(userLookup).getUserByIdAndPassNonLoggingIn("brian", "grib");
				will(returnValue(new AnonymousUser()));
		}});
		
		newRequestGuff();
		addAuth("brian","grib");
		f.doFilter(req, res, chain);
		user = (User) req.getAttribute(SSOClientFilter.USER_KEY);
		assertNotNull(user);
		assertFalse(user.isFoundUser());
	}

	@Test
	public void testBasicAuthBase64Padding() throws Exception {
		wireFilter();

		BaseConfiguration configuration = new BaseConfiguration();
		configuration.addProperty("httpbasic.allow", true);
		configuration.addProperty("mode", "old");
		setConfigAndHandler(configuration);

		// Users A, B, C have 0, 1 and 2 characters of padding each

		final User alice = new User("alice");
		alice.setFoundUser(true);

		final User bob = new User("bob");
		bob.setFoundUser(true);

		final User cyle = new User("cyle");
		cyle.setFoundUser(true);

		m.checking(new Expectations(){{
			one(userLookup).getUserByIdAndPassNonLoggingIn("alice", "aaa");
				will(returnValue(alice));
			one(userLookup).getUserByIdAndPassNonLoggingIn("bob", "aaaa");
				will(returnValue(bob));
			one(userLookup).getUserByIdAndPassNonLoggingIn("cyle", "aa");
				will(returnValue(cyle));
			one(userLookup).getOnCampusService(); will(returnValue(onCampusService));
		}});

		// header values generated from cURL
		req.addHeader("Authorization", "Basic YWxpY2U6YWFh");
		f.doFilter(req, res, chain);

		User user = (User) req.getAttribute(SSOClientFilter.USER_KEY);
		assertNotNull(user);
		assertTrue("should be found user", user.isFoundUser());
		assertEquals("alice", ((HttpServletRequest)chain.getRequest()).getRemoteUser());

		newRequestGuff();

		req.addHeader("Authorization", "Basic Ym9iOmFhYWE=");
		f.doFilter(req, res, chain);

		user = (User) req.getAttribute(SSOClientFilter.USER_KEY);
		assertNotNull(user);
		assertTrue("should be found user", user.isFoundUser());
		assertEquals("bob", ((HttpServletRequest)chain.getRequest()).getRemoteUser());

		newRequestGuff();

		req.addHeader("Authorization", "Basic Y3lsZTphYQ==");
		f.doFilter(req, res, chain);

		user = (User) req.getAttribute(SSOClientFilter.USER_KEY);
		assertNotNull(user);
		assertTrue("should be found user", user.isFoundUser());
		assertEquals("cyle", ((HttpServletRequest)chain.getRequest()).getRemoteUser());

	}

	@Test
	public void testMasquerade() throws Exception {
		wireFilter();

		newRequestGuff();
		addAuth("andy", "legume");

		final User andy = new User("andy");
		andy.setFoundUser(true);

		final User brian = new User("brian");
		brian.setFoundUser(true);

		m.checking(new Expectations() {{
			one(userLookup).getUserByIdAndPassNonLoggingIn("andy", "legume");
			will(returnValue(andy));

			one(userLookup).getOnCampusService();
			will(returnValue(onCampusService));

			one(userLookup).getGroupService();
			will(returnValue(groupService));

			one(groupService).isUserInGroup("andy", "admin-group");
			will(returnValue(true));

			one(userLookup).getUserByUserId("brian");
			will(returnValue(brian));
		}});

		req.setCookies(new Cookie("masqueradeAs", "brian"));

		BaseConfiguration configuration = new BaseConfiguration();
		configuration.addProperty("httpbasic.allow", true);
		configuration.addProperty("mode", "old");
		configuration.addProperty("masquerade.group", "admin-group");
		setConfigAndHandler(configuration);
		f.doFilter(req, res, chain);

		User user = (User) req.getAttribute(SSOClientFilter.USER_KEY);
		assertNotNull(user);
		assertEquals("brian", user.getUserId());

		User actualUser = (User) req.getAttribute(SSOClientFilter.ACTUAL_USER_KEY);
		assertNotNull(actualUser);
		assertEquals("andy", actualUser.getUserId());
	}

	/**
	 * When an application form POSTs an urlencoded form, it should be able to read that
	 * InputStream itself. If we call getParameter or anything else that consumes the request body,
	 * applications end up with a consumed InputStream with no data.
	 *
	 * We need to fire up Jetty to verify the behaviour of a real life request/response;
	 * MockHttpServletRequest will let you re-read its input stream many times.
	 */
	@Test
	public void inputStreamNotConsumed() throws Exception {
		int port = 20000 + new Random().nextInt(10000);
		Server server = new Server(port);
		try {
			WebAppContext webapp = new WebAppContext();
			webapp.setContextPath("/");
			webapp.setWar(getClass().getResource("/test-webapp").getFile());

			server.setHandler(webapp);
			server.start();

			try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
				HttpPost request = new HttpPost(String.format("http://localhost:%s/diagnostic", port));
				request.setHeader("Content-Type","application/x-www-form-urlencoded");
				request.setEntity(new UrlEncodedFormEntity(Arrays.asList(
					new BasicNameValuePair("field1","value1"),
					new BasicNameValuePair("field2","value2")
				)));

				try (CloseableHttpResponse response = client.execute(request)) {
					// Our DiagnosticServlet reports back as response headers
					assertEquals(response.getStatusLine().getReasonPhrase(), 200, response.getStatusLine().getStatusCode());
					assertEquals("27", response.getFirstHeader("Request-Body-Length").getValue());
					// Check that SSOClientFilter did actually run
					assertEquals("false", response.getFirstHeader("User-Found").getValue());
				}
			}
		} finally {
			server.stop();
		}

	}
}
