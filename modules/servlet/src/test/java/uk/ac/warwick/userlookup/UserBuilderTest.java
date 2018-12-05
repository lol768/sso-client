package uk.ac.warwick.userlookup;

import junit.framework.TestCase;
import org.junit.Assert;

import java.util.HashMap;
import java.util.Map;

public class UserBuilderTest extends TestCase {

    public void testBuildSamlUser() {
        Map<String, String> attributes = new HashMap<>();
        attributes.put("staff", "true");
        attributes.put("cn", "u1673477");
        attributes.put("warwickuniid", "1673477");
        attributes.put("warwickdeptcode", "IN");

        UserBuilder sut = new UserBuilder();
        String warwickId = sut.populateUserFromSAML(attributes).getWarwickId();
        Assert.assertEquals("1673477", warwickId);
    }

    public void testBuildSentryUser() {
        Map<String, String> attributes = new HashMap<>();
        attributes.put("user", "u1673477");
        attributes.put("id", "1673477");
        attributes.put("deptcode", "IN");

        UserBuilder sut = new UserBuilder();
        String warwickId = sut.populateUserFromSentry(attributes).getWarwickId();
        Assert.assertEquals("1673477", warwickId);
    }

}
