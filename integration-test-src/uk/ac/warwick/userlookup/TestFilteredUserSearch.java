package uk.ac.warwick.userlookup;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;

import junit.framework.TestCase;

import org.apache.log4j.Logger;

/**
 * @author cusaab (Chris May)
 * 
 */
public class TestFilteredUserSearch extends TestCase {

	private static final String ORIGIN_SENTRY_URL = "https://websignon-test.warwick.ac.uk/origin/sentry";
	
	private static final Logger LOGGER = Logger.getLogger(TestFilteredUserSearch.class);
	

	public void testFindUsersByName() throws Exception {

		HashMap filterValues = new HashMap();
		filterValues.put("sn", "Howes");
		filterValues.put("givenName", "Nichola*");
		List users = doSearch(filterValues);

		assertEquals("Should have found right number of users", 1, users.size());

	}

	public void testFindUsersByName2() throws Exception {

		HashMap filterValues = new HashMap();
		filterValues.put("sn", "Wood");
		filterValues.put("givenName", "David*");
		List users = doSearch(filterValues);

		assertTrue("Should have found more than one user", users.size() > 1);

	}

	public void testFindUsersByName3() throws Exception {

		HashMap filterValues = new HashMap();
		filterValues.put("givenName", "A*");
		List users = doSearch(filterValues);

		assertEquals("Should have found right number of users", 2298, users.size());

	}

	public void testFindUsersUniversityNumber() throws Exception {

		HashMap filterValues = new HashMap();
		filterValues.put("warwickuniid", "9170726");
		List users = doSearch(filterValues);

		assertEquals("Should have found right number of users", 1, users.size());

	}

	public void testFindDisabledUserCode() throws Exception {

		HashMap filterValues = new HashMap();
		filterValues.put("cn", "pyubag");
		List users = doSearch(filterValues);
		assertTrue("Should have found no users", users.isEmpty());

	}

	public void testFindUsersByUserCode() throws Exception {

		HashMap filterValues = new HashMap();
		filterValues.put("cn", "cusbb");
		List users = doSearch(filterValues);

		assertEquals("Should have found right number of users", 1, users.size());

	}

	public void testFindUserWithStrangeName() throws Exception {

		HashMap filterValues = new HashMap();
		filterValues.put("cn", "csufac");
		List users = doSearch(filterValues);

		assertEquals("Should have found right number of users", 1, users.size());

		User user = (User) users.get(0);

		assertNotNull(user);

	}

	public void testFindUsersByDepartment() throws Exception {

		HashMap filterValues = new HashMap();
		filterValues.put("ou", "Information Technology Services");
		filterValues.put("cn", "cus*");
		//filterValues.put("warwickuniid", "*");
		List users = doSearch(filterValues);
		List allUsers = doSearch(filterValues, true);

		assertEquals("Expected max users (100) in results", 100, allUsers.size());
		
		assertTrue("Expected live users to be less than 100 but more than 0", users.size() < 100 && users.size() > 0);

	}

	public void testFindUsersByDepartmentCode() throws Exception {

		HashMap filterValues = new HashMap();
		filterValues.put("warwickdeptcode", "IN");
		//filterValues.put("warwickuniid", "*");
		List users = doSearch(filterValues);
		

		assertTrue("Should have found right number of users, not " + users.size(), users.size() > 100);
	}

	public void testFindUserByUserId() throws Exception {

		HashMap filterValues = new HashMap();
		filterValues.put("cn", "cusaab");
		List users = doSearch(filterValues);

		assertEquals("Should have found right number of users", 1, users.size());

	}

	public void testFindOldUserByUserId() throws Exception {

		HashMap filterValues = new HashMap();
		filterValues.put("cn", "rtsbs");
		List users = doSearch(filterValues);

		assertEquals("Should have found right number of users", 0, users.size());

	}

	private List doSearch(HashMap filterValues) {
		return doSearch(filterValues, false);
	}
	
	private List doSearch(HashMap filterValues, boolean findDisabled) {
		UserLookupInterface lookup = UserLookup.getInstance();
		List users = lookup.findUsersWithFilter(filterValues, findDisabled);
		assertNotNull("Should have returned a list of users!", users);
		System.out.println("Found " + users.size() + " users!");

		for (int i = 0; i < users.size(); i++) {
			User user = (User) users.get(i);
			LOGGER.debug("Found user:" + user.getFullName() + " - " + user.getEmail() + " - " + user.getUserId());
		}

		return users;

	}

	protected void setUp() throws Exception {
		super.setUp();
		// System.setProperty("userlookup.ldapUrl", "ldaps://nds.warwick.ac.uk");
		//System.setProperty("userlookup.ldapUrl", "ldaps://nds.warwick.ac.uk");
		System.setProperty("userlookup.ssosUrl", ORIGIN_SENTRY_URL);
	}



}