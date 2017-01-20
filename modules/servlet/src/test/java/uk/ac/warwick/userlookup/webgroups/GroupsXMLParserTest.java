package uk.ac.warwick.userlookup.webgroups;

import junit.framework.TestCase;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.AttributesImpl;
import uk.ac.warwick.userlookup.Group;
import uk.ac.warwick.userlookup.webgroups.GroupsInfoXMLResponseHandler.GroupsInfoXMLParser;

import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class GroupsXMLParserTest extends TestCase {

	public GroupsXMLParserTest() {
		super();
	}

	public void testEmptyResult() throws Exception {
		String xmlToParse = "<groups/>";
		GroupsXMLParser xmlParser = new GroupsXMLParser();
		doParse(xmlToParse, xmlParser);

		Collection groups = xmlParser.getGroups();
		assertTrue("should be empty", groups.isEmpty());
	}
	
	public void testSso1003() throws Exception {
		GroupsXMLParser xmlParser = new GroupsXMLParser();
		
		xmlParser.startDocument();
		xmlParser.startElement("", "groups", "groups", null);
		AttributesImpl groupAtts = new AttributesImpl();
		groupAtts.addAttribute("", "name", "name", "", "in-amazing");
		xmlParser.startElement("", "group", "group", groupAtts);
		xmlParser.startElement("", "lastupdateddate", "lastupdateddate", null);
		
		String chars = "2008-10-12 06:29";
		String chars2 = ":36";
		
		xmlParser.characters(chars.toCharArray(), 0, chars.length());
		xmlParser.characters(chars2.toCharArray(), 0, chars2.length());
		xmlParser.endElement("", "lastupdateddate", "lastupdateddate");
		xmlParser.endElement("", "group", "group");
		xmlParser.endElement("", "groups", "groups");
		
		List<Group> list = new ArrayList<Group>(xmlParser.getGroups());
		Group group = list.get(0);
		assertEquals("in-amazing", group.getName());
		assertEquals(2008 - 1900, group.getLastUpdatedDate().getYear());
	}
	
	

	public void testFullGroups() throws Exception {
		String firstTitle = "the title";
		String firstName = "the name";
		String secondTitle = "the second title";
		String secondName = "the second name";
		String xmlToParse = "<groups>" + "<group name=\"" + firstName + "\">" + "<title>" + firstTitle + "</title>" + "<owners>"
				+ "<owner userId=\"ownerA\"></owner>" + "<owner userId=\"ownerB\"></owner>" + "</owners>" + "<users>"
				+ "<user userId=\"memberA\"></user>" + "<user userId=\"memberB\"></user>" + "</users>"
				+ "<department>departmentA</department>" + "</group>" + "<group name=\"" + secondName + "\">" + "<title>"
				+ secondTitle + "</title>" + "<owners>" + "<owner userId=\"ownerC\"></owner>" + "</owners>" + "<users>"
				+ "<user userId=\"memberC\"></user>" + "</users>" + "<department>departmentB</department>" + "<lastupdateddate>2010-10-12 06:29:42</lastupdateddate></group>"
				+ "</groups>";
		GroupsXMLParser xmlParser = new GroupsXMLParser();
		doParse(new ByteArrayInputStream(xmlToParse.getBytes()), xmlParser);

		List groups = new ArrayList(xmlParser.getGroups());
		assertEquals("number of groups", 2, groups.size());
		Group groupA = (Group) groups.get(0);
		Group groupB = (Group) groups.get(1);
		assertEquals("first group name", firstName, groupA.getName());
		assertEquals("first group title", firstTitle, groupA.getTitle());
		assertEquals("groupA owners", 2, groupA.getOwners().size());
		assertTrue("groupA, ownerA", groupA.getOwners().contains("ownerA"));
		assertTrue("groupA, ownerB", groupA.getOwners().contains("ownerB"));
		assertEquals("groupA members", 2, groupA.getUserCodes().size());
		assertTrue("groupA, memberA", groupA.getUserCodes().contains("memberA"));
		assertTrue("groupA, memberB", groupA.getUserCodes().contains("memberB"));
		assertEquals("groupA, department", "departmentA", groupA.getDepartment());

		assertEquals("second group name", secondName, groupB.getName());
		assertEquals("second group title", secondTitle, groupB.getTitle());
		assertEquals("groupB owners", 1, groupB.getOwners().size());
		assertTrue("groupB, ownerC", groupB.getOwners().contains("ownerC"));
		assertEquals("groupB members", 1, groupB.getUserCodes().size());
		assertTrue("groupB, memberC", groupB.getUserCodes().contains("memberC"));
		assertEquals("groupB, department", "departmentB", groupB.getDepartment());
		assertEquals(29, groupB.getLastUpdatedDate().getMinutes());

	}
	
	public void testGroupInfo() throws Exception {
		GroupsInfoXMLParser xmlParser = new GroupsInfoXMLParser();
		String xml = "<info>\n" +
				"  <members>713</members>\n" +
				"</info>\n";
		SAXParserFactory factory = SAXParserFactory.newInstance();
		SAXParser parser = factory.newSAXParser();
		XMLReader reader = parser.getXMLReader();
		reader.setContentHandler(xmlParser);
		reader.parse(new InputSource(new ByteArrayInputStream(xml.getBytes())));
		
		GroupInfo info = xmlParser.getGroupInfo();
		assertEquals(713, info.getSize());
	}

	public void testXml() throws Exception {
		
		String title = "EEE team 4 students/o5/06";

		String xml = "<groups><group name=\"es-eee team 4\"><title>" + title + "</title> " +
						"</group></groups>";

		GroupsXMLParser xmlParser = new GroupsXMLParser();
		doParse(xml, xmlParser);

		List groups = new ArrayList(xmlParser.getGroups());
		
		Group group = (Group) groups.get(0);
		
		assertEquals(title,group.getTitle());
		
	}

	public void testRestricted() throws Exception {
		String xml = "<groups><group name=\"in-restricted\"><title>Test restricted group</title><restricted>true</restricted></group></groups>";

		GroupsXMLParser xmlParser = new GroupsXMLParser();
		doParse(xml, xmlParser);

		ArrayList<Group> groups = new ArrayList<>(xmlParser.getGroups());

		assertTrue(groups.get(0).isRestricted());
	}

	public void testNotRestricted() throws Exception {
		String xml = "<groups><group name=\"un-restricted\"><title>Test restricted group</title><restricted>false</restricted></group></groups>";

		GroupsXMLParser xmlParser = new GroupsXMLParser();
		doParse(xml, xmlParser);

		ArrayList<Group> groups = new ArrayList<>(xmlParser.getGroups());

		assertFalse(groups.get(0).isRestricted());
	}

	private void doParse(final String xmlToParse, final GroupsXMLParser xmlParser) throws FactoryConfigurationError,
			ParserConfigurationException, SAXException, IOException {
		doParse(new ByteArrayInputStream(xmlToParse.getBytes()), xmlParser);
	}
	
	private void doParse(final InputStream is, final GroupsXMLParser xmlParser) throws FactoryConfigurationError,
			ParserConfigurationException, SAXException, IOException {
		SAXParserFactory factory = SAXParserFactory.newInstance();
		factory.setFeature("http://xml.org/sax/features/validation", false);
		SAXParser parser = factory.newSAXParser();
		XMLReader reader = parser.getXMLReader();
		reader.setContentHandler(xmlParser);
		reader.parse(new InputSource(is));
	}
}
