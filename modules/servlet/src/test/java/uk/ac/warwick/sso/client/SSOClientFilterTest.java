package uk.ac.warwick.sso.client;

import junit.framework.TestCase;

import org.apache.commons.configuration.BaseConfiguration;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import sun.misc.BASE64Encoder;
import uk.ac.warwick.sso.client.core.OnCampusServiceImpl;
import uk.ac.warwick.userlookup.*;

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
		
		SSOClientFilter f = new SSOClientFilter();
		f.setUserLookup(userLookup);
		
		BaseConfiguration configuration = new BaseConfiguration();
		configuration.addProperty("httpbasic.allow", true);
		configuration.addProperty("mode", "old");
		SSOConfiguration config = new SSOConfiguration(configuration);
		f.setConfig(config);
		
		addAuth("brian","potato");
		
		final User brian = new User("brian");
		brian.setFoundUser(true);
		
		m.checking(new Expectations(){{
			one(userLookup).getUserByIdAndPassNonLoggingIn("brian", "potato");
				will(returnValue(brian));
			one(userLookup).getOnCampusService(); will(returnValue(new OnCampusServiceImpl()));
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
}