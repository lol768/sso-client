package uk.ac.warwick.sso.client;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.beans.HasPropertyWithValue.hasProperty;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import static org.hamcrest.core.DescribedAs.describedAs;

import uk.ac.warwick.userlookup.AnonymousUser;
import uk.ac.warwick.userlookup.User;

/**
 * Methods for static importing in tests. 
 */
@SuppressWarnings("unchecked")
public class TestMethods {
	
	private TestMethods() {}
	
	public static Matcher<User> verified() { return isVerified(true); }
	
	public static <T> Matcher<T> isVerified(boolean verified) {
		Matcher<T> hasProperty = hasProperty("verified", is(verified));
		return describedAs("verified user", hasProperty);
	}
	
	public static Matcher<User> found() {
		Matcher<User> hasProperty = hasProperty("foundUser", is(true));
		return describedAs("found user", hasProperty);
	}
	
	public static Matcher<User> anonymous() {
		Matcher<User> anonymous = allOf(verified(), not(found()));
		return describedAs("anonymous user", anonymous);
	}
	
	public static Matcher<User> unverified() {
		return not(verified());
	}
	
	/**
	 * Create a valid test User object.
	 */
	public static User user(String userId) {
		User user = new User(userId);
		user.setFullName(String.format("%sy %serson", userId, userId));
		user.setFoundUser(true);
		return user;
	}
	
	/**
	 * Create an AnonymousUser.
	 */
	public static AnonymousUser anon(String userId) {
		AnonymousUser user = new AnonymousUser();
		user.setUserId(userId);
		return user;
	}
	
	public static Matcher<Map<?,?>> emptyMap() {
		return new BaseMatcher<Map<?,?>>() {
			public boolean matches(Object item) {
				return ((Map)item).isEmpty();
			}

			public void describeTo(Description description) {
				description.appendText("empty");
			}
			
		};
	}
	
	public static byte[] read(InputStream is) throws IOException {
		try {
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			byte[] buf = new byte[1024*8];
			int read = 0;
			while ((read = is.read(buf)) > -1) {
				os.write(buf, 0, read);
			}
			return os.toByteArray();
		} finally {
			is.close();
		}
	}
}
