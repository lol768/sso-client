package uk.ac.warwick.userlookup.webgroups;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import junit.framework.TestCase;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import uk.ac.warwick.userlookup.Group;
import uk.ac.warwick.userlookup.webgroups.GroupsInfoXMLResponseHandler.GroupsInfoXMLParser;

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
				+ "<user userId=\"memberC\"></user>" + "</users>" + "<department>departmentB</department>" + "</group>"
				+ "</groups>";
		GroupsXMLParser xmlParser = new GroupsXMLParser();
		doParse(xmlToParse, xmlParser);

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

	private void doParse(final String xmlToParse, final GroupsXMLParser xmlParser) throws FactoryConfigurationError,
			ParserConfigurationException, SAXException, IOException {
		SAXParserFactory factory = SAXParserFactory.newInstance();
		SAXParser parser = factory.newSAXParser();
		XMLReader reader = parser.getXMLReader();
		reader.setContentHandler(xmlParser);
		reader.parse(new InputSource(new ByteArrayInputStream(xmlToParse.getBytes())));
	}
}
