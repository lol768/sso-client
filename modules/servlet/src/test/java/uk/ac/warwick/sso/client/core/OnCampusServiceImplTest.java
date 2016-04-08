package uk.ac.warwick.sso.client.core;

import java.util.Arrays;

import junit.framework.TestCase;

import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.springframework.util.StringUtils;

public class OnCampusServiceImplTest extends TestCase {

	private static final String SITEBUILDER = "137.205.195.118"; //dionysus-sbr
	private static final String SEARCH = "137.205.243.19"; //search
	private static final String WEBSIGNON = "137.205.247.95"; //op-wsos-ap
	private static final String WWWCACHE = "137.205.192.27"; //wwwcache
	private static final String LOCALHOST = "127.0.0.1";
	private static final String OFFCAMPUS_USER = "212.250.162.12";  //ntlworld.com
	private static final String OFFCAMPUS_USER2 = "62.253.165.47";  //ntl.com
	private static final String ONCAMPUS_USER = "137.205.194.140"; //java-monkey
	
	private static final String X_FORWARDED_FOR = "X-FORWARDED-FOR";
	
	private OnCampusService service;
	private HttpRequest request;

	private JUnit4Mockery m = new JUnit4Mockery();
	
	protected void setUp() throws Exception {
		service = new OnCampusServiceImpl();
		resetRequest();
	}

	@Override
	public void tearDown() {
		m.assertIsSatisfied();
	}
	
	private void resetRequest() {
		request = m.mock(HttpRequest.class);
	}

	/**
	 * The nearest IP is on campus and there's no forwarded header, so	
	 * this is a direct request from an oncampus machine to an application
	 * without any Apache rewrite.
	 */
	public void testDirectOnCampus() throws Exception {
		m.checking(new Expectations() {{
			allowing(request).getRemoteAddr(); will(returnValue(ONCAMPUS_USER));
			allowing(request).getHeader(X_FORWARDED_FOR); will(returnValue(null));
		}});
		assertTrue("Should be on campus", service.isOnCampus(request));
	}
	
	/**
	 * If remoteAddr is offcampus, then it doesn't matter what the forwarding
	 * headers are.. the nearest host is offcampus so anything further away
	 * is irrelevant.
	 * 
	 * The test assert that it doesn't even try to check the headers.
	 */
	public void testDirectOffCampus() throws Exception {
		m.checking(new Expectations() {{
			allowing(request).getRemoteAddr(); will(returnValue(OFFCAMPUS_USER));
			never(request).getHeader(X_FORWARDED_FOR);
		}});

		assertFalse(service.isOnCampus(request));
	}
	
	// typical request through Apache rewrite. only one ip entry so it
	// can be absolutely trusted since our Apache wrote it.
	public void testOnCampusThroughSingleProxy() throws Exception {
		m.checking(new Expectations() {{
			allowing(request).getRemoteAddr(); will(returnValue(LOCALHOST));
			one(request).getHeader(X_FORWARDED_FOR); will(returnValue(ONCAMPUS_USER));
		}});

		assertTrue(service.isOnCampus(request));
	}
	
	/**
	 * Where an app like Sitebuilder proxies to Search's apache, which has its
	 * own proxy.
	 */
	public void testThroughTwoApachesOnCampus() throws Exception {
		m.checking(new Expectations() {{
			allowing(request).getRemoteAddr(); will(returnValue(LOCALHOST));
			one(request).getHeader(X_FORWARDED_FOR); will(returnValue(xff(new String[] {ONCAMPUS_USER, SITEBUILDER})));
		}});

		assertTrue(service.isOnCampus(request));
	}
	
	public void testThroughTwoApachesOffCampus() throws Exception {
		m.checking(new Expectations() {{
			allowing(request).getRemoteAddr(); will(returnValue(LOCALHOST));
			one(request).getHeader(X_FORWARDED_FOR); will(returnValue(xff(new String[] {OFFCAMPUS_USER, SITEBUILDER})));
		}});

		assertFalse(service.isOnCampus(request));
	}
	
	/**
	 * Silly example but possible, and it should handle the case. A request gets rewritten through
	 * many servers (all our own) before finally landing in a local apache.
	 */
	public void testManyApachesOnCampus() throws Exception {
		m.checking(new Expectations() {{
			allowing(request).getRemoteAddr(); will(returnValue(LOCALHOST));
			one(request).getHeader(X_FORWARDED_FOR); will(returnValue(xff(new String[] {ONCAMPUS_USER, WEBSIGNON, SITEBUILDER, SEARCH})));
		}});
		assertTrue(service.isOnCampus(request));
	}

	public void testManyApachesOffCampus() throws Exception {
		m.checking(new Expectations() {{
			allowing(request).getRemoteAddr(); will(returnValue(LOCALHOST));
			one(request).getHeader(X_FORWARDED_FOR); will(returnValue(xff(new String[] {OFFCAMPUS_USER, WEBSIGNON, SITEBUILDER, SEARCH})));
		}});
		assertFalse(service.isOnCampus(request));
	}
	
	/**
	 * Case where x-forwarded-for contains an oncampus user and one of our servers, BUT inbetween them
	 * is an offcampus address. This doesn't make sense, and there's no reason an oncampus user would need
	 * to proxy through somewhere offcampus.
	 * 
	 * In this case, it shouldn't really matter if the leftmost IP is oncampus or not; if we find an offcampus IP at all
	 * then we're not oncampus (except when wwwcache is involved, which we're testing separately).
	 */
	public void testUnknownInChain() throws Exception {
		m.checking(new Expectations() {{
			allowing(request).getRemoteAddr(); will(returnValue(LOCALHOST));
			one(request).getHeader(X_FORWARDED_FOR); will(returnValue(xff(new String[] {ONCAMPUS_USER, OFFCAMPUS_USER, SITEBUILDER})));
		}});

		assertFalse(service.isOnCampus(request));
	}
	
	/**
	 * We trust wwwcache. Going right to left in xforwarded for, if there are only
	 * app servers before finding the wwwcache IP, then we can trust that the request
	 * is coming through wwwcache and we can let it in, even if the IPs further left
	 * are offcampus (one of the aims of wwwcache being that you should appear oncampus)
	 */
	public void testWwwcache() throws Exception {
		//assume the nearest request is a proxying Apache
		m.checking(new Expectations() {{
			allowing(request).getRemoteAddr(); will(returnValue(LOCALHOST));
			one(request).getHeader(X_FORWARDED_FOR); will(returnValue(xff(new String[] {OFFCAMPUS_USER, OFFCAMPUS_USER2, WWWCACHE})));
		}});
		assertTrue(service.isOnCampus(request));
	}
	
	/**
	 * Though we trust wwwcache, if it then got proxied by an unknown proxy after that then we can no longer
	 * trust x-forwarded-for, so let's return false. 
	 */
	public void testWwwcacheNotFirst() throws Exception {
		m.checking(new Expectations() {{
			allowing(request).getRemoteAddr(); will(returnValue(LOCALHOST));
			one(request).getHeader(X_FORWARDED_FOR); will(returnValue(xff(new String[] {OFFCAMPUS_USER, WWWCACHE, OFFCAMPUS_USER2})));
		}});
		assertFalse(service.isOnCampus(request));
	}
	
	/**
	 * wwwcache is right next to us, no apache in the way, so it's easy to see.
	 * probably don't need this explicit test since we're allowing direct oncampus requests
	 * anyway, but it can't hurt to be thorough (except for that one time...)
	 */
	public void testWwwcacheDirect() throws Exception {
		m.checking(new Expectations() {{
			allowing(request).getRemoteAddr(); will(returnValue(WWWCACHE));
			//one(request).getHeader(X_FORWARDED_FOR); will(returnValue(xff(new String[] {X_FORWARDED_FOR, OFFCAMPUS_USER})));
			never(request).getHeader(X_FORWARDED_FOR);
		}});
		assertTrue(service.isOnCampus(request));
	}
	
	// convenience method
	private String xff(String[] ips) {
		return StringUtils.collectionToDelimitedString(Arrays.asList(ips), ", ");
	}
	
	// sanity test for convenience method!
	public void testXff() throws Exception {
		assertEquals(
				OFFCAMPUS_USER+", "+OFFCAMPUS_USER2+", "+WWWCACHE, 
				xff(new String[]{OFFCAMPUS_USER, OFFCAMPUS_USER2, WWWCACHE})
				);
	}
}
