package uk.ac.warwick.userlookup;

import junit.framework.TestCase;

public final class UserAttributesTest extends TestCase {
	public void testUserNoShortDept() {
		User user = new User();
		user.setDepartment("Institute of Snobbery");
		assertEquals("Institute of Snobbery", user.getDepartment());
		assertEquals("Institute of Snobbery", user.getShortDepartment());
		assertEquals("Institute of Snobbery", user.getShortDepartmentHtml());
	}
	
	public void testUserWithShortDept() {
		User user = new User();
		user.setDepartment("Institute of Snobbery");
		user.setShortDepartment("Snobs");
		assertEquals("Institute of Snobbery", user.getDepartment());
		assertEquals("Snobs", user.getShortDepartment());
		assertEquals("Snobs", user.getShortDepartmentHtml());
	}
	
	public void testUserWithAbbrShortDept() {
		User user = new User();
		user.setDepartment("Institute of Snobbery");
		user.setShortDepartment("IOS");
		assertEquals("Institute of Snobbery", user.getDepartment());
		assertEquals("IOS", user.getShortDepartment());
		assertEquals("<abbr title=\"Institute of Snobbery\">IOS</abbr>", user.getShortDepartmentHtml());
	}
	
	public void testUserWithAbbrShortDeptEscaping() {
		User user = new User();
		user.setDepartment("\"John\" Institute of Snobbery & Stuff <arf>");
		user.setShortDepartment("IOS");
		assertEquals("\"John\" Institute of Snobbery & Stuff <arf>", user.getDepartment());
		assertEquals("IOS", user.getShortDepartment());
		assertEquals("<abbr title=\"&quot;John&quot; Institute of Snobbery &amp; Stuff &lt;arf&gt;\">IOS</abbr>", user.getShortDepartmentHtml());
	}
	
	public void testUserWithNoDepartment() {
		User user = new AnonymousUser();
		
		assertNull(user.getDepartment());
		assertNull(user.getShortDepartment());
		assertNull(user.getShortDepartmentHtml());
	}
}
