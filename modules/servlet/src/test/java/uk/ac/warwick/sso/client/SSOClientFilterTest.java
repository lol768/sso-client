package uk.ac.warwick.sso.client;

import junit.framework.TestCase;
import org.apache.commons.configuration.BaseConfiguration;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import sun.misc.BASE64Encoder;
import uk.ac.warwick.sso.client.cache.UserCache;
import uk.ac.warwick.sso.client.core.OnCampusService;
import uk.ac.warwick.sso.client.core.OnCampusServiceImpl;
import uk.ac.warwick.userlookup.*;

import javax.servlet.http.Cookie;

public class SSOClientFilterTest extends TestCase {
	private MockHttpServletRequest req;
	private MockHttpServletResponse res;
	private MockFilterChain chain;
	
	public void testGetUserLookup() {
		SSOClientFilter f = new SSOClientFilter();
		UserLookupInterface u = f.getUserLookup();
		assertNotNull(u);
	}
	
	@Override
	protected void setUp() throws Exception {
		newRequestGuff();
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

	public void testSetUserLookup() throws Exception {
		UserLookup u = new UserLookup();
		SSOClientFilter f = new SSOClientFilter();
		f.setUserLookup(u);
		// Should be the exact same instance
		assertSame("UserLookup wasn't stored", u, f.getUserLookup());
	}
	
	public void testBasicAuthCache() throws Exception {
		Mockery m = new Mockery();

		final UserLookupInterface userLookup = m.mock(UserLookupInterface.class);
		final OnCampusService onCampusService = new OnCampusServiceImpl();
		
		SSOClientFilter f = new SSOClientFilter();
		f.setUserLookup(userLookup);
		
		BaseConfiguration configuration = new BaseConfiguration();
		configuration.addProperty("httpbasic.allow", true);
		configuration.addProperty("mode", "old");
		SSOConfiguration config = new SSOConfiguration(configuration);
		f.setConfig(config);
		f.setHandler(new SSOClientHandlerImpl(config, userLookup, m.mock(UserCache.class), onCampusService));
		
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

	public void testMasquerade() throws Exception {
		Mockery m = new Mockery();

		final UserLookupInterface userLookup = m.mock(UserLookupInterface.class);
		final GroupService groupService = m.mock(GroupService.class);
		final OnCampusService onCampusService = new OnCampusServiceImpl();

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

		SSOClientFilter f = new SSOClientFilter();
		f.setUserLookup(userLookup);

		req.setCookies(new Cookie("masqueradeAs", "brian"));

		BaseConfiguration configuration = new BaseConfiguration();
		configuration.addProperty("httpbasic.allow", true);
		configuration.addProperty("mode", "old");
		configuration.addProperty("masquerade.group", "admin-group");

		SSOConfiguration config = new SSOConfiguration(configuration);
		f.setConfig(config);
		f.setHandler(new SSOClientHandlerImpl(config, userLookup, m.mock(UserCache.class), onCampusService));
		f.doFilter(req, res, chain);

		User user = (User) req.getAttribute(SSOClientFilter.USER_KEY);
		assertNotNull(user);
		assertEquals("brian", user.getUserId());

		User actualUser = (User) req.getAttribute(SSOClientFilter.ACTUAL_USER_KEY);
		assertNotNull(actualUser);
		assertEquals("andy", actualUser.getUserId());
	}
}
