package uk.ac.warwick.userlookup;

import junit.framework.TestCase;

public class AdapterUserBuilderTest extends TestCase {

    public void testBuildUser() {
        User user = AdapterUserBuilder.buildUser(new MockUserAdapter());

        assertEquals("g.blogs@warwick.ac.uk", user.getEmail());
        assertEquals("Gemma Blogs", user.getFullName());
        assertEquals("Gemma", user.getFirstName());
        assertEquals("Blogs", user.getLastName());
        assertEquals("u1490600", user.getUserId());
        assertEquals("Staff", user.getUserType());
        assertEquals("old-WarwickSSO-token", user.getOldWarwickSSOToken());
        assertEquals("IT Services", user.getDepartment());
        assertEquals("IN", user.getDepartmentCode());
        assertEquals("ITS", user.getShortDepartment());
        assertEquals("1490600", user.getWarwickId());
        assertTrue(user.isStaff());
        assertFalse(user.isStudent());
        assertFalse(user.isAlumni());
        assertFalse(user.isLoggedIn());
        assertFalse(user.isLoginDisabled());
    }

}
