package uk.ac.warwick.sso.client;

import org.junit.Test;

import static org.junit.Assert.*;

import static org.hamcrest.Matchers.*;

/**

 */
public class SSOClientVersionLoaderTest {

    @Test
    public void testGetVersion() throws Exception {
        assertThat(SSOClientVersionLoader.getVersion(), not(isEmptyString()));
    }
}