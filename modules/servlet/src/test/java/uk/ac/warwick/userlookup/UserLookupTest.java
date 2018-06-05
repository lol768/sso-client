package uk.ac.warwick.userlookup;

import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static uk.ac.warwick.sso.client.TestMethods.*;

import java.util.*;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import org.jmock.Mockery;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import uk.ac.warwick.util.collections.Pair;

@SuppressWarnings("unchecked")
public class UserLookupTest {
	Mockery mockery = new Mockery();
	
	private TestSentryServer sentry;
	private UserLookup ul;

	private User nickHowes;
	private User nickFury;

	@Before
	public void before() {
		sentry = new TestSentryServer();
		ul = new UserLookup();
		ul.setSsosUrl(sentry.getPath());
		// cache may be disk based so clear everything out
		ul.clearCaches();

		nickHowes = new User("cusebr");
		nickHowes.setFoundUser(true);
		nickHowes.setWarwickId("1234567");
		nickHowes.setFirstName("Nick");
		nickHowes.setLastName("Howes");
		nickHowes.setUserSource("WarwickADS");
		nickHowes.setEmail("n.howes@warwick.ac.uk");
		nickHowes.setWarwickPrimary(true);
		nickHowes.setExtraProperties(new ImmutableMap.Builder()
				.put("warwickitsclass", "Staff")
				.build());

		nickFury = new User("cusraa");
		nickFury.setFoundUser(true);
		nickFury.setWarwickId("1234568");
		nickFury.setFirstName("Nick");
		nickFury.setLastName("Fury");
		nickFury.setUserSource("WarwickADS");
		nickFury.setEmail("n.howes@warwick.ac.uk");
		nickFury.setWarwickPrimary(true);
		nickFury.setExtraProperties(new ImmutableMap.Builder()
				.put("warwickitsclass", "Staff")
				.build());
	}

	private Map<String,String> toSentryResult(User user) {
		Map<String, String> result = new HashMap<String, String>();
		result.put("returnType","4");
		result.put("lastname",user.getLastName());
		result.put("firstname", user.getFirstName());
		result.put("id", user.getWarwickId());
		result.put("user", user.getUserId());
		result.putAll(user.getExtraProperties());
		result.put("urn:websignon:usersource", "WarwickADS");
		return result;
	}

	private Map<String,String> toSearchResult(User user) {
		Map<String, String> searchResult = new HashMap<String, String>();
		searchResult.put("sn",user.getLastName());
		searchResult.put("givenName", user.getFirstName());
		searchResult.put("warwickuniid", user.getWarwickId());
		searchResult.put("cn", user.getUserId());
		searchResult.put("mail", user.getEmail());
		return searchResult;
	}

	@After public void after() {
		ul.shutdown();
	}
	
	/**
	 * Asserts that when some lookup fails due to a backend problem, UserLookup
	 * will catch the exception and return an UnverifiedUser.
	 */
	@Test public void returnsUnverifiedUserOnFailure() throws Exception {
		sentry.willReturnErrors();
		sentry.run(() -> {
			assertThat("user by id", ul.getUserByUserId("cusaab"), is(unverified()) );
			assertThat("user by token", ul.getUserByToken("abcdefg"), is(unverified()) );
			assertThat("user by number", ul.getUserByWarwickUniId("01234567"), is(unverified()));

			//fetching multiple users
			Map<String, User> usersMap = ul.getUsersByUserIds(asList("aaa","bbb"));
			assertThat(usersMap, hasEntry(is("aaa"), is(unverified()) ));
			assertThat(usersMap, hasEntry(is("bbb"), is(unverified()) ));

			// multiple users by Uni ID
			Map<String, User> usersMap2 = ul.getUsersByWarwickUniIds(asList("111","222"));
			assertThat(usersMap2, hasEntry(is("111"), is(unverified()) ));
			assertThat(usersMap2, hasEntry(is("222"), is(unverified()) ));


			List<User> filtered = ul.findUsersWithFilter(singletonMap("sn","Aigabe"));
			assertTrue(filtered.isEmpty());
		});
	}
	
	/**
	 * Searching for a user by Warwick ID should then do a sentry lookup in
	 * order to get the more detailed User object. This test checks that this
	 * happens and that we don't end up using a cached version.
	 */
	@Test public void getUserByWarwickUniId() throws Exception {
		sentry.setSearchResults(singletonList(toSearchResult(nickHowes)));
		sentry.setResults(singletonList(toSentryResult(nickHowes)));
		sentry.run(() -> {
			User nick = ul.getUserByWarwickUniId("1234567");
			assertEquals("Howes", nick.getLastName());
			assertEquals("Staff", nick.getExtraProperty("warwickitsclass"));
			assertEquals("WarwickADS", nick.getUserSource());
		});
	}

	@Test public void getUsersByWarwickUniIds() throws Exception {
		sentry.setSearchResults(asList(toSearchResult(nickHowes), toSearchResult(nickFury)));
		sentry.setResults(asList(toSentryResult(nickHowes), toSentryResult(nickFury)));
		sentry.run(() -> {
			Map<String, User> result = ul.getUsersByWarwickUniIds(asList("1234567", "1234568", "1234569"));
			assertEquals("Should have batched both ID and usercode lookups", 2, sentry.getRequestCount());
			assertThat(result, hasEntry(equalTo("1234567"), hasProperty("foundUser", equalTo(true))));
			assertEquals("Must have rich attributes from usercode lookup", "Staff", result.get("1234567").getExtraProperty("warwickitsclass"));
			assertThat(result, hasEntry(equalTo("1234568"), hasProperty("foundUser", equalTo(true))));
			assertThat(result, hasEntry(equalTo("1234569"), hasProperty("foundUser", equalTo(false))));
		});
	}

	@Test public void returnsAnonymousUserWhenNotFound() throws Exception {
		sentry.willReturnUsers(new AnonymousUser());
		sentry.run(() -> {
			User anon = ul.getUserByUserId("invisibleman");
			assertThat(anon, is(anonymous()));
		});
	}
	
	@Test public void returnsAnonymousUsersWhenSomeNotFound() throws Exception {
		sentry.willReturnUsers(user("alan"), user("boris"));
		sentry.run(() -> {
			Map<String, User> result = ul.getUsersByUserIds(asList("alan","gobald","whoareyou","boris"));
			assertThat(result, hasEntry(is("alan"), is(found()) ));
			assertThat(result, hasEntry(is("boris"), is(found()) ));
			assertThat(result, hasEntry(is("gobald"), is(anonymous()) ));
			assertThat(result, hasEntry(is("whoareyou"), is(anonymous()) ));
		});
	}

	@Test public void basicAuth() throws Exception {
		sentry.setSuccessType("2");
		sentry.willReturnUsers(user("reg"));
		sentry.run(() -> {
			try {
				assertFalse(ul.getAuthCache().contains("reg"));

				User user = ul.getUserByIdAndPassNonLoggingIn("reg", "r3g");

				assertEquals("reg", user.getUserId());
				assertTrue(ul.getAuthCache().contains("reg"));
				Pair<String, User> result = ul.getAuthCache().get("reg");
				assertEquals("reg", result.getRight().getUserId());
				assertEquals(CacheDigests.digest("r3g"), result.getLeft());
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});
	}
}
