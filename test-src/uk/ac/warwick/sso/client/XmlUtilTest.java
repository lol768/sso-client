package uk.ac.warwick.sso.client;

import javax.xml.parsers.DocumentBuilder;

import uk.ac.warwick.sso.client.util.Xml;

import junit.framework.TestCase;

public class XmlUtilTest extends TestCase {
	public void testCreateDocumentBuilder() throws Exception {
		DocumentBuilder builder = Xml.newDocumentBuilder();
	}
}
