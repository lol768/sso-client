package uk.ac.warwick.userlookup;

import static org.junit.Assert.assertThat;
import static org.hamcrest.beans.HasPropertyWithValue.hasProperty;
import static org.hamcrest.core.Is.*;

import org.hamcrest.core.Is;
import org.jmock.Mockery;
import org.junit.Before;
import org.junit.Test;

public class UserLookupApiTest {
	private Mockery m;
	private UserLookup userLookup;	
	
	@Before public void before() {
		userLookup = new UserLookup();
	}
	
	@Test public void shouldReturnAnonymousUserWhenMissing() {
		User anon = userLookup.getUserByUserId("unlikelytoexist");
		
		
		assertThat(anon, hasProperty("foundUser", is(false)));
	}
}
