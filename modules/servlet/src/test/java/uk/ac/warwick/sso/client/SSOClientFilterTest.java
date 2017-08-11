package uk.ac.warwick.sso.client;

import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;
import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import sun.misc.BASE64Encoder;
import uk.ac.warwick.sso.client.cache.UserCache;
import uk.ac.warwick.sso.client.core.OnCampusService;
import uk.ac.warwick.sso.client.core.OnCampusServiceImpl;
import uk.ac.warwick.userlookup.*;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

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
		req.addHeader("Authorization", "Basic " + new BASE64Encoder().encodeBuffer((user + ":" + pass).getBytes("UTF-8")));
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

	@Test
	public void shouldAddSameSiteCookieToTheRightCookie() {
		String originalSetCookieValueInString = "SSC-Cat: 123; Max-Age=1234, Random-key: value; Secure";
		String actual = HandleFilter.getSameSiteStrictCookieForSSC(originalSetCookieValueInString, "SSC-Cat");
		String expected = "SSC-Cat: 123; Max-Age=1234; SameSite=Strict, Random-key: value; Secure";
		assertEquals(expected, actual);
	}
}
