package uk.ac.warwick.userlookup;

import static java.util.Collections.*;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.collection.IsMapContaining.*;
import static org.junit.Assert.*;
import static uk.ac.warwick.sso.client.TestMethods.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jmock.Mockery;
import org.junit.Before;
import org.junit.Test;
import org.yecht.MapStyle;

@SuppressWarnings("unchecked")
public class UserLookupTest {
	Mockery mockery = new Mockery();
	
	private TestSentryServer sentry;
	private UserLookup ul;
	
	@Before public void before() {
		 sentry = new TestSentryServer();
		 ul = new UserLookup();
		 ul.setSsosUrl(sentry.getPath());
	}
	
	/**
	 * Asserts that when some lookup fails due to a backend problem, UserLookup
	 * will catch the exception and return an UnverifiedUser.
	 */
	@Test public void returnsUnverifiedUserOnFailure() throws Exception {
		sentry.willReturnErrors();
		sentry.run(new Runnable(){ public void run() {
			assertThat("user by id", ul.getUserByUserId("cusaab"), is(unverified()) );
			assertThat("user by token", ul.getUserByToken("abcdefg"), is(unverified()) );
			assertThat("user by number", ul.getUserByWarwickUniId("01234567"), is(unverified()));
			
			//fetching multiple users
			Map<String, User> usersMap = ul.getUsersByUserIds(Arrays.asList("aaa","bbb"));
			assertThat(usersMap, hasEntry(is("aaa"), is(unverified()) ));
			assertThat(usersMap, hasEntry(is("bbb"), is(unverified()) ));
			
			List<User> filtered = ul.findUsersWithFilter(mapFrom("sn","Aigabe"));
			assertTrue(filtered.isEmpty());
		}});
	}
	
	/**
	 * Searching for a user by Warwick ID should then do a sentry lookup in
	 * order to get the more detailed User object. This test checks that this
	 * happens and that we don't end up using a cached version.
	 */
	@Test public void getUserByWarwickUniId() throws Exception {
		Map<String, String> searchResult = new HashMap<String, String>();
		searchResult.put("sn","Howes");
		searchResult.put("givenName", "Nick");
		searchResult.put("warwickuniid", "1234567");
		searchResult.put("cn", "cusebr");
		searchResult.put("mail", "n.howes@warwick.ac.uk");
		
		Map<String, String> result = new HashMap<String, String>();
		result.put("returnType","4");
		result.put("lastname","Howes");
		result.put("firstname", "Nick");
		result.put("id", "1234567");
		result.put("user", "cusebr");
		result.put("warwickitsclass", "Staff");
		
		sentry.setSearchResults(singletonList(searchResult));
		sentry.setResults(singletonList(result));
		sentry.run(new Runnable(){ public void run() {
			User nick = ul.getUserByWarwickUniId("1234567");
			assertEquals("Staff", nick.getExtraProperty("warwickitsclass"));
		}});
	}
	
	@Test public void returnsAnonymousUserWhenNotFound() throws Exception {
		sentry.willReturnUsers(new AnonymousUser());
		sentry.run(new Runnable() { public void run() {
			User anon = ul.getUserByUserId("invisibleman");
			assertThat(anon, is(anonymous()));
		}});
	}
	
	@Test public void returnsAnonymousUsersWhenSomeNotFound() throws Exception {
		sentry.willReturnUsers(user("alan"), user("boris"));
		sentry.run(new Runnable() {
			public void run() {
				Map<String, User> result = ul.getUsersByUserIds(Arrays.asList("alan","gobald","whoareyou","boris"));
				assertThat(result, hasEntry(is("alan"), is(found()) ));
				assertThat(result, hasEntry(is("boris"), is(found()) ));
				assertThat(result, hasEntry(is("gobald"), is(anonymous()) ));
				assertThat(result, hasEntry(is("whoareyou"), is(anonymous()) ));
			}
		});
	}
}
