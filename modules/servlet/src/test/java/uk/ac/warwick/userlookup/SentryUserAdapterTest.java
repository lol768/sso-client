package uk.ac.warwick.userlookup;

import junit.framework.TestCase;

import java.util.HashMap;
import java.util.Map;

public class SentryUserAdapterTest extends TestCase {

    public void testUserAttributes() {
        Map<String, String> attributes = new HashMap<>();
        attributes.put("dept", "IT Services");
        attributes.put("deptcode", "IN");
        attributes.put("deptshort", "ITS");
        attributes.put("email", "g.blogs@warwick.ac.uk");
        attributes.put("firstname", "Gemma");
        attributes.put("id", "1490600");
        attributes.put("lastname", "Blogs");
        attributes.put("name", "Gemma Blogs");
        attributes.put("staff", "true");
        attributes.put("token", "xyz");
        attributes.put("urn:websignon:usertype", "Staff");
        attributes.put("user", "u1490600");

        SentryUserAdapter a = new SentryUserAdapter(attributes);

        assertEquals("g.blogs@warwick.ac.uk", a.getEmail());
        assertEquals("Gemma Blogs", a.getFullName());
        assertEquals("Gemma", a.getFirstName());
        assertEquals("Blogs", a.getLastName());
        assertEquals("u1490600", a.getUserId());
        assertEquals("Staff", a.getUserType());
        assertEquals("xyz", a.getOldWarwickSSOToken());
        assertEquals("IT Services", a.getDepartment());
        assertEquals("IN", a.getDepartmentCode());
        assertEquals("ITS", a.getDepartmentShortName());
        assertEquals("1490600", a.getUniversityID());
        assertEquals(attributes, a.getAttributes());

        assertTrue(a.isStaff());
        assertFalse(a.isStudent());
        assertFalse(a.isAlumni());
        assertTrue(a.isLoggedIn());
        assertFalse(a.isLoginDisabled());
    }

}
