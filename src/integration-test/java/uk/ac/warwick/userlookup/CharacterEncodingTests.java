/*
 * Created on 23-Feb-2006
 *
 */
package uk.ac.warwick.userlookup;

import java.util.Hashtable;
import java.util.Map;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;

import junit.framework.TestCase;

/**
 * http://www.alanwood.net/demos/wgl4.html#w1E00
 * 
 * @author Kieran Shaw
 * 
 */
public class CharacterEncodingTests extends TestCase {

	private static final char U_UMLAUT = (char)252;
	
	public final void testEncoding() throws Exception {

		Character chr = new Character(U_UMLAUT);
		System.out.println(U_UMLAUT);

		System.out.println(new String(new byte[] { (byte) chr.charValue()}, "UTF-8"));
		System.out.println(new String(new byte[] { (byte) chr.charValue()}, "ISO-8859-1"));
		
		System.out.println(new String(new byte[] { -4}, "UTF-8"));
		System.out.println(new String(new byte[] { -4}, "ISO-8859-1"));

	}

	public final void testEncodeHuhner() throws Exception {

		String Huhner = "Huhner";
		byte[] huhnerBytes = Huhner.getBytes();

		assertNotNull(huhnerBytes);

	}

	public final void testSentryEncoding() throws Exception {

		UserLookup userLookup = new UserLookup();
		userLookup.setSsosUrl("https://websignon.warwick.ac.uk/origin/sentry");
		User user = userLookup.getUserByUserId("gsufcd");

		assertNotNull(user);

		assertEquals("H"+U_UMLAUT+"hner", user.getLastName());

	}

	public final void testSentryEncoding2() throws Exception {

		UserLookup userLookup = new UserLookup();
		userLookup.setSsosUrl("http://websignon.warwick.ac.uk/sentry");
		User user = userLookup.getUserByUserId("gsufcd");

		assertNotNull(user);

		assertEquals("H"+U_UMLAUT+"hner", user.getLastName());

	}
	
	public final void testSentryEncoding3() throws Exception {

		UserLookup userLookup = new UserLookup();
		userLookup.setSsosUrl("http://websignon-test.warwick.ac.uk/sentry");
		User user = userLookup.getUserByUserId("gsufcd");

		assertNotNull(user);

		assertEquals("H"+U_UMLAUT+"hner", user.getLastName());

	}

}
