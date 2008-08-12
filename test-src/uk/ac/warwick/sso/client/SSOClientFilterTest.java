package uk.ac.warwick.sso.client;

import uk.ac.warwick.userlookup.UserLookup;
import junit.framework.TestCase;

public class SSOClientFilterTest extends TestCase {
	public void testGetUserLookup() {
		SSOClientFilter f = new SSOClientFilter();
		UserLookup u = f.getUserLookup();
		assertNotNull(u);
	}
}
