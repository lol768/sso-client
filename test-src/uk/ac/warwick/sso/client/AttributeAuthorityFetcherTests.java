/*
 * Created on 30-Jun-2005
 *
 */
package uk.ac.warwick.sso.client;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.opensaml.SAMLAssertion;
import org.opensaml.SAMLAttribute;
import org.opensaml.SAMLAttributeStatement;
import org.opensaml.SAMLNameIdentifier;
import org.opensaml.SAMLResponse;
import org.opensaml.SAMLSubject;

import uk.ac.warwick.userlookup.User;

public class AttributeAuthorityFetcherTests extends TestCase {

	public final void testFetcher() throws Exception {

		MockAttributeAuthorityResponseFetcher2 fetcher = new MockAttributeAuthorityResponseFetcher2();

		fetcher.setSAMLResponse(generateMockResponse());

		User user = fetcher.getUserFromSubject(generateMockSubject());
		
		assertNotNull(user);
		
		assertTrue(user.isStaff());
		
		assertEquals(user.getExtraProperty("givenName"),"Kieran");

	}

	private SAMLSubject generateMockSubject() throws Exception {

		SAMLSubject subject = new SAMLSubject();
		SAMLNameIdentifier nameId = new SAMLNameIdentifier("12345", "origin", "tickettype");
		subject.setName(nameId);
		return subject;

	}

	private SAMLResponse generateMockResponse() throws Exception {
		SAMLResponse resp = new SAMLResponse();
		SAMLAssertion assertion = new SAMLAssertion();

		SAMLAttributeStatement statement = new SAMLAttributeStatement();

		statement.setSubject(generateMockSubject());
		List values = new ArrayList();
		values.add("SSC_VALUE");
		SAMLAttribute attr = new SAMLAttribute("urn:websignon:ssc", "urn:ac:uk:warwick:websignon", null, 0, values);
		statement.addAttribute(attr);
		
		values = new ArrayList();
		values.add("Kieran");
		attr = new SAMLAttribute("givenName", "urn:ac:uk:warwick:websignon", null, 0, values);
		statement.addAttribute(attr);
		
		values = new ArrayList();
		values.add("Shaw");
		attr = new SAMLAttribute("sn", "urn:ac:uk:warwick:websignon", null, 0, values);
		statement.addAttribute(attr);
		
		values = new ArrayList();
		values.add("true");
		attr = new SAMLAttribute("staff", "urn:ac:uk:warwick:websignon", null, 0, values);
		statement.addAttribute(attr);
		
		values = new ArrayList();
		values.add("WarwickNDS");
		attr = new SAMLAttribute("urn:websignon:usersource", "urn:ac:uk:warwick:websignon", null, 0, values);
		statement.addAttribute(attr);
		
		
		assertion.addStatement(statement);
		resp.addAssertion(assertion);

		return resp;
	}

}
