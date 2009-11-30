package uk.ac.warwick.userlookup;

import junit.framework.TestCase;

public class UserLookupFactoryTest extends TestCase {
	
	public void setUp() {
		UserLookupFactory.clear();
	}
	
	public void testCreatingInstance() {
		UserLookupInterface u = UserLookupFactory.getInstance();
		assertNotNull("UserLookup was null", u);
		assertTrue(u instanceof UserLookup);
	}
	
	public void testCreatingCustomClass() {
		UserLookupFactory.setInstanceClass(MyUserLookup.class);
		UserLookupInterface u = UserLookupFactory.getInstance();
		assertNotNull("argh", u);
		assertTrue(u instanceof MyUserLookup);
	}
	
	public static class MyUserLookup extends UserLookup {
		public MyUserLookup() {
			super();
		}
	}
}
