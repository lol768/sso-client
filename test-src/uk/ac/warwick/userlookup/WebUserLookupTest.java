package uk.ac.warwick.userlookup;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

public class WebUserLookupTest extends TestCase {
	
	/**
	 * Starts a fake server. Checks a few things about userlookup
	 * 
	 * - that its batch user lookup works in general
	 * - that the batch lookup chunks into a few requests
	 */
	public void testConnect() throws Exception {
		final List<String> manyUsers = new ArrayList<String>();
		for (int i=1; i<=250; i++) {
			manyUsers.add("cuxx"+i);
		}

		// Configure a test server that will return all requested users as
		// found.
		final TestSentryServer sentry = new TestSentryServer();
		sentry.willReturnAllUsers();
		
		sentry.run(new Runnable() {
			public void run() {
				try {
					WebServiceTimeoutConfig config = new WebServiceTimeoutConfig(0, 0);
					String version = null;
					String apiKey = null;
					WebUserLookup wul = new WebUserLookup(sentry.getPath(), config, version, apiKey);
					Map<String, User> usersById = wul.getUsersById(manyUsers);
					assertEquals(3, sentry.getRequestCount());
					assertEquals(250, usersById.size());
				} catch (UserLookupException e) {
					e.printStackTrace();
					fail("Error looking up users");
				}
			}
		});
	}

	

}
