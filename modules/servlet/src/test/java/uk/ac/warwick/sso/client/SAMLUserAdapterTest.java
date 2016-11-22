package uk.ac.warwick.sso.client;

import junit.framework.TestCase;
import org.opensaml.SAMLAttribute;
import org.opensaml.SAMLException;

import java.util.Collections;
import java.util.Properties;

public class SAMLUserAdapterTest extends TestCase {

    public void testUserAttributes() {
        Properties properties = new Properties();
        putAttribute(properties, "mail", "g.blogs@warwick.ac.uk");
        putAttribute(properties, "givenName", "Gemma");
        putAttribute(properties, "sn", "Blogs");
        putAttribute(properties, "cn", "u1490600");
        putAttribute(properties, "warwickuniid", "1490600");
        putAttribute(properties, "urn:websignon:usertype", "Staff");
        putAttribute(properties, "staff", "true");
        putAttribute(properties, "ou", "IT Services");
        putAttribute(properties, "warwickdeptcode", "IN");
        putAttribute(properties, "deptshort", "ITS");
        putAttribute(properties, "urn:websignon:loggedin", "true");
        putAttribute(properties, "logindisabled", "true");

        SAMLUserAdapter a = new SAMLUserAdapter(properties);

        assertEquals("g.blogs@warwick.ac.uk", a.getEmail());
        assertEquals("Gemma", a.getFirstName());
        assertEquals("Blogs", a.getLastName());
        assertEquals("u1490600", a.getUserId());
        assertEquals("Staff", a.getUserType());
        assertEquals("IT Services", a.getDepartment());
        assertEquals("IN", a.getDepartmentCode());
        assertEquals("ITS", a.getDepartmentShortName());
        assertEquals("1490600", a.getUniversityID());

        assertTrue(a.isStaff());
        assertFalse(a.isStudent());
        assertFalse(a.isAlumni());
        assertTrue(a.isLoggedIn());
        assertTrue(a.isLoginDisabled());
    }

    private void putAttribute(Properties properties, String name, String value) {
        try {
            properties.put(name, new SAMLAttribute(name, "", null, -1, Collections.singletonList(value)));
        } catch (SAMLException e) {
            e.printStackTrace();
        }
    }

}
