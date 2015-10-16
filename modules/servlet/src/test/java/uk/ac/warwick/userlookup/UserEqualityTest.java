package uk.ac.warwick.userlookup;

import junit.framework.TestCase;

public final class UserEqualityTest extends TestCase {
    public void testDifferentUserId() {
        User userA = new User();
        userA.setUserId("theUserId");
        
        User userB = new User();
        userB.setUserId(userA.getUserId() + "different");
        assertFalse("equals userA", userA.equals(userB));
        assertFalse("equals userB", userB.equals(userA));
        assertFalse("hashCode", userA.hashCode() == userB.hashCode());
    }
    
    public void testSameUserId() {
        User userA = new User();
        userA.setUserId("theUserId");
        
        User userB = new User();
        userB.setUserId(userA.getUserId());
        assertTrue("equals userA", userA.equals(userB));
        assertTrue("equals userB", userB.equals(userA));
        assertTrue("hashCode", userA.hashCode() == userB.hashCode());
    }

    public void testNullUserId() {
        User userA = new User();
        userA.setUserId(null);
        
        User userB = new User();

        assertTrue("equals userA", userA.equals(userB));
        assertTrue("equals userB", userB.equals(userA));
        assertTrue("hashCode", userA.hashCode() == userB.hashCode());
    }
}
