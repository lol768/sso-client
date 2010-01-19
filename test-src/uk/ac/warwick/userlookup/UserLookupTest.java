package uk.ac.warwick.userlookup;

import java.util.Arrays;
import java.util.Map;

import junit.framework.TestCase;

import org.jmock.Expectations;
import org.jmock.Mockery;

public class UserLookupTest extends TestCase {
	Mockery mockery = new Mockery();
	
	/**
	 * Asserts that when some lookup fails due to a backend exception, UserLookup
	 * will catch the exception and return an UnverifiedUser.
	 */
	public void testFailedLookups() throws Exception {
		final UserLookupBackend backend = mockery.mock(UserLookupBackend.class);
		final Exception expectedException = new UserLookupException("The Internet is dead");
		mockery.checking(new Expectations(){{
			one(backend).getUserById("testuser"); will(throwException(expectedException));
			one(backend).getUserByToken("abcdefg"); will(throwException(expectedException));
			one(backend).getUsersById(Arrays.asList("aaa","bbb")); will(throwException(expectedException));
		}});
		
		UserLookup ul = new UserLookup();
		ul.setUserLookupBackend(backend);
		
		User testuser = ul.getUserByUserId("testuser");
		assertFalse(testuser.isVerified());
		
		User tokenUser = ul.getUserByToken("abcdefg");
		assertFalse(tokenUser.isVerified());
		
		Map<String, User> usersMap = ul.getUsersByUserIds(Arrays.asList("aaa","bbb"));
		assertEquals(2, usersMap.size());
		assertFalse(usersMap.get("aaa").isVerified());
		assertFalse(usersMap.get("bbb").isVerified());
		
		mockery.assertIsSatisfied();
	}
}
