package uk.ac.warwick.sso.client;

import junit.framework.TestCase;
import uk.ac.warwick.userlookup.UserLookupInterface;

public class SSOClientFilterTest extends TestCase {
	public void testGetUserLookup() {
		SSOClientFilter f = new SSOClientFilter();
		UserLookupInterface u = f.getUserLookup();
		assertNotNull(u);
	}
}
