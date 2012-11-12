package uk.ac.warwick.userlookup;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import uk.ac.warwick.userlookup.cache.Cache;

import junit.framework.TestCase;

/**
 * @author kieranshaw
 * 
 */
public class TestUserLookup extends TestCase {

	private UserLookup _userLookup;

	private final String TEST_LDAP_USER_ID = "psumd";

	private final String TEST_LDAP_USER_PASS = "slapp-r";
	
	private final String TEST_USER_ID = "cusaab";

	/**
	 * Constructor for TestUserLookup.
	 * 
	 * @param arg0
	 */
	public TestUserLookup(String arg0) {
		super(arg0);
	}

	public void testGetUserByAuth() throws Exception {
		assertTrue("User returned was blank!!", getLoggedInUser().isFoundUser());
	}

	public void testGetInvalidUserByAuth() throws Exception {
		User user = getUserLookup().getUserByIdAndPass("test", "badpassword");
		assertFalse("A user was returned and should not have been", user.isFoundUser());
	}

	public void testGetUserByWarwickUniId() throws Exception {

		// used with false as second argument to skip chris' disabled user accounts 
		User user = getUserLookup().getUserByWarwickUniId("0270358", false);
		assertTrue("A user was returned", user.isFoundUser());
		assertEquals("Should have found right user", "cusaab", user.getUserId());

	}
	
	public void testGetRightUserByWarwickUniId() throws Exception {

		User user = getUserLookup().getUserByWarwickUniId("9170726");
		assertTrue("A user was returned", user.isFoundUser());
		assertEquals("Should have found right user", "curef", user.getUserId());
	}
	
	public void testUnverifiedUser() throws Exception {
		
	}

//	public void testGetUserMultipleUsersByWarwickUniId() throws Exception {
//
//		List usersInDepartmentCode = getUserLookup().getUsersInDepartmentCode("IN");
//		Iterator iterator = usersInDepartmentCode.iterator();
//		while (iterator.hasNext()) {
//			User user = (User) iterator.next();
//			User userByUniId = getUserLookup().getUserByWarwickUniId(user.getWarwickId());
//			assertEquals(user.getUserId(), userByUniId.getUserId());
//
//		}
//
//	}

	public void testGetSSOUserByUserAndPass() throws Exception {
		User user = getUserLookup().getUserByIdAndPass("sbtest2", "sbtest2");
		assertTrue("User returned was blank!!", user.isFoundUser());
	}

	public void testGetUserByToken() throws Exception {
		User user = getUserLookup().getUserByToken(getLoggedInUser().getOldWarwickSSOToken());
		assertNotNull("User returned was null!", user);
		assertEquals("User not the expected user!", "kieran.shaw@warwick.ac.uk", user.getEmail());
	}

	public void testGetUserByInvalidToken() throws Exception {
		User user = getUserLookup().getUserByToken("sso3d9ac0b2e9a8d");
		assertTrue("A user was returned and should not have been", !user.isFoundUser());
	}

	public void testGetStaffUserByUserId() throws Exception {
		UserLookup lookup = new UserLookup();
		// lookup.setSsosUrl("http://localhost.warwick.ac.uk:8080/wsos/sentry");
		User user = lookup.getUserByUserId("cusaab");

		assertTrue("User returned was blank!!", user.isFoundUser());
		assertTrue("User is not staff!", user.isStaff());
		assertTrue("User is not staff!", !user.isStudent());
		assertEquals("User not the expected user!", "chris.may@warwick.ac.uk".toLowerCase(), user.getEmail().toLowerCase());
		assertEquals("User not the expected user!", "Chris May", user.getFullName());
		assertEquals("Did not find correct warwick id", "0270358", user.getWarwickId());
		assertEquals("Did not find correct department", "Information Technology Services", user.getDepartment());
		assertEquals("Did not find correct departmentCode", "IN", user.getDepartmentCode());

		assertEquals("Did not find correct warwickattendancemode", "F", user
				.getExtraProperty(ExtraProperties.WARWICK_ATTENDANCE_MODE));

	}

	public void testGetInvalidUserById() throws Exception {

		User user = getUserLookup().getUserByUserId("myfriendbob");
		assertTrue("A user was returned and should not have been", !user.isFoundUser());
	}

	public void testGetSSOUserById() throws Exception {

		User user = getUserLookup().getUserByUserId("dalejohn");
		assertTrue("A user was not found", user.isFoundUser());
	}

	public void testGetSSOUserById2() throws Exception {

		User user = getUserLookup().getUserByUserId("cusaab");
		assertTrue("A user was was not returned and should have been", user.isFoundUser());
		assertEquals("Did not find correct warwick id", "0270358", user.getWarwickId());
	}
	
	public void testGetMultipleUsers() throws Exception {
		assertTrue(getUserLookup().isUserByUserIdCacheEmpty());
		
		List userIdList = new ArrayList() {{;
			add("cusfal");
			add("cusebr");
		}};
		Map results = getUserLookup().getUsersByUserIds(userIdList );
		assertEquals(2, results.size());
		
		User cusfal = (User) results.get("cusfal");
		User cusebr = (User) results.get("cusebr");
		
		assertEquals("cusfal", cusfal.getUserId());
		assertEquals("cusebr", cusebr.getUserId());
	}

	public void testNotFoundUsersAreReturnedAnonymous() throws Exception {
		assertTrue(getUserLookup().isUserByUserIdCacheEmpty());
		
		final String name = "noboody777";
		
		Map results = getUserLookup().getUsersByUserIds(new ArrayList() {{
			add(name);
		}});
		
		assertEquals(1, results.size());
		User u = (User) results.get(name);
		assertEquals(name, u.getUserId());
		assertFalse(u.isFoundUser());
	}

	private User getLoggedInUser() throws Exception {
		User user = getUserLookup().getUserByIdAndPass("sbtest2", "sbtest2");
		assertTrue("User returned was blank!!", user.isFoundUser());
		return user;
	}

	public void testGetUserWithNull() throws Exception {

		User user = getUserLookup().getUserByIdAndPass(null, null);
		assertTrue("A user was returned and should not have been", !user.isFoundUser());
		user = getUserLookup().getUserByToken(null);
		assertTrue("A user was returned and should not have been", !user.isFoundUser());
		user = getUserLookup().getUserByUserId(null);
		assertTrue("A user was returned and should not have been", !user.isFoundUser());

	}

	public void testLookupReturnsBlankUserWhenSSOIsDown() throws Exception {

		String token = getLoggedInUser().getOldWarwickSSOToken();
		System.setProperty("userlookup.useRemoteBeanSSO", "false");
		System.setProperty("userlookup.ssosUrl", "http://www.warwick.ac.uk/asdfa");
		getUserLookup().getUserByTokenCache().clear();
		User user = getUserLookup().getUserByToken(token);
		System.setProperty("userlookup.ssosUrl", "http://www.warwick.ac.uk/elab_ssos/ssos.php");
		System.setProperty("userlookup.useRemoteBeanSSO", "true");
		assertNotNull("Should have got a blank user, not a null user!", user);

		assertTrue("Should have found blank user, not a populated user", !user.isFoundUser());

	}

	public void testGenerateToken() throws Exception {

		String token = UserLookup.generateRandomTicket();
		assertEquals("Wrong length token", 33, token.length());

		String token2 = UserLookup.generateRandomTicket();

		assertTrue("Tokens should be different!", !token.equals(token2));

	}

	public void testUserlookupCaching() throws Exception {

		UserLookup lookup = new UserLookup();
		Cache<String, User> cache = lookup.getUserByTokenCache();
		cache.setMaxSize(5);
		// increase this parameter for debugging, or the cache will time out
		// items
		// before you reach the end of the test
		cache.setTimeout(3);
		

		User user = lookup.getUserByUserId(TEST_USER_ID);

		assertTrue("Failed to get user from cache!", user.getUserId().equals(TEST_USER_ID));
		assertTrue("User is not in cache!", cache.contains(TEST_USER_ID));

		user = lookup.getUserByUserId(TEST_USER_ID);

		assertTrue("Failed to get user from cache!", user.getUserId().equals(TEST_USER_ID));

		user = lookup.getUserByUserId("cusfal");
		user = lookup.getUserByUserId("cuscab");
		user = lookup.getUserByUserId("cusxac");

		assertEquals("Cache wrong size!", 4, cache.getStatistics().getCacheSize());

		cache.setMaxSize(4);

		User item = cache.get(TEST_USER_ID);
		assertNotNull("Should have returned " + TEST_USER_ID, item);

		user = lookup.getUserByUserId("curef");

		assertEquals("Cache wrong size!", 4, cache.getStatistics().getCacheSize());
//
//		// first-inserted item has now been moved out to make space
//		assertFalse("User "+TEST_USER_ID+" should not be in cache now!", cache.containsKey(TEST_USER_ID));
//		// second-inserted should be OK
//		assertTrue("User cusfal should be be in cache now!", cache.containsKey("cusfal"));
//
//		assertFalse("Should have found user as it should not timed out", cache.get("curef").isExpired());
//
//		Thread.sleep(cache.getTimeout() * 1100);
//
//		assertTrue("Should not have found user as it should have timed out", cache.get("curef").isExpired());

	}

	/**
	 * @see junit.framework.TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		super.setUp();
		System.setProperty("java.security.auth.login.config", "c:\\apps\\jboss\\client\\auth.conf");
		//System.setProperty("userlookup.ldapUrl", "ldap://nds.warwick.ac.uk");
		// System.setProperty("userlookup.ssosUrl",
		// "http://websignon.warwick.ac.uk/sentry");

		// System.setProperty("userlookup.ssosUrl", "http://moleman.warwick.ac.uk:8080/origin/sentry");
		System.setProperty("userlookup.ssosUrl", "https://websignon.warwick.ac.uk/sentry");

		setUserLookup(new UserLookup());
	}

	private void setUserLookup(UserLookup userLookup) {
		_userLookup = userLookup;
	}

	private UserLookup getUserLookup() {
		return _userLookup;
	}

}
