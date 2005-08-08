/*
 * Created on 08-Aug-2005
 *
 */
package uk.ac.warwick.sso.client;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;


public class AllTests extends TestCase {
	
	public static Test suite() {

		TestSuite ts = new TestSuite();
		ts.addTestSuite(AttributeAuthorityFetcherTests.class);
		ts.addTestSuite(BasicAuthTests.class);
		ts.addTestSuite(ConfigurationTests.class);
		ts.addTestSuite(ShireCommandTests.class);
		ts.addTestSuite(SSOLinkGeneratingTests.class);
		ts.addTestSuite(SSOProxyCookieHelperTests.class);
		return ts;

	}

}
