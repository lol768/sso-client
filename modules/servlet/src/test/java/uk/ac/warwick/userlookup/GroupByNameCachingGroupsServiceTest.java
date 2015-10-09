package uk.ac.warwick.userlookup;

import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import uk.ac.warwick.userlookup.webgroups.GroupNotFoundException;


public class GroupByNameCachingGroupsServiceTest {

    private JUnit4Mockery mockery = new JUnit4Mockery();
    private GroupService delegate = mockery.mock(GroupService.class);
    private GroupByNameCachingGroupsService service = new GroupByNameCachingGroupsService(delegate);

    @Before
    public void setUp() throws Exception {

    }

    /**
     * SSO-1488 should cache the result even if the group didn't exist.
     */
    @Test
    public void testMissingGroupsCached() throws Exception {
        mockery.checking(new Expectations() {{
            one(delegate).getGroupByName("test-group-name"); will(throwException(new GroupNotFoundException("test-group-name")));
        }});

        try {
            service.getGroupByName("test-group-name");
            Assert.fail("Should have thrown GNFE");
        } catch (GroupNotFoundException e) {
            // ok
        }
        try {
            service.getGroupByName("test-group-name");
            Assert.fail("Should have thrown GNFE");
        } catch (GroupNotFoundException e) {
            // ok
        }

        // Should only have been one call to the underlying group service.
        mockery.assertIsSatisfied();
    }
}
