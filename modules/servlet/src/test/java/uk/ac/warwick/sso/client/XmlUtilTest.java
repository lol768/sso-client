package uk.ac.warwick.sso.client;

import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;

import uk.ac.warwick.sso.client.util.Xml;

import junit.framework.TestCase;

public class XmlUtilTest extends TestCase {
	public void testCreateDocumentBuilder() throws Exception {
		DocumentBuilder builder = Xml.newDocumentBuilder();
	}
	
	public void testParseDtd() throws Exception {
		DocumentBuilder builder = Xml.newDocumentBuilder();
		
		InputStream resource = getClass().getClassLoader().getResourceAsStream("resources/html-with-dtd.html");
		builder.parse(resource);
	}
}
